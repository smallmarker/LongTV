package com.loong.longtv.core.spider

import com.github.catvod.crawler.Spider
import com.loong.longtv.data.bean.Site
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpiderRegistry @Inject constructor() {

    private val siteFactories: Map<String, (Site) -> Spider> = mapOf(
        "ddys" to { DDysSpider() },
        "dyrs" to { DyrsSpider() }
    )

    private val apiFactories: Map<String, (Site) -> Spider> = mapOf(
        "native_ddys" to { DDysSpider() },
        "native_dyrs" to { DyrsSpider() }
    )

    fun create(site: Site): Spider? {
        siteFactories[site.key]?.let { return it(site) }
        apiFactories[site.api]?.let { return it(site) }
        return null
    }
}
