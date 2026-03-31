package com.loong.longtv.core.spider

import android.net.Uri
import android.text.TextUtils
import com.github.catvod.crawler.Spider
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.loong.longtv.data.bean.HomeContent
import com.loong.longtv.data.bean.HomeTabItem
import com.loong.longtv.data.bean.PlayerContent
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.HashMap
import java.util.Locale

class CmsApiSpider(
    private val api: String
) : Spider() {

    private val gson = Gson()
    private val client = OkHttpClient()

    override fun homeContent(filter: Boolean): String {
        val body = get(api)
        val classes = runCatching {
            JsonParser.parseString(body).asJsonObject
                .getAsJsonArray("class")
                ?.mapNotNull { item ->
                    item.takeIf { it.isJsonObject }?.asJsonObject?.toHomeTab()
                }
                .orEmpty()
        }.getOrDefault(emptyList())
        return gson.toJson(HomeContent(tabs = classes, filters = emptyMap()))
    }

    override fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String {
        val builder = Uri.parse(api).buildUpon()
            .appendQueryParameter("ac", "detail")
            .appendQueryParameter("t", tid)
            .appendQueryParameter("pg", pg)
        extend.forEach { (key, value) ->
            if (!value.isNullOrBlank()) builder.appendQueryParameter(key, value)
        }
        return get(builder.build().toString())
    }

    override fun detailContent(ids: List<String>): String {
        return get(
            Uri.parse(api).buildUpon()
                .appendQueryParameter("ac", "detail")
                .appendQueryParameter("ids", ids.firstOrNull().orEmpty())
                .build()
                .toString()
        )
    }

    override fun searchContent(key: String, quick: Boolean): String {
        return searchContent(key, quick, "1")
    }

    override fun searchContent(key: String, quick: Boolean, pg: String): String {
        val candidates = listOf(
            Uri.parse(api).buildUpon()
                .appendQueryParameter("ac", "detail")
                .appendQueryParameter("wd", key)
                .appendQueryParameter("pg", pg)
                .build()
                .toString(),
            Uri.parse(api).buildUpon()
                .appendQueryParameter("wd", key)
                .appendQueryParameter("pg", pg)
                .build()
                .toString(),
            Uri.parse(api).buildUpon()
                .appendQueryParameter("ac", "videolist")
                .appendQueryParameter("wd", key)
                .appendQueryParameter("pg", pg)
                .build()
                .toString()
        )
        return candidates.firstNotNullOfOrNull { url ->
            runCatching {
                val body = get(url)
                gson.fromJson(body, Any::class.java)
                body
            }.getOrNull()
        } ?: gson.toJson(emptyPage())
    }

    override fun playerContent(flag: String, id: String, vipFlags: MutableList<String>?): String {
        val directUrl = MediaUrlHelper.extractDirectMediaUrl(id)
        val playUrl = directUrl ?: id
        val parse = if (directUrl != null || isDirectPlayUrl(id)) 0 else 1
        return gson.toJson(
            PlayerContent(
                parse = parse,
                url = playUrl,
                playUrl = playUrl
            )
        )
    }

    private fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", DEFAULT_UA)
            .build()
        client.newCall(request).execute().use { response ->
            return response.body?.string().orEmpty()
        }
    }

    private fun isDirectPlayUrl(url: String): Boolean {
        if (TextUtils.isEmpty(url)) return false
        if (MediaUrlHelper.isDirectMediaUrl(url)) return true
        val lower = url.lowercase(Locale.getDefault())
        return lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("file://") ||
            lower.endsWith(".m3u8") ||
            lower.endsWith(".mp4") ||
            lower.endsWith(".mpd")
    }

    companion object {
        private const val DEFAULT_UA = "Mozilla/5.0 (Linux; Android 13; TV) AppleWebKit/537.36 Chrome/123.0.0.0 Safari/537.36"
    }

    private fun JsonObject.toHomeTab(): HomeTabItem? {
        val typeId = getString("type_id", "typeId", "id")
        val typeName = getString("type_name", "typeName", "name")
        if (typeId.isBlank() || typeName.isBlank()) return null
        return HomeTabItem(type_id = typeId, type_name = typeName)
    }

    private fun JsonObject.getString(vararg names: String): String {
        names.forEach { name ->
            val value = get(name)
            if (value != null && !value.isJsonNull) return value.asString.orEmpty()
        }
        return ""
    }

    private fun emptyPage() = com.loong.longtv.data.bean.CategoryContent(
        code = 1,
        pagecount = 1,
        total = 0,
        page = 1,
        msg = "",
        limit = 0,
        list = emptyList()
    )
}
