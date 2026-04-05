package com.loong.longtv.core.spider

import com.google.gson.JsonParser
import com.loong.longtv.data.bean.HomeContent
import com.loong.longtv.data.bean.HomeTabItem
import com.loong.longtv.data.bean.PlayerContent
import com.loong.longtv.data.bean.Video
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.Locale
import java.util.regex.Pattern

class DDysSpider : PrivateHtmlSpider(DEFAULT_HOST) {

    override fun homeContent(filter: Boolean): String {
        val tabs = runCatching {
            parseHomeTabs(getDocument(host))
        }.getOrDefault(emptyList())
        return gson.toJson(HomeContent(tabs.ifEmpty { defaultTabs() }, emptyMap()))
    }

    override fun homeVideoContent(): String {
        val document = getDocument(host)
        val items = parseCardList(document).take(24)
        return gson.toJson(pageOf(items, 1, 1))
    }

    override fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String {
        val page = pg.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val basePath = categoryPath(tid)
        val url = if (page == 1) "$host$basePath" else "$host$basePath/page/$page"
        val document = getDocument(url)
        val items = parseCardList(document, tid)
        return gson.toJson(pageOf(items, page, parsePageCount(document)))
    }

    override fun detailContent(ids: MutableList<String>?): String {
        val detailUrl = normalizeUrl(ids?.firstOrNull().orEmpty())
        val document = getDocument(detailUrl)
        val titleBlock = document.selectFirst("h1")
        val metaText = document
            .selectFirst("div.text-xs.md\\:text-sm.text-gray-600.dark\\:text-gray-400.mb-3.md\\:mb-4.font-light")
            ?.text()
            .orEmpty()
        val metaParts = metaText.split("·").map { it.trim() }.filter { it.isNotEmpty() }

        val playFrom = mutableListOf<String>()
        val playUrls = mutableListOf<String>()
        document.select("button[id^=source-tab-]").forEachIndexed { index, button ->
            val sourceName = button.text().ifBlank { "线路${index + 1}" }
            val sourceUrl = extractSwitchSourceUrl(button.attr("onclick"))
            if (sourceUrl.isNotBlank()) {
                playFrom += sourceName
                playUrls += formatPlayUrl(sourceUrl)
            }
        }

        if (playFrom.isEmpty()) {
            document.select("iframe[src]").forEachIndexed { index, frame ->
                val sourceUrl = frame.absUrl("src")
                if (sourceUrl.isNotBlank()) {
                    playFrom += "线路${index + 1}"
                    playUrls += "播放$$sourceUrl"
                }
            }
        }

        val video = Video(
            vod_id = detailUrl,
            vod_name = titleBlock?.ownText()?.trim().orEmpty(),
            vod_pic = document.selectFirst("img[fetchpriority=high], img[src*=img.ddys.io]")?.absUrl("src").orEmpty(),
            vod_remarks = titleBlock?.selectFirst("span")?.text().orEmpty().ifBlank { metaParts.drop(2).joinToString(" / ") },
            type_id = findCategoryId(detailUrl),
            type_name = categoryName(findCategoryId(detailUrl), defaultTabs()),
            vod_actor = labelValue(document, "主演"),
            vod_area = metaParts.getOrNull(1).orEmpty(),
            vod_content = document.selectFirst(".prose")?.text().orEmpty(),
            vod_director = labelValue(document, "导演"),
            vod_lang = "",
            vod_play_from = playFrom.joinToString("$$$"),
            vod_play_url = playUrls.joinToString("$$$"),
            vod_state = "",
            vod_tag = metaParts.drop(2).joinToString(" / "),
            vod_time = "",
            vod_year = metaParts.getOrNull(0).orEmpty()
        )
        return gson.toJson(pageOf(listOf(video), 1, 1))
    }

    override fun searchContent(key: String?, quick: Boolean): String {
        return searchContent(key, quick, "1")
    }

    override fun hotSearchContent(): String {
        val response = getResponse("$host/api/hot-movies", referer = host)
        val titles = runCatching {
            JsonParser.parseString(response.body)
                .asJsonObject
                .get("data")
                .asJsonArray
                .mapNotNull { element ->
                    element.asJsonObject
                        .get("title")
                        ?.asString
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                }
        }.getOrDefault(emptyList())
        return gson.toJson(titles)
    }

