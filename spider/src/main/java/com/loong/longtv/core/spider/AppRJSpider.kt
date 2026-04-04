package com.loong.longtv.core.spider

import android.content.Context
import com.github.catvod.crawler.Spider
import com.github.catvod.utils.Util
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.loong.longtv.data.bean.CategoryContent
import com.loong.longtv.data.bean.HomeContent
import com.loong.longtv.data.bean.HomeTabItem
import com.loong.longtv.data.bean.PlayerContent
import com.loong.longtv.data.bean.Video
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.HashMap

open class AppRJSpider(
    private val defaultHost: String = "",
    private val secret: String = "kj5649ertj84ks89r4jh8s45hf84hjfds04k"
) : Spider() {

    protected val gson = Gson()
    protected val client = OkHttpClient()
    protected var host: String = defaultHost

    override fun init(context: Context?, extend: String?) {
        host = resolveHost(extend).ifBlank { defaultHost }.trimEnd('/')
    }

    override fun homeContent(filter: Boolean): String {
        return gson.toJson(
            HomeContent(
                tabs = classTabs(),
                filters = emptyMap()
            )
        )
    }

    override fun homeVideoContent(): String {
        return categoryContent(featuredTypeId(), "1", false, hashMapOf())
    }

    override fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String {
        val root = post("/v2/home/type_search") {
            add("type_id", tid.ifBlank { defaultHomeTypeId() })
            add("page", pg)
        }.asJsonObject
        val data = root.getAsJsonObject("data")
        val items = data?.getAsJsonArray("list")?.map { toVideo(it.asJsonObject) }.orEmpty()
        val page = pg.toIntOrNull() ?: 1
        return gson.toJson(
            CategoryContent(
                code = root.get("code")?.asInt ?: 1,
                pagecount = inferPageCount(items.size, page),
                total = inferTotal(data, items.size, page),
                page = page,
                msg = root.get("msg")?.asString.orEmpty(),
                limit = items.size,
                list = items
            )
        )
    }

    override fun detailContent(ids: MutableList<String>?): String {
        val id = ids?.firstOrNull().orEmpty()
        val root = post("/v2/home/vod_details") {
            add("vod_id", id)
        }.asJsonObject
        val data = root.getAsJsonObject("data") ?: JsonObject()
        val video = toVideo(data)
        val vodPlayList = data.getAsJsonArray("vod_play_list")
        if (vodPlayList != null && vodPlayList.size() > 0) {
            val playFrom = mutableListOf<String>()
            val playUrls = mutableListOf<String>()
            vodPlayList.forEach { item ->
                val source = item.asJsonObject
                val flag = readString(source, "flag", "from", "player_info", "player_name")
                if (!shouldKeepPlaySource(flag)) return@forEach
                val urls = source.getAsJsonArray("urls") ?: return@forEach
                val parsePrefix = firstString(source.getAsJsonArray("parse_urls"))
                    ?: readString(source, "parse_url", "parse")
                val episodes = urls.mapNotNull { urlElement ->
                    val urlObject = urlElement.asJsonObject
                    val episodeName = readString(urlObject, "name", "title").ifBlank { "播放" }
                    val playTarget = readString(urlObject, "url", "play_url", "link")
                    if (playTarget.isBlank()) return@mapNotNull null
                    val finalTarget = buildPlayTarget(parsePrefix.orEmpty(), playTarget, episodeName)
                    "$episodeName$$finalTarget"
                }
                if (episodes.isNotEmpty()) {
                    playFrom += flag.ifBlank { "线路${playFrom.size + 1}" }
                    playUrls += episodes.joinToString("#")
                }
            }
            return gson.toJson(pageOf(listOf(video.copy(vod_play_from = playFrom.joinToString("$$$"), vod_play_url = playUrls.joinToString("$$$")))))
        }
        return gson.toJson(pageOf(listOf(video)))
    }

    override fun searchContent(key: String?, quick: Boolean): String {
        return searchContent(key, quick, "1")
    }

    override fun searchContent(key: String?, quick: Boolean, pg: String?): String {
        val page = pg ?: "1"
        val root = post("/v2/home/search") {
            add("keyword", key.orEmpty())
            add("page", page)
        }.asJsonObject
        val data = root.getAsJsonObject("data")
        val items = data?.getAsJsonArray("list")?.map { toVideo(it.asJsonObject) }.orEmpty()
        val currentPage = page.toIntOrNull() ?: 1
        return gson.toJson(
            CategoryContent(
                code = root.get("code")?.asInt ?: 1,
                pagecount = inferPageCount(items.size, currentPage),
                total = inferTotal(data, items.size, currentPage),
                page = currentPage,
                msg = root.get("msg")?.asString.orEmpty(),
                limit = items.size,
                list = items
            )
        )
    }

    override fun playerContent(flag: String?, id: String?, vipFlags: MutableList<String>?): String {
        val rawInput = id.orEmpty()
        val episodeName = rawInput.substringAfter("~", "")
        val candidate = rawInput.substringBefore("~")
        val direct = MediaUrlHelper.extractDirectMediaUrl(candidate)
        if (direct != null) {
            return gson.toJson(
                PlayerContent(
                    parse = 0,
                    url = direct,
                    playUrl = direct,
                    header = playbackHeaders()
                )
            )
        }

        val resolved = runCatching { resolvePlayback(candidate) }.getOrNull()
        if (!resolved.isNullOrBlank()) {
            return gson.toJson(
                PlayerContent(
                    parse = 0,
                    url = resolved,
                    playUrl = resolved,
                    header = playbackHeaders()
                )
            )
        }

        val extracted = extractFallbackPlaybackUrl(candidate)
        if (!extracted.isNullOrBlank()) {
            return gson.toJson(
                PlayerContent(
                    parse = 1,
                    url = extracted,
                    playUrl = extracted,
                    header = playbackHeaders()
                )
            )
        }

        return gson.toJson(
            PlayerContent(
                parse = 1,
                url = candidate,
                playUrl = candidate,
                header = playbackHeaders() + mapOf("X-LongTV-Episode" to episodeName)
            )
        )
    }

    protected open fun playbackHeaders(): Map<String, String> = buildMap {
        put("User-Agent", "okhttp/3.14.9")
        if (host.isNotBlank()) {
            put("Referer", "$host/")
            put("Origin", host)
        }
    }

    protected open fun requestHeaders(): Map<String, String> = mapOf(
        "User-Agent" to "okhttp-okgo/jeasonlzy",
        "Content-Type" to "application/x-www-form-urlencoded",
        "Accept" to "application/json, text/plain, */*"
    )

    protected open fun classTabs(): List<HomeTabItem> {
        val defaultNames = listOf("电影", "电视剧", "综艺", "动漫")
        val defaultIds = listOf("1", "2", "4", "3")
        val configured = readConfiguredClasses()
        if (configured.isNotEmpty()) return configured
        return defaultNames.zip(defaultIds).map { (name, id) ->
            HomeTabItem(type_id = id, type_name = name)
        }
    }

    protected open fun featuredTypeId(): String = "0"

    protected open fun defaultHomeTypeId(): String = classTabs().firstOrNull()?.type_id ?: "1"

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

    protected fun toVideo(obj: JsonObject): Video {
        return Video(
            vod_id = readString(obj, "vod_id", "id"),
            vod_name = readString(obj, "vod_name", "name", "title"),
            vod_pic = normalizeUrl(readString(obj, "vod_pic", "pic", "cover")),
            vod_remarks = readString(obj, "vod_remarks", "remarks"),
            type_id = readString(obj, "type_id", "tid"),
            type_name = readString(obj, "vod_class", "type_name", "type"),
            vod_actor = readString(obj, "vod_actor", "actor"),
            vod_area = readString(obj, "vod_area", "area"),
            vod_content = readString(obj, "vod_content", "content", "vod_blurb"),
            vod_director = readString(obj, "vod_director", "director"),
            vod_lang = readString(obj, "vod_lang", "lang"),
            vod_play_from = readString(obj, "vod_play_from"),
            vod_play_url = readString(obj, "vod_play_url"),
            vod_state = readString(obj, "vod_state", "state"),
            vod_tag = readString(obj, "vod_tag", "tag"),
            vod_time = readString(obj, "vod_time", "time"),
            vod_year = readString(obj, "vod_year", "year"),
            vod_score = readString(obj, "vod_score", "score", "vod_douban_score", "douban_score", "rating")
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

    protected fun inferPageCount(itemCount: Int, currentPage: Int): Int {
        return if (itemCount >= 20) currentPage + 1 else currentPage
    }

    protected fun inferTotal(data: JsonObject?, itemCount: Int, currentPage: Int): Int {
        val reported = listOf("total", "count", "records", "total_count")
            .firstNotNullOfOrNull { key -> data?.get(key)?.takeIf { !it.isJsonNull }?.asInt }
        return reported ?: (itemCount + ((currentPage - 1).coerceAtLeast(0) * itemCount))
    }

    protected fun post(path: String, fillBody: FormBody.Builder.() -> Unit): JsonElement {
        val timestamp = (System.currentTimeMillis() / 1000L).toString()
        val form = FormBody.Builder()
            .add("sign", Util.md5(secret + timestamp))
            .add("timestamp", timestamp)
            .apply(fillBody)
            .build()
        val requestBuilder = Request.Builder()
            .url("$host$path")
            .post(form)
        requestHeaders().forEach { (key, value) -> requestBuilder.header(key, value) }
        client.newCall(requestBuilder.build()).execute().use { response ->
            return JsonParser.parseString(response.body?.string().orEmpty())
        }
    }

    protected fun get(url: String, headers: Map<String, String> = playbackHeaders()): String {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (key, value) -> requestBuilder.header(key, value) }
        client.newCall(requestBuilder.build()).execute().use { response ->
            return response.body?.string().orEmpty()
        }
    }

    protected open fun shouldKeepPlaySource(flag: String): Boolean = flag.isNotBlank()

    protected open fun buildPlayTarget(parsePrefix: String, playTarget: String, episodeName: String): String {
        return buildString {
            append(parsePrefix)
            append(playTarget)
            append("~")
            append(episodeName)
        }
    }

    private fun resolvePlayback(candidate: String): String? {
        if (candidate.isBlank()) return null
        MediaUrlHelper.extractDirectMediaUrl(candidate)?.let { return it }
        val body = get(candidate)
        if (body.isBlank()) return null
        MediaUrlHelper.extractDirectMediaUrl(body)?.let { return it }
        extractMediaUrlFromJson(body)?.let { return it }
        extractFallbackPlaybackUrl(candidate)?.let { return it }
        return null
    }

    private fun extractMediaUrlFromJson(body: String): String? {
        val root = runCatching { JsonParser.parseString(body) }.getOrNull() ?: return null
        return findMediaUrl(root)
    }

    private fun findMediaUrl(element: JsonElement?): String? {
        if (element == null || element.isJsonNull) return null
        if (element.isJsonPrimitive) {
            return MediaUrlHelper.extractDirectMediaUrl(element.asString)
        }
        if (element.isJsonArray) {
            element.asJsonArray.forEach { item ->
                findMediaUrl(item)?.let { return it }
            }
            return null
        }
        val obj = element.asJsonObject
        listOf("url", "playUrl", "link", "video").forEach { key ->
            findMediaUrl(obj.get(key))?.let { return it }
        }
        findMediaUrl(obj.get("data"))?.let { return it }
        return null
    }

    private fun extractFallbackPlaybackUrl(candidate: String): String? {
        val fromQuery = runCatching {
            val raw = candidate.substringAfter("url=", "")
            if (raw.isBlank()) null else URLDecoder.decode(raw, "UTF-8")
        }.getOrNull()
        MediaUrlHelper.extractDirectMediaUrl(fromQuery)?.let { return it }
        return MediaUrlHelper.extractDirectMediaUrl(candidate)
    }

    private fun readConfiguredClasses(): List<HomeTabItem> {
        val extendValue = extendConfig ?: return emptyList()
        val names = when {
            extendValue.isJsonObject -> splitValues(readString(extendValue.asJsonObject, "class_name", "className", "classes"))
            else -> emptyList()
        }
        val ids = when {
            extendValue.isJsonObject -> splitValues(readString(extendValue.asJsonObject, "class_url", "classUrl", "class_ids"))
            else -> emptyList()
        }
        if (names.isEmpty() || ids.isEmpty()) return emptyList()
        return names.zip(ids).map { (name, id) -> HomeTabItem(type_id = id, type_name = name) }
    }

    private var extendConfig: JsonElement? = null

    private fun resolveHost(extend: String?): String {
        if (extend.isNullOrBlank()) return defaultHost
        val cleaned = extend.trim()
        return runCatching {
            val element = JsonParser.parseString(cleaned)
            extendConfig = element
            when {
                element.isJsonPrimitive -> element.asString
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    readString(obj, "host", "url", "site", "siteUrl")
                }
                else -> defaultHost
            }
        }.getOrElse {
            extendConfig = null
            cleaned.trim('"')
        }
    }

    private fun splitValues(raw: String): List<String> {
        return raw.split("&", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun firstString(array: JsonArray?): String? {
        if (array == null || array.size() == 0) return null
        val value = array.firstOrNull() ?: return null
        if (value.isJsonNull) return null
        return value.asString
    }
}
