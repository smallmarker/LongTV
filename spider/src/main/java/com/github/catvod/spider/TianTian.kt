package com.github.catvod.spider

import android.content.Context
import android.util.Base64
import android.util.Log
import com.github.catvod.crawler.Spider
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 天天影院 Spider.
 *
 * ext格式: {"url": "https://www.baixiaotangtop.com"}
 */
class TianTian : Spider() {

    private var baseUrl = DEFAULT_BASE
    private val gson = Gson()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    override fun init(context: Context, extend: String) {
        baseUrl = runCatching {
            JSONObject(extend).optString("url", DEFAULT_BASE)
        }.getOrElse { extend.ifBlank { DEFAULT_BASE } }
            .trim()
            .trimEnd('/')
            .ifBlank { DEFAULT_BASE }
        Log.d(TAG, "TianTian initialized with baseUrl=$baseUrl")
    }

    override fun homeContent(filter: Boolean): String {
        val doc = getDoc("/")
        val classes = linkedMapOf<String, String>()
        doc?.select("a[href*=/vodtype/], a[href*=/vodshow/]")?.forEach { a ->
            val id = extractTypeId(a.attr("href"))
            val name = clean(a.text())
            if (id in MAIN_CLASS_IDS && name.isNotBlank() && name !in IGNORE_NAV) classes[id] = name
        }
        if (classes.isEmpty()) MAIN_CLASSES.forEach { (id, name) -> classes[id] = name }
        return gson.toJson(
            mapOf(
                "class" to classes.map { (id, name) ->
                    mapOf("type_id" to id, "type_name" to name)
                },
                "filters" to emptyMap<String, Any>()
            )
        )
    }

    override fun homeVideoContent(): String {
        val doc = getDoc("/") ?: return emptyListResult()
        val items = parseVodItems(doc).take(24)
        return listResult(1, if (items.isEmpty()) 0 else 1, items.size, items)
    }

    override fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String {
        val page = pg.toIntOrNull()?.takeIf { it > 0 } ?: 1
        val doc = getDoc("/vodshow/$tid--------$page---.html") ?: return emptyListResult(page)
        val items = parseVodItems(doc)
        val pageCount = parsePageCount(doc).takeIf { it > 0 } ?: if (items.isEmpty()) page else page + 1
        return listResult(page, pageCount, pageCount * LIMIT, items)
    }

    override fun detailContent(ids: List<String>): String {
        val id = ids.firstOrNull().orEmpty()
        if (id.isBlank()) return emptyListResult()
        val doc = getDoc(detailPath(id)) ?: return emptyListResult()
        val playLines = parsePlayLines(doc)

        val item = linkedMapOf<String, Any>(
            "vod_id" to id,
            "vod_name" to firstText(doc, ".ewave-content__detail h1 .title", "h1.title", "h1"),
            "vod_pic" to absoluteUrl(
                firstAttr(doc, ".ewave-content__thumb img", "data-original")
                    .ifBlank { firstAttr(doc, ".ewave-content__thumb img", "data-src") }
                    .ifBlank { firstAttr(doc, ".ewave-content__thumb img", "src") }
            ),
            "vod_remarks" to firstText(doc, ".ewave-content__thumb .pic-text", ".pic-text"),
            "type_name" to detailField(doc, "类型"),
            "vod_year" to detailField(doc, "年份"),
            "vod_area" to detailField(doc, "地区"),
            "vod_lang" to detailField(doc, "语言"),
            "vod_actor" to detailField(doc, "主演"),
            "vod_director" to detailField(doc, "导演"),
            "vod_content" to detailContent(doc),
            "vod_time" to detailField(doc, "更新"),
            "vod_play_from" to playLines.joinToString("\$\$\$") { it.name },
            "vod_play_url" to playLines.joinToString("\$\$\$") { line ->
                line.episodes.joinToString("#") { "${it.name}\$${it.url}" }
            }
        )
        return gson.toJson(mapOf("list" to listOf(item)))
    }

    override fun searchContent(key: String, quick: Boolean): String = searchContent(key, quick, "1")

    override fun searchContent(key: String, quick: Boolean, pg: String): String {
        val page = pg.toIntOrNull()?.takeIf { it > 0 } ?: 1
        val encoded = urlEncode(key)
        val ajax = get("/index.php/ajax/suggest?mid=1&wd=$encoded&page=$page")
        val ajaxResult = parseSuggestSearch(ajax, page)
        if (ajaxResult != null) return ajaxResult

        val doc = getDoc("/vodsearch/$encoded----------$page---.html") ?: return emptyListResult(page)
        val items = parseVodItems(doc)
        return listResult(page, parsePageCount(doc), items.size, items)
    }

