package com.loong.longtv.di

import com.loong.longtv.engine.config.ConfigSource

/**
 * 预定义配置源 - 用于测试和默认加载
 */
object DefaultSources {

    /** 本地饭太硬包 */
    val LOCAL_FANTY = ConfigSource.Asset(
        name = "饭太硬(本地)",
        assetPath = "tvbox/fanty"
    )

    /** 在线饭太硬 */
    val REMOTE_FANTY = ConfigSource.Remote(
        name = "饭太硬(在线)",
        url = "http://www.饭太硬.cc/tv"
    )

    /** 所有可用配置源 */
    val all: List<ConfigSource> = listOf(LOCAL_FANTY, REMOTE_FANTY)

    /** 默认配置源 */
    val default: ConfigSource = LOCAL_FANTY
}
