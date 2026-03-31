package com.loong.longtv.core.spider

import android.content.Context
import com.github.catvod.crawler.Spider
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.loong.longtv.data.bean.CategoryContent
import com.loong.longtv.data.bean.Filter
import com.loong.longtv.data.bean.FilterValue
import com.loong.longtv.data.bean.HomeContent
import com.loong.longtv.data.bean.HomeTabItem
import com.loong.longtv.data.bean.PlayerContent
import com.loong.longtv.data.bean.Video
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.HashMap

open class AppV2Spider(
    private val defaultHost: String = "",
    private val defaultHeaders: Map<String, String> = mapOf("User-Agent" to "okhttp/4.12.0")
) : Spider() {

    protected val gson = Gson()
    protected val client = OkHttpClient()
    protected var host: String = defaultHost

    override fun init(context: Context?, extend: String?) {
        host = resolveHost(extend).ifBlank { defaultHost }.trimEnd('/')
    }

    override fun homeContent(filter: Boolean): String {
        val content = runCatching {
            val root = getJson("$host/api.php/app/nav?token=").asJsonObject
            val classes = mutableListOf<HomeTabItem>()
            val filters = linkedMapOf<String, List<Filter>>()
            val filterNameMap = mapOf(
                "class" to "类型",
                "area" to "地区",
                "lang" to "语言",
                "year" to "年份",
                "letter" to "字母",
                "by" to "排序",
                "sort" to "排序"
            )

            root.getAsJsonArray("list").forEach { itemElement ->
                val item = itemElement.asJsonObject
                val typeId = item.get("type_id").asString
                classes += HomeTabItem(
                    type_id = typeId,
                    type_name = item.get("type_name").asString
                )

                val typeExtend = item.getAsJsonObject("type_extend")
                val siteFilters = mutableListOf<Filter>()
                filterNameMap.forEach { (key, label) ->
                    val value = typeExtend?.get(key)?.asString.orEmpty().trim()
                    if (value.isBlank()) return@forEach
                    val values = value.split(",")
                        .mapNotNull { raw ->
                            raw.trim().takeIf { it.isNotEmpty() }?.let { FilterValue(it, it) }
                        }
                    if (values.isNotEmpty()) {
                        siteFilters += Filter(key, label, values)
                    }
                }
                if (siteFilters.isNotEmpty()) filters[typeId] = siteFilters
            }

            HomeContent(tabs = classes, filters = filters)
        }.getOrElse {
            HomeContent(tabs = defaultHomeTabs(), filters = emptyMap())
        }

        return gson.toJson(content)
    }

    override fun homeVideoContent(): String {
        val directHome = runCatching {
            val root = getJson("$host/api.php/app/index_video?token=").asJsonObject
            val list = mutableListOf<Video>()
            root.getAsJsonArray("list").forEach { block ->
                block.asJsonObject.getAsJsonArray("vlist").forEach { item ->
                    list += toVideo(item.asJsonObject)
                }
            }
            pageOf(list)
        }.getOrNull()
        if (directHome != null && directHome.list.isNotEmpty()) {
            return gson.toJson(directHome)
        }

        val fallbackPage = defaultHomeTabs()
            .asSequence()
            .mapNotNull { tab: HomeTabItem ->
                runCatching {
                    gson.fromJson(
                        categoryContent(tab.type_id, "1", false, hashMapOf()),
                        CategoryContent::class.java
                    )
                }.getOrNull()
            }
            .firstOrNull { page: CategoryContent -> page.list.isNotEmpty() }
            ?: pageOf(emptyList())
        return gson.toJson(fallbackPage)
    }

    override fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String {
        val url = requireNotNull("$host/api.php/app/video".toHttpUrlOrNull()).newBuilder()
            .addQueryParameter("pg", pg)
            .addQueryParameter("tid", tid)
            .addQueryParameter("class", extend["class"].orEmpty())
            .addQueryParameter("area", extend["area"].orEmpty())
            .addQueryParameter("lang", extend["lang"].orEmpty())
            .addQueryParameter("year", extend["year"].orEmpty())
            .addQueryParameter("token", "")
            .build()
            .toString()
        val root = getJson(url).asJsonObject
        val items = root.getAsJsonArray("list").map { toVideo(it.asJsonObject) }
        return gson.toJson(
            CategoryContent(
                code = root.get("code")?.asInt ?: 1,
                pagecount = root.get("pagecount")?.asInt ?: 1,
                total = root.get("total")?.asInt ?: items.size,
                page = root.get("page")?.asInt ?: pg.toIntOrNull() ?: 1,
                msg = root.get("msg")?.asString.orEmpty(),
                limit = root.get("limit")?.asInt ?: items.size,
                list = items
            )
        )
    }

    override fun detailContent(ids: MutableList<String>?): String {
        val id = ids?.firstOrNull().orEmpty()
        val url = "$host/api.php/app/video_detail?id=${URLEncoder.encode(id, "UTF-8")}&token="
        val root = getJson(url).asJsonObject
        val data = root.getAsJsonObject("data").deepCopy()
        data.remove("pause_advert_list")
        data.remove("init_advert_list")
        data.remove("vod_url_with_player")
        val video = toVideo(data)
        return gson.toJson(pageOf(listOf(video)))
    }

    override fun searchContent(key: String?, quick: Boolean): String {
        return searchContent(key, quick, "1")
    }

    override fun searchContent(key: String?, quick: Boolean, pg: String?): String {
        val page = pg ?: "1"
        val encodedKey = URLEncoder.encode(key.orEmpty(), "UTF-8")
        val candidates = listOf(
            "$host/api.php/app/search?text=$encodedKey&pg=$page&token=",
            "$host/api.php/app/search?wd=$encodedKey&pg=$page&token=",
            "$host/api.php/app/search?keywords=$encodedKey&pg=$page&token="
        )
        val root = candidates.firstNotNullOfOrNull { url ->
            runCatching { getJson(url).asJsonObject }.getOrNull()
        } ?: return gson.toJson(pageOf(emptyList()))
        val items = extractVideoArray(root).map { toVideo(it.asJsonObject) }
        return gson.toJson(
            CategoryContent(
                code = 1,
                pagecount = root.get("pagecount")?.asInt ?: 1,
                total = root.get("total")?.asInt ?: items.size,
                page = page.toIntOrNull() ?: 1,
                msg = root.get("msg")?.asString.orEmpty(),
                limit = items.size,
                list = items
            )
        )
    }

    protected fun extractVideoArray(root: JsonObject): List<JsonElement> {
        val directArrayFields = listOf("list", "data", "searchList", "search_list")
        directArrayFields.forEach { field ->
            val value = root.get(field)
            if (value != null && value.isJsonArray) return value.asJsonArray.toList()
            if (value != null && value.isJsonObject) {
                val obj = value.asJsonObject
                directArrayFields.forEach nested@{ nestedField ->
                    val nestedValue = obj.get(nestedField)
                    if (nestedValue != null && nestedValue.isJsonArray) return nestedValue.asJsonArray.toList()
                }
            }
        }
        return emptyList()
    }

    override fun playerContent(flag: String?, id: String?, vipFlags: MutableList<String>?): String {
        return gson.toJson(
            PlayerContent(
                parse = 0,
                url = id.orEmpty(),
                header = playbackHeaders(),
                playUrl = id.orEmpty()
            )
        )
    }

    protected open fun playbackHeaders(): Map<String, String> = requestHeaders()

    protected fun requestHeaders(): Map<String, String> = defaultHeaders

    protected fun getJson(url: String): JsonElement {
        val requestBuilder = Request.Builder().url(url)
        requestHeaders().forEach { (key, value) -> requestBuilder.header(key, value) }
        client.newCall(requestBuilder.build()).execute().use { response ->
            return JsonParser.parseString(response.body?.string().orEmpty())
        }
    }

    protected fun pageOf(items: List<Video>): CategoryContent {
        return CategoryContent(
            code = 1,
            pagecount = 1,
            total = items.size,
            page = 1,
            msg = "",
            limit = items.size,
            list = items
        )
    }

    protected open fun defaultHomeTabs(): List<HomeTabItem> {
        return listOf(
            HomeTabItem(type_id = "1", type_name = "电影"),
            HomeTabItem(type_id = "2", type_name = "剧集"),
            HomeTabItem(type_id = "3", type_name = "综艺"),
            HomeTabItem(type_id = "4", type_name = "动漫")
        )
    }

    protected fun toVideo(obj: JsonObject): Video {
        return Video(
            vod_id = readString(obj, "vod_id", "id"),
            vod_name = readString(obj, "vod_name", "vod_title", "name"),
            vod_pic = normalizeUrl(readString(obj, "vod_pic", "pic", "cover")),
            vod_remarks = readString(obj, "vod_remarks", "remarks", "last", "totalEpisode"),
            type_id = readString(obj, "type_id", "tid"),
            type_name = readString(obj, "type_name", "type"),
            vod_actor = readString(obj, "vod_actor", "actor"),
            vod_area = readString(obj, "vod_area", "area"),
            vod_content = readString(obj, "vod_content", "content", "vod_blurb", "intro"),
            vod_director = readString(obj, "vod_director", "director"),
            vod_lang = readString(obj, "vod_lang", "lang"),
            vod_play_from = readString(obj, "vod_play_from"),
            vod_play_url = readString(obj, "vod_play_url"),
            vod_state = readString(obj, "vod_state", "state"),
            vod_tag = readString(obj, "vod_tag", "tag"),
            vod_time = readString(obj, "vod_time", "time"),
            vod_year = readString(obj, "vod_year", "year")
        )
    }

    protected fun readString(obj: JsonObject, vararg names: String): String {
        names.forEach { name ->
            val value = obj.get(name) ?: return@forEach
            if (!value.isJsonNull) return value.asString.orEmpty()
        }
        return ""
    }

    protected fun normalizeUrl(url: String): String {
        if (url.isBlank()) return url
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://")) return url
        val trimmed = url.trimStart('/')
        return "$host/$trimmed"
    }

    private fun resolveHost(extend: String?): String {
        if (extend.isNullOrBlank()) return defaultHost
        val cleaned = extend.trim()
        return runCatching {
            val element = JsonParser.parseString(cleaned)
            when {
                element.isJsonPrimitive -> element.asString
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    readString(obj, "host", "url", "site", "siteUrl")
                }
                else -> defaultHost
            }
        }.getOrElse {
            cleaned.trim('"')
        }
    }
}
