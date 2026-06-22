package com.loong.longtv.engine.config

/**
 * 配置源 - LongTV 仅加载应用内置配置。
 *
 * 配置文件定义了：
 * - sites: 站点列表（每个站点对应一个Spider类）
 * - lives: 直播源列表
 * - doh: DNS over HTTPS配置
 */
sealed class ConfigSource {

    /** 本地assets配置（如饭太硬本地包） */
    data class Asset(
        val name: String,
        val assetPath: String // e.g. "tvbox/fanty"
    ) : ConfigSource()

    fun displayName(): String = when (this) {
        is Asset -> name
    }
}
