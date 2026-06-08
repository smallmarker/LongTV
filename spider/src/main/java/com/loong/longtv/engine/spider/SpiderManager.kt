package com.loong.longtv.engine.spider

import android.content.Context
import android.util.Log
import com.github.catvod.crawler.Spider
import com.loong.longtv.engine.config.ConfigManager
import com.loong.longtv.engine.source.model.SiteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Spider管理器 - 管理Spider实例的生命周期
 *
 * 职责：
 * 1. 接收ConfigManager加载的配置
 * 2. 使用SpiderLoader加载Jar
 * 3. 按需创建/缓存Spider实例
 * 4. 管理Spider的init/destroy生命周期
 */
class SpiderManager(
    private val context: Context,
    private val configManager: ConfigManager
) {
    private val loader = SpiderLoader(context)
    private val spiderCache = ConcurrentHashMap<String, Spider>()
    private val initMutex = Mutex()

    /**
     * 初始化Spider引擎 - 加载Jar
     * 应在配置加载成功后调用
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        initMutex.withLock {
            val jarFile = configManager.resolveSpiderJar()
            if (jarFile == null) {
                Log.w(TAG, "No spider jar configured")
                return@withContext false
            }
            loader.loadJar(jarFile)
        }
    }

    /**
     * 获取或创建Spider实例
     * 包含初始化（调用Spider.init）
     */
    suspend fun getSpider(site: SiteConfig): Spider? = withContext(Dispatchers.IO) {
        if (!site.isSpiderSite()) {
            Log.w(TAG, "Site ${site.key} is not a spider site")
            return@withContext null
        }

        spiderCache[site.key]?.let { return@withContext it }

        if (!loader.isLoaded()) {
            initialize()
        }

        val className = site.spiderClassName()
        val spider = loader.createSpider(className) ?: run {
            Log.e(TAG, "Failed to create spider for ${site.key} (class=$className)")
            return@withContext null
        }

        // 初始化Spider
        runCatching {
            spider.init(context, site.extendString())
        }.onFailure {
            Log.e(TAG, "Spider init failed for ${site.key}", it)
            return@withContext null
        }

        spiderCache[site.key] = spider
        Log.d(TAG, "Spider created and initialized: ${site.key}")
        spider
    }

    /**
     * 销毁指定站点的Spider
     */
    fun destroySpider(siteKey: String) {
        spiderCache.remove(siteKey)?.let { spider ->
            runCatching { spider.destroy() }
            Log.d(TAG, "Spider destroyed: $siteKey")
        }
    }

    /**
     * 销毁所有Spider，释放资源
     */
    fun destroyAll() {
        spiderCache.forEach { (key, spider) ->
            runCatching { spider.destroy() }
            Log.d(TAG, "Spider destroyed: $key")
        }
        spiderCache.clear()
        loader.release()
    }

    fun isInitialized(): Boolean = loader.isLoaded()

    companion object {
        private const val TAG = "SpiderManager"
    }
}
