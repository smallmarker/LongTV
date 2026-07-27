package com.loong.longtv.engine.source.model

import com.google.gson.JsonElement
import java.net.InetAddress

/**
 * TVBox配置文件的完整数据模型
 */
data class TvBoxConfig(
    val spider: String = "",
    val wallpaper: String = "",
    val sites: List<SiteConfig> = emptyList(),
    val doh: List<DoHConfig> = emptyList(),
    val lives: List<LiveConfig> = emptyList(),
    val parses: List<ParseConfig> = emptyList(),
    val flags: List<String> = emptyList()
)

/**
 * 站点配置 - 对应配置文件中sites数组的每一项
 *
 * type: 站点类型
 *   0 = xml直链站（CMS接口）
 *   1 = json直链站（CMS接口）
 *   3 = Spider站（Jar插件）
 *
 * api: Spider类名标识
 *   "csp_XXX" → 对应Jar中的 com.github.catvod.spider.XXX
 */
data class SiteConfig(
    val key: String,
    val name: String,
    val type: Int = 3,
    val api: String = "",
    val searchable: Int = 0,
    val quickSearch: Int = 0,
    val changeable: Int = 0,
    val ext: JsonElement? = null,
    val jar: String? = null,
    val playerType: Int = 0,
    val timeout: Int = 15
) {
    fun isSearchable(): Boolean = searchable != 0
    fun isQuickSearchable(): Boolean = quickSearch != 0

    fun isSpiderSite(): Boolean = type == 3 && api.startsWith("csp_")

    /** 获取Spider类名: csp_XXX → XXX */
    fun spiderClassName(): String = api.removePrefix("csp_")

    /** 获取ext的字符串值（用于Spider.init的extend参数） */
    fun extendString(): String {
        val value = ext ?: return ""
        return if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
            value.asString
        } else {
            value.toString()
        }
    }
}

data class DoHConfig(
    val name: String = "",
    val url: String = "",
    val ips: List<String> = emptyList()
) {
    fun toInetAddresses(): List<InetAddress>? {
        return runCatching {
            ips.map { InetAddress.getByName(it) }.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }
}

data class LiveConfig(
    val name: String = "",
    val type: Int = 0,
    val url: String = "",
    val playerType: Int = 0,
    val epg: String? = null,
    val logo: String? = null,
    val ua: String? = null,
    val timeout: Int = 10
)

data class LiveChannel(
    val name: String,
    val url: String,
    val group: String = "",
    val logo: String = "",
    val sourceName: String = "",
)

/**
 * 解析接口配置 - 当Spider返回parse=1时使用
 */
data class ParseConfig(
    val name: String = "",
    val type: Int = 0,
    val url: String = "",
    val ext: JsonElement? = null
)
