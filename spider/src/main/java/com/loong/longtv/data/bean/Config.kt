package com.loong.longtv.data.bean

import com.google.gson.JsonElement
import java.net.InetAddress

/**
 * @author zl
 * @Date 2025/3/11
 **/
data class Config(
    val spider: String,
    val wallpaper: String,
    val sites: List<Site>,
    val doh: List<DoH>,
    val logo: String,
    val lives: List<Live>
) {
    /**
     * 获取第一个站点
     */
    fun firstSite(): Site? = sites.firstOrNull()
}

data class Site(
    // 站点唯一标识
    val key: String,
    // 站点名称
    val name: String,
    // 站点类型
    val type: Int,
    // API接口地址
    val api: String,
    // 是否可搜索
    val searchable: Int,
    // 是否快速搜索
    val quickSearch: Int,
    // 是否可切换
    val changeable: Int,
    // 扩展信息（JSON格式）
    val ext: JsonElement? = null,
    // 播放器类型
    val playerType: Int? = null,
    // 默认是否启用
    val enabledByDefault: Boolean = true,
) {
    /**
     * 是否可搜索
     */
    fun isSearchable() = searchable != 0

    /**
     * 获取API键（移除前缀）
     */
    fun apiKey() = api.removePrefix("csp_")

    // 辅助方法检查类型
    fun isExtObject(): Boolean = ext?.isJsonObject == true

    fun isExitArray(): Boolean = ext?.isJsonArray == true

    fun isExtPrimitive(): Boolean = ext?.isJsonPrimitive == true
}

data class DoH(
    val name: String,
    val url: String,
    val ips: List<String>
) {
    /**
     * 获取IP地址列表
     */
    fun getHosts(): List<InetAddress>? {
        return runCatching {
            ips.map { InetAddress.getByName(it) }.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }
}

data class Live(
    val name: String,
    val type: Int,
    val url: String,
    val playerType: Int,
    val epg: String? = null,
    val logo: String? = null
)
