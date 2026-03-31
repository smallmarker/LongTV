package com.loong.longtv.ui.home.repository

import android.util.Log
import com.github.catvod.crawler.Spider
import com.loong.longtv.core.spider.SpiderRegistry
import com.loong.longtv.App
import com.loong.longtv.data.bean.CategoryContent
import com.loong.longtv.data.bean.HomeContent
import com.loong.longtv.data.bean.HomeTabItem
import com.loong.longtv.data.bean.PlayerContent
import com.loong.longtv.data.bean.Site
import com.loong.longtv.data.bean.Video
import com.loong.longtv.utils.GsonUtils.fromJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

interface SpiderRepository {
    suspend fun loadHomeContent(site: Site): ApiResult<HomeContent?>
    suspend fun loadHomeVideoContent(site: Site): ApiResult<CategoryContent?>
    suspend fun loadCategoryContent(site: Site, item: HomeTabItem, page: Int): ApiResult<CategoryContent?>
    suspend fun searchContent(site: Site, keyword: String): ApiResult<CategoryContent?>
    suspend fun detailContent(site: Site, ids: List<String>): ApiResult<Video?>
    suspend fun playerContent(site: Site, flag: String, id: String): ApiResult<PlayerContent?>
    fun clear()
}

@Singleton
class SpiderRepositoryImpl @Inject constructor(
    private val spiderRegistry: SpiderRegistry
) : SpiderRepository {

    private val spiderMap = ConcurrentHashMap<String, Spider>()

    override suspend fun loadHomeContent(site: Site): ApiResult<HomeContent?> =
        runSpider(site, "loadHomeContent") { spider ->
            spider.homeContent(true).fromJson<HomeContent>()
        }

    override suspend fun loadHomeVideoContent(site: Site): ApiResult<CategoryContent?> =
        runSpider(site, "loadHomeVideoContent") { spider ->
            val json = spider.homeVideoContent()
            if (json.isBlank()) {
                CategoryContent(
                    code = 1,
                    pagecount = 1,
                    total = 0,
                    page = 1,
                    msg = "",
                    limit = 0,
                    list = emptyList()
                )
            } else {
                json.fromJson<CategoryContent>()
            }
        }

    override suspend fun loadCategoryContent(
        site: Site,
        item: HomeTabItem,
        page: Int
    ): ApiResult<CategoryContent?> =
        runSpider(site, "loadCategoryContent") { spider ->
            spider.categoryContent(item.type_id, page.toString(), true, hashMapOf())
                .fromJson<CategoryContent>()
        }

    override suspend fun searchContent(site: Site, keyword: String): ApiResult<CategoryContent?> =
        runSpider(site, "searchContent") { spider ->
            spider.searchContent(keyword, false).fromJson<CategoryContent>()
        }

    override suspend fun detailContent(site: Site, ids: List<String>): ApiResult<Video?> =
        runSpider(site, "detailContent") { spider ->
            spider.detailContent(ids).fromJson<CategoryContent>().list.firstOrNull()
        }

    override suspend fun playerContent(site: Site, flag: String, id: String): ApiResult<PlayerContent?> =
        runSpider(site, "playerContent") { spider ->
            spider.playerContent(flag, id, emptyList()).fromJson<PlayerContent>()
        }

    override fun clear() {
        spiderMap.values.forEach(Spider::destroy)
        spiderMap.clear()
    }

    private suspend fun <T> runSpider(
        site: Site,
        action: String,
        block: (Spider) -> T
    ): ApiResult<T> {
        return try {
            val result = withTimeout(15_000) {
                withContext(Dispatchers.IO) {
                    block(getSpider(site))
                }
            }
            ApiResult.Success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Spider action failed: ${site.key}/$action", e)
            ApiResult.Error("${site.name} $action 失败", e)
        }
    }

    private fun getSpider(site: Site): Spider {
        return spiderMap.getOrPut(site.key) {
            val spider = requireNotNull(spiderRegistry.create(site)) {
                "Spider not registered for ${site.key}"
            }
            spider.init(App.instance, site.ext?.toString().orEmpty())
            spider
        }
    }

    companion object {
        private const val TAG = "SpiderRepository"
    }
}
