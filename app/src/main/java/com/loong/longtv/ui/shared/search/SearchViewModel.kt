package com.loong.longtv.ui.shared.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loong.longtv.data.SourcePreference
import com.loong.longtv.data.room.dao.SearchHistoryDao
import com.loong.longtv.data.room.entity.SearchHistoryEntity
import com.loong.longtv.di.DefaultSources
import com.loong.longtv.engine.TvBoxEngine
import com.loong.longtv.engine.source.model.SiteConfig
import com.loong.longtv.engine.source.model.VodInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class TagGroup(
    val title: String,
    val tags: List<String>
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val dao: SearchHistoryDao,
    private val engine: TvBoxEngine,
    private val sourcePreference: SourcePreference,
) : ViewModel() {
    private val _hotKeywords = MutableStateFlow<List<String>>(emptyList())
    private val _tvSuggestions = MutableStateFlow<List<String>>(emptyList())
    private val _tvSuggestionKeyword = MutableStateFlow("")
    private val _tvSuggestionsLoading = MutableStateFlow(false)
    val tvSuggestions: StateFlow<List<String>> = _tvSuggestions.asStateFlow()
    val tvSuggestionsLoading: StateFlow<Boolean> = _tvSuggestionsLoading.asStateFlow()

    private val _searchState = MutableStateFlow<List<TagGroup>>(emptyList())
    val searchState: StateFlow<List<TagGroup>> = _searchState.asStateFlow()

    private val _searchResult = MutableStateFlow<Map<String, List<VodInfo>>>(emptyMap())
    val searchResult = _searchResult.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading = _searchLoading.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()

    private var lastSearchKeyword = ""

    val history = dao.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            history.collectLatest(::updateSearchState)
        }
        viewModelScope.launch {
            sourcePreference.popularSiteKey.drop(1).collect {
                refreshHotKeywords()
            }
        }
        refreshHotKeywords()
    }

    fun addKeyword(keyword: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            dao.insert(SearchHistoryEntity(keyword = keyword))
        }
    }

    fun clear() {
        viewModelScope.launch { dao.clearAll() }
    }

    fun removeKeyword(keyword: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch { dao.deleteByKeyword(keyword) }
    }

    fun loadTvSuggestions(keyword: String) {
        val trimmed = keyword.trim()
        _tvSuggestionKeyword.value = trimmed
        if (trimmed.isBlank()) {
            _tvSuggestions.value = emptyList()
            _tvSuggestionsLoading.value = false
            return
        }
        viewModelScope.launch {
            delay(180)
            if (_tvSuggestionKeyword.value != trimmed) return@launch
            _tvSuggestionsLoading.value = true
            val suggestions = fetchCurrentSiteSuggestions(trimmed)
            if (_tvSuggestionKeyword.value == trimmed) {
                _tvSuggestions.value = suggestions
                _tvSuggestionsLoading.value = false
            }
        }
    }

    fun searchContent(wd: String, force: Boolean = false) {
        val keyword = wd.trim()
        if (keyword.isBlank()) return
        if (!force && keyword == lastSearchKeyword && (_searchLoading.value || _searchResult.value.isNotEmpty())) {
            return
        }
        lastSearchKeyword = keyword

        viewModelScope.launch {
            _searchLoading.value = true
            _searchError.value = null
            _searchResult.value = emptyMap()
            ensureConfigStarted()

            val searchableSites = engine.currentConfig?.sites
                ?.filter { it.isSpiderSite() && it.isSearchable() }
                .orEmpty()

            // 搜索超时上限：单站最多等 8 秒，避免慢站拖慢整体
            val searchTimeoutMs = 8_000L

            supervisorScope {
                val tasks = searchableSites.map { site ->
                    async {
                        val result = engine.sourceRepository.search(site, keyword, timeoutMs = searchTimeoutMs)
                        val list: List<VodInfo> = result.getOrElse {
                            Log.w(TAG, "search failed: ${site.key}", it)
                            return@async site to emptyList<VodInfo>()
                        }.list
                        site to list
                    }
                }

                // 每个站点完成后立即更新 UI，不等待慢站
                tasks.forEach { task ->
                    launch {
                        val (site, list) = task.await()
                        if (list.isEmpty()) return@launch
                        val label = if (searchableSites.size > 1) "[${site.name}]" else site.name
                        _searchResult.update { current ->
                            val updated = LinkedHashMap(current)
                            // 维护"全部"分组
                            val allVideos = (updated["全部"] ?: emptyList()) + list
                            updated["全部"] = allVideos
                            updated[label] = list
                            updated
                        }
                        // 第一条结果出现时立即关闭 loading
                        if (_searchLoading.value) _searchLoading.value = false
                    }
                }

                // 等所有任务完成再做收尾
                tasks.forEach { it.await() }
            }

            _searchLoading.value = false
            if (_searchResult.value.isEmpty()) {
                _searchError.value = "没有搜索到结果，或者当前启用站点暂时不可用"
            }
            addKeyword(keyword)
        }
    }

    private fun updateSearchState(historyList: List<SearchHistoryEntity>) {
        val recentKeywords = historyList.map { it.keyword }.toSet()
        val hotTags = _hotKeywords.value.filterNot(recentKeywords::contains)
        val searchableSites = engine.currentConfig?.sites
            ?.filter { it.isSpiderSite() && it.isSearchable() }
            .orEmpty()

        _searchState.value = buildList {
            if (hotTags.isNotEmpty()) add(TagGroup("热门搜索", hotTags))
            if (searchableSites.isNotEmpty()) {
                add(TagGroup("已启用站点", searchableSites.map { it.name }))
            }
        }
    }

    private fun refreshHotKeywords() {
        viewModelScope.launch {
            ensureConfigStarted()
            val hot = loadCurrentSiteHotKeywords()
                .mapNotNull { it.normalizeKeyword().takeIf(String::isNotBlank) }
                .distinct()
                .take(MAX_HOT_KEYWORDS)
            _hotKeywords.value = hot
            updateSearchState(history.value)
        }
    }

    private suspend fun loadCurrentSiteHotKeywords(): List<String> {
        val site = currentSearchSite() ?: return emptyList()
        return withTimeoutOrNull(SPIDER_HOT_TIMEOUT_MS) {
            val homeVideos = engine.sourceRepository.getHomeVideos(site)
                .getOrNull()
                ?.list
                .orEmpty()

            val categoryVideos = if (homeVideos.isEmpty()) {
                val firstCategory = engine.sourceRepository.getHomeContent(site)
                    .getOrNull()
                    ?.categories
                    ?.firstOrNull()
                firstCategory?.let {
                    engine.sourceRepository.getCategoryContent(site, it.id)
                        .getOrNull()
                        ?.list
                }.orEmpty()
            } else {
                emptyList()
            }

            (homeVideos + categoryVideos)
                .mapNotNull { it.name.normalizeKeyword().takeIf(String::isNotBlank) }
                .take(MAX_HOT_KEYWORDS)
        }.orEmpty()
    }

    private suspend fun fetchCurrentSiteSuggestions(keyword: String): List<String> {
        ensureConfigStarted()
        val site = currentSearchSite() ?: return emptyList()
        return withTimeoutOrNull(SUGGESTION_TIMEOUT_MS) {
            engine.sourceRepository.search(site, keyword, timeoutMs = SUGGESTION_TIMEOUT_MS)
                .getOrNull()
                ?.list
                .orEmpty()
                .mapNotNull { it.name.normalizeKeyword().takeIf(String::isNotBlank) }
                .distinct()
                .take(MAX_SUGGESTIONS)
        }.orEmpty()
    }

    private suspend fun ensureConfigStarted() {
        if (engine.currentConfig != null) return
        val source = sourcePreference.load() ?: DefaultSources.default
        engine.start(source)
    }

    private fun currentSearchSite(): SiteConfig? {
        val sites = engine.currentConfig?.sites
            ?.filter { it.isSpiderSite() && it.isSearchable() }
            .orEmpty()
        val preferredKey = sourcePreference.loadPopularSiteKey()
        return sites.firstOrNull { it.key == preferredKey } ?: sites.firstOrNull()
    }

    companion object {
        private const val TAG = "SearchViewModel"
        private const val MAX_HOT_KEYWORDS = 16
        private const val SPIDER_HOT_TIMEOUT_MS = 5_000L
        private const val SUGGESTION_TIMEOUT_MS = 4_000L
        private const val MAX_SUGGESTIONS = 12
    }
}

private fun String.normalizeKeyword(): String =
    trim().replace("<", "").replace(">", "")
        .replace("《", "").replace("》", "")
        .replace("-", " ").substringBefore(" ").trim()
