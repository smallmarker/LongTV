package com.loong.longtv.core.spider

class ZhuiJuAppRJSpider : AppRJSpider(
    defaultHost = "http://v.lkuys.cn"
) {

    private val allowedFlags = listOf("nby", "mgtv", "qq", "qiyi")

    override fun shouldKeepPlaySource(flag: String): Boolean {
        val lower = flag.lowercase()
        return allowedFlags.any(lower::contains)
    }

    override fun playbackHeaders(): Map<String, String> {
        return buildMap {
            putAll(super.playbackHeaders())
            put("Content-Type", "application/x-www-form-urlencoded")
        }
    }

    override fun featuredTypeId(): String = "0"
}

class ReBoAppRJSpider : AppRJSpider(
    defaultHost = "http://v.rbotv.cn"
) {

    private val allowedFlags = listOf("nby", "mgtv", "qq", "qiyi")

    override fun shouldKeepPlaySource(flag: String): Boolean {
        val lower = flag.lowercase()
        return allowedFlags.any(lower::contains)
    }

    override fun playbackHeaders(): Map<String, String> {
        return buildMap {
            putAll(super.playbackHeaders())
            put("Content-Type", "application/x-www-form-urlencoded")
        }
    }

    override fun featuredTypeId(): String = "0"
}

class TianTianAppRJSpider : AppRJSpider(
    defaultHost = "http://tt.ysdqjs.cn"
) {

    private val allowedFlags = listOf("nby", "mgtv", "qq", "qiyi")

    override fun shouldKeepPlaySource(flag: String): Boolean {
        val lower = flag.lowercase()
        return allowedFlags.any(lower::contains)
    }

    override fun playbackHeaders(): Map<String, String> {
        return buildMap {
            putAll(super.playbackHeaders())
            put("Content-Type", "application/x-www-form-urlencoded")
        }
    }

    override fun featuredTypeId(): String = "0"
}
