package com.loong.longtv.ui.shared.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loong.longtv.data.favorite.FavoriteRepository
import com.loong.longtv.data.history.WatchHistoryRepository
import com.loong.longtv.data.room.entity.FavoriteEntity
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
    val playableMedia: PlayableMedia? = null,
    val resumePositionMs: Long = 0,
    val favorite: Boolean = false,
)

private enum class PlayRequestMode {
    KeepSelected,
    AutoFallback
}

@HiltViewModel
class VideoDetailViewModel @Inject constructor(
    private val engine: TvBoxEngine,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val favoriteRepository: FavoriteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoDetailUiState(contentLoading = true))
    val uiState: StateFlow<VideoDetailUiState> = _uiState.asStateFlow()
    private var playRequestVersion = 0

    fun loadDetail(siteKey: String, id: String) {
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
                    playableMedia = null
                )
            }

            val result = engine.sourceRepository.getDetail(site, id)
            if (result.isSuccess) {
                val detail = result.getOrThrow()
                val history = watchHistoryRepository.find(siteKey, id)
                val lines = detail?.getPlayLines().orEmpty()
                val historySourceIndex = lines
                    .indexOfFirst { it.flag == history?.sourceName }
                    .takeIf { it >= 0 }
                val selectedSourceIndex = historySourceIndex ?: lines.indexOfFirstPlayable().coerceAtLeast(0)
                val selectedEpisodeIndex = lines.getOrNull(selectedSourceIndex)?.episodes
                    ?.indexOfFirst { it.url == history?.episodeUrl }
                    ?.takeIf { it >= 0 } ?: 0

                _uiState.update {
                    it.copy(
                        contentLoading = false,
                        detail = detail,
                        selectedSourceIndex = selectedSourceIndex,
                        selectedEpisodeIndex = selectedEpisodeIndex,
                        playableMedia = null,
                        resumePositionMs = history?.positionMs ?: 0,
                        favorite = favoriteRepository.find(siteKey, id) != null,
                        error = null
                    )
                }
                val playLine = lines.getOrNull(selectedSourceIndex)
                if (playLine != null && playLine.episodes.isNotEmpty()) {
                    playEpisode(playLine, selectedEpisodeIndex, PlayRequestMode.AutoFallback)
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
        viewModelScope.launch {
            val state = _uiState.value
            val detail = state.detail ?: return@launch
            val lines = detail.getPlayLines()
            val history = watchHistoryRepository.find(state.siteKey, detail.id)
            val source = lines.getOrNull(index) ?: return@launch
            val episodeIndex = state.selectedEpisodeIndex
                .takeIf { it in source.episodes.indices }
                ?: source.episodes
                    .indexOfFirst { it.url == history?.episodeUrl }
                    .takeIf { it >= 0 }
                ?: 0
            val resumePosition = if (history?.sourceName == source.flag) history?.positionMs ?: 0 else 0
            _uiState.update {
                it.copy(
                    selectedSourceIndex = index,
                    selectedEpisodeIndex = episodeIndex,
                    playableMedia = null,
                    resumePositionMs = resumePosition,
                    error = null
                )
            }
            if (source.episodes.isNotEmpty()) {
                playEpisode(source, episodeIndex, PlayRequestMode.KeepSelected)
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

    private fun playEpisode(playLine: PlayLine, episodeIndex: Int, mode: PlayRequestMode) {
        val state = _uiState.value
        val site = engine.currentConfig?.sites?.firstOrNull { it.key == state.siteKey } ?: return
        val detail = state.detail ?: return
        val lines = detail.getPlayLines()
        val requestedSourceIndex = lines.indexOfFirst { it.flag == playLine.flag }
            .takeIf { it >= 0 } ?: state.selectedSourceIndex
        val episode = playLine.episodes.getOrNull(episodeIndex) ?: return
        val requestVersion = ++playRequestVersion

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    playerLoading = true,
                    selectedSourceIndex = requestedSourceIndex,
                    selectedEpisodeIndex = episodeIndex,
                    playableMedia = null,
                    error = null
                )
            }

            val candidates = if (mode.usesFallbackCandidates()) {
                lines.playCandidates(requestedSourceIndex, episodeIndex)
            } else {
                listOf(PlayCandidate(requestedSourceIndex, playLine, episodeIndex))
            }

            val finalAttempt = resolvePlayableCandidate(site, candidates, mode)
            if (requestVersion != playRequestVersion) return@launch

            val readyCandidate = finalAttempt.readyCandidate
            val readyMedia = finalAttempt.readyMedia
            if (readyCandidate != null && readyMedia != null) {
                applyPlayableCandidate(readyCandidate, readyMedia)
                return@launch
            }

            val resolvedCandidate = finalAttempt.firstResolvedCandidate
            val resolvedMedia = finalAttempt.firstResolvedMedia
            if (resolvedCandidate != null && resolvedMedia != null) {
                applyPlayableCandidate(resolvedCandidate, resolvedMedia)
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

    fun saveProgress(positionMs: Long, durationMs: Long, updateResumeState: Boolean = false) {
        val state = _uiState.value
        val detail = state.detail ?: return
        val lines = detail.getPlayLines()
        val playLine = lines.getOrNull(state.selectedSourceIndex) ?: return
        val episode = playLine.episodes.getOrNull(state.selectedEpisodeIndex) ?: return
        viewModelScope.launch {
            watchHistoryRepository.save(
                WatchHistoryEntity(
                    historyKey = "${state.siteKey}_${detail.id}",
                    siteKey = state.siteKey,
                    videoId = detail.id,
                    title = detail.name,
                    cover = detail.pic,
                    sourceName = playLine.flag,
                    episodeName = episode.name,
                    episodeUrl = episode.url,
                    positionMs = positionMs,
                    durationMs = durationMs,
                )
            )
            if (updateResumeState) {
                _uiState.update { it.copy(resumePositionMs = positionMs) }
            }
        }
    }

    fun applyFullscreenProgress(positionMs: Long) {
        if (positionMs < 0) return
        saveProgress(positionMs, 0, updateResumeState = true)
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

    private suspend fun resolveResumePosition(playLine: PlayLine, episode: Episode): Long {
        val state = _uiState.value
        val history = watchHistoryRepository.find(state.siteKey, state.detail?.id.orEmpty())
        return if (history?.sourceName == playLine.flag && history.episodeUrl == episode.url) {
            history.positionMs
        } else {
            0
        }
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

    private suspend fun applyPlayableCandidate(candidate: PlayCandidate, playableMedia: PlayableMedia) {
        val episode = candidate.playLine.episodes.getOrNull(candidate.episodeIndex)
        _uiState.update {
            it.copy(
                playerLoading = false,
                selectedSourceIndex = candidate.sourceIndex,
                selectedEpisodeIndex = candidate.episodeIndex,
                playableMedia = playableMedia,
                resumePositionMs = if (episode != null) resolveResumePosition(candidate.playLine, episode) else 0,
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
