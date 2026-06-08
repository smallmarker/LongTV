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

@HiltViewModel
class VideoDetailViewModel @Inject constructor(
    private val engine: TvBoxEngine,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val favoriteRepository: FavoriteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoDetailUiState(contentLoading = true))
    val uiState: StateFlow<VideoDetailUiState> = _uiState.asStateFlow()

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
                val selectedSourceIndex = lines
                    .indexOfFirst { it.flag == history?.sourceName }
                    .takeIf { it >= 0 } ?: 0
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
                    playEpisode(playLine, selectedEpisodeIndex)
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
            val episodeIndex = source.episodes
                .indexOfFirst { it.url == history?.episodeUrl }
                .takeIf { it >= 0 } ?: 0
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
        }
    }

    fun retryCurrentEpisode() {
        val state = _uiState.value
        val detail = state.detail ?: return
        val lines = detail.getPlayLines()
        val playLine = lines.getOrNull(state.selectedSourceIndex) ?: return
        playEpisode(playLine, state.selectedEpisodeIndex)
    }

    fun playEpisode(playLine: PlayLine, episodeIndex: Int) {
        val state = _uiState.value
        val site = engine.currentConfig?.sites?.firstOrNull { it.key == state.siteKey } ?: return
        val episode = playLine.episodes.getOrNull(episodeIndex) ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(playerLoading = true, error = null) }
            val result = engine.sourceRepository.getPlayerInfo(site, playLine.flag, episode.url)
            if (result.isSuccess) {
                val playerInfo = result.getOrThrow()
                val playableMedia = engine.playerResolver.resolve(playerInfo)
                if (playableMedia == null) {
                    _uiState.update { it.copy(playerLoading = false, error = "播放地址为空，暂时无法播放") }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        playerLoading = false,
                        selectedEpisodeIndex = episodeIndex,
                        playableMedia = playableMedia,
                        resumePositionMs = resolveResumePosition(playLine, episode),
                        error = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(playerLoading = false, error = result.exceptionOrNull()?.message ?: "获取播放地址失败")
                }
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
}
