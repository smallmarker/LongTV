package com.loong.longtv.ui.shared.child

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.loong.longtv.data.SourcePreference
import com.loong.longtv.di.DefaultSources
import com.loong.longtv.engine.TvBoxEngine
import com.loong.longtv.engine.source.model.Category
import com.loong.longtv.engine.source.model.HomeContent
import com.loong.longtv.engine.source.model.SiteConfig
import com.loong.longtv.engine.source.model.VodInfo
import com.loong.longtv.ui.shared.popular.PopularPagingSource
import com.loong.longtv.ui.shared.popular.PopularUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChildEducationViewModel @Inject constructor(
    private val engine: TvBoxEngine,
    private val sourcePreference: SourcePreference,
) : ViewModel() {
    private val pagerCache = mutableMapOf<String, Flow<PagingData<VodInfo>>>()
    private val _uiState = MutableStateFlow(PopularUiState())
    val uiState: StateFlow<PopularUiState> = _uiState.asStateFlow()

    init {
        initEngine()
    }

    fun refresh() {
        val site = _uiState.value.currentSite ?: return initEngine()
        loadSite(site)
    }

    fun selectTab(index: Int) {
        val state = _uiState.value
        if (index !in state.tabs.indices) return
        val site = state.currentSite ?: return
        val tab = state.tabs[index]
        if (index == state.selectedTabIndex) {
            toggleFilters(tab.id)
        } else {
            showCachedTabOrCreate(site, tab, index)
        }
    }

    fun focusTab(index: Int) {
        val state = _uiState.value
        if (index !in state.tabs.indices || index == state.selectedTabIndex) return
        val site = state.currentSite ?: return
        showCachedTabOrCreate(site, state.tabs[index], index)
    }

    fun selectFilter(key: String, value: String) {
        val state = _uiState.value
        val site = state.currentSite ?: return
        val tab = state.tabs.getOrNull(state.selectedTabIndex) ?: return
        val nextFilters = state.selectedFilters.toMutableMap().apply {
            if (value.isBlank()) remove(key) else put(key, value)
        }
        val flow = createPagerFlow(site, tab, nextFilters)
        pagerCache[tab.cacheKey(site, nextFilters)] = flow
        _uiState.update {
            it.copy(
                selectedFilters = nextFilters,
                videoPagingFlow = flow,
                errorMessage = null
            )
        }
    }

    private fun initEngine() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, errorMessage = null) }
            val source = sourcePreference.load() ?: DefaultSources.default
            val result = engine.start(source)
            if (result.isSuccess) {
                val site = result.getOrThrow().sites
                    .firstOrNull { it.isSpiderSite() && it.key == CHILD_EDUCATION_SITE_KEY }
                if (site == null) {
                    _uiState.update {
                        it.copy(loading = false, errorMessage = "没有可用少儿教育源")
                    }
                } else {
                    loadSite(site)
                }
            } else {
                _uiState.update {
                    it.copy(
                        loading = false,
                        errorMessage = "加载少儿教育源失败: ${result.exceptionOrNull()?.message.orEmpty()}"
                    )
                }
            }
        }
    }

    private fun loadSite(site: SiteConfig) {
        viewModelScope.launch {
            pagerCache.clear()
            _uiState.update {
                it.copy(
                    currentSite = site,
                    loading = true,
                    videoPagingFlow = emptyFlow(),
                    filterPanelVisible = false,
                    selectedFilters = emptyMap(),
                    filters = emptyMap(),
                    errorMessage = null
                )
            }

            val result = engine.sourceRepository.getHomeContent(site)
            if (result.isSuccess) {
                val homeContent = result.getOrThrow()
                val tabs = buildTabs(homeContent)
                _uiState.update {
                    it.copy(
                        loading = false,
                        tabs = tabs,
                        selectedTabIndex = 0,
                        filters = homeContent.filters,
                        filterPanelVisible = false,
                        selectedFilters = emptyMap(),
                        errorMessage = null
                    )
                }
                tabs.firstOrNull()?.let { tab -> showCachedTabOrCreate(site, tab, 0) }
            } else {
                _uiState.update {
                    it.copy(
                        loading = false,
                        tabs = emptyList(),
                        selectedTabIndex = 0,
                        videoPagingFlow = emptyFlow(),
                        filterPanelVisible = false,
                        selectedFilters = emptyMap(),
                        filters = emptyMap(),
                        errorMessage = result.exceptionOrNull()?.message ?: "加载少儿专区失败"
                    )
                }
            }
        }
    }

    private fun showCachedTabOrCreate(site: SiteConfig, tab: Category, index: Int) {
        val flow = pagerCache.getOrPut(tab.cacheKey(site, emptyMap())) {
            createPagerFlow(site, tab, emptyMap())
        }
        _uiState.update {
            it.copy(
                selectedTabIndex = index,
                videoPagingFlow = flow,
                filterPanelVisible = false,
                selectedFilters = emptyMap(),
                errorMessage = null
            )
        }
    }

    private fun toggleFilters(tabId: String) {
        val hasFilters = _uiState.value.filters[tabId].orEmpty().isNotEmpty()
        _uiState.update {
            it.copy(filterPanelVisible = if (hasFilters) !it.filterPanelVisible else false)
        }
    }

    private fun createPagerFlow(
        site: SiteConfig,
        tab: Category,
        filters: Map<String, String>
    ): Flow<PagingData<VodInfo>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                initialLoadSize = 20,
                prefetchDistance = 4,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                PopularPagingSource(
                    site = site,
                    tab = tab,
                    filters = filters,
                    engine = engine,
                    homeTabId = HOME_TAB_ID,
                    useHomeVideoContent = true
                )
            }
        ).flow.cachedIn(viewModelScope)
    }

    private fun buildTabs(homeContent: HomeContent?): List<Category> {
        return buildList {
            add(Category(HOME_TAB_ID, "首页"))
            addAll(homeContent?.categories.orEmpty())
        }.distinctBy { it.id }
    }

    private fun Category.cacheKey(site: SiteConfig, filters: Map<String, String>): String {
        val filterKey = filters.toSortedMap().entries.joinToString("&") { "${it.key}=${it.value}" }
        return "${site.key}:$id:$filterKey"
    }

    companion object {
        private const val HOME_TAB_ID = "__home__"
        private const val CHILD_EDUCATION_SITE_KEY = "少儿教育"
    }
}
