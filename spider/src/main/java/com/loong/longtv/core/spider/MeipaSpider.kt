package com.loong.longtv.core.spider

class MeipaSpider : AppV2Spider(
    defaultHost = "https://g.c494.com",
    defaultHeaders = mapOf(
        "User-Agent" to "Dart/2.10 (dart:io)",
        "platform_version" to "RP1A.200720.011",
        "version" to "2.2.3",
        "copyright" to "xiaogui",
        "platform" to "android",
        "client_name" to "576O5p+P5b2x6KeG"
    )
) {
    override fun playbackHeaders(): Map<String, String> {
        return mapOf("User-Agent" to "Lavf/58.12.100")
    }
}
