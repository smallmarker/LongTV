package com.loong.longtv.core.spider

import com.google.gson.Gson
import com.loong.longtv.data.bean.CategoryContent
import com.loong.longtv.data.bean.HomeContent
import com.loong.longtv.data.bean.PlayerContent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

@Ignore("Manual smoke tests for live private sites.")
class PrivateSiteSpiderSmokeTest {

    private val gson = Gson()

    @Test
    fun ddysSmokeTest() {
        smokeTest(
            spider = DDysSpider(),
            extend = "https://ddys.io",
            keyword = "流浪地球"
        )
    }

    @Test
    fun dyrsSmokeTest() {
        smokeTest(
            spider = DyrsSpider(),
            extend = "https://dyrs.net",
            keyword = "流浪地球"
        )
    }

    private fun smokeTest(
        spider: PrivateHtmlSpider,
        extend: String,
        keyword: String
    ) {
        spider.init(null, extend)

        val home = gson.fromJson(spider.homeContent(true), HomeContent::class.java)
        assertTrue("Home tabs should not be empty", home.tabs.isNotEmpty())

        val homeVideos = gson.fromJson(spider.homeVideoContent(), CategoryContent::class.java)
        assertTrue("Home videos should not be empty", homeVideos.list.isNotEmpty())

        val search = gson.fromJson(spider.searchContent(keyword, false), CategoryContent::class.java)
        assertTrue("Search results should not be empty", search.list.isNotEmpty())

        val detailId = search.list.first().vod_id
        val detail = gson.fromJson(
            spider.detailContent(mutableListOf(detailId)),
            CategoryContent::class.java
        )
        val video = detail.list.firstOrNull()
        assertNotNull("Detail video should exist", video)
        assertFalse("Play infos should not be empty", video!!.playInfos.isEmpty())

        val firstSource = video.playInfos.first()
        val firstEpisode = firstSource.urlInfos.firstOrNull()
        assertNotNull("First episode should exist", firstEpisode)

        val player = gson.fromJson(
            spider.playerContent(firstSource.flag, firstEpisode!!.url, mutableListOf()),
            PlayerContent::class.java
        )
        assertTrue(
            "Player url should not be blank",
            player.url.isNotBlank() || player.playUrl.isNotBlank()
        )
    }
}
