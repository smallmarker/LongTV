package com.loong.longtv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loong.longtv.data.bean.CategoryContent
import com.loong.longtv.data.bean.HomeContent
import com.loong.longtv.data.bean.HomeTabItem
import com.loong.longtv.data.bean.Site
import com.loong.longtv.data.site.SiteRepository
import com.loong.longtv.ui.home.repository.ApiResult
import com.loong.longtv.ui.home.repository.SpiderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val spiderRepository: SpiderRepository,
) : ViewModel() {

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
        if (index == state.selectedTabIndex && state.videoState !is ApiResult.Error) return
        val site = state.currentSite ?: return
        loadTab(site, state.tabs[index], index)
    }

    private fun loadSite(site: Site, keepSelection: Boolean) {
        viewModelScope.launch {
            val enabledSites = siteRepository.getEnabledSites()
            _uiState.update {
                it.copy(
                    sites = enabledSites,
                    currentSite = site,
                    homeState = ApiResult.Loading,
                    videoState = ApiResult.Loading,
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
                        loadTab(site, tab, selectedTabIndex)
                    } ?: _uiState.update {
                        it.copy(videoState = ApiResult.Success(emptyCategoryContent()))
                    }
                }

                is ApiResult.Error -> {
                    val fallbackTabs = listOf(HomeTabItem(HOME_TAB_ID, "推荐"))
                    _uiState.update {
                        it.copy(
                            sites = enabledSites,
                            currentSite = site,
                            homeState = result,
                            tabs = fallbackTabs,
                            selectedTabIndex = 0,
                            videoState = ApiResult.Loading,
                            errorMessage = "${result.message}，已回退到推荐页"
                        )
                    }
                    loadTab(site, fallbackTabs.first(), 0)
                }

                ApiResult.Loading -> Unit
            }
        }
    }

    private fun loadTab(site: Site, tab: HomeTabItem, index: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedTabIndex = index,
                    videoState = ApiResult.Loading,
                    errorMessage = null
                )
            }

            val result = if (tab.type_id == HOME_TAB_ID) {
                spiderRepository.loadHomeVideoContent(site)
            } else {
                spiderRepository.loadCategoryContent(site, tab, page = 1)
            }

            _uiState.update {
                it.copy(
                    selectedTabIndex = index,
                    videoState = result,
                    errorMessage = (result as? ApiResult.Error)?.message
                )
            }
        }
    }

    private fun buildTabs(homeContent: HomeContent?): List<HomeTabItem> {
        return buildList {
            add(HomeTabItem(HOME_TAB_ID, "推荐"))
            addAll(homeContent?.tabs.orEmpty())
        }.distinctBy { it.type_id }
    }

    private fun emptyCategoryContent() = CategoryContent(
        code = 1,
        pagecount = 1,
        total = 0,
        page = 1,
        msg = "",
        limit = 0,
        list = emptyList()
    )

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
    val videoState: ApiResult<CategoryContent?> = ApiResult.Loading,
    val errorMessage: String? = null
)
