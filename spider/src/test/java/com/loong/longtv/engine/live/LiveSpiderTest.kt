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
}
