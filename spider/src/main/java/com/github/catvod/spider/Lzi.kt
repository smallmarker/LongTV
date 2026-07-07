package com.github.catvod.spider

import android.content.Context
import android.util.Log
import com.github.catvod.crawler.Spider
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 量子资源站 Spider.
 *
 * ext格式:
 * {
 *   "url": "http://cj.lziapi.com/api.php/provide/vod/",
 *   "playurl": "json:https://lziplayer.com/?url=",
 *   "categories": ["国产剧", "国产动漫", ...]
 * }
 */
class Lzi : Spider() {

    private var apiUrl = DEFAULT_API
    private var parsePrefix = DEFAULT_PARSE
    private var preferredCategories = DEFAULT_CATEGORIES
    private val gson = Gson()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    override fun init(context: Context, extend: String) {
        runCatching {
            val ext = JSONObject(extend)
            apiUrl = ext.optString("url", DEFAULT_API).trim().ifBlank { DEFAULT_API }
            parsePrefix = ext.optString("playurl", DEFAULT_PARSE).trim()
            preferredCategories = ext.optJSONArray("categories")?.toStringList()?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_CATEGORIES
        }.onFailure {
            apiUrl = extend.trim().ifBlank { DEFAULT_API }
        }
        Log.d(TAG, "Lzi initialized with apiUrl=$apiUrl")
    }

    override fun homeContent(filter: Boolean): String {
        val root = getJson(mapOf("ac" to "list")) ?: return emptyHomeContent()
        val classes = parseClasses(root)
        val selected = preferredCategories.mapNotNull { name ->
            classes.firstOrNull { it.name == name }
                ?: classes.firstOrNull { normalizeCategory(it.name) == normalizeCategory(name) }
        }.distinctBy { it.id }.ifEmpty { classes }
        return gson.toJson(
            mapOf(
                "class" to selected.map { mapOf("type_id" to it.id, "type_name" to it.name) },
                "filters" to emptyMap<String, Any>()
            )
        )
    }

    override fun homeVideoContent(): String {
        val root = getJson(mapOf("ac" to "videolist", "pg" to "1")) ?: return emptyListResult()
        return parseListResult(root)
    }

    override fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String {
        val page = pg.toIntOrNull()?.takeIf { it > 0 } ?: 1
        if (tid == "0") return homeVideoContent()
        val root = getJson(mapOf("ac" to "videolist", "t" to tid, "pg" to page.toString()))
            ?: return emptyListResult(page)
        return parseListResult(root)
    }

    override fun detailContent(ids: List<String>): String {
        val id = ids.firstOrNull().orEmpty()
        if (id.isBlank()) return emptyListResult()
        val root = getJson(mapOf("ac" to "videolist", "ids" to id)) ?: return emptyListResult()
        val list = root.optJSONArray("list") ?: return emptyListResult()
        val item = list.optJSONObject(0) ?: return emptyListResult()
        return gson.toJson(mapOf("list" to listOf(parseVod(item, detail = true))))
    }

    override fun searchContent(key: String, quick: Boolean): String = searchContent(key, quick, "1")

    override fun searchContent(key: String, quick: Boolean, pg: String): String {
        val page = pg.toIntOrNull()?.takeIf { it > 0 } ?: 1
        val root = getJson(mapOf("ac" to "videolist", "wd" to key, "pg" to page.toString()))
            ?: return emptyListResult(page)
        return parseListResult(root)
    }

    override fun playerContent(flag: String, id: String, vipFlags: List<String>): String {
        val url = id.trim()
        return when {
            isDirectVideo(url) -> playerResult(url)
            parsePrefix.startsWith("json:") -> playerResult(parsePrefix.removePrefix("json:") + urlEncode(url), parse = 1)
            parsePrefix.isNotBlank() -> playerResult(parsePrefix + urlEncode(url), parse = 1)
            else -> playerResult(url, parse = 1)
        }
    }

