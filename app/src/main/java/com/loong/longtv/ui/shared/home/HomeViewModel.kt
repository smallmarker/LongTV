package com.loong.longtv.ui.shared.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.loong.longtv.data.bean.HomeContent
import com.loong.longtv.data.bean.HomeTabItem
import com.loong.longtv.data.bean.Site
import com.loong.longtv.data.bean.Video
import com.loong.longtv.data.repository.ApiResult
import com.loong.longtv.data.repository.SpiderRepository
import com.loong.longtv.data.site.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val spiderRepository: SpiderRepository,
) : ViewModel() {
    private val pagerCache = mutableMapOf<String, Flow<PagingData<Video>>>()

    private val _uiState = MutableStateFlow(
        HomeUiState(
            sites = siteRepository.getEnabledSites()
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val targetSite = _uiState.value.currentSite ?: siteRepository.getDefaultSite()
        loadSite(targetSite, keepSelection = false)
    }

    fun selectSite(siteKey: String) {
        val site = siteRepository.getSite(siteKey) ?: return
        if (_uiState.value.currentSite?.key == site.key) return
        siteRepository.setDefaultSite(site.key)
        loadSite(site, keepSelection = false)
    }

    fun selectTab(index: Int) {
        val state = _uiState.value
        if (index !in state.tabs.indices) return
        val site = state.currentSite ?: return
        val tab = state.tabs[index]
        if (index == state.selectedTabIndex) {
            refreshPager(site, tab, index)
        } else {
            showCachedTabOrCreate(site, tab, index)
        }
    }

    fun focusTab(index: Int) {
        val state = _uiState.value
        if (index !in state.tabs.indices) return
        if (index == state.selectedTabIndex) return
        val site = state.currentSite ?: return
        showCachedTabOrCreate(site, state.tabs[index], index)
    }

    private fun loadSite(site: Site, keepSelection: Boolean) {
        viewModelScope.launch {
            val enabledSites = siteRepository.getEnabledSites()
            pagerCache.clear()
            _uiState.update {
                it.copy(
                    sites = enabledSites,
                    currentSite = site,
                    homeState = ApiResult.Loading,
                    videoPagingFlow = emptyFlow(),
                    errorMessage = null
                )
            }

            when (val result = spiderRepository.loadHomeContent(site)) {
                is ApiResult.Success -> {
                    val tabs = buildTabs(result.data)
                    val selectedTabIndex = if (keepSelection) {
                        _uiState.value.selectedTabIndex.coerceIn(0, tabs.lastIndex.coerceAtLeast(0))
                    } else {
                        0
                    }
                    _uiState.update {
                        it.copy(
                            sites = enabledSites,
                            currentSite = site,
                            homeState = result,
                            tabs = tabs,
                            selectedTabIndex = selectedTabIndex,
                            errorMessage = null
                        )
                    }
                    tabs.getOrNull(selectedTabIndex)?.let { tab ->
                        showCachedTabOrCreate(site, tab, selectedTabIndex)
                    }
                }

                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            sites = enabledSites,
                            currentSite = site,
                            homeState = result,
                            tabs = emptyList(),
                            selectedTabIndex = 0,
                            videoPagingFlow = emptyFlow(),
                            errorMessage = result.message
                        )
                    }
                }

                ApiResult.Loading -> Unit
            }
        }
    }

    private fun showCachedTabOrCreate(site: Site, tab: HomeTabItem, index: Int) {
        val flow = pagerCache.getOrPut(tab.cacheKey(site)) {
            createPagerFlow(site, tab)
        }
        _uiState.update {
            it.copy(
                selectedTabIndex = index,
                videoPagingFlow = flow,
                errorMessage = null
            )
        }
    }

    private fun refreshPager(site: Site, tab: HomeTabItem, index: Int) {
        val flow = createPagerFlow(site, tab)
        pagerCache[tab.cacheKey(site)] = flow
        _uiState.update {
            it.copy(
                selectedTabIndex = index,
                videoPagingFlow = flow,
                errorMessage = null
            )
        }
    }

    private fun createPagerFlow(site: Site, tab: HomeTabItem): Flow<PagingData<Video>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                initialLoadSize = 20,
                prefetchDistance = 4,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                HomePagingSource(
                    site = site,
                    tab = tab,
                    spiderRepository = spiderRepository,
                    homeTabId = HOME_TAB_ID
                )
            }
        ).flow.cachedIn(viewModelScope)
    }

    private fun buildTabs(homeContent: HomeContent?): List<HomeTabItem> {
        return buildList {
            add(HomeTabItem(HOME_TAB_ID, "首页"))
            addAll(homeContent?.tabs.orEmpty())
        }.distinctBy { it.type_id }
    }

    private fun HomeTabItem.cacheKey(site: Site): String = "${site.key}:${type_id}"

    companion object {
        private const val HOME_TAB_ID = "__home__"
    }
}

data class HomeUiState(
    val sites: List<Site> = emptyList(),
    val currentSite: Site? = null,
    val tabs: List<HomeTabItem> = emptyList(),
    val selectedTabIndex: Int = 0,
    val homeState: ApiResult<HomeContent?> = ApiResult.Loading,
    val videoPagingFlow: Flow<PagingData<Video>> = emptyFlow(),
    val errorMessage: String? = null
)
