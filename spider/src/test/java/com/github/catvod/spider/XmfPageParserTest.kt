package com.github.catvod.spider

import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class XmfPageParserTest {
    private fun doc(html: String) = Jsoup.parse(html, "https://www.xmftv.com/")
    private val card = """
        <article class="card"><a href="/vod/665"><img src="/media/44603.jpg">
        <h3>吞噬星空</h3><span class="remark">第245集</span><p><em>6.8</em></p></a></article>
    """.trimIndent()
    private val home = """
        <title>小蜜蜂TV</title><header><nav><a href="/vod/type/17">动漫</a></nav></header>
        <main><div class="grid">$card</div></main>
    """.trimIndent()

    @Test fun validatesContentInsteadOfTitleAlone() {
        assertTrue(XmfPageParser.isHome(doc(home)))
        assertFalse(XmfPageParser.isHome(doc("<title>小蜜蜂TV</title><p>正在跳转</p>")))
        assertFalse(XmfPageParser.isHome(doc(home.replace("<h3>吞噬星空</h3>", ""))))
        assertFalse(XmfPageParser.isHome(doc(home.replace("小蜜蜂TV", "域名出售"))))
    }

    @Test fun readsNavigationIdsWithoutAssumingLegacyIds() {
        val page = doc(home + "<section class='nav-panel'><nav><a href='/vod/type/17'>动漫</a><a href='/vod/type/2'>电影</a></nav></section>")
        assertEquals(listOf("17", "2"), XmfPageParser.categories(page).map { it["type_id"] })
    }

    @Test fun resolvesImagesAndSeparatesNewCatalogueIds() {
        val items = XmfPageParser.items(doc(card + card))
        assertEquals(1, items.size)
        assertEquals("xmftv:665", items.single()["vod_id"])
        assertEquals("https://www.xmftv.com/media/44603.jpg", items.single()["vod_pic"])
        assertEquals("6.8", items.single()["vod_score"])
    }

    @Test fun readsDecodedFiltersAndLastPage() {
        val page = doc("""
            <div class="filter-row"><b>地区</b><a href="/vod/type/2">全部</a>
            <a href="/vod/type/2?area=%E4%B8%AD%E5%9B%BD">中国</a></div>
            <a href="/vod/type/2?page=2">下一页</a><a href="/vod/type/2?page=1628">末页</a>
        """)
        val filter = XmfPageParser.filters(page).single()
        assertEquals("area", filter["key"])
        assertEquals(listOf(mapOf("n" to "全部", "v" to ""), mapOf("n" to "中国", "v" to "中国")), filter["value"])
        assertEquals(1628, XmfPageParser.pageCount(page))
    }

    @Test fun readsMetadataAndRelativeEpisodePaths() {
        val page = doc("""
            <article class="detail"><img class="poster" src="/media/1.jpg"><h1>影片</h1>
            <div class="detail-tags"><span>2026</span><span>中国</span><span>动画</span></div>
            <dl><dt>主演</dt><dd>演员</dd></dl></article><section class="summary-panel"><p>简介</p></section>
            <div class="episodes"><a href="/play/665/1">第01集</a><a href="/play/665/2">第02集</a></div>
        """)
        val item = XmfPageParser.detail(page, "xmftv:665")
        assertEquals("演员", item["vod_actor"])
        assertEquals("简介", item["vod_content"])
        assertEquals("第01集\$/play/665/1#第02集\$/play/665/2", item["vod_play_url"])
    }

    @Test fun selectsDirectStreamAndResolvesFallbackWhenAlone() {
        assertEquals("https://cdn.example.com/index.m3u8", XmfPageParser.playerUrl(doc("""
            <select data-parser-select><option data-emergency="true" value="/hls/manifest.m3u8?u=abc&amp;s=123">备用</option>
            <option data-emergency="false" value="https://cdn.example.com/index.m3u8">直连</option></select>
        """)))
        assertEquals("https://www.xmftv.com/hls/manifest.m3u8?u=abc&s=123", XmfPageParser.playerUrl(doc("""
            <select data-parser-select><option value="/hls/manifest.m3u8?u=abc&amp;s=123">备用</option></select>
        """)))
        assertEquals("", XmfPageParser.playerUrl(doc("<select data-parser-select><option value='javascript:void(0)'>无效</option></select>")))
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsChangedDetailInsteadOfReturningEmptyEpisodes() {
        XmfPageParser.detail(doc("<h1>维护中</h1>"), "xmftv:665")
    }

    @Test fun distinguishesEmptySearchResultsFromInvalidPages() {
        val emptySearch = doc("<main><div class='grid'></div></main>")
        assertTrue(XmfPageParser.supportsPage(emptySearch, "/search?q=missing"))
        assertFalse(XmfPageParser.supportsPage(emptySearch, "/"))
        assertFalse(XmfPageParser.supportsPage(doc("<h1>验证页面</h1>"), "/vod/type/2"))
        assertFalse(XmfPageParser.supportsPage(doc(home), "/play/665/1"))
    }
}
