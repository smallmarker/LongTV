package com.loong.longtv.ui.shared.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loong.longtv.data.EpisodeSkipPreference
import com.loong.longtv.data.EpisodeSkipSettings
import com.loong.longtv.data.favorite.FavoriteRepository
import com.loong.longtv.data.history.WatchHistoryRepository
import com.loong.longtv.data.room.entity.FavoriteEntity
import com.loong.longtv.data.room.entity.EpisodeProgressEntity
import com.loong.longtv.data.room.entity.WatchHistoryEntity
import com.loong.longtv.engine.TvBoxEngine
import com.loong.longtv.engine.player.PlayableMedia
import com.loong.longtv.engine.source.model.Episode
import com.loong.longtv.engine.source.model.PlayLine
import com.loong.longtv.engine.source.model.VodInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoDetailUiState(
    val contentLoading: Boolean = false,
    val playerLoading: Boolean = false,
    val error: String? = null,
    val siteKey: String = "",
    val siteName: String = "",
    val detail: VodInfo? = null,
    val selectedSourceIndex: Int = 0,
    val selectedEpisodeIndex: Int = 0,
    val playbackSessionId: Int = 0,
    val playableMedia: PlayableMedia? = null,
    val resumePositionMs: Long = 0,
    val favorite: Boolean = false,
    val episodeSkipSettings: EpisodeSkipSettings = EpisodeSkipSettings(),
)

private enum class PlayRequestMode {
    KeepSelected,
    AutoFallback
}

private val episodeNumberPattern = Regex("""(?:第\s*|ep\.?\s*)?([0-9]+)\s*[集话話]?""", RegexOption.IGNORE_CASE)

internal fun Episode.recoveryLabel(): String? {
    val label = name.trim()
    val number = episodeNumberPattern.matchEntire(label)?.groupValues?.get(1)?.toIntOrNull()
    return number?.let { "#$it" }
        ?: label.lowercase().takeIf { it.isNotBlank() && it != "播放" && it != "立即播放" }
}

