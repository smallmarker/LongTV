package com.loong.longtv.engine.config

import android.content.Context
import android.util.Log
import com.loong.longtv.engine.source.model.TvBoxConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 配置管理器 - TVBox架构的入口点
 *
 * 职责：
 * 1. 加载/切换当前活跃的内置配置
 * 2. 提供当前配置给SpiderManager和UI层
 */
class ConfigManager(
    private val context: Context,
    private val parser: ConfigParser = ConfigParser()
) {
    @Volatile
    private var _currentConfig: TvBoxConfig? = null
    val currentConfig: TvBoxConfig? get() = _currentConfig

    @Volatile
    private var _currentSource: ConfigSource? = null
    val currentSource: ConfigSource? get() = _currentSource

    /**
     * 加载配置 - 核心方法
     */
    suspend fun loadConfig(source: ConfigSource): Result<TvBoxConfig> = withContext(Dispatchers.IO) {
        runCatching {
            val config = when (source) {
                is ConfigSource.Asset -> loadFromAsset(source)
            }
            _currentConfig = config
            _currentSource = source
            Log.i(TAG, "Config loaded: ${source.displayName()}, sites=${config.sites.size}")
            config
        }.onFailure {
            Log.e(TAG, "Failed to load config: ${source.displayName()}", it)
        }
    }

    private fun loadFromAsset(source: ConfigSource.Asset): TvBoxConfig {
        return parser.parseFromAsset(context, source.assetPath)
    }

    fun clear() {
        _currentConfig = null
        _currentSource = null
    }

    companion object {
        private const val TAG = "ConfigManager"
    }
}
