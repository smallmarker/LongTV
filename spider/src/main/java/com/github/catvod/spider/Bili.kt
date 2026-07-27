package com.github.catvod.spider

import android.content.Context
import android.util.Log
import com.github.catvod.crawler.Spider
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Bilibili 少儿教育 Spider.
 *
 * ext格式:
 * {
 *   "json": "./json/少儿教育.json",
 *   "cookie": "http://127.0.0.1:9978/file/TVBox/bili_cookie.txt"
 * }
 */
class Bili : Spider() {

    private lateinit var context: Context
    private var cookieUrl = ""
    private var cookie = ""
    private var cookieLoaded = false
    private var config = BiliConfig.default()
    private val gson = Gson()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    override fun init(context: Context, extend: String) {
        this.context = context.applicationContext
        runCatching {
            val ext = JSONObject(extend)
            cookieUrl = ext.optString("cookie")
            val jsonPath = ext.optString("json")
            config = readConfig(jsonPath).getOrElse { BiliConfig.default() }
        }.onFailure {
            config = BiliConfig.default()
        }
        Log.d(TAG, "Bili initialized classes=${config.classes.size}")
    }

    override fun homeContent(filter: Boolean): String {
        return gson.toJson(
            mapOf(
                "class" to config.classes.map { item ->
                    mapOf("type_id" to item.id, "type_name" to item.name)
                },
                "filters" to emptyMap<String, Any>()
            )
        )
    }

    override fun homeVideoContent(): String {
        val items = config.home.flatMap { keyword ->
            searchVideos(keyword, 1, HOME_KEYWORD_LIMIT)
        }.distinctBy { it["vod_id"] }.take(HOME_LIMIT)
        return listResult(1, 1, items.size, items)
    }

    override fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String {
        val page = pg.toIntOrNull()?.takeIf { it > 0 } ?: 1
        val keyword = config.classes.firstOrNull { it.id == tid }?.keyword ?: tid
        val items = searchVideos(keyword, page, PAGE_LIMIT)
        return listResult(page, if (items.isEmpty()) page else page + 1, items.size, items)
    }

    override fun detailContent(ids: List<String>): String {
        val bvid = ids.firstOrNull().orEmpty().substringBefore("|")
        if (bvid.isBlank()) return emptyListResult()
        val root = getJson("https://api.bilibili.com/x/web-interface/view?bvid=$bvid")
        val data = root?.optJSONObject("data") ?: return emptyListResult()
        val pages = data.optJSONArray("pages") ?: JSONArray()
        val episodes = mutableListOf<String>()
        for (i in 0 until pages.length()) {
            val page = pages.optJSONObject(i) ?: continue
            val cid = page.optString("cid")
            if (cid.isBlank()) continue
            val name = clean(page.optString("part")).ifBlank { "P${page.optInt("page", i + 1)}" }
            episodes.add("${name}\$${bvid}|${cid}")
        }
        val item = linkedMapOf<String, Any>(
            "vod_id" to bvid,
            "vod_name" to clean(data.optString("title")),
            "vod_pic" to absoluteImage(data.optString("pic")),
            "vod_remarks" to "共${pages.length()}P",
            "type_name" to data.optString("tname"),
            "vod_year" to "",
            "vod_area" to "Bilibili",
            "vod_actor" to data.optJSONObject("owner")?.optString("name").orEmpty(),
            "vod_director" to data.optJSONObject("owner")?.optString("name").orEmpty(),
            "vod_content" to clean(data.optString("desc")),
            "vod_play_from" to "B站",
            "vod_play_url" to episodes.joinToString("#")
        )
        return gson.toJson(mapOf("list" to listOf(item)))
    }

    override fun searchContent(key: String, quick: Boolean): String = emptyListResult()

    override fun searchContent(key: String, quick: Boolean, pg: String): String = emptyListResult()

