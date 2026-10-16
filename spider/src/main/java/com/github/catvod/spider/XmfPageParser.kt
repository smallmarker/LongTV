package com.github.catvod.spider

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Document

internal object XmfPageParser {
    val FILTER_KEYS = setOf("type_id", "genre", "area", "year", "language", "sort")

    fun isHome(doc: Document): Boolean =
        doc.title().contains("小蜜蜂") && categories(doc).isNotEmpty() && items(doc).isNotEmpty()

    fun supportsPage(doc: Document, path: String): Boolean = when {
        path == "/" -> isHome(doc)
        path.startsWith("/play/") -> playerUrl(doc).isNotBlank()
        path.startsWith("/vod/type/") || path.startsWith("/search?") -> doc.selectFirst("main .grid") != null
        path.startsWith("/vod/") -> doc.selectFirst(".detail h1") != null &&
            doc.selectFirst(".episodes a[href^=/play/]") != null
        else -> false
    }

    fun categories(doc: Document): List<Map<String, String>> =
        doc.select("header nav a[href], .nav-panel nav a[href]").mapNotNull { a ->
            val id = Regex("^/vod/type/(\\d+)$").find(a.attr("href"))?.groupValues?.get(1)
            val name = a.text().trim()
            if (id == null || name.isBlank()) null else mapOf("type_id" to id, "type_name" to name)
        }.distinctBy { it["type_id"] }

    fun items(doc: Document): List<Map<String, String>> = doc.select("article.card").mapNotNull { card ->
        val link = card.selectFirst("a[href]") ?: return@mapNotNull null
        val id = Regex("^/vod/(\\d+)$").find(link.attr("href"))?.groupValues?.get(1)
            ?: return@mapNotNull null
        val name = card.selectFirst("h3")?.text().orEmpty().trim()
        if (name.isBlank()) return@mapNotNull null
        val img = card.selectFirst("img")
        mapOf("vod_id" to "xmftv:$id", "vod_name" to name,
            "vod_pic" to (img?.absUrl("data-src").orEmpty().ifBlank { img?.absUrl("src").orEmpty() }),
            "vod_remarks" to card.selectFirst(".remark")?.text().orEmpty(),
            "vod_score" to card.selectFirst("p em")?.text().orEmpty())
    }.distinctBy { it["vod_id"] }

    fun filters(doc: Document): List<Map<String, Any>> = doc.select(".filter-row").mapNotNull { row ->
        val urls = row.select("a[href]").mapNotNull { it.absUrl("href").toHttpUrlOrNull() }
        val key = urls.asSequence().flatMap { it.queryParameterNames.asSequence() }
            .firstOrNull { it in FILTER_KEYS } ?: return@mapNotNull null
        val values = row.select("a[href]").map { a ->
            mapOf("n" to a.text(), "v" to a.absUrl("href").toHttpUrlOrNull()?.queryParameter(key).orEmpty())
        }.distinctBy { it["v"] }
        mapOf("key" to key, "name" to row.selectFirst("b")?.text().orEmpty(), "value" to values)
    }

    fun pageCount(doc: Document): Int = doc.select("a[href]").mapNotNull {
        it.absUrl("href").toHttpUrlOrNull()?.queryParameter("page")?.toIntOrNull()
    }.maxOrNull() ?: 1

    fun detail(doc: Document, id: String): Map<String, String> {
        val tags = doc.select(".detail-tags span").map { it.text() }
        fun field(name: String) = doc.select(".detail dt").firstOrNull { it.text() == name }
            ?.nextElementSibling()?.text().orEmpty()
        val episodes = doc.select(".episodes a[href]").filter {
            it.attr("href").matches(Regex("/play/\\d+/\\d+"))
        }.distinctBy { it.attr("href") }
        check(doc.selectFirst(".detail h1") != null && episodes.isNotEmpty()) { "小蜜蜂详情结构已变化或没有选集" }
        return mapOf("vod_id" to id, "vod_name" to doc.selectFirst(".detail h1")!!.text(),
            "vod_pic" to doc.selectFirst("img.poster")?.absUrl("src").orEmpty(),
            "vod_remarks" to doc.selectFirst(".poster-wrap span")?.text().orEmpty(),
            "vod_year" to tags.getOrNull(0).orEmpty(), "vod_area" to tags.getOrNull(1).orEmpty(),
            "type_name" to tags.getOrNull(2).orEmpty(), "vod_lang" to field("语言"),
            "vod_actor" to field("主演"), "vod_director" to field("导演"),
            "vod_content" to doc.selectFirst(".summary-panel p")?.text().orEmpty(),
            "vod_play_from" to doc.selectFirst(".source-label")?.text().orEmpty().ifBlank { "小蜜蜂线路" },
            "vod_play_url" to episodes.joinToString("#") { "${it.text()}\$${it.attr("href")}" })
    }

    fun playerUrl(doc: Document): String {
        val options = doc.select("[data-parser-select] option[value]")
        val direct = options.firstOrNull { it.attr("data-emergency") == "false" }
            ?: options.firstOrNull()
        return direct?.absUrl("value").orEmpty().takeIf { it.toHttpUrlOrNull() != null }.orEmpty()
    }
}