    private fun getJson(params: Map<String, String>): JSONObject? {
        val url = buildUrl(params)
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string()?.let(::JSONObject) else null
            }
        }.onFailure { Log.e(TAG, "request failed: $url", it) }.getOrNull()
    }

    private fun buildUrl(params: Map<String, String>): String {
        val separator = if (apiUrl.contains("?")) "&" else "?"
        return apiUrl.trimEnd('?') + separator + params.entries.joinToString("&") { (key, value) ->
            "$key=${urlEncode(value)}"
        }
    }

    private fun parseListResult(root: JSONObject): String {
        val page = root.optFlexibleInt("page", 1)
        val pageCount = root.optFlexibleInt("pagecount", 1)
        val limit = root.optFlexibleInt("limit", LIMIT)
        val total = root.optFlexibleInt("total", 0)
        val list = root.optJSONArray("list") ?: JSONArray()
        val items = mutableListOf<Map<String, Any>>()
        for (i in 0 until list.length()) {
            val item = list.optJSONObject(i) ?: continue
            items.add(parseVod(item, detail = false))
        }
        return gson.toJson(
            mapOf(
                "page" to page,
                "pagecount" to pageCount,
                "limit" to limit,
                "total" to total,
                "list" to items
            )
        )
    }

    private fun parseVod(item: JSONObject, detail: Boolean): Map<String, Any> {
        val vod = linkedMapOf<String, Any>(
            "vod_id" to item.optString("vod_id"),
            "vod_name" to item.optString("vod_name"),
            "vod_pic" to item.optString("vod_pic"),
            "vod_remarks" to item.optString("vod_remarks"),
            "type_id" to item.optString("type_id"),
            "type_name" to item.optString("type_name"),
            "vod_year" to item.optString("vod_year"),
            "vod_area" to item.optString("vod_area"),
            "vod_lang" to item.optString("vod_lang"),
            "vod_score" to item.optScore(),
            "vod_time" to item.optString("vod_time")
        )
        if (detail) {
            val play = reorderPlayLines(item.optString("vod_play_from"), item.optString("vod_play_url"))
            vod["vod_actor"] = item.optString("vod_actor")
            vod["vod_director"] = item.optString("vod_director")
            vod["vod_content"] = Jsoup.parse(item.optString("vod_content")).text()
            vod["vod_play_from"] = play.first
            vod["vod_play_url"] = play.second
        }
        return vod
    }

    private fun parseClasses(root: JSONObject): List<Category> {
        val list = root.optJSONArray("class") ?: return emptyList()
        val classes = mutableListOf<Category>()
        for (i in 0 until list.length()) {
            val item = list.optJSONObject(i) ?: continue
            val id = item.optString("type_id")
            val name = item.optString("type_name")
            if (id.isNotBlank() && name.isNotBlank()) classes.add(Category(id, name))
        }
        return classes
    }

    private fun reorderPlayLines(playFrom: String, playUrl: String): Pair<String, String> {
        val flags = playFrom.split("\$\$\$").filter { it.isNotBlank() }
        val urls = playUrl.split("\$\$\$")
        if (flags.size <= 1) return playFrom to playUrl
        val order = flags.indices.sortedBy { index ->
            val flag = flags.getOrNull(index).orEmpty()
            val url = urls.getOrNull(index).orEmpty()
            if (flag.contains("m3u8", true) || url.contains(".m3u8", true)) 0 else 1
        }
        return order.joinToString("\$\$\$") { flags[it] } to
            order.joinToString("\$\$\$") { urls.getOrElse(it) { "" } }
    }

    private fun playerResult(url: String, parse: Int = 0): String {
        return gson.toJson(
            mapOf(
                "parse" to parse,
                "url" to url,
                "header" to mapOf("User-Agent" to UA)
            )
        )
    }

    private fun emptyHomeContent() = gson.toJson(mapOf("class" to emptyList<Any>(), "filters" to emptyMap<String, Any>()))

    private fun emptyListResult(page: Int = 1) =
        gson.toJson(mapOf("page" to page, "pagecount" to 0, "limit" to LIMIT, "total" to 0, "list" to emptyList<Any>()))

    private fun JSONArray.toStringList(): List<String> {
        val values = mutableListOf<String>()
        for (i in 0 until length()) optString(i).trim().takeIf { it.isNotBlank() }?.let(values::add)
        return values
    }

    private fun JSONObject.optFlexibleInt(key: String, default: Int): Int {
        return optString(key).toIntOrNull() ?: optInt(key, default)
    }

    private fun JSONObject.optScore(): String {
        val keys = listOf("vod_score", "vod_douban_score", "score")
        return keys.firstNotNullOfOrNull { key ->
            optString(key).trim().takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
        }.orEmpty()
    }

    private fun normalizeCategory(name: String): String {
        return name.replace("片", "").trim()
    }

    private fun isDirectVideo(url: String): Boolean {
        return url.contains(".m3u8", true) || url.contains(".mp4", true)
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private data class Category(val id: String, val name: String)

    companion object {
        private const val TAG = "Lzi"
        private const val DEFAULT_API = "http://cj.lziapi.com/api.php/provide/vod/"
        private const val DEFAULT_PARSE = "json:https://lziplayer.com/?url="
        private const val LIMIT = 20
        private const val UA = "Mozilla/5.0 (Linux; Android 12; LongTV) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"
        private val DEFAULT_CATEGORIES = listOf(
            "国产剧",
            "国产动漫",
            "泰国剧",
            "台湾剧",
            "香港剧",
            "欧美剧",
            "韩国剧",
            "日本剧",
            "动漫",
            "体育",
            "剧情片",
            "动作片",
            "爱情片",
            "喜剧片"
        )
    }
}
