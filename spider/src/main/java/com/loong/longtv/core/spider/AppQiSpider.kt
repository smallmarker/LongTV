package com.loong.longtv.core.spider

open class AppQiSpider : AppGetSpider() {

    override fun defaultConfig(): AppGetConfig {
        return AppGetConfig(
            siteApi = "qiji"
        )
    }

    override fun requestHeaders(timestamp: String): Map<String, String> {
        return buildMap {
            putAll(super.requestHeaders(timestamp))
            put("Accept", "application/json, text/plain, */*")
        }
    }
}

class JiaFeiAppQiSpider : AppQiSpider() {

    override fun defaultConfig(): AppGetConfig {
        return AppGetConfig(
            siteApi = "qiji",
            dataKey = "dksvndkaAjekakcE",
            dataIv = "dksvndkaAjekakcE"
        )
    }
}

class YunYunAppQiSpider : AppQiSpider() {

    override fun defaultConfig(): AppGetConfig {
        return AppGetConfig(
            siteApi = "qiji",
            dataKey = "staraugust123456",
            dataIv = "staraugust123456",
            version = "120"
        )
    }
}
