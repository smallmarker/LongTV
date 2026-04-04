package com.loong.longtv.ui.shared.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParser
import com.loong.longtv.data.bean.TagGroup
import com.loong.longtv.data.bean.Video
import com.loong.longtv.data.room.dao.SearchHistoryDao
import com.loong.longtv.data.room.entity.SearchHistoryEntity
import com.loong.longtv.data.site.SiteRepository
import com.loong.longtv.data.repository.ApiResult
import com.loong.longtv.data.repository.SpiderRepository
import com.loong.longtv.ui.shared.settings.spiderFamily
import com.loong.longtv.utils.GsonUtils.toJson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

/**
 * @author zl
 * @Date 2025/10/20
 **/
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val dao: SearchHistoryDao,
    private val spiderRepository: SpiderRepository,
    private val siteRepository: SiteRepository,
) : ViewModel() {
    private val client = OkHttpClient()
    private val _hotKeywords = MutableStateFlow<List<String>>(emptyList())
    private val _tvSuggestions = MutableStateFlow<List<String>>(emptyList())
    private val _tvSuggestionKeyword = MutableStateFlow("")
    private val _tvSuggestionsLoading = MutableStateFlow(false)
    val tvSuggestions: StateFlow<List<String>> = _tvSuggestions.asStateFlow()
    val tvSuggestionsLoading: StateFlow<Boolean> = _tvSuggestionsLoading.asStateFlow()

    private val _searchState = MutableStateFlow<List<TagGroup>>(emptyList())
    val searchState: StateFlow<List<TagGroup>> = _searchState.asStateFlow()

    private val _searchResult = MutableStateFlow<Map<String, List<Video>>>(emptyMap())
    val searchResult = _searchResult.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading = _searchLoading.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()

    // 历史记录列表，自动更新 UI
    val history = dao.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            history.collectLatest(::updateSearchState)
        }
        refreshHotKeywords()
    }

    // 添加一条历史记录
    fun addKeyword(keyword: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            dao.insert(SearchHistoryEntity(keyword = keyword))
        }
    }

    // 清空历史
    fun clear() {
        viewModelScope.launch {
            dao.clearAll()
        }
    }

    fun removeKeyword(keyword: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            dao.deleteByKeyword(keyword)
        }
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
            _tvSuggestions.value = fetchTvSuggestions(trimmed)
            if (_tvSuggestionKeyword.value == trimmed) {
                _tvSuggestionsLoading.value = false
            }
        }
    }

    /**
     * 搜索内容
     */
    fun searchContent(wd: String) {
        viewModelScope.launch {
            Log.d("SearchViewModel", "search wd: $wd")
            _searchLoading.value = true
            _searchError.value = null
            _searchResult.value = emptyMap()
            val grouped = linkedMapOf<String, List<Video>>()
            val allVideos = mutableListOf<Video>()
            val enabledSites = siteRepository.getEnabledSites().filter { it.isSearchable() }
            supervisorScope {
                val tasks = enabledSites
                    .map { site ->
                        async {
                            Log.d("SearchViewModel", "search site: ${site.toJson()}")
                            val list = when (val result = spiderRepository.searchContent(site, wd)) {
                                is ApiResult.Success -> result.data?.list.orEmpty()
                                is ApiResult.Error -> {
                                    Log.w("SearchViewModel", "search site failed: ${site.key}", result.exception)
                                    emptyList()
                                }
                                else -> emptyList()
                            }.onEach { it.key = site.key }
                            site to list
                        }
                    }
                tasks.forEach { task ->
                    val (site, list) = task.await()
                    if (list.isEmpty()) return@forEach
                    grouped[site.displayName(enabledSites.size > 1)] = list
                    allVideos += list
                }
            }
            _searchResult.value = buildMap {
                if (allVideos.isNotEmpty()) put("全部", allVideos.toList())
                putAll(grouped)
            }
            if (_searchResult.value.isEmpty()) {
                _searchError.value = "没有搜索到结果，或者当前启用站点暂时不可用"
            }
            _searchLoading.value = false
            addKeyword(wd)
        }
    }

    private fun updateSearchState(historyList: List<SearchHistoryEntity>) {
        val groups = buildList {
            val recentKeywords = historyList.map { it.keyword }.toSet()
            val hotTags = _hotKeywords.value.filterNot(recentKeywords::contains)
            if (hotTags.isNotEmpty()) {
                add(
                    TagGroup(
                        title = "热门搜索",
                        tags = hotTags
                    )
                )
            }

            val siteTags = siteRepository.getEnabledSites()
                .filter { it.isSearchable() }
                .map { it.displayName(showFamily = false) }
            if (siteTags.isNotEmpty()) {
                add(
                    TagGroup(
                        title = "已启用站点",
                        tags = siteTags
                    )
                )
            }
        }
        _searchState.value = groups
    }

    private fun refreshHotKeywords() {
        viewModelScope.launch {
            val hot = loadSiteHotKeywords().ifEmpty { loadTencentHotKeywords() }
            _hotKeywords.value = hot
            updateSearchState(history.value)
        }
    }

    private suspend fun loadSiteHotKeywords(): List<String> {
        val site = siteRepository.getDefaultSite()
        return when (val result = spiderRepository.loadHotSearchContent(site)) {
            is ApiResult.Success -> result.data.orEmpty()
                .map { it.normalizeHotKeyword() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(12)

            else -> emptyList()
        }
    }

    private suspend fun loadTencentHotKeywords(): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = requireNotNull("https://node.video.qq.com/x/api/hot_search".toHttpUrlOrNull())
                .newBuilder()
                .addQueryParameter("channdlId", "0")
                .addQueryParameter("_", System.currentTimeMillis().toString())
                .build()
            val request = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val body = response.body?.string().orEmpty()
                val listInfo = JsonParser.parseString(body)
                    .asJsonObject
                    .getAsJsonObject("data")
                    .getAsJsonObject("mapResult")
                    .getAsJsonObject("0")
                    .getAsJsonArray("listInfo")

                listInfo.mapNotNull { element ->
                    element.asJsonObject
                        .get("title")
                        ?.asString
                        ?.normalizeHotKeyword()
                }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(12)
            }
        }.getOrElse { throwable ->
            Log.w("SearchViewModel", "load tencent hot keywords failed", throwable)
            emptyList()
        }
    }

    private suspend fun fetchTvSuggestions(keyword: String): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = requireNotNull("https://tv.aiseet.atianqi.com/i-tvbin/qtv_video/search/get_search_smart_box".toHttpUrlOrNull())
                .newBuilder()
                .addQueryParameter("format", "json")
                .addQueryParameter("page_num", "0")
                .addQueryParameter("page_size", "50")
                .addQueryParameter("key", keyword)
                .build()
            val request = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val body = response.body?.string().orEmpty()
                JsonParser.parseString(body)
                    .asJsonObject
                    .getAsJsonObject("data")
                    .getAsJsonObject("search_data")
                    .getAsJsonArray("vecGroupData")
                    .firstOrNull()
                    ?.asJsonObject
                    ?.getAsJsonArray("group_data")
                    ?.mapNotNull { element ->
                        element.asJsonObject
                            .getAsJsonObject("dtReportInfo")
                            ?.getAsJsonObject("reportData")
                            ?.get("keyword_txt")
                            ?.asString
                            ?.trim()
                            ?.takeIf(String::isNotBlank)
                    }
                    ?.distinct()
                    ?.take(12)
                    .orEmpty()
            }
        }.getOrElse { throwable ->
            Log.w("SearchViewModel", "load tv suggestions failed", throwable)
            emptyList()
        }
    }
}

private fun com.loong.longtv.data.bean.Site.displayName(showFamily: Boolean): String {
    return if (showFamily) "[${spiderFamily().label}] $name" else name
}

private fun String.normalizeHotKeyword(): String {
    return trim()
        .replace("<", "")
        .replace(">", "")
        .replace("《", "")
        .replace("》", "")
        .replace("-", " ")
        .substringBefore(" ")
        .trim()
}