    override fun playerContent(flag: String, id: String, vipFlags: List<String>): String {
        val playUrl = id.trim()
        if (isDirectVideo(playUrl)) return playerResult(playUrl)
        val html = get(playUrl) ?: return playerResult(playUrl, parse = 1)
        val url = extractPlayerUrl(html)
        return if (url.isNotBlank()) playerResult(url) else playerResult(playUrl, parse = 1)
    }

    private fun extractPlayerUrl(html: String): String {
        val playerJson = PLAYER_JSON_REGEX.find(html)?.groupValues?.getOrNull(1).orEmpty()
        if (playerJson.isBlank()) return ""
        return runCatching {
            val root = JSONObject(playerJson)
            val url = root.optString("url").replace("\\/", "/").replace("\\u0026", "&")
            val parsed = when (root.optInt("encrypt", 0)) {
                1 -> decode(url)
                2 -> decode(String(Base64.decode(url, Base64.DEFAULT), Charsets.UTF_8))
                else -> url
            }
            absoluteUrl(parsed)
        }.onFailure { Log.e(TAG, "parse player failed", it) }.getOrDefault("")
    }

    private fun parseVodItems(doc: Document): List<Map<String, String>> {
        return doc.select("li:has(a[href*=/voddetail/]), .ewave-vodlist__item:has(a[href*=/voddetail/])")
            .mapNotNull { item ->
                val link = item.selectFirst("a[href*=/voddetail/]") ?: return@mapNotNull null
                val thumb = item.selectFirst(".ewave-vodlist__thumb")
                val id = extractVodId(link.attr("href"))
                val name = clean(thumb?.attr("title").orEmpty())
                    .ifBlank { clean(link.attr("title")) }
                    .ifBlank { clean(item.selectFirst("img")?.attr("alt").orEmpty()) }
                    .ifBlank { clean(item.selectFirst(".title, .ewave-vodlist__title")?.text().orEmpty()) }
                if (id.isBlank() || name.isBlank()) return@mapNotNull null

                val pic = thumb?.attr("data-original").orEmpty()
                    .ifBlank { thumb?.attr("data-src").orEmpty() }
                    .ifBlank { item.selectFirst("img")?.attr("data-original").orEmpty() }
                    .ifBlank { item.selectFirst("img")?.attr("data-src").orEmpty() }
                    .ifBlank { item.selectFirst("img")?.attr("src").orEmpty() }
                mapOf(
                    "vod_id" to id,
                    "vod_name" to name,
                    "vod_pic" to absoluteUrl(pic),
                    "vod_remarks" to clean(item.selectFirst(".pic-text, .remarks")?.text().orEmpty()),
                    "vod_score" to clean(item.selectFirst(".pic-tag, .score")?.text().orEmpty())
                )
            }
            .distinctBy { it["vod_id"] }
    }

    private fun parseSuggestSearch(json: String?, page: Int): String? {
        if (json.isNullOrBlank() || !json.trimStart().startsWith("{")) return null
        return runCatching {
            val root = JSONObject(json)
            val list = root.optJSONArray("list") ?: return null
            val items = mutableListOf<Map<String, String>>()
            for (i in 0 until list.length()) {
                val item = list.optJSONObject(i) ?: continue
                val id = item.optString("id")
                val name = item.optString("name")
                if (id.isBlank() || name.isBlank()) continue
                items.add(
                    mapOf(
                        "vod_id" to id,
                        "vod_name" to name,
                        "vod_pic" to absoluteUrl(item.optString("pic")),
                        "vod_remarks" to ""
                    )
                )
            }
            listResult(
                root.optInt("page", page),
                root.optInt("pagecount", 1),
                root.optInt("total", items.size),
                items
            )
        }.onFailure { Log.e(TAG, "parse suggest failed", it) }.getOrNull()
    }

    private fun parsePlayLines(doc: Document): List<PlayLine> {
        val names = doc.select(".playlist-slide .nav-tabs a, .ewave-playlist__tabs a, .ewave-tabs__nav a")
            .map { clean(it.text()) }
            .filter { it.isNotBlank() }
        val lines = mutableListOf<PlayLine>()
        doc.select(".ewave-content__playlist, .playlist-list, .anthology-list").forEachIndexed { index, container ->
            val episodes = container.select("a[href*=/vodplay/]").mapNotNull { a ->
                val name = clean(a.text()).ifBlank { "第${index + 1}集" }
                val url = absoluteUrl(a.attr("href"))
                if (url.isBlank()) null else Episode(name, url)
            }.distinctBy { it.url }
            if (episodes.isNotEmpty()) {
                lines.add(PlayLine(names.getOrNull(index).orEmpty().ifBlank { "线路${index + 1}" }, episodes))
            }
        }
        return lines.distinctBy { it.episodes.firstOrNull()?.url.orEmpty() }
    }

