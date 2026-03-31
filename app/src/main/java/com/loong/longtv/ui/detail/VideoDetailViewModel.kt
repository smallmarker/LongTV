package com.loong.longtv.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loong.longtv.data.bean.PlayInfo
import com.loong.longtv.data.bean.Video
import com.loong.longtv.data.favorite.FavoriteRepository
import com.loong.longtv.data.history.WatchHistoryRepository
import com.loong.longtv.data.player.PlayableMedia
import com.loong.longtv.data.player.PlayerResolver
import com.loong.longtv.data.room.entity.FavoriteEntity
import com.loong.longtv.data.room.entity.WatchHistoryEntity
import com.loong.longtv.data.site.SiteRepository
import com.loong.longtv.ui.home.repository.ApiResult
import com.loong.longtv.ui.home.repository.SpiderRepository
import com.loong.longtv.ui.settings.SpiderFamily
import com.loong.longtv.ui.settings.spiderFamily
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
    val siteApi: String = "",
    val spiderFamily: SpiderFamily = SpiderFamily.CUSTOM,
    val detail: Video? = null,
    val selectedSourceIndex: Int = 0,
    val selectedEpisodeIndex: Int = 0,
    val playableMedia: PlayableMedia? = null,
    val resumePositionMs: Long = 0,
    val favorite: Boolean = false,
)

@HiltViewModel
class VideoDetailViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val spiderRepository: SpiderRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val favoriteRepository: FavoriteRepository,
    private val playerResolver: PlayerResolver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoDetailUiState(contentLoading = true))
    val uiState: StateFlow<VideoDetailUiState> = _uiState.asStateFlow()

    fun loadDetail(key: String, id: String) {
        viewModelScope.launch {
            val site = siteRepository.getSite(key)
            if (site == null) {
                _uiState.value = VideoDetailUiState(error = "站点不存在")
                return@launch
            }

            _uiState.update {
                it.copy(
                    contentLoading = true,
                    playerLoading = false,
                    error = null,
                    siteKey = key,
                    siteName = site.name,
                    siteApi = site.api,
                    spiderFamily = site.spiderFamily(),
                    playableMedia = null
                )
            }

            when (val result = spiderRepository.detailContent(site, listOf(id))) {
                is ApiResult.Success -> {
                    val detail = result.data
                    val history = watchHistoryRepository.find(key, id)
                    val selectedSourceIndex = detail?.playInfos
                        ?.indexOfFirst { playInfo -> playInfo.flag == history?.sourceName }
                        ?.takeIf { it >= 0 }
                        ?: 0
                    val selectedPlayInfo = detail?.playInfos?.getOrNull(selectedSourceIndex)
                    val selectedEpisodeIndex = selectedPlayInfo?.urlInfos
                        ?.indexOfFirst { episode -> episode.url == history?.episodeUrl }
                        ?.takeIf { it >= 0 }
                        ?: 0

                    _uiState.update {
                        it.copy(
                            contentLoading = false,
                            detail = detail,
                            selectedSourceIndex = selectedSourceIndex,
                            selectedEpisodeIndex = selectedEpisodeIndex,
                            playableMedia = null,
                            resumePositionMs = history?.positionMs ?: 0,
                            favorite = favoriteRepository.find(key, id) != null,
                            error = null
                        )
                    }
                }

                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(contentLoading = false, error = result.message)
                    }
                }

                ApiResult.Loading -> Unit
            }
        }
    }

    fun selectSource(index: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val detail = state.detail
            val source = detail?.playInfos?.getOrNull(index)
            val history = if (detail != null) {
                watchHistoryRepository.find(state.siteKey, detail.vod_id)
            } else {
                null
            }
            val episodeIndex = source?.urlInfos
                ?.indexOfFirst { episode -> episode.url == history?.episodeUrl }
                ?.takeIf { it >= 0 }
                ?: 0
            val resumePosition = if (history?.sourceName == source?.flag) history?.positionMs ?: 0 else 0
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
        val playInfo = detail.playInfos.getOrNull(state.selectedSourceIndex) ?: return
        playEpisode(playInfo, state.selectedEpisodeIndex)
    }

    fun playEpisode(playInfo: PlayInfo, episodeIndex: Int) {
        val site = siteRepository.getSite(_uiState.value.siteKey) ?: return
        val episode = playInfo.urlInfos.getOrNull(episodeIndex) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(playerLoading = true, error = null) }
            when (val result = spiderRepository.playerContent(site, playInfo.flag, episode.url)) {
                is ApiResult.Success -> {
                    val playableMedia = playerResolver.resolve(result.data)
                    if (playableMedia == null) {
                        _uiState.update {
                            it.copy(
                                playerLoading = false,
                                error = "播放地址为空，暂时无法播放"
                            )
                        }
                        return@launch
                    }
                    if (playableMedia.requiresParse) {
                        _uiState.update {
                            it.copy(
                                playerLoading = false,
                                selectedEpisodeIndex = episodeIndex,
                                playableMedia = playableMedia,
                                resumePositionMs = 0,
                                error = "当前线路需要进一步解析，内置解析未命中"
                            )
                        }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            playerLoading = false,
                            selectedEpisodeIndex = episodeIndex,
                            playableMedia = playableMedia,
                            resumePositionMs = currentResumePosition(playInfo, episode.url),
                            error = null
                        )
                    }
                }

                is ApiResult.Error -> {
                    _uiState.update { it.copy(playerLoading = false, error = result.message) }
                }

                ApiResult.Loading -> Unit
            }
        }
    }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        val state = _uiState.value
        val detail = state.detail ?: return
        val playInfo = detail.playInfos.getOrNull(state.selectedSourceIndex) ?: return
        val episode = playInfo.urlInfos.getOrNull(state.selectedEpisodeIndex) ?: return
        viewModelScope.launch {
            watchHistoryRepository.save(
                WatchHistoryEntity(
                    historyKey = "${state.siteKey}_${detail.vod_id}",
                    siteKey = state.siteKey,
                    videoId = detail.vod_id,
                    title = detail.vod_name,
                    cover = detail.vod_pic,
                    sourceName = playInfo.flag,
                    episodeName = episode.name,
                    episodeUrl = episode.url,
                    positionMs = positionMs,
                    durationMs = durationMs,
                )
            )
            _uiState.update { it.copy(resumePositionMs = positionMs) }
        }
    }

    fun applyFullscreenProgress(positionMs: Long) {
        if (positionMs < 0) return
        saveProgress(positionMs, 0)
    }

    fun toggleFavorite() {
        val state = _uiState.value
        val detail = state.detail ?: return
        viewModelScope.launch {
            val favoriteKey = "${state.siteKey}_${detail.vod_id}"
            if (state.favorite) {
                favoriteRepository.remove(favoriteKey)
                _uiState.update { it.copy(favorite = false) }
            } else {
                favoriteRepository.save(
                    FavoriteEntity(
                        favoriteKey = favoriteKey,
                        siteKey = state.siteKey,
                        videoId = detail.vod_id,
                        title = detail.vod_name,
                        cover = detail.vod_pic,
                    )
                )
                _uiState.update { it.copy(favorite = true) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun currentResumePosition(playInfo: PlayInfo, episodeUrl: String): Long {
        val state = _uiState.value
        val history = watchHistoryRepository.find(state.siteKey, state.detail?.vod_id.orEmpty())
        return if (history?.sourceName == playInfo.flag && history.episodeUrl == episodeUrl) {
            history.positionMs
        } else {
            0
        }
    }
}
