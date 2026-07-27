package com.loong.longtv.engine.source.model

import com.google.gson.annotations.SerializedName

/**
 * Spider返回的数据模型 - 与TVBox Spider接口协议对齐
 */

/** homeContent返回 - 分类列表 + 筛选条件 */
data class HomeContent(
    @SerializedName("class")
    val categories: List<Category> = emptyList(),
    val filters: Map<String, List<Filter>> = emptyMap()
)

data class Category(
    @SerializedName("type_id")
    val id: String = "",
    @SerializedName("type_name")
    val name: String = ""
)

data class Filter(
    val key: String = "",
    val name: String = "",
    val value: List<FilterValue> = emptyList()
)

data class FilterValue(
    val n: String = "",
    val v: String = ""
)

/** categoryContent / homeVideoContent / searchContent / detailContent 返回 */
data class VodListResult(
    val page: Int = 1,
    val pagecount: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
    val list: List<VodInfo> = emptyList()
)

/**
 * 影片信息 - TVBox标准字段
 */
data class VodInfo(
    @SerializedName("vod_id")
    val id: String = "",
    @SerializedName("vod_name")
    val name: String = "",
    @SerializedName("vod_pic")
    val pic: String = "",
    @SerializedName("vod_remarks")
    val remarks: String = "",
    @SerializedName("type_id")
    val typeId: String = "",
    @SerializedName("type_name")
    val typeName: String = "",
    @SerializedName("vod_year")
    val year: String = "",
    @SerializedName("vod_area")
    val area: String = "",
    @SerializedName("vod_lang")
    val lang: String = "",
    @SerializedName("vod_actor")
    val actor: String = "",
    @SerializedName("vod_director")
    val director: String = "",
    @SerializedName("vod_content")
    val content: String = "",
    @SerializedName("vod_play_from")
    val playFrom: String = "",
    @SerializedName("vod_play_url")
    val playUrl: String = "",
    @SerializedName("vod_tag")
    val tag: String = "",
    @SerializedName("vod_time")
    val time: String = "",
    @SerializedName("vod_score")
    val score: String = "",
    @SerializedName("vod_state")
    val state: String = ""
) {
    /** 来源站点key，运行时填入 */
    var siteKey: String = ""

    /**
     * 运行时生成的封面请求。Spider 协议只传 vod_pic 字符串，因此请求头不会进入
     * JSON 或历史记录；由 SourceRepository 在拿到站点上下文后补齐。
     */
    @Transient
    var poster: PosterImage = PosterImage(pic)

    /**
     * 解析播放线路
     * playFrom: "线路1$$$线路2"
     * playUrl:  "第1集$url1#第2集$url2$$$第1集$url3#第2集$url4"
     */
    fun getPlayLines(): List<PlayLine> {
        if (playFrom.isBlank() || playUrl.isBlank()) return emptyList()
        val flags = playFrom.split("\$\$\$")
        val urls = playUrl.split("\$\$\$")
        return flags.indices.map { i ->
            val flag = flags.getOrElse(i) { "线路${i + 1}" }.ifBlank { "线路${i + 1}" }
            val episodes = parseEpisodes(urls.getOrElse(i) { "" })
            PlayLine(flag = flag, episodes = episodes)
        }
    }

    private fun parseEpisodes(raw: String): List<Episode> {
        if (raw.isBlank()) return emptyList()
        val parts = if (raw.contains("#")) raw.split("#") else listOf(raw)
        return parts.mapIndexedNotNull { index, part ->
            val segments = part.split("$", limit = 2)
            when {
                segments.size >= 2 -> Episode(name = segments[0], url = segments[1])
                segments.isNotEmpty() && segments[0].isNotBlank() ->
                    Episode(name = "第${index + 1}集", url = segments[0])
                else -> null
            }
        }
    }
}

/** 图片地址和它所需的防盗链请求头。 */
data class PosterImage(
    val url: String = "",
    val headers: Map<String, String> = emptyMap()
)

/** 播放线路 */
data class PlayLine(
    val flag: String,
    val episodes: List<Episode>
)

/** 单集 */
data class Episode(
    val name: String,
    val url: String
)

/**
 * playerContent返回 - 播放解析结果
 *
 * parse: 0=已解析出直链, 1=需要调用解析接口
 * url: 播放地址或待解析地址
 * playUrl: 备选播放地址
 */
data class PlayerInfo(
    val parse: Int = 0,
    val url: String = "",
    val playUrl: String = "",
    val header: Map<String, String> = emptyMap(),
    val jx: Int = 0,
    val flag: String = ""
) {
    /** 获取实际使用的URL */
    fun effectiveUrl(): String = playUrl.ifBlank { url }

    /** 是否需要进一步解析 */
    fun needsParse(): Boolean = parse == 1
}