    override fun playerContent(flag: String, id: String, vipFlags: List<String>): String {
        val parts = id.split("|", limit = 2)
        val bvid = parts.getOrNull(0).orEmpty()
        val cid = parts.getOrNull(1).orEmpty()
        if (bvid.isBlank() || cid.isBlank()) return playerResult("", parse = 1)
        val url = "https://api.bilibili.com/x/player/playurl" +
            "?bvid=$bvid&cid=$cid&qn=32&fnval=0&fourk=0&platform=html5&high_quality=1"
        val data = getJson(url)?.optJSONObject("data")
        val durl = data?.optJSONArray("durl")
        val playUrls = buildList {
            val first = durl?.optJSONObject(0)
            first?.optString("url")?.takeIf { it.isNotBlank() }?.let(::add)
            val backups = first?.optJSONArray("backup_url")
            if (backups != null) {
                for (i in 0 until backups.length()) {
                    backups.optString(i).takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }.distinct()
        val headers = playerHeaders(bvid)
        val playUrl = playUrls.firstOrNull { isPlayable(it, headers) } ?: playUrls.firstOrNull().orEmpty()
        return if (playUrl.isNotBlank()) playerResult(playUrl, headers = headers) else playerResult(id, parse = 1)
    }

    private fun searchVideos(keyword: String, page: Int, limit: Int): List<Map<String, String>> {
        if (keyword.isBlank()) return emptyList()
        val url = "https://api.bilibili.com/x/web-interface/search/type" +
            "?search_type=video&keyword=${urlEncode(keyword)}&page=$page&page_size=$limit"
        val result = getJson(url)?.optJSONObject("data")?.optJSONArray("result") ?: return emptyList()
        val items = mutableListOf<Map<String, String>>()
        for (i in 0 until result.length()) {
            val item = result.optJSONObject(i) ?: continue
            val bvid = item.optString("bvid")
            val name = cleanHtml(item.optString("title"))
            if (bvid.isBlank() || name.isBlank() || item.optInt("is_charge_video", 0) != 0) continue
            items.add(
                mapOf(
                    "vod_id" to bvid,
                    "vod_name" to name,
                    "vod_pic" to absoluteImage(item.optString("pic")),
                    "vod_remarks" to clean(item.optString("duration")),
                    "vod_actor" to clean(item.optString("author")),
                    "vod_content" to cleanHtml(item.optString("description"))
                )
            )
        }
        return items
    }

    private fun getJson(url: String): JSONObject? {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .header("Referer", "https://www.bilibili.com/")
                .apply {
                    val value = loadCookie()
                    if (value.isNotBlank()) header("Cookie", value)
                }
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string()?.let(::JSONObject) else null
            }
        }.onFailure { Log.e(TAG, "request failed: $url", it) }.getOrNull()
    }

