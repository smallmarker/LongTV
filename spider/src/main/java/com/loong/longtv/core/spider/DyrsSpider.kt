package com.loong.longtv.core.spider

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.loong.longtv.data.bean.HomeContent
import com.loong.longtv.data.bean.HomeTabItem
import com.loong.longtv.data.bean.PlayerContent
import com.loong.longtv.data.bean.Video
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class DyrsSpider : PrivateHtmlSpider(DEFAULT_ENTRY) {

    override fun homeContent(filter: Boolean): String {
        val tabs = runCatching { parseHomeTabs(getDocument(host)) }.getOrDefault(defaultTabs())
        return gson.toJson(HomeContent(tabs.ifEmpty { defaultTabs() }, emptyMap()))
    }

    override fun homeVideoContent(): String {
        val document = getDocument(host)
        val items = parseVideoCards(document).take(24)
        return gson.toJson(pageOf(items, 1, 1))
    }

    override fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String {
        val page = pg.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val basePath = normalizeCategoryPath(tid)
        val separator = if (basePath.contains("?")) "&" else "?"
        val pageUrl = if (page <= 1) "$host$basePath" else "$host$basePath${separator}page=${page - 1}"
        val document = getDocument(pageUrl)
        val items = parseVideoCards(document, categoryId = tid)
        return gson.toJson(pageOf(items, page, parsePageCount(document)))
    }

    override fun detailContent(ids: MutableList<String>?): String {
        val detailUrl = normalizeUrl(ids?.firstOrNull().orEmpty())
        val document = getDocument(detailUrl)
        val meta = parseSchemaObject(document)
        val playSources = parseAllPlaySources(detailUrl, document)

        val video = Video(
            vod_id = detailUrl,
            vod_name = readSchemaString(meta, "name"),
            vod_pic = readSchemaString(meta, "image").ifBlank {
                document.selectFirst("meta[property=og:image]")?.attr("content").orEmpty()
            },
            vod_remarks = readSchemaString(meta, "alternateName"),
            type_id = inferTypeId(detailUrl, document),
            type_name = inferTypeName(detailUrl, document),
            vod_actor = readSchemaNames(meta, "actor"),
            vod_area = readSchemaString(meta, "countryOfOrigin"),
            vod_content = readSchemaString(meta, "description").ifBlank {
                document.selectFirst("meta[name=description]")?.attr("content").orEmpty()
            },
            vod_director = readSchemaNames(meta, "director"),
            vod_lang = readSchemaString(meta, "inLanguage"),
            vod_play_from = playSources.map { it.origin }.joinToString("$$$"),
            vod_play_url = playSources.map { source ->
                source.items.joinToString("#") { "${it.title}\$${it.url}" }
            }.joinToString("$$$"),
            vod_state = "",
            vod_tag = readSchemaGenres(meta),
            vod_time = readSchemaString(meta, "releaseDate"),
            vod_year = readSchemaString(meta, "year")
        )
        return gson.toJson(pageOf(listOf(video), 1, 1))
    }

    override fun searchContent(key: String?, quick: Boolean): String {
        return searchContent(key, quick, "1")
    }

    override fun hotSearchContent(): String {
        val titles = runCatching {
            parseHotTitles(getDocument(host))
                .ifEmpty { parseHotRankTitles() }
                .take(12)
        }.getOrDefault(emptyList())
        return gson.toJson(titles)
    }

    override fun searchContent(key: String?, quick: Boolean, pg: String?): String {
        val keyword = key.orEmpty().trim()
        if (keyword.isBlank()) return gson.toJson(pageOf(emptyList(), 1, 1))
        val page = pg?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val searchUrl = if (page <= 1) {
            "$host/s.html?name=${urlEncode(keyword)}"
        } else {
            "$host/s.html?name=${urlEncode(keyword)}&page=${page - 1}"
        }
        val document = getDocument(searchUrl)
        return gson.toJson(pageOf(parseVideoCards(document), page, parsePageCount(document)))
    }

    override fun playerContent(flag: String?, id: String?, vipFlags: MutableList<String>?): String {
        val target = normalizeUrl(id.orEmpty())
        val finalUrl = resolvePlayUrl(target)
        return gson.toJson(
            PlayerContent(
                parse = if (MediaUrlHelper.isDirectMediaUrl(finalUrl) || finalUrl.contains("/api/m3u8")) 0 else 1,
                url = finalUrl,
                playUrl = finalUrl,
                header = mapOf(
                    "Referer" to "$host/",
                    "Origin" to host,
                    "User-Agent" to userAgent()
                )
            )
        )
    }

    override fun resolveHost(extend: String?): String {
        val candidates = buildList {
            val raw = extend?.trim()?.trim('"').orEmpty()
            if (raw.startsWith("http://") || raw.startsWith("https://")) add(raw.trimEnd('/'))
            add(DEFAULT_ENTRY)
            add(BACKUP_ENTRY)
        }.distinct()
        candidates.forEach { entry ->
            val least = runCatching { resolveLeastHost(entry) }.getOrNull()
            if (!least.isNullOrBlank()) return least
        }
        return DEFAULT_FALLBACK_HOST
    }

    private fun resolveLeastHost(entry: String): String {
        val response = getResponse("${entry.trimEnd('/')}/api/videox/least", referer = "${entry.trimEnd('/')}/")
        val json = JsonParser.parseString(response.body).asJsonObject
        val least = json.get("least")?.asString.orEmpty().trimEnd('/')
        return least.ifBlank { entry.trimEnd('/') }
    }

    private fun parseHomeTabs(document: Document): List<HomeTabItem> {
        return document.select("nav > a[href^=/]")
            .mapNotNull { anchor ->
                val href = anchor.attr("href").trim()
                if (!isPrimaryCategoryPath(href)) return@mapNotNull null
                val title = anchor.text().trim()
                val typeId = normalizeTypeId(href)
                if (typeId.isBlank() || title.isBlank()) null
                else HomeTabItem(typeId, normalizeTabTitle(title))
            }
            .distinctBy { it.type_id }
            .ifEmpty { defaultTabs() }
    }

    private fun parseVideoCards(document: Document, categoryId: String = ""): List<Video> {
        return document.select("a[data-url][title], a.block.aspect-\\[2\\/3\\][title]")
            .mapNotNull { anchor ->
                val href = anchor.absUrl("href")
                val title = anchor.attr("title").trim().ifBlank { anchor.text().trim() }
                if (href.isBlank() || title.isBlank()) return@mapNotNull null
                val card = anchor.parent()
                val typeId = if (categoryId.isBlank()) inferTypeId(href, document) else normalizeTypeId(categoryId)
                Video(
                    vod_id = href,
                    vod_name = title,
                    vod_pic = resolvePoster(anchor),
                    vod_remarks = card?.selectFirst(".text-gray-500.text-xs, .text-gray-400.text-xs")?.text().orEmpty(),
                    type_id = typeId,
                    type_name = inferTypeName(typeId, document),
                    vod_actor = "",
                    vod_area = "",
                    vod_content = "",
                    vod_director = "",
                    vod_lang = "",
                    vod_play_from = "",
                    vod_play_url = "",
                    vod_state = "",
                    vod_tag = card?.selectFirst("span.text-\\[#409EFF\\]")?.text().orEmpty(),
                    vod_time = "",
                    vod_year = ""
                )
            }
            .distinctBy { it.vod_id }
    }

    private fun resolvePoster(anchor: org.jsoup.nodes.Element): String {
        val image = anchor.selectFirst("img[data-src], img[src]") ?: return ""
        val dataSrc = image.attr("data-src").ifBlank { image.attr("src") }
        return normalizeUrl(dataSrc)
    }

    private fun parseHotTitles(document: Document): List<String> {
        return document.select(".swiper-index-list .swiper-index-item .line-clamp-1")
            .map { it.text().trim() }
            .filter(String::isNotBlank)
            .distinct()
    }

    private fun parseHotRankTitles(): List<String> {
        return HOT_RANK_PATHS.asSequence()
            .flatMap { path ->
                runCatching { parseVideoCards(getDocument("$host$path")) }
                    .getOrDefault(emptyList())
                    .asSequence()
                    .map(Video::vod_name)
            }
            .filter(String::isNotBlank)
            .distinct()
            .take(12)
            .toList()
    }

    private fun parsePageCount(document: Document): Int {
        val pages = document.select("a[href*=page=]")
            .mapNotNull { anchor ->
                Regex("page=(\\d+)").find(anchor.attr("href"))?.groupValues?.getOrNull(1)?.toIntOrNull()
            }
        return (pages.maxOrNull()?.plus(1)) ?: 1
    }

    private fun parseAllPlaySources(detailUrl: String, document: Document): List<PlaySource> {
        val originLinks = document.select("#originTabs a[href]")
            .mapNotNull { anchor ->
                val origin = anchor.selectFirst("button")?.attr("data-origin").orEmpty()
                val href = anchor.absUrl("href")
                if (origin.isBlank() || href.isBlank()) null else origin to href
            }
            .distinctBy { it.first }

        val currentSources = parseVodList(document)
        val currentOrigin = currentSources.origin

        return buildList {
            if (currentSources.items.isNotEmpty()) add(currentSources)
            originLinks.forEach { (origin, href) ->
                if (origin == currentOrigin) return@forEach
                val source = runCatching { parseVodList(getDocument(href)) }.getOrNull()
                if (source != null && source.items.isNotEmpty()) add(source)
            }
        }.ifEmpty {
            listOf(
                PlaySource(
                    origin = "默认线路",
                    items = listOf(PlayItem("播放", detailUrl))
                )
            )
        }
    }

    private fun parseVodList(document: Document): PlaySource {
        val raw = Regex("var\\s+dyrs_vod_list\\s*=\\s*JSON\\.parse\\('(.*?)'\\);", RegexOption.DOT_MATCHES_ALL)
            .find(document.html())
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
        if (raw.isBlank()) return PlaySource("", emptyList())

        val decoded = decodeJsStringLiteral(raw)
        val array = JsonParser.parseString(decoded).asJsonArray
        val origin = array.firstOrNull()?.asJsonObject?.get("origin")?.asString.orEmpty()
        val items = array.mapNotNull { element ->
            val obj = element.asJsonObject
            val title = obj.get("title")?.asString.orEmpty().ifBlank { "播放" }
            val url = normalizeUrl(obj.get("url")?.asString.orEmpty())
            if (url.isBlank()) null else PlayItem(title, url)
        }
        return PlaySource(origin.ifBlank { "默认线路" }, items)
    }

    private fun decodeJsStringLiteral(raw: String): String {
        return buildString(raw.length) {
            var index = 0
            while (index < raw.length) {
                val char = raw[index]
                if (char != '\\' || index == raw.lastIndex) {
                    append(char)
                    index++
                    continue
                }
                when (val next = raw[index + 1]) {
                    '\\' -> {
                        append('\\')
                        index += 2
                    }
                    '/' -> {
                        append('/')
                        index += 2
                    }
                    '\'' -> {
                        append('\'')
                        index += 2
                    }
                    '"' -> {
                        append('"')
                        index += 2
                    }
                    'b' -> {
                        append('\b')
                        index += 2
                    }
                    'f' -> {
                        append('\u000C')
                        index += 2
                    }
                    'n' -> {
                        append('\n')
                        index += 2
                    }
                    'r' -> {
                        append('\r')
                        index += 2
                    }
                    't' -> {
                        append('\t')
                        index += 2
                    }
                    'u' -> {
                        val hexEnd = index + 6
                        val hex = raw.substring(index + 2, minOf(hexEnd, raw.length))
                        if (hex.length == 4) {
                            append(hex.toInt(16).toChar())
                            index = hexEnd
                        } else {
                            append('\\')
                            append(next)
                            index += 2
                        }
                    }
                    else -> {
                        append(next)
                        index += 2
                    }
                }
            }
        }
    }

    private fun parseSchemaObject(document: Document): JsonObject {
        val schemaText = document.selectFirst("script[type=application/ld+json]")?.data().orEmpty()
        return runCatching { JsonParser.parseString(schemaText).asJsonObject }.getOrElse { JsonObject() }
    }

    private fun readSchemaString(obj: JsonObject, key: String): String {
        return obj.get(key)?.takeIf { !it.isJsonNull }?.asString.orEmpty()
    }

    private fun readSchemaNames(obj: JsonObject, key: String): String {
        val value = obj.get(key) ?: return ""
        return when {
            value.isJsonArray -> value.asJsonArray.mapNotNull { item ->
                when {
                    item.isJsonObject -> item.asJsonObject.get("name")?.asString
                    item.isJsonPrimitive -> item.asString
                    else -> null
                }
            }.joinToString(" / ")

            value.isJsonObject -> value.asJsonObject.get("name")?.asString.orEmpty()
            else -> value.asString
        }
    }

    private fun readSchemaGenres(obj: JsonObject): String {
        val value = obj.get("genre") ?: return ""
        return when {
            value.isJsonArray -> value.asJsonArray.map { it.asString }.joinToString(" / ")
            else -> value.asString
        }
    }

    private fun inferTypeId(url: String, document: Document): String {
        val normalized = normalizeTypeId(url)
        if (normalized.isNotBlank()) return normalized
        val breadcrumb = document.select("a[href$=.html], a[href*=.html?]")
            .mapNotNull { anchor -> normalizeTypeId(anchor.attr("href")).takeIf { it.isNotBlank() } }
            .firstOrNull()
        return breadcrumb ?: "dianying.html"
    }

    private fun inferTypeName(value: String, document: Document): String {
        val typeId = normalizeTypeId(value)
        val fromNav = parseHomeTabs(document).firstOrNull { it.type_id == typeId }?.type_name
        return fromNav ?: categoryName(typeId, defaultTabs())
    }

    private fun normalizeTypeId(path: String): String {
        val decoded = URLDecoder.decode(path, StandardCharsets.UTF_8.name())
        return decoded.substringAfterLast('/')
            .substringBefore('?')
            .takeIf { it.endsWith(".html") && it in allowedTypeIds() }
            .orEmpty()
    }

    private fun normalizeCategoryPath(tid: String): String {
        val decoded = URLDecoder.decode(tid, StandardCharsets.UTF_8.name()).trimStart('/')
        return if (decoded.startsWith("http://") || decoded.startsWith("https://")) decoded else "/$decoded"
    }

    private fun normalizeTabTitle(raw: String): String {
        return raw.removeSuffix("频道").trim().ifBlank { raw.trim() }
    }

    private fun isPrimaryCategoryPath(path: String): Boolean {
        val normalized = path.substringBefore('#').trim()
        if ('?' in normalized) return false
        return normalized.startsWith("/") && normalizeTypeId(normalized).isNotBlank()
    }

    private fun allowedTypeIds(): Set<String> {
        return setOf("dianying.html", "dianshiju.html", "zongyi.html", "dongman.html", "duanju.html")
    }

    private fun defaultTabs(): List<HomeTabItem> {
        return listOf(
            HomeTabItem("dianying.html", "电影"),
            HomeTabItem("dianshiju.html", "电视剧"),
            HomeTabItem("zongyi.html", "综艺"),
            HomeTabItem("dongman.html", "动漫"),
            HomeTabItem("duanju.html", "短剧")
        )
    }

    private fun resolvePlayUrl(input: String): String {
        val url = normalizeUrl(input)
        if (!url.contains("/api/m3u8")) return url
        val body = getResponse(url).body
        if (!body.startsWith("#EXTM3U")) return url
        val nested = body.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("/api/m3u8") || it.startsWith("http://") || it.startsWith("https://") }
            .orEmpty()
        return if (nested.isBlank()) url else normalizeUrl(nested)
    }

    private fun urlEncode(text: String): String {
        return java.net.URLEncoder.encode(text, StandardCharsets.UTF_8.name())
    }

    private data class PlaySource(
        val origin: String,
        val items: List<PlayItem>
    )

    private data class PlayItem(
        val title: String,
        val url: String
    )

    companion object {
        private const val DEFAULT_ENTRY = "https://dyrs.net"
        private const val BACKUP_ENTRY = "https://dyrs8.net"
        private const val DEFAULT_FALLBACK_HOST = "https://dyrsok.org"
        private val HOT_RANK_PATHS = listOf(
            "/dianying.html?sort_field=play_hot",
            "/dianshiju.html?sort_field=play_hot",
            "/zongyi.html?sort_field=play_hot",
            "/dongman.html?sort_field=play_hot",
            "/duanju.html?sort_field=play_hot"
        )
    }
}
