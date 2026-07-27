package com.loong.longtv.ui.shared.popular

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.loong.longtv.engine.TvBoxEngine
import com.loong.longtv.engine.source.model.Category
import com.loong.longtv.engine.source.model.SiteConfig
import com.loong.longtv.engine.source.model.VodInfo

class PopularPagingSource(
    private val site: SiteConfig,
    private val tab: Category,
    private val filters: Map<String, String>,
    private val engine: TvBoxEngine,
    private val homeTabId: String,
    private val useHomeVideoContent: Boolean = false
) : PagingSource<Int, VodInfo>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, VodInfo> {
        val page = params.key ?: 1
        val result = if (tab.id == homeTabId && useHomeVideoContent) {
            if (page == 1) {
                engine.sourceRepository.getHomeVideos(site)
            } else {
                return LoadResult.Page(data = emptyList(), prevKey = page - 1, nextKey = null)
            }
        } else {
            val categoryId = if (tab.id == homeTabId) "0" else tab.id
            engine.sourceRepository.getCategoryContent(site, categoryId, page, HashMap(filters))
        }

        return if (result.isSuccess) {
            val content = result.getOrThrow()
            val items = content.list.onEach { it.siteKey = site.key }
            LoadResult.Page(
                data = items,
                prevKey = if (page == 1) null else page - 1,
                nextKey = when {
                    content.page >= content.pagecount -> null
                    items.isEmpty() -> null
                    else -> page + 1
                }
            )
        } else {
            LoadResult.Error(result.exceptionOrNull() ?: IllegalStateException("Unknown error"))
        }
    }

    override fun getRefreshKey(state: PagingState<Int, VodInfo>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchorPosition)
        return page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
    }
}
