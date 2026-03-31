package com.loong.longtv.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loong.longtv.data.bean.TagGroup
import com.loong.longtv.data.bean.Video
import com.loong.longtv.data.room.dao.SearchHistoryDao
import com.loong.longtv.data.room.entity.SearchHistoryEntity
import com.loong.longtv.data.site.SiteRepository
import com.loong.longtv.ui.home.repository.ApiResult
import com.loong.longtv.ui.home.repository.SpiderRepository
import com.loong.longtv.ui.settings.spiderFamily
import com.loong.longtv.utils.GsonUtils.toJson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
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
            supervisorScope {
                val tasks = siteRepository.getEnabledSites()
                    .filter { it.isSearchable() }
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
                    grouped[site.displayNameWithFamily()] = list
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
            if (historyList.isNotEmpty()) {
                add(
                    TagGroup(
                        title = "最近搜索",
                        tags = historyList.map { it.keyword }
                    )
                )
            }

            val siteTags = siteRepository.getEnabledSites()
                .filter { it.isSearchable() }
                .map { it.displayNameWithFamily() }
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
}

private fun com.loong.longtv.data.bean.Site.displayNameWithFamily(): String {
    return "[${spiderFamily().label}] $name"
}