    override fun searchContent(key: String?, quick: Boolean, pg: String?): String {
        val keyword = key.orEmpty().trim()
        if (keyword.isBlank()) return gson.toJson(pageOf(emptyList(), 1, 1))
        val document = searchDocument(keyword)
        val items = parseSearchResults(document)
        return gson.toJson(pageOf(items, 1, 1))
    }

    override fun playerContent(flag: String?, id: String?, vipFlags: MutableList<String>?): String {
        val resolved = resolvePlayableUrl(id.orEmpty())
        return gson.toJson(
            PlayerContent(
                parse = if (MediaUrlHelper.isDirectMediaUrl(resolved)) 0 else 1,
                url = resolved,
                playUrl = resolved,
                header = mapOf(
                    "Referer" to "$host/",
                    "Origin" to host,
                    "User-Agent" to userAgent()
                )
            )
        )
    }

    private fun searchDocument(keyword: String): Document {
        val csrfPage = getResponse("$host/search")
        val csrfToken = Jsoup.parse(csrfPage.body).selectFirst("meta[name=csrf-token]")?.attr("content").orEmpty()
        return postDocument(
            url = "$host/search",
            form = mapOf("q" to keyword, "type" to "movie"),
            headers = mapOf("X-CSRF-TOKEN" to csrfToken).filterValues { it.isNotBlank() },
            cookies = csrfPage.cookies,
            referer = "$host/search"
        )
    }

    private fun parseSearchResults(document: Document): List<Video> {
        return document.select("a[href^=/movie/], a[href^=/series/], a[href^=/variety/], a[href^=/anime/]")
            .mapNotNull { anchor ->
                val href = anchor.absUrl("href")
                if (href.isBlank()) return@mapNotNull null
                val image = anchor.selectFirst("img[src]")?.absUrl("src").orEmpty()
                val title = anchor
                    .selectFirst("h1, h2, h3, h4, .entry-title, .post-title")
                    ?.text()
                    ?.trim()
                    .orEmpty()
                    .ifBlank { anchor.selectFirst("img")?.attr("alt").orEmpty().trim() }
                if (title.isBlank()) return@mapNotNull null
                Video(
                    vod_id = href,
                    vod_name = title,
                    vod_pic = image,
                    vod_remarks = "",
                    type_id = findCategoryId(href),
                    type_name = categoryName(findCategoryId(href), defaultTabs()),
                    vod_actor = "",
                    vod_area = "",
                    vod_content = "",
                    vod_director = "",
                    vod_lang = "",
                    vod_play_from = "",
                    vod_play_url = "",
                    vod_state = "",
                    vod_tag = "",
                    vod_time = "",
                    vod_year = "",
                    vod_score = extractScore(anchor.text())
                )
            }
            .distinctBy { it.vod_id }
    }

    private fun parseHomeTabs(document: Document): List<HomeTabItem> {
        return document.select("nav a[href]")
            .mapNotNull { anchor ->
                val href = anchor.attr("href").trim()
                val name = anchor.text().trim()
                val typeId = categoryIdFromPath(href)
                if (typeId.isBlank() || name.isBlank() || href in EXCLUDED_NAV_PATHS) null
                else HomeTabItem(typeId, name)
            }
            .distinctBy { it.type_id }
    }

    private fun parseCardList(document: Document, categoryId: String = ""): List<Video> {
        return document.select(".movie-card").mapNotNull { card ->
            val titleLink = card.selectFirst("h1 a[href], h2 a[href], h3 a[href], h4 a[href]")
            val fallbackLink = card.select("a[href]").lastOrNull()
            val link = titleLink ?: fallbackLink ?: return@mapNotNull null
            val href = link.absUrl("href")
            val title = titleLink?.text()?.trim().orEmpty()
                .ifBlank { link.selectFirst("img")?.attr("alt").orEmpty().trim() }
            if (href.isBlank() || title.isBlank()) return@mapNotNull null
            val typeId = if (categoryId.isBlank()) findCategoryId(href) else categoryId
            val remarks = card.selectFirst(".text-xs.text-gray-500")?.text().orEmpty()
            val cardText = card.text()
            Video(
                vod_id = href,
                vod_name = title,
                vod_pic = card.selectFirst("img[src]")?.absUrl("src").orEmpty(),
                vod_remarks = remarks,
                type_id = typeId,
                type_name = categoryName(typeId, defaultTabs()),
                vod_actor = "",
                vod_area = "",
                vod_content = "",
                vod_director = "",
                vod_lang = "",
                vod_play_from = "",
                vod_play_url = "",
                vod_state = "",
                vod_tag = "",
                vod_time = "",
                vod_year = extractYear(remarks),
                vod_score = extractScore(cardText)
            )
        }
    }

