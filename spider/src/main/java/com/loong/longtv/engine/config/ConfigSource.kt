package com.loong.longtv.engine.config

/**
 * 配置源 - TVBox的核心：一切从配置文件开始
 *
 * 配置文件定义了：
 * - spider: Jar插件地址（本地路径或远程URL）
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

    /** 远程URL配置 */
    data class Remote(
        val name: String,
        val url: String // e.g. "http://www.饭太硬.cc/tv"
    ) : ConfigSource()

    /** 本地文件配置 */
    data class Local(
        val name: String,
        val filePath: String
    ) : ConfigSource()

    fun displayName(): String = when (this) {
        is Asset -> name
        is Remote -> name
        is Local -> name
    }
}
