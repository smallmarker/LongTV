package com.github.catvod.spider

import android.content.Context
import android.util.Log
import com.github.catvod.crawler.Spider
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 小蜜蜂影院 Spider
 *
 * ext格式: {"url": "https://www.xmfyy.cc"}
 */
class Xmfyy : Spider() {

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
            .replace("https://www.xmfyy.com", DEFAULT_BASE)
            .replace("http://www.xmfyy.com", DEFAULT_BASE)
        Log.d(TAG, "Xmfyy initialized with baseUrl=$baseUrl")
    }

    override fun homeContent(filter: Boolean): String {
        val doc = getDoc("/")
        val classes = linkedMapOf<String, String>()
        doc?.select("a[href*=/index.php/vod/type/id/], a[href*=/index.php/vod/show/id/]")?.forEach { a ->
            val id = extractTypeId(a.attr("href"))
            val name = clean(a.text())
            if (id in MAIN_CLASS_IDS && name.isNotBlank() && name !in IGNORE_NAV) classes[id] = name
        }
        if (classes.isEmpty()) MAIN_CLASSES.forEach { (id, name) -> classes[id] = name }
        val filters = classes.keys.associateWith { tid ->
            parseFilters(getDoc("/index.php/vod/show/id/$tid.html"), tid)
        }.filterValues { it.isNotEmpty() }
        return gson.toJson(
            mapOf(
                "class" to classes.map { (id, name) ->
                    mapOf("type_id" to id, "type_name" to name)
                },
                "filters" to filters
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
        val doc = getDoc(buildCategoryPath(tid, page, extend)) ?: return emptyListResult()
        val items = parseVodItems(doc)
        val pageCount = parsePageCount(doc).takeIf { it > 0 } ?: if (items.isEmpty()) page else page + 1
        val total = Regex("\\$\\('.hl-total'\\)\\.html\\('(\\d+)'\\)")
            .find(doc.html())?.groupValues?.getOrNull(1)?.toIntOrNull() ?: (pageCount * LIMIT)
        return listResult(page, pageCount, total, items)
    }

    override fun detailContent(ids: List<String>): String {
        val id = ids.firstOrNull().orEmpty()
        if (id.isBlank()) return emptyListResult()
        val doc = getDoc(detailPath(id)) ?: return emptyListResult()

        val playLines = parsePlayLines(doc)
        val item = linkedMapOf<String, Any>(
            "vod_id" to id,
            "vod_name" to firstText(doc, ".hl-dc-title", ".hl-vod-name h2", "h1"),
            "vod_pic" to absoluteUrl(firstAttr(doc, ".hl-dc-pic .hl-item-thumb, .hl-item-pic .hl-item-thumb", "data-original")),
            "vod_remarks" to detailField(doc, "状态"),
            "type_name" to detailField(doc, "类型"),
            "vod_year" to detailField(doc, "年份"),
            "vod_area" to detailField(doc, "地区"),
            "vod_lang" to detailField(doc, "语言"),
            "vod_actor" to detailField(doc, "主演"),
            "vod_director" to detailField(doc, "导演"),
            "vod_content" to detailField(doc, "简介"),
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
        val encoded = urlEncode(key)
        val ajax = get("/index.php/ajax/suggest?mid=1&wd=$encoded")
        val ajaxResult = parseSuggestSearch(ajax, pg)
        if (ajaxResult != null) return ajaxResult

        val page = pg.toIntOrNull()?.takeIf { it > 0 } ?: 1
        val path = if (page <= 1) {
            "/index.php/vod/search/wd/$encoded.html"
        } else {
            "/index.php/vod/search/page/$page/wd/$encoded.html"
        }
        val doc = getDoc(path) ?: return emptyListResult()
        val items = parseVodItems(doc)
        val total = Regex("\\$\\('.hl-total'\\)\\.html\\('(\\d+)'\\)")
            .find(doc.html())?.groupValues?.getOrNull(1)?.toIntOrNull() ?: items.size
        return listResult(page, parsePageCount(doc), total, items)
    }

    override fun playerContent(flag: String, id: String, vipFlags: List<String>): String {
        if (id.contains(".m3u8", ignoreCase = true) || id.contains(".mp4", ignoreCase = true)) {
            return playerResult(id)
        }
        val html = get(id) ?: return playerResult(id, parse = 1)
        val url = extractPlayerUrl(html)
        return if (url.isNotBlank()) playerResult(url) else playerResult(id, parse = 1)
    }

    private fun extractPlayerUrl(html: String): String {
        val playerJson = PLAYER_JSON_REGEX.find(html)?.groupValues?.getOrNull(1).orEmpty()
        if (playerJson.isBlank()) return ""
        return runCatching {
            JSONObject(playerJson).optString("url")
        }.getOrDefault("").replace("\\/", "/").replace("\\u0026", "&")
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

    private fun parseVodItems(doc: Document): List<Map<String, String>> {
        return doc.select("li:has(a.hl-item-thumb[href*=/index.php/vod/detail/id/])")
            .mapNotNull { li ->
                val thumb = li.selectFirst("a.hl-item-thumb[href*=/index.php/vod/detail/id/]") ?: return@mapNotNull null
                val id = extractVodId(thumb.attr("href"))
                val name = clean(thumb.attr("title")).ifBlank {
                    clean(li.selectFirst(".hl-item-title a")?.text().orEmpty())
                }
                if (id.isBlank() || name.isBlank()) return@mapNotNull null
                mapOf(
                    "vod_id" to id,
                    "vod_name" to name,
                    "vod_pic" to absoluteUrl(thumb.attr("data-original").ifBlank { thumb.attr("src") }),
                    "vod_remarks" to clean(li.selectFirst(".remarks")?.text().orEmpty()),
                    "vod_score" to clean(li.selectFirst(".score")?.text().orEmpty())
                )
            }
            .distinctBy { it["vod_id"] }
    }

    private fun parseSuggestSearch(json: String?, pg: String): String? {
        if (json.isNullOrBlank() || !json.trimStart().startsWith("{")) return null
        return runCatching {
            val root = JSONObject(json)
            val list = root.optJSONArray("list") ?: return null
            val items = mutableListOf<Map<String, String>>()
            for (i in 0 until list.length()) {
                val item = list.optJSONObject(i) ?: continue
                items.add(
                    mapOf(
                        "vod_id" to item.optString("id"),
                        "vod_name" to item.optString("name"),
                        "vod_pic" to item.optString("pic"),
                        "vod_remarks" to ""
                    )
                )
            }
            listResult(
                root.optInt("page", pg.toIntOrNull() ?: 1),
                root.optInt("pagecount", 1),
                root.optInt("total", items.size),
                items
            )
        }.onFailure { Log.e(TAG, "parse suggest failed", it) }.getOrNull()
    }

    private fun parsePlayLines(doc: Document): List<PlayLine> {
        val names = doc.select(".hl-tabs-btn").map { clean(it.text()) }.filter { it.isNotBlank() }
        val lines = mutableListOf<PlayLine>()
        doc.select(".hl-plays-list").forEachIndexed { index, ul ->
            val episodes = ul.select("a[href*=/index.php/vod/play/id/]").mapNotNull { a ->
                val name = clean(a.text()).ifBlank { "第${index + 1}集" }
                val url = absoluteUrl(a.attr("href"))
                if (url.isBlank()) null else Episode(name, url)
            }.distinctBy { it.url }
            if (episodes.isNotEmpty()) {
                lines.add(PlayLine(names.getOrNull(index).orEmpty().ifBlank { "线路${index + 1}" }, episodes))
            }
        }
        return lines
    }

    private fun buildCategoryPath(tid: String, page: Int, extend: HashMap<String, String>): String {
        val typeId = extend["type"].orEmpty().ifBlank { tid }
        val segments = mutableListOf<String>()
        extend["by"]?.takeIf { it.isNotBlank() }?.let { segments.add("by/$it") }
        segments.add("id/$typeId")
        extend["year"]?.takeIf { it.isNotBlank() }?.let { segments.add("year/${urlEncode(it)}") }
        extend["lang"]?.takeIf { it.isNotBlank() }?.let { segments.add("lang/${urlEncode(it)}") }
        extend["letter"]?.takeIf { it.isNotBlank() }?.let { segments.add("letter/${urlEncode(it)}") }
        if (page > 1) segments.add("page/$page")
        return "/index.php/vod/show/${segments.joinToString("/")}.html"
    }

    private fun parseFilters(doc: Document?, tid: String): List<Map<String, Any>> {
        if (doc == null) return emptyList()
        val filters = mutableListOf<Map<String, Any>>()
        filters.addAll(parseSortFilter(doc))
        doc.select(".hl-filter-wrap").forEach { wrap ->
            val name = clean(wrap.selectFirst(".hl-filter-text span")?.text().orEmpty())
            val key = when (name) {
                "分类" -> "type"
                "年份" -> "year"
                "语言" -> "lang"
                "字母" -> "letter"
                else -> return@forEach
            }
            val values = wrap.select("a").mapNotNull { a -> parseFilterValue(a, key, tid) }
                .distinctBy { it.first }
            if (values.size > 1) filters.add(filter(key, name, values))
        }
        return filters.distinctBy { it["key"] }
    }

    private fun parseSortFilter(doc: Document): List<Map<String, Any>> {
        val values = doc.select(".hl-site-tabs a[href*=/index.php/vod/show/]").mapNotNull { a ->
            val label = clean(a.text()).removePrefix("按").ifBlank { return@mapNotNull null }
            val by = Regex("/by/([^/]+)").find(a.attr("href"))?.groupValues?.getOrNull(1).orEmpty()
            decode(by) to label
        }.distinctBy { it.first }
        return if (values.isNotEmpty()) listOf(filter("by", "排序", listOf("" to "全部") + values)) else emptyList()
    }

    private fun parseFilterValue(a: Element, key: String, tid: String): Pair<String, String>? {
        val label = clean(a.text()).ifBlank { return null }
        if (label == "全部" || a.attr("href").startsWith("javascript")) return "" to label
        val href = a.attr("href")
        val value = when (key) {
            "type" -> extractTypeId(href).takeIf { it != tid }.orEmpty()
            else -> Regex("/$key/([^/.]+)").find(href)?.groupValues?.getOrNull(1)?.let(::decode).orEmpty()
        }
        return value.takeIf { it.isNotBlank() }?.let { it to label }
    }

    private fun filter(key: String, name: String, values: List<Pair<String, String>>): Map<String, Any> {
        return mapOf(
            "key" to key,
            "name" to name,
            "value" to values.map { (value, label) -> mapOf("n" to label, "v" to value) }
        )
    }

    private fun detailField(doc: Document, label: String): String {
        val item = doc.select(".hl-full-box li").firstOrNull { it.selectFirst("em")?.text()?.contains(label) == true }
            ?: return ""
        item.select("em").remove()
        return clean(item.text().replace("/", " ").replace("  ", " "))
    }

    private fun parsePageCount(doc: Document): Int {
        val text = doc.selectFirst(".hl-page-total, .hl-page-tips a")?.text().orEmpty()
        return Regex("/\\s*(\\d+)").find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: doc.select(".hl-page-wrap a").mapNotNull { it.text().toIntOrNull() }.maxOrNull()
            ?: 1
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
        return if (id.startsWith("http")) id else "/index.php/vod/detail/id/$id.html"
    }

    private fun extractTypeId(url: String): String {
        return Regex("/(?:type|show)/id/(\\d+)").find(url)?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun extractVodId(url: String): String {
        return Regex("/detail/id/(\\d+)").find(url)?.groupValues?.getOrNull(1).orEmpty()
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

    private fun emptyHomeContent() = gson.toJson(mapOf("class" to emptyList<Any>(), "filters" to emptyMap<String, Any>()))

    private fun emptyListResult() = listResult(1, 0, 0, emptyList())

    private fun clean(text: String): String = text.replace(Regex("\\s+"), " ").trim()

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun decode(value: String): String {
        return runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
    }

    private data class PlayLine(val name: String, val episodes: List<Episode>)

    private data class Episode(val name: String, val url: String)

    companion object {
        private const val TAG = "Xmfyy"
        private const val DEFAULT_BASE = "https://www.xmfyy.cc"
        private const val UA = "Mozilla/5.0 (Linux; Android 12; LongTV) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"
        private const val LIMIT = 20
        private val PLAYER_JSON_REGEX = Regex("""var\s+player_[a-zA-Z0-9_]+\s*=\s*(\{.*?\})</script>""", RegexOption.DOT_MATCHES_ALL)
        private val IGNORE_NAV = setOf("首页", "留言", "最新", "排行", "全部", "RSS订阅")
        private val MAIN_CLASSES = listOf("2" to "连续剧", "1" to "电影", "4" to "动漫", "3" to "综艺")
        private val MAIN_CLASS_IDS = MAIN_CLASSES.map { it.first }.toSet()
    }
}
