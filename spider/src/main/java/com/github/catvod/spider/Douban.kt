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
import kotlin.math.ceil

/**
 * 豆瓣片单 Spider.
 *
 * 这个源只提供热门片单和豆瓣资料，不提供播放线路；适合作为首页热门影视的默认展示源。
 */
class Douban : Spider() {

    private val gson = Gson()
    private val detailCache = linkedMapOf<String, JSONObject>()
    private var categories = listOf(FALLBACK_CATEGORY)
    private var homeCollectionId = FALLBACK_CATEGORY.id

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    override fun init(context: Context, extend: String) {
        if (extend.isBlank()) {
            Log.w(TAG, "missing extend categories, use fallback category")
            return
        }
        runCatching {
            val ext = JSONObject(extend)
            categories = ext.optCategoryArray("categories")
                ?: ext.optCategoryArray("collections")
                ?: ext.optCategoryArray("tabs")
                ?: listOf(FALLBACK_CATEGORY).also {
                    Log.w(TAG, "missing categories in extend, use fallback category")
                }
            homeCollectionId = ext.optString("home")
                .ifBlank { ext.optString("homeId") }
                .ifBlank { categories.firstOrNull()?.id.orEmpty() }
                .ifBlank { FALLBACK_CATEGORY.id }
        }.onFailure {
            Log.w(TAG, "parse extend failed, use fallback category: ${it.message}")
            categories = listOf(FALLBACK_CATEGORY)
            homeCollectionId = FALLBACK_CATEGORY.id
        }
    }

    override fun homeContent(filter: Boolean): String {
        return gson.toJson(
            mapOf(
                "class" to categories.map { category ->
                    mapOf("type_id" to category.id, "type_name" to category.name)
                },
                "filters" to emptyMap<String, Any>()
            )
        )
    }

    override fun homeVideoContent(): String {
        val items = loadCollection(homeCollectionId, 0, HOME_LIMIT)
        return listResult(page = 1, pageCount = 1, total = items.size, items = items)
    }

    override fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String {
        val page = pg.toIntOrNull()?.takeIf { it > 0 } ?: 1
        val collectionId = extend["collection"]?.takeIf { it.isNotBlank() } ?: tid
        val start = (page - 1) * PAGE_LIMIT
        val items = loadCollection(collectionId, start, PAGE_LIMIT)
        val pageCount = when {
            items.isEmpty() -> page
            collectionId == "movie_top250" -> ceil(250.0 / PAGE_LIMIT).toInt()
            else -> page + 1
        }
        return listResult(page = page, pageCount = pageCount, total = items.size, items = items)
    }

    override fun detailContent(ids: List<String>): String {
        val id = ids.firstOrNull().orEmpty()
        if (id.isBlank()) return emptyListResult()
        val cached = detailCache[id]
        val subject = cached ?: getJson("$API_BASE/movie/${urlEncode(id)}?$API_KEY")
        val item = subject?.toVodDetail(id) ?: JSONObject().apply {
            put("vod_id", id)
            put("vod_name", "豆瓣条目 $id")
            put("vod_content", "该条目来自豆瓣热门片单，暂无播放线路。")
            put("vod_play_from", "")
            put("vod_play_url", "")
        }
        return gson.toJson(mapOf("list" to listOf(item.toMap())))
    }

    override fun searchContent(key: String, quick: Boolean): String = searchContent(key, quick, "1")

    override fun searchContent(key: String, quick: Boolean, pg: String): String {
        if (key.isBlank()) return emptyListResult()
        val page = pg.toIntOrNull()?.takeIf { it > 0 } ?: 1
        val start = (page - 1) * PAGE_LIMIT
        val root = getJson("$API_BASE/search/weixin?q=${urlEncode(key)}&start=$start&count=$PAGE_LIMIT&$API_KEY")
            ?: return emptyListResult()
        val subjects = root.optJSONArray("subjects")
            ?: root.optJSONArray("items")
            ?: JSONArray()
        val items = subjects.toVodItems()
        return listResult(page = page, pageCount = if (items.isEmpty()) page else page + 1, total = items.size, items = items)
    }

    override fun playerContent(flag: String, id: String, vipFlags: List<String>): String {
        return gson.toJson(mapOf("parse" to 1, "url" to id))
    }

    private fun loadCollection(collectionId: String, start: Int, count: Int): List<Map<String, String>> {
        val root = getJson("$API_BASE/subject_collection/${urlEncode(collectionId)}/items?$API_KEY&start=$start&count=$count")
            ?: return emptyList()
        val items = root.optJSONArray("subject_collection_items")
            ?: root.optJSONArray("items")
            ?: JSONArray()
        return items.toVodItems()
    }

    private fun JSONArray.toVodItems(): List<Map<String, String>> {
        val values = mutableListOf<Map<String, String>>()
        for (i in 0 until length()) {
            val item = optJSONObject(i)?.normalizeSubjectObject() ?: continue
            val id = item.optString("id")
            val title = item.optString("title")
            if (id.isBlank() || title.isBlank()) continue
            detailCache[id] = item
            values.add(
                mapOf(
                    "vod_id" to id,
                    "vod_name" to title,
                    "vod_pic" to item.posterUrl(),
                    "vod_remarks" to item.ratingText(),
                    "vod_score" to item.ratingValue(),
                    "vod_year" to item.yearText(),
                    "vod_area" to item.areaText(),
                    "vod_actor" to item.actorText(),
                    "vod_content" to item.descriptionText()
                )
            )
        }
        trimCache()
        return values
    }

    private fun JSONObject.normalizeSubjectObject(): JSONObject {
        optJSONObject("subject")?.let { return it }
        optJSONObject("target")?.let { return it }
        return this
    }

