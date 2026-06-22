package com.loong.longtv.engine.spider

import android.content.Context
import android.util.Log
import com.github.catvod.crawler.Spider

/**
 * Spider加载器 - 仅从应用自身classLoader创建随包内置的Spider。
 */
class SpiderLoader(private val context: Context) {

    @Volatile
    private var appOnlyMode = false

    /**
     * 根据短类名创建Spider实例。
     *
     * @param className 短类名（如 "AppRJ"）
     */
    fun createSpider(className: String): Spider? {
        val fullClassName = "$SPIDER_PACKAGE.$className"
        return runCatching {
            val clazz = context.classLoader.loadClass(fullClassName)
            (clazz.getDeclaredConstructor().newInstance() as? Spider)?.also {
                appOnlyMode = true
                Log.d(TAG, "Spider created from app classLoader: $fullClassName")
            }
        }.getOrElse {
            Log.e(TAG, "Spider not found in app classLoader: $fullClassName", it)
            null
        }
    }

    fun isLoaded(): Boolean = appOnlyMode

    fun release() {
        appOnlyMode = false
    }

    companion object {
        private const val TAG = "SpiderLoader"
        private const val SPIDER_PACKAGE = "com.github.catvod.spider"
    }
}
