package com.loong.longtv.data.bean

/**
 * @author zl
 * @Date 2025/10/10
 **/
data class CategoryContent(
    // 状态码
    val code: Int,
    // 总页数
    val pagecount: Int,
    // 总视频数
    val total: Int,
    // 当前页码
    val page: Int,
    // 状态描述
    val msg: String,
    // 每页视频数
    val limit: Int,
    // 视频列表
    val list: List<Video>
)

data class Video(
    // 视频 id
    val vod_id: String,
    // 视频名称
    val vod_name: String,
    // 视频封面
    val vod_pic: String,
    // 视频描述
    val vod_remarks: String,
    // 分类 id
    val type_id: String,
    // 分类名称
    val type_name: String,
    // 演员
    val vod_actor: String,
    // 地区
    // 地区
    val vod_area: String,
    // 内容
    val vod_content: String,
    // 导演
    val vod_director: String,
    // 语言
    val vod_lang: String,
    // 播放来源
    val vod_play_from: String,
    // 播放地址
    val vod_play_url: String,
    // 状态
    val vod_state: String,
    // 标签
    val vod_tag: String,
    // 发布时间
    val vod_time: String,
    // 年份
    val vod_year: String,
    // 评分
    val vod_score: String = "",
) {
    // 视频来源
    var key = ""

    val playInfos: List<PlayInfo>
        get() {
            val playFlags = vod_play_from.split("\$\$\$")
            val playUrls = vod_play_url.split("\$\$\$")
            return mutableListOf<PlayInfo>().apply {
                for ((i, playUrl) in playUrls.withIndex()) {
                    if (playUrl.isEmpty()) {
                        continue
                    }
                    val flag = if (i >= playFlags.size) {
                        "线路${i + 1}"
                    } else {
                        if (playFlags[i].isEmpty()) "线路${i + 1}" else playFlags[i]
                    }
                    val str = if (playUrl.contains("#")) {
                        playUrl.split("#")
                    } else {
                        listOf(playUrl)
                    }
                    val urlInfos = mutableListOf<UrlInfo>().apply {
                        for ((i, s) in str.withIndex()) {
                            val ss = s.split("$", limit = 2)
                            when {
                                ss.size > 1 -> {
                                    add(UrlInfo(ss[0], ss[1]))
                                }
                                ss.isNotEmpty() -> {
                                    add(UrlInfo((i + 1).toString(), ss[0]))
                                }
                            }
                        }
                    }
                    add(PlayInfo(flag, playUrl, urlInfos))
                }
            }
        }
}

data class PlayInfo(
    // 视频播放来源
    val flag: String,
    // 视频播放地址
    val playUrl: String,
    // 视频播放地址列表
    val urlInfos: List<UrlInfo>
)

data class UrlInfo(
    val name: String,
    val url: String
)