    private fun loadCookie(): String {
        if (cookie.isNotBlank()) return cookie
        if (cookieLoaded) return ""
        if (cookieUrl.isBlank()) return ""
        cookieLoaded = true
        cookie = runCatching {
            val request = Request.Builder().url(cookieUrl).header("User-Agent", UA).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty().trim() else ""
            }
        }.getOrDefault("")
        return cookie
    }

    private fun readConfig(path: String): Result<BiliConfig> = runCatching {
        val assetPath = path
            .removePrefix("./")
            .let { if (it.startsWith("tvbox/")) it else "tvbox/longtv/$it" }
        val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        BiliConfig.from(JSONObject(json))
    }

    private fun playerResult(url: String, parse: Int = 0, headers: Map<String, String> = playerHeaders()): String {
        return gson.toJson(
            mapOf(
                "parse" to parse,
                "url" to url,
                "header" to headers
            )
        )
    }

    private fun playerHeaders(bvid: String = ""): Map<String, String> {
        val referer = if (bvid.isBlank()) {
            "https://www.bilibili.com/"
        } else {
            "https://www.bilibili.com/video/$bvid/"
        }
        return linkedMapOf(
            "User-Agent" to UA,
            "Referer" to referer,
            "Origin" to "https://www.bilibili.com",
            "Accept" to "*/*"
        ).apply {
            loadCookie().takeIf { it.isNotBlank() }?.let { put("Cookie", it) }
        }
    }

    private fun isPlayable(url: String, headers: Map<String, String>): Boolean {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .headers(headers.toOkHttpHeaders())
                .header("Range", "bytes=0-1")
                .build()
            client.newCall(request).execute().use { response ->
                response.code in 200..299 || response.code == 206
            }
        }.onFailure { Log.w(TAG, "play url probe failed: ${it.message}") }.getOrDefault(false)
    }

    private fun Map<String, String>.toOkHttpHeaders(): okhttp3.Headers {
        val builder = okhttp3.Headers.Builder()
        forEach { (name, value) ->
            if (name.isNotBlank() && value.isNotBlank()) builder.add(name, value)
        }
        return builder.build()
    }

    private fun listResult(page: Int, pageCount: Int, total: Int, items: List<Map<String, String>>): String {
        return gson.toJson(mapOf("page" to page, "pagecount" to pageCount, "limit" to PAGE_LIMIT, "total" to total, "list" to items))
    }

    private fun emptyListResult() = listResult(1, 0, 0, emptyList())

    private fun absoluteImage(url: String): String {
        return when {
            url.startsWith("//") -> "https:$url"
            else -> url
        }
    }

    private fun cleanHtml(text: String): String {
        return clean(text.replace(Regex("<.*?>"), ""))
    }

    private fun clean(text: String): String = text.replace(Regex("\\s+"), " ").trim()

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private data class BiliConfig(
        val home: List<String>,
        val classes: List<BiliClass>
    ) {
        companion object {
            fun from(root: JSONObject): BiliConfig {
                val home = root.optJSONArray("home").toStringList()
                val classes = root.optJSONArray("classes").toClassList()
                return BiliConfig(
                    home = home.ifEmpty { classes.map { it.keyword } },
                    classes = classes.ifEmpty { default().classes }
                )
            }

            fun default(): BiliConfig = BiliConfig(
                home = listOf("宝宝巴士", "幼儿启蒙", "儿童英语"),
                classes = listOf(
                    BiliClass("baby", "幼儿启蒙", "宝宝巴士 幼儿启蒙"),
                    BiliClass("english", "英语启蒙", "儿童英语启蒙"),
                    BiliClass("math", "数学思维", "儿童数学思维"),
                    BiliClass("science", "科普百科", "儿童科普 动画"),
                    BiliClass("story", "绘本故事", "儿童绘本故事")
                )
            )

            private fun JSONArray?.toStringList(): List<String> {
                if (this == null) return emptyList()
                val values = mutableListOf<String>()
                for (i in 0 until length()) optString(i).trim().takeIf { it.isNotBlank() }?.let(values::add)
                return values
            }

            private fun JSONArray?.toClassList(): List<BiliClass> {
                if (this == null) return emptyList()
                val values = mutableListOf<BiliClass>()
                for (i in 0 until length()) {
                    val item = optJSONObject(i) ?: continue
                    val id = item.optString("type_id").ifBlank { item.optString("id") }
                    val name = item.optString("type_name").ifBlank { item.optString("name") }
                    val keyword = item.optString("keyword").ifBlank { name }
                    if (id.isNotBlank() && name.isNotBlank()) values.add(BiliClass(id, name, keyword))
                }
                return values
            }
        }
    }

    private data class BiliClass(val id: String, val name: String, val keyword: String)

    companion object {
        private const val TAG = "Bili"
        private const val PAGE_LIMIT = 20
        private const val HOME_LIMIT = 12
        private const val HOME_KEYWORD_LIMIT = 4
        private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0 Safari/537.36"
    }
}
