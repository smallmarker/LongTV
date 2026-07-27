package com.loong.longtv.engine.live

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveSpiderTest {

    @Test
    fun parseM3uChannels() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-name="CCTV-1" tvg-logo="https://example.com/cctv1.png" group-title="央视",CCTV-1 综合
            https://example.com/cctv1.m3u8
            #EXTINF:-1 group-title="卫视",湖南卫视
            https://example.com/hunan.m3u8
        """.trimIndent()

        val channels = M3uLiveParser.parse(content, "test")

        assertEquals(2, channels.size)
        assertEquals("CCTV-1 综合", channels[0].name)
        assertEquals("央视", channels[0].group)
        assertEquals("https://example.com/cctv1.png", channels[0].logo)
        assertEquals("https://example.com/hunan.m3u8", channels[1].url)
    }

    @Test
    fun parseTxtChannels() {
        val content = """
            央视,#genre#
            CCTV-1 综合,https://example.com/cctv1.m3u8
            卫视,#genre#
            湖南卫视,https://example.com/hunan.m3u8
        """.trimIndent()

        val channels = TxtLiveParser.parse(content, "txt")

        assertEquals(2, channels.size)
        assertEquals("央视", channels[0].group)
        assertEquals("湖南卫视", channels[1].name)
        assertEquals("卫视", channels[1].group)
    }

    @Test
    fun parseTxtSkipsSchedulePlaceholders() {
        val content = """
            女篮赛评,#genre#
            07-19⬇️全国女子篮球锦标赛⬇️🏀📡,http://rihou.cc:555/tv/t
            14:00上海浦发银行🆚福建金篮,http://rihou.cc:555/tv/v
            国🔊19篮评述,http://example.com/live/19.m3u8
            CCTV-10接口,http://42.194.148.122:1234/624878405
        """.trimIndent()

        val channels = TxtLiveParser.parse(content, "txt")

        assertEquals(1, channels.size)
        assertEquals("国🔊19篮评述", channels[0].name)
        assertEquals("http://example.com/live/19.m3u8", channels[0].url)
    }
}