    private fun parsePageCount(document: Document): Int {
        return document.select(".pagination-number, .pagination-last")
            .mapNotNull { it.text().trim().toIntOrNull() }
            .maxOrNull() ?: 1
    }

    private fun resolvePlayableUrl(raw: String): String {
        MediaUrlHelper.extractDirectMediaUrl(raw)?.let { return it }
        if (!raw.startsWith("http://") && !raw.startsWith("https://")) return raw

        val body = getResponse(raw).body
        MediaUrlHelper.extractDirectMediaUrl(body)?.let { return it }
        extractIframeUrl(body)?.let { return it }
        extractMediaUrl(body)?.let { return it }
        return raw
    }

    private fun extractIframeUrl(body: String): String? {
        val iframeUrl = Jsoup.parse(body, host).selectFirst("iframe[src]")?.absUrl("src").orEmpty()
        if (iframeUrl.isBlank()) return null
        val iframeBody = getResponse(iframeUrl).body
        return MediaUrlHelper.extractDirectMediaUrl(iframeBody)
            ?: extractMediaUrl(iframeBody)
            ?: iframeUrl
    }

    private fun extractMediaUrl(body: String): String? {
        val matcher = MEDIA_PATTERN.matcher(body)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun extractSwitchSourceUrl(onClick: String): String {
        val matcher = SWITCH_SOURCE_PATTERN.matcher(onClick)
        return if (matcher.find()) matcher.group(1).orEmpty() else ""
    }

    private fun formatPlayUrl(raw: String): String {
        return if (raw.contains('$')) raw else "播放$$raw"
    }

    private fun labelValue(document: Document, label: String): String {
        return document.select("span")
            .firstOrNull { it.text().trim().startsWith("$label：") }
            ?.nextElementSibling()
            ?.text()
            .orEmpty()
    }

    private fun categoryPath(tid: String): String {
        val normalized = tid.trim().trim('/')
        if (normalized.isBlank()) return "/movie"
        return "/$normalized"
    }

    private fun findCategoryId(url: String): String {
        return categoryIdFromPath(url).ifBlank { "movie" }
    }

    private fun categoryIdFromPath(path: String): String {
        val normalized = path.substringBefore('?').substringBefore('#').trim()
        if (!normalized.startsWith("/")) return ""
        val segments = normalized.split('/').filter { it.isNotBlank() }
        if (segments.size != 1) return ""
        val segment = segments.first().lowercase(Locale.getDefault())
        return segment.takeIf { it.isNotBlank() && it !in EXCLUDED_CATEGORY_SEGMENTS }.orEmpty()
    }

    private fun defaultTabs(): List<HomeTabItem> {
        return listOf(
            HomeTabItem("movie", "电影"),
            HomeTabItem("series", "电视剧"),
            HomeTabItem("variety", "综艺"),
            HomeTabItem("anime", "动漫")
        )
    }

    private fun extractYear(text: String): String {
        return Regex("(19|20)\\d{2}").find(text)?.value.orEmpty()
    }

    private fun extractScore(text: String): String {
        return Regex("(?<!\\d)(?:10(?:\\.0)?|[1-9](?:\\.\\d)?)(?!\\d)")
            .findAll(text)
            .map { it.value }
            .firstOrNull { value ->
                value.contains('.') || value.toIntOrNull() in 1..10
            }
            .orEmpty()
    }

    companion object {
        private const val DEFAULT_HOST = "https://ddys.io"
        private val EXCLUDED_NAV_PATHS = setOf("/", "/login", "/collections")
        private val EXCLUDED_CATEGORY_SEGMENTS = setOf("login", "collections", "register", "user", "tag", "search")
        private val SWITCH_SOURCE_PATTERN =
            Pattern.compile("switchSource\\([^,]+,\\s*'([^']+)'")
        private val MEDIA_PATTERN =
            Pattern.compile("(https?://[^'\"\\\\s]+\\.(?:m3u8|mp4)(?:\\?[^'\"\\\\s]*)?)", Pattern.CASE_INSENSITIVE)
    }
}