    private fun detailField(doc: Document, label: String): String {
        val labelRegex = Regex(
            "${Regex.escape(label)}[:：]\\s*(.*?)(?=\\s*(?:类型|地区|年份|语言|主演|导演|更新|状态)[:：]|$)"
        )
        doc.select(".ewave-content__detail .data, .data").forEach { data ->
            val match = labelRegex.find(clean(data.text())) ?: return@forEach
            return clean(match.groupValues[1].replace("/", " "))
        }
        return ""
    }

    private fun detailContent(doc: Document): String {
        val node = doc.selectFirst("#desc .ewave-pannel_bd, #desc, .desc")
        return clean(node?.text().orEmpty())
            .removePrefix("剧情简介")
            .removePrefix("简介")
            .trim()
    }

    private fun parsePageCount(doc: Document): Int {
        val text = doc.selectFirst(".ewave-page .num, .page-tips, .pagination")?.text().orEmpty()
        return Regex("/\\s*(\\d+)").find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: doc.select("a[href*=/vodshow/]").mapNotNull { a ->
                Regex("/vodshow/\\d+--------(\\d+)---").find(a.attr("href"))
                    ?.groupValues?.getOrNull(1)?.toIntOrNull()
            }.maxOrNull()
            ?: doc.select(".ewave-page a, .pagination a").mapNotNull { it.text().toIntOrNull() }.maxOrNull()
            ?: 1
    }

    private fun getDoc(pathOrUrl: String): Document? = get(pathOrUrl)?.let { Jsoup.parse(it, baseUrl) }

    private fun get(pathOrUrl: String): String? {
        val url = if (pathOrUrl.startsWith("http")) pathOrUrl else baseUrl + pathOrUrl
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .header("Referer", "$baseUrl/")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        }.onFailure { Log.e(TAG, "request failed: $url", it) }.getOrNull()
    }

    private fun firstText(doc: Document, vararg selectors: String): String {
        return selectors.firstNotNullOfOrNull { selector ->
            clean(doc.selectFirst(selector)?.text().orEmpty()).takeIf { it.isNotBlank() }
        }.orEmpty()
    }

    private fun firstAttr(doc: Document, selector: String, attr: String): String {
        return doc.selectFirst(selector)?.attr(attr).orEmpty()
    }

    private fun detailPath(id: String): String {
        return if (id.startsWith("http")) id else "/voddetail/$id.html"
    }

    private fun extractTypeId(url: String): String {
        return Regex("/(?:vodtype|vodshow)/(\\d+)").find(url)?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun extractVodId(url: String): String {
        return Regex("/voddetail/(\\d+)\\.html").find(url)?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun absoluteUrl(url: String): String {
        if (url.isBlank()) return ""
        return when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> baseUrl + url
            else -> url
        }
    }

    private fun playerResult(url: String, parse: Int = 0): String {
        return gson.toJson(
            mapOf(
                "parse" to parse,
                "url" to url,
                "header" to mapOf("User-Agent" to UA, "Referer" to "$baseUrl/")
            )
        )
    }

    private fun listResult(page: Int, pageCount: Int, total: Int, items: List<Map<String, String>>): String {
        return gson.toJson(mapOf("page" to page, "pagecount" to pageCount, "limit" to LIMIT, "total" to total, "list" to items))
    }

    private fun emptyListResult(page: Int = 1) = listResult(page, 0, 0, emptyList())

    private fun isDirectVideo(url: String): Boolean {
        return url.contains(".m3u8", ignoreCase = true) || url.contains(".mp4", ignoreCase = true)
    }

    private fun clean(text: String): String = text.replace(Regex("\\s+"), " ").trim()

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun decode(value: String): String {
        return runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
    }

    private data class PlayLine(val name: String, val episodes: List<Episode>)

    private data class Episode(val name: String, val url: String)

    companion object {
        private const val TAG = "TianTian"
        private const val DEFAULT_BASE = "https://www.baixiaotangtop.com"
        private const val UA = "Mozilla/5.0 (Linux; Android 12; LongTV) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"
        private const val LIMIT = 20
        private val PLAYER_JSON_REGEX = Regex("""var\s+player_[a-zA-Z0-9_]+\s*=\s*(\{.*?\})</script>""", RegexOption.DOT_MATCHES_ALL)
        private val IGNORE_NAV = setOf("首页", "留言", "最新", "排行", "全部", "APP")
        private val MAIN_CLASSES = listOf("1" to "电影", "2" to "电视剧", "3" to "综艺", "4" to "动漫", "36" to "短剧")
        private val MAIN_CLASS_IDS = MAIN_CLASSES.map { it.first }.toSet()
    }
}
