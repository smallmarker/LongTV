package com.loong.longtv.engine.source

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.loong.longtv.engine.source.model.HomeContent
import com.loong.longtv.engine.source.model.PlayerInfo
import com.loong.longtv.engine.source.model.SiteConfig
import com.loong.longtv.engine.source.model.VodInfo
import com.loong.longtv.engine.source.model.VodListResult
import com.loong.longtv.engine.spider.SpiderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * 统一数据源 - 对UI层暴露的唯一数据接口
 *
 * 所有数据获取都经过此类，内部调度Spider完成实际工作。
 * UI层无需了解Spider的存在。
 */
class SourceRepository(
    private val spiderManager: SpiderManager,
    private val gson: Gson = Gson()
) {

    /**
     * 获取站点首页分类
     */
    suspend fun getHomeContent(site: SiteConfig): Result<HomeContent> =
        callSpider(site, "homeContent") { spider ->
            val json = spider.homeContent(true)
            parseJson<HomeContent>(json) ?: HomeContent()
        }

    /**
     * 获取首页推荐视频
     */
    suspend fun getHomeVideos(site: SiteConfig): Result<VodListResult> =
        callSpider(site, "homeVideoContent") { spider ->
            val json = spider.homeVideoContent()
            if (json.isBlank()) VodListResult()
            else parseJson<VodListResult>(json) ?: VodListResult()
        }

    /**
     * 获取分类内容（带分页）
     */
    suspend fun getCategoryContent(
        site: SiteConfig,
        categoryId: String,
        page: Int = 1,
        filters: HashMap<String, String> = hashMapOf()
    ): Result<VodListResult> =
        callSpider(site, "categoryContent") { spider ->
            val json = spider.categoryContent(categoryId, page.toString(), true, filters)
            parseJson<VodListResult>(json) ?: VodListResult()
        }

    /**
     * 获取视频详情
     */
    suspend fun getDetail(site: SiteConfig, vodId: String): Result<VodInfo?> =
        callSpider(site, "detailContent") { spider ->
            val json = spider.detailContent(mutableListOf(vodId))
            val result = parseJson<VodListResult>(json)
            result?.list?.firstOrNull()?.also { it.siteKey = site.key }
        }

    /**
     * 搜索
     */
    suspend fun search(site: SiteConfig, keyword: String, page: Int = 1, timeoutMs: Long = site.timeout * 1000L): Result<VodListResult> =
        callSpider(site, "searchContent", timeoutMs = timeoutMs) { spider ->
            val json = if (page > 1) {
                spider.searchContent(keyword, false, page.toString())
            } else {
                spider.searchContent(keyword, false)
            }
            val result = parseJson<VodListResult>(json) ?: VodListResult()
            result.copy(list = result.list.map { it.also { v -> v.siteKey = site.key } })
        }

    /**
     * 获取播放信息
     */
    suspend fun getPlayerInfo(site: SiteConfig, flag: String, episodeUrl: String): Result<PlayerInfo> =
        callSpider(site, "playerContent") { spider ->
            val json = spider.playerContent(flag, episodeUrl, mutableListOf())
            parseJson<PlayerInfo>(json) ?: PlayerInfo()
        }

    /**
     * 通用Spider调用包装 - 超时控制 + 异常处理
     */
    private suspend fun <T> callSpider(
        site: SiteConfig,
        action: String,
        timeoutMs: Long = site.timeout * 1000L,
        block: (com.github.catvod.crawler.Spider) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            val spider = spiderManager.getSpider(site)
                ?: throw IllegalStateException("Cannot create spider for ${site.key}")
            withTimeout(timeoutMs) {
                withContext(Dispatchers.IO) {
                    block(spider)
                }
            }
        }.onFailure {
            Log.e(TAG, "[${site.key}] $action failed: ${it.message}", it)
        }
    }

    private inline fun <reified T> parseJson(json: String?): T? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            gson.fromJson<T>(json, object : TypeToken<T>() {}.type)
        }.onFailure {
            Log.w(TAG, "JSON parse failed: ${it.message}")
        }.getOrNull()
    }

    companion object {
        private const val TAG = "SourceRepository"
    }
}
