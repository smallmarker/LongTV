package com.loong.longtv.ui.shared.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loong.longtv.data.SourcePreference
import com.loong.longtv.data.favorite.FavoriteRepository
import com.loong.longtv.data.history.WatchHistoryRepository
import com.loong.longtv.data.room.entity.FavoriteEntity
import com.loong.longtv.data.room.entity.WatchHistoryEntity
import com.loong.longtv.di.DefaultSources
import com.loong.longtv.engine.TvBoxEngine
import com.loong.longtv.engine.source.model.SiteConfig
import com.loong.longtv.engine.source.model.VodInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val engine: TvBoxEngine,
    private val sourcePreference: SourcePreference,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val favoriteRepository: FavoriteRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        initEngine()
        observeLocalLibrary()
        observePopularSitePreference()
    }

    fun refresh() {
        initEngine()
    }

    private fun initEngine() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, errorMessage = null) }
            val source = sourcePreference.load() ?: DefaultSources.default
            val result = engine.start(source)
            if (result.isSuccess) {
                val sites = result.getOrThrow().sites.filter { it.isSpiderSite() }
                val site = sites.selectPopularSite(sourcePreference.loadPopularSiteKey())
                val childEducationSite = sites.firstOrNull { it.key == CHILD_EDUCATION_SITE_KEY }
                _uiState.update { state ->
                    state.copy(
                        loading = false,
                        sourceName = site?.name.orEmpty(),
                        popularVideos = emptyList(),
                        childEducationVideos = emptyList(),
                        errorMessage = null,
                    )
                }
                site?.let { loadPopularVideos(it) }
                childEducationSite?.let { loadChildEducationVideos(it) }
            } else {
                _uiState.update { state ->
                    state.copy(
                        loading = false,
                        popularVideos = emptyList(),
                        childEducationVideos = emptyList(),
                        errorMessage = "加载播放源失败: ${result.exceptionOrNull()?.message.orEmpty()}",
                    )
                }
            }
        }
    }

    private fun loadPopularVideos(site: SiteConfig) {
        viewModelScope.launch {
            val result = engine.sourceRepository.getHomeVideos(site)
            val videos = result.getOrNull()?.list.orEmpty().ifEmpty {
                engine.sourceRepository.getCategoryContent(site, "0")
                    .getOrNull()
                    ?.list
                    .orEmpty()
            }.take(MAX_MEDIA_ITEMS).map { it.also { video -> video.siteKey = site.key } }
            _uiState.update { it.copy(popularVideos = videos) }
        }
    }

    private fun loadChildEducationVideos(site: SiteConfig) {
        viewModelScope.launch {
            val result = engine.sourceRepository.getHomeVideos(site)
            val videos = result.getOrNull()?.list.orEmpty().ifEmpty {
                engine.sourceRepository.getCategoryContent(site, "baby")
                    .getOrNull()
                    ?.list
                    .orEmpty()
            }.take(MAX_MEDIA_ITEMS).map { it.also { video -> video.siteKey = site.key } }
            _uiState.update { it.copy(childEducationVideos = videos) }
        }
    }

    private fun observePopularSitePreference() {
        viewModelScope.launch {
            sourcePreference.popularSiteKey.drop(1).collect { siteKey ->
                val site = engine.currentConfig?.sites
                    ?.filter { it.isSpiderSite() }
                    ?.selectPopularSite(siteKey)
                    ?: return@collect
                _uiState.update { it.copy(popularVideos = emptyList(), sourceName = site.name) }
                loadPopularVideos(site)
            }
        }
    }

    private fun List<SiteConfig>.selectPopularSite(siteKey: String?): SiteConfig? {
        val popularSites = filterNot { it.key == CHILD_EDUCATION_SITE_KEY }
        return popularSites.firstOrNull { it.key == siteKey } ?: popularSites.firstOrNull()
    }

    private fun observeLocalLibrary() {
        viewModelScope.launch {
            combine(
                watchHistoryRepository.recent(),
                favoriteRepository.all()
            ) { histories, favorites ->
                HomeLibraryState(
                    recentWatching = histories.take(MAX_MEDIA_ITEMS).map { it.toHomeMediaItem() },
                    favorites = favorites.take(MAX_MEDIA_ITEMS).map { it.toHomeMediaItem() }
                )
            }.collect { library ->
                _uiState.update {
                    it.copy(
                        recentWatching = library.recentWatching,
                        favorites = library.favorites
                    )
                }
            }
        }
    }

    private fun WatchHistoryEntity.toHomeMediaItem(): HomeMediaItem {
        return HomeMediaItem(
            siteKey = siteKey,
            videoId = videoId,
            title = title,
            cover = cover,
            meta = listOf(episodeName, positionMs.formatPlaybackPosition())
                .filter { it.isNotBlank() }
                .joinToString(" ")
        )
    }

    private fun FavoriteEntity.toHomeMediaItem(): HomeMediaItem {
        return HomeMediaItem(
            siteKey = siteKey,
            videoId = videoId,
            title = title,
            cover = cover,
            meta = "已收藏"
        )
    }

    private fun Long.formatPlaybackPosition(): String {
        if (this <= 0L) return ""
        val totalSeconds = this / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%02d:%02d".format(minutes, seconds)
    }

    companion object {
        private const val MAX_MEDIA_ITEMS = 8
        private const val CHILD_EDUCATION_SITE_KEY = "少儿教育"
    }
}

data class HomeUiState(
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val sourceName: String = "",
    val popularVideos: List<VodInfo> = emptyList(),
    val childEducationVideos: List<VodInfo> = emptyList(),
    val recentWatching: List<HomeMediaItem> = emptyList(),
    val favorites: List<HomeMediaItem> = emptyList(),
)

data class HomeMediaItem(
    val siteKey: String,
    val videoId: String,
    val title: String,
    val cover: String,
    val meta: String,
)

private data class HomeLibraryState(
    val recentWatching: List<HomeMediaItem>,
    val favorites: List<HomeMediaItem>,
)