@HiltViewModel
class VideoDetailViewModel @Inject constructor(
    private val engine: TvBoxEngine,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val favoriteRepository: FavoriteRepository,
    private val episodeSkipPreference: EpisodeSkipPreference,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoDetailUiState(contentLoading = true))
    val uiState: StateFlow<VideoDetailUiState> = _uiState.asStateFlow()
    private var playRequestVersion = 0
    private var detailRequestKey: Pair<String, String>? = null

    internal fun loadDetail(siteKey: String, id: String, searchResume: PlaybackSearchResume? = null) {
        val requestKey = siteKey to id
        val current = _uiState.value
        if (detailRequestKey == requestKey && (current.contentLoading || current.detail != null)) return
        detailRequestKey = requestKey
        ++playRequestVersion
        val site = engine.currentConfig?.sites?.firstOrNull { it.key == siteKey }
        if (site == null) {
            _uiState.value = VideoDetailUiState(error = "站点不存在: $siteKey")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    contentLoading = true,
                    playerLoading = false,
                    error = null,
                    siteKey = siteKey,
                    siteName = site.name,
                    playableMedia = null,
                    detail = null
                )
            }

            val result = engine.sourceRepository.getDetail(site, id)
            if (detailRequestKey != requestKey) return@launch
            if (result.isSuccess) {
                val detail = result.getOrThrow()
                val history = watchHistoryRepository.find(siteKey, id)
                val favorite = favoriteRepository.find(siteKey, id) != null
                if (detailRequestKey != requestKey) return@launch
                val lines = detail?.getPlayLines().orEmpty()
                val resumeFromSearch = searchResume?.takeIf { detail != null && it.matches(detail) }
                val resumeCandidates = resumeFromSearch?.let { hint ->
                    lines.mapIndexedNotNull { index, line ->
                        hint.episodeIndex(line)?.let { PlayCandidate(index, line, it) }
                    }
                }.orEmpty()
                val resumeCandidate = resumeCandidates.firstOrNull()
                val historySourceIndex = lines
                    .indexOfFirst { it.flag == history?.sourceName }
                    .takeIf { it >= 0 }
                val selectedSourceIndex = resumeCandidate?.sourceIndex ?: historySourceIndex
                    ?: lines.indexOfFirstPlayable().coerceAtLeast(0)
                val playLine = lines.getOrNull(selectedSourceIndex)
                val selectedEpisodeIndex = resumeCandidate?.episodeIndex ?: if (playLine != null && history?.sourceName == playLine.flag) {
                    history.episodeIndex.takeIf { playLine.episodes.getOrNull(it)?.name == history.episodeName }
                        ?: playLine.episodes.indices.singleOrNull { playLine.episodes[it].name == history.episodeName }
                        ?: playLine.episodes.indices.singleOrNull { playLine.episodes[it].url == history.episodeUrl }
                        ?: 0
                } else {
                    0
                }

                _uiState.update {
                    it.copy(
                        contentLoading = false,
                        detail = detail,
                        selectedSourceIndex = selectedSourceIndex,
                        selectedEpisodeIndex = selectedEpisodeIndex,
                        playableMedia = null,
                        resumePositionMs = 0,
                        favorite = favorite,
                        episodeSkipSettings = episodeSkipPreference.load(siteKey, detail?.id ?: id),
                        error = null
                    )
                }
                if (playLine != null && playLine.episodes.isNotEmpty()) {
                    playEpisode(
                        playLine, selectedEpisodeIndex, PlayRequestMode.AutoFallback,
                        matchedCandidates = resumeCandidates.takeIf { it.isNotEmpty() },
                        resumeOverrideMs = resumeFromSearch?.positionMs?.takeIf { resumeCandidate != null }
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        contentLoading = false,
                        error = result.exceptionOrNull()?.message ?: "加载详情失败"
                    )
                }
            }
        }
    }

    fun selectSource(index: Int) {
        val state = _uiState.value
        val source = state.detail?.getPlayLines()?.getOrNull(index) ?: return
        val episodeIndex = state.selectedEpisodeIndex.takeIf { it in source.episodes.indices } ?: 0
        if (source.episodes.isNotEmpty()) {
            playEpisode(source, episodeIndex, PlayRequestMode.KeepSelected)
        } else {
            ++playRequestVersion
            _uiState.update {
                it.copy(
                    selectedSourceIndex = index,
                    selectedEpisodeIndex = episodeIndex,
                    playableMedia = null,
                    playerLoading = false,
                    resumePositionMs = 0,
                    error = null
                )
            }
        }
    }

    fun retryCurrentEpisode() {
        val state = _uiState.value
        val detail = state.detail ?: return
        val lines = detail.getPlayLines()
        val playLine = lines.getOrNull(state.selectedSourceIndex) ?: return
        playEpisode(playLine, state.selectedEpisodeIndex, PlayRequestMode.AutoFallback)
    }

    fun playEpisode(playLine: PlayLine, episodeIndex: Int) {
        playEpisode(playLine, episodeIndex, PlayRequestMode.KeepSelected)
    }

    fun saveEpisodeSkipSettings(settings: EpisodeSkipSettings) {
        val state = _uiState.value
        val detail = state.detail ?: return
        episodeSkipPreference.save(state.siteKey, detail.id, settings)
        _uiState.update { it.copy(episodeSkipSettings = settings) }
    }

    fun recoverFullscreenPlayback(positionMs: Long, switchLine: Boolean) {
        val state = _uiState.value
        if (state.playerLoading) return
        val detail = state.detail ?: return
        val site = engine.currentConfig?.sites?.firstOrNull { it.key == state.siteKey } ?: return
        val lines = detail.getPlayLines()
        val source = lines.getOrNull(state.selectedSourceIndex) ?: return
        val episode = source.episodes.getOrNull(state.selectedEpisodeIndex) ?: return
        val episodeLabel = episode.recoveryLabel()
        val candidates = if (switchLine) {
            val alternateOffsets = if (source.uniqueEpisodeIndex(episodeLabel) == state.selectedEpisodeIndex) {
                1 until lines.size
            } else {
                IntRange.EMPTY
            }
            alternateOffsets.mapNotNull { offset ->
                val sourceIndex = (state.selectedSourceIndex + offset) % lines.size
                val line = lines[sourceIndex]
                val episodeIndex = line.uniqueEpisodeIndex(episodeLabel) ?: return@mapNotNull null
                PlayCandidate(sourceIndex, line, episodeIndex)
            }
        } else {
            listOf(PlayCandidate(state.selectedSourceIndex, source, state.selectedEpisodeIndex))
        }

        val resumePosition = positionMs.coerceAtLeast(0)
        val requestVersion = ++playRequestVersion
        _uiState.update {
            it.copy(
                playerLoading = candidates.isNotEmpty(),
                playableMedia = null,
                resumePositionMs = resumePosition,
                playbackSessionId = requestVersion,
                error = if (candidates.isEmpty()) "其他线路没有找到同一集，可以搜同名试试" else null
            )
        }
        if (candidates.isEmpty()) return

        viewModelScope.launch {
            val attempt = resolvePlayableCandidate(site, candidates, PlayRequestMode.AutoFallback)
            if (requestVersion != playRequestVersion) return@launch
            val candidate = attempt.readyCandidate
            val media = attempt.readyMedia
            if (candidate != null && media != null) {
                applyPlayableCandidate(state.siteKey, detail.id, candidate, media, requestVersion, resumePosition)
            } else {
                _uiState.update {
                    it.copy(playerLoading = false, error = if (switchLine) "其他线路暂时无法播放" else "当前线路重试失败")
                }
            }
        }
    }

    private fun playEpisode(
        playLine: PlayLine,
        episodeIndex: Int,
        mode: PlayRequestMode,
        matchedCandidates: List<PlayCandidate>? = null,
        resumeOverrideMs: Long? = null
    ) {
        val state = _uiState.value
        val site = engine.currentConfig?.sites?.firstOrNull { it.key == state.siteKey } ?: return
        val detail = state.detail ?: return
        val lines = detail.getPlayLines()
        val requestedSourceIndex = lines.indexOfFirst { it.flag == playLine.flag }
            .takeIf { it >= 0 } ?: state.selectedSourceIndex
        if (episodeIndex !in playLine.episodes.indices) return
        val requestVersion = ++playRequestVersion

        _uiState.update {
            it.copy(
                playerLoading = true,
                selectedSourceIndex = requestedSourceIndex,
                selectedEpisodeIndex = episodeIndex,
                playbackSessionId = requestVersion,
                playableMedia = null,
                resumePositionMs = 0,
                error = null
            )
        }

        viewModelScope.launch {
            val candidates = matchedCandidates ?: if (mode.usesFallbackCandidates()) {
                lines.playCandidates(requestedSourceIndex, episodeIndex)
            } else {
                listOf(PlayCandidate(requestedSourceIndex, playLine, episodeIndex))
            }

            val finalAttempt = resolvePlayableCandidate(site, candidates, mode)
            if (requestVersion != playRequestVersion) return@launch

            val readyCandidate = finalAttempt.readyCandidate
            val readyMedia = finalAttempt.readyMedia
            if (readyCandidate != null && readyMedia != null) {
                applyPlayableCandidate(state.siteKey, detail.id, readyCandidate, readyMedia, requestVersion, resumeOverrideMs)
                return@launch
            }

            val resolvedCandidate = finalAttempt.firstResolvedCandidate
            val resolvedMedia = finalAttempt.firstResolvedMedia
            if (resolvedCandidate != null && resolvedMedia != null) {
                applyPlayableCandidate(state.siteKey, detail.id, resolvedCandidate, resolvedMedia, requestVersion, resumeOverrideMs)
                return@launch
            }

            _uiState.update {
                it.copy(
                    playerLoading = false,
                    selectedSourceIndex = requestedSourceIndex,
                    selectedEpisodeIndex = episodeIndex,
                    playableMedia = null,
                    error = finalAttempt.lastError ?: "没有找到可用线路，请手动切换线路重试"
                )
            }
        }
    }

    internal fun saveProgress(
        state: VideoDetailUiState,
        playLine: PlayLine?,
        positionMs: Long,
        durationMs: Long
    ) {
        if (state.playableMedia == null || playLine == null || durationMs <= 0 || positionMs < 0) return
        val detail = state.detail ?: return
        val episode = playLine.episodes.getOrNull(state.selectedEpisodeIndex) ?: return
        val active = _uiState.value
        val updateRecent = active.siteKey == state.siteKey && active.detail?.id == detail.id &&
            active.selectedSourceIndex == state.selectedSourceIndex &&
            active.selectedEpisodeIndex == state.selectedEpisodeIndex &&
            active.playbackSessionId == state.playbackSessionId
        val recent = WatchHistoryEntity(
            historyKey = "${state.siteKey}_${detail.id}",
            siteKey = state.siteKey,
            videoId = detail.id,
            title = detail.name,
            cover = detail.pic,
            sourceName = playLine.flag,
            episodeName = episode.name,
            episodeUrl = episode.url,
            positionMs = positionMs.coerceAtMost(durationMs),
            durationMs = durationMs,
            episodeIndex = state.selectedEpisodeIndex
        )
        viewModelScope.launch {
            watchHistoryRepository.saveProgress(
                EpisodeProgressEntity(
                    siteKey = recent.siteKey,
                    videoId = recent.videoId,
                    sourceName = playLine.flag,
                    episodeIndex = state.selectedEpisodeIndex,
                    episodeName = episode.name,
                    episodeUrl = episode.url,
                    positionMs = recent.positionMs,
                    durationMs = durationMs,
                    updateTime = recent.updateTime
                ),
                recent.takeIf { updateRecent }
            )
        }
    }

    fun applyFullscreenProgress(positionMs: Long) {
        if (positionMs < 0) return
        _uiState.update { if (it.playableMedia == null) it else it.copy(resumePositionMs = positionMs) }
    }

    fun toggleFavorite() {
        val state = _uiState.value
        val detail = state.detail ?: return
        viewModelScope.launch {
            val favoriteKey = "${state.siteKey}_${detail.id}"
            if (state.favorite) {
                favoriteRepository.remove(favoriteKey)
                _uiState.update { it.copy(favorite = false) }
            } else {
                favoriteRepository.save(
                    FavoriteEntity(
                        favoriteKey = favoriteKey,
                        siteKey = state.siteKey,
                        videoId = detail.id,
                        title = detail.name,
                        cover = detail.pic,
                    )
                )
                _uiState.update { it.copy(favorite = true) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private data class PlayCandidate(
        val sourceIndex: Int,
        val playLine: PlayLine,
        val episodeIndex: Int
    )

    private data class PlayAttemptResult(
        val readyCandidate: PlayCandidate? = null,
        val readyMedia: PlayableMedia? = null,
        val firstResolvedCandidate: PlayCandidate? = null,
        val firstResolvedMedia: PlayableMedia? = null,
        val lastError: String? = null
    ) {
        fun hasResolvedMedia(): Boolean = readyMedia != null || firstResolvedMedia != null
    }

    private suspend fun resolvePlayableCandidate(
        site: com.loong.longtv.engine.source.model.SiteConfig,
        candidates: List<PlayCandidate>,
        mode: PlayRequestMode
    ): PlayAttemptResult {
        var lastError: String? = null
        var firstResolvedCandidate: PlayCandidate? = null
        var firstResolvedMedia: PlayableMedia? = null

        for (candidate in candidates) {
            val candidateEpisode = candidate.playLine.episodes.getOrNull(candidate.episodeIndex) ?: continue
            val result = engine.sourceRepository.getPlayerInfo(site, candidate.playLine.flag, candidateEpisode.url)
            if (result.isFailure) {
                lastError = result.exceptionOrNull()?.message ?: "获取播放地址失败"
                continue
            }

            val playableMedia = engine.playerResolver.resolve(result.getOrThrow())
            if (playableMedia == null) {
                lastError = "播放地址为空，暂时无法播放"
                continue
            }
            if (firstResolvedMedia == null) {
                firstResolvedCandidate = candidate
                firstResolvedMedia = playableMedia
            }

            if (mode.usesFallbackCandidates() && !playableMedia.canInlinePlay()) {
                lastError = "线路 ${candidate.playLine.flag} 暂不可播，已尝试切换其它线路"
                continue
            }

            return PlayAttemptResult(
                readyCandidate = candidate,
                readyMedia = playableMedia,
                firstResolvedCandidate = firstResolvedCandidate,
                firstResolvedMedia = firstResolvedMedia,
                lastError = lastError
            )
        }

        return PlayAttemptResult(
            firstResolvedCandidate = firstResolvedCandidate,
            firstResolvedMedia = firstResolvedMedia,
            lastError = lastError
        )
    }

    private suspend fun applyPlayableCandidate(
        siteKey: String,
        videoId: String,
        candidate: PlayCandidate,
        playableMedia: PlayableMedia,
        requestVersion: Int,
        resumeOverrideMs: Long? = null
    ) {
        val episodes = candidate.playLine.episodes
        val episode = episodes[candidate.episodeIndex]
        val progress = if (resumeOverrideMs != null) null else watchHistoryRepository.findEpisode(
            siteKey, videoId, candidate.playLine.flag, candidate.episodeIndex, episode.name, episode.url,
            matchByName = episode.name.isNotBlank() && episodes.count { it.name == episode.name } == 1,
            matchByUrl = episode.url.isNotBlank() && episodes.count { it.url == episode.url } == 1
        )
        if (requestVersion != playRequestVersion) return
        _uiState.update {
            it.copy(
                playerLoading = false,
                selectedSourceIndex = candidate.sourceIndex,
                selectedEpisodeIndex = candidate.episodeIndex,
                playableMedia = playableMedia,
                resumePositionMs = resumeOverrideMs ?: progress?.positionMs
                    ?.takeUnless { progress.durationMs > 0 && it >= progress.durationMs } ?: 0L,
                error = null
            )
        }
    }

    private fun List<PlayLine>.indexOfFirstPlayable(): Int {
        return indexOfFirst { line -> line.episodes.any { it.url.isNotBlank() } }
    }

    private fun PlayableMedia.canInlinePlay(): Boolean {
        return url.isNotBlank() && !needsParse()
    }

    private fun PlayRequestMode.usesFallbackCandidates(): Boolean {
        return this == PlayRequestMode.AutoFallback
    }

    private fun List<PlayLine>.playCandidates(
        preferredSourceIndex: Int,
        preferredEpisodeIndex: Int
    ): List<PlayCandidate> {
        if (isEmpty()) return emptyList()
        val sourceIndices = indices
        val sourceOrder = buildList {
            if (preferredSourceIndex in sourceIndices) add(preferredSourceIndex)
            addAll(sourceIndices.filterNot { it == preferredSourceIndex })
        }
        return sourceOrder.mapNotNull { sourceIndex ->
            val line = getOrNull(sourceIndex) ?: return@mapNotNull null
            val episodeIndex = preferredEpisodeIndex.takeIf { it in line.episodes.indices }
                ?: line.episodes.indexOfFirst { it.url.isNotBlank() }.takeIf { it >= 0 }
                ?: return@mapNotNull null
            PlayCandidate(sourceIndex, line, episodeIndex)
        }
    }

}
