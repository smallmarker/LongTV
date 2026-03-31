package com.loong.longtv.core.spider

class FeiMaoAppGetSpider : AppGetSpider() {

    override fun defaultConfig(): AppGetConfig {
        return AppGetConfig(
            host = "https://mac.555618.xyz",
            dataKey = "#getapp@TMD@2025",
            version = "201",
            siteApi = "get"
        )
    }

    override fun requestHeaders(timestamp: String): Map<String, String> {
        return buildMap {
            putAll(super.requestHeaders(timestamp))
            put("Accept", "application/json, text/plain, */*")
        }
    }
}

class GuangYingAppGetSpider : AppGetSpider() {

    override fun defaultConfig(): AppGetConfig {
        return AppGetConfig(
            host = "https://www.guangyingmi.com",
            dataKey = "230fnsodfsdhfon2",
            dataIv = "230fnsodfsdhfon2"
        )
    }

    override fun requestHeaders(timestamp: String): Map<String, String> {
        return buildMap {
            putAll(super.requestHeaders(timestamp))
            put("Accept", "application/json, text/plain, */*")
        }
    }
}