    private fun JSONObject.toVodDetail(id: String): JSONObject {
        return JSONObject()
            .put("vod_id", optString("id").ifBlank { id })
            .put("vod_name", optString("title"))
            .put("vod_pic", posterUrl())
            .put("vod_remarks", ratingText())
            .put("vod_score", ratingValue())
            .put("vod_year", yearText())
            .put("vod_area", areaText())
            .put("vod_actor", actorText())
            .put("vod_director", directorText())
            .put("vod_content", descriptionText().ifBlank { "该条目来自豆瓣热门片单，暂无播放线路。" })
            .put("vod_play_from", "")
            .put("vod_play_url", "")
    }

    private fun JSONObject.posterUrl(): String {
        return optString("cover_url")
            .ifBlank { optJSONObject("pic")?.optString("large").orEmpty() }
            .ifBlank { optJSONObject("pic")?.optString("normal").orEmpty() }
            .ifBlank { optString("image") }
    }

    private fun JSONObject.ratingValue(): String {
        return optJSONObject("rating")?.optString("value")
            ?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
            .orEmpty()
    }

    private fun JSONObject.ratingText(): String {
        val rating = ratingValue()
        return if (rating.isBlank()) optString("card_subtitle") else "豆瓣 $rating"
    }

    private fun JSONObject.yearText(): String {
        return optString("year").ifBlank {
            Regex("""\b(19|20)\d{2}\b""").find(optString("card_subtitle"))?.value.orEmpty()
        }
    }

    private fun JSONObject.areaText(): String {
        val areas = optJSONArray("countries") ?: optJSONArray("areas")
        if (areas != null) {
            return (0 until areas.length()).mapNotNull { areas.optString(it).takeIf(String::isNotBlank) }
                .joinToString(" ")
        }
        return optString("card_subtitle")
            .split("/")
            .getOrNull(1)
            ?.trim()
            .orEmpty()
    }

    private fun JSONObject.actorText(): String {
        val actors = optJSONArray("actors") ?: optJSONArray("casts")
        if (actors != null) return actors.toNameList()
        val parts = optString("card_subtitle").split("/")
        return parts.drop(3).joinToString(" / ") { it.trim() }
    }

    private fun JSONObject.directorText(): String {
        val directors = optJSONArray("directors") ?: return ""
        return directors.toNameList()
    }

    private fun JSONArray.toNameList(): String {
        return (0 until length()).mapNotNull { index ->
            val value = opt(index)
            when (value) {
                is JSONObject -> value.optString("name").ifBlank { value.optString("title") }
                else -> value?.toString().orEmpty()
            }.takeIf { it.isNotBlank() }
        }.joinToString(" / ")
    }

    private fun JSONObject.descriptionText(): String {
        return optString("intro")
            .ifBlank { optString("summary") }
            .ifBlank { optString("description") }
            .ifBlank { optString("card_subtitle") }
    }

    private fun getJson(url: String): JSONObject? {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .headers(requestHeaders())
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let(::JSONObject)
                } else {
                    Log.w(TAG, "HTTP ${response.code} for $url")
                    null
                }
            }
        }.onFailure { Log.e(TAG, "request failed: $url", it) }.getOrNull()
    }

    private fun requestHeaders(): okhttp3.Headers {
        return okhttp3.Headers.Builder()
            .add("Host", "frodo.douban.com")
            .add("Connection", "Keep-Alive")
            .add("Referer", "https://servicewechat.com/wx2f9b06c1de1ccfca/84/page-frame.html")
            .add("User-Agent", UA)
            .build()
    }

    private fun JSONObject.toMap(): Map<String, Any> {
        val map = linkedMapOf<String, Any>()
        keys().forEach { key -> map[key] = opt(key) ?: "" }
        return map
    }

    private fun JSONObject.optCategoryArray(key: String): List<DoubanCategory>? {
        val array = optJSONArray(key) ?: return null
        val values = mutableListOf<DoubanCategory>()
        for (i in 0 until array.length()) {
            when (val value = array.opt(i)) {
                is JSONObject -> {
                    val id = value.optString("id")
                        .ifBlank { value.optString("type_id") }
                        .ifBlank { value.optString("key") }
                    val name = value.optString("name")
                        .ifBlank { value.optString("type_name") }
                        .ifBlank { id }
                    if (id.isNotBlank() && name.isNotBlank()) values.add(DoubanCategory(id, name))
                }
                is String -> {
                    val id = value.trim()
                    if (id.isNotBlank()) values.add(DoubanCategory(id, id))
                }
            }
        }
        return values.distinctBy { it.id }.takeIf { it.isNotEmpty() }
    }

    private fun listResult(page: Int, pageCount: Int, total: Int, items: List<Map<String, String>>): String {
        return gson.toJson(mapOf("page" to page, "pagecount" to pageCount, "limit" to PAGE_LIMIT, "total" to total, "list" to items))
    }

    private fun emptyListResult() = listResult(1, 0, 0, emptyList())

    private fun trimCache() {
        while (detailCache.size > MAX_CACHE_SIZE) {
            val firstKey = detailCache.keys.firstOrNull() ?: return
            detailCache.remove(firstKey)
        }
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

    companion object {
        private const val TAG = "Douban"
        private const val API_BASE = "https://frodo.douban.com/api/v2"
        private const val API_KEY = "apikey=0ac44ae016490db2204ce0a042db2916"
        private const val PAGE_LIMIT = 20
        private const val HOME_LIMIT = 12
        private const val MAX_CACHE_SIZE = 160
        private const val UA =
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36 MicroMessenger/7.0.9.501 NetType/WIFI MiniProgramEnv/Windows WindowsWechat"

        private val FALLBACK_CATEGORY = DoubanCategory("subject_real_time_hotest", "实时热门")
    }

    private data class DoubanCategory(val id: String, val name: String)
}
