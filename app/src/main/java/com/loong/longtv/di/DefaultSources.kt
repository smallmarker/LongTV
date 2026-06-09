package com.loong.longtv.di

import com.loong.longtv.engine.config.ConfigSource

/**
 * 预定义配置源
 */
object DefaultSources {

    /** 本地 LongTV 默认配置包（内置Spider，无需外部jar） */
    val LOCAL_LONGTV = ConfigSource.Asset(
        name = "LongTV(默认)",
        assetPath = "tvbox/longtv"
    )

    /** 在线饭太硬 */
    val REMOTE_FANTY = ConfigSource.Remote(
        name = "饭太硬(在线)",
        url = "http://www.饭太硬.cc/tv"
    )

    /** 所有可用配置源 */
    val all: List<ConfigSource> = listOf(LOCAL_LONGTV, REMOTE_FANTY)

    /** 默认配置源 */
    val default: ConfigSource = LOCAL_LONGTV
}
