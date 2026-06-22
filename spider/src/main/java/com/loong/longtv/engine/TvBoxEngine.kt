package com.loong.longtv.engine

import android.content.Context
import com.loong.longtv.engine.config.ConfigManager
import com.loong.longtv.engine.config.ConfigSource
import com.loong.longtv.engine.player.PlayerResolver
import com.loong.longtv.engine.source.SourceRepository
import com.loong.longtv.engine.source.model.TvBoxConfig
import com.loong.longtv.engine.spider.SpiderManager

/**
 * TVBox引擎门面 - 整个引擎层的统一入口
 *
 * 组合了配置管理、Spider管理、数据源和播放解析四大核心模块。
 * App层通过此类与引擎交互。
 */
class TvBoxEngine(context: Context) {

    val configManager = ConfigManager(context)
    val spiderManager = SpiderManager(context)
    val sourceRepository = SourceRepository(spiderManager)
    val playerResolver = PlayerResolver()

    /** 当前活跃配置 */
    val currentConfig: TvBoxConfig? get() = configManager.currentConfig

    /**
     * 启动引擎 - 加载配置 + 初始化Spider
     */
    suspend fun start(source: ConfigSource): Result<TvBoxConfig> {
        val configResult = configManager.loadConfig(source)
        if (configResult.isSuccess) {
            spiderManager.initialize()
        }
        return configResult
    }

    /**
     * 切换配置源
     */
    suspend fun switchSource(source: ConfigSource): Result<TvBoxConfig> {
        shutdown()
        return start(source)
    }

    /**
     * 关闭引擎，释放所有资源
     */
    fun shutdown() {
        spiderManager.destroyAll()
        configManager.clear()
    }
}
