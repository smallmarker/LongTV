package com.loong.longtv.ui.shared.home

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.loong.longtv.data.bean.HomeTabItem
import com.loong.longtv.data.bean.Site
import com.loong.longtv.data.bean.Video
import com.loong.longtv.data.repository.ApiResult
import com.loong.longtv.data.repository.SpiderRepository

class HomePagingSource(
    private val site: Site,
    private val tab: HomeTabItem,
    private val spiderRepository: SpiderRepository,
    private val homeTabId: String
) : PagingSource<Int, Video>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Video> {
        val page = params.key ?: 1
        val result = if (tab.type_id == homeTabId) {
            spiderRepository.loadHomeVideoContent(site)
        } else {
            spiderRepository.loadCategoryContent(site, tab, page)
        }

        return when (result) {
            is ApiResult.Success -> {
                val content = result.data
                val items = content?.list.orEmpty()
                items.forEach { it.key = site.key }
                val nextKey = when {
                    tab.type_id == homeTabId -> null
                    content == null -> null
                    content.page >= content.pagecount -> null
                    items.isEmpty() -> null
                    else -> page + 1
                }
                LoadResult.Page(
                    data = items,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = nextKey
                )
            }

            is ApiResult.Error -> LoadResult.Error(result.exception ?: IllegalStateException(result.message))
            ApiResult.Loading -> LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Video>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchorPosition)
        return page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
    }
}
