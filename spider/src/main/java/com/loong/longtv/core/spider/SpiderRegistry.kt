package com.loong.longtv.core.spider

import com.github.catvod.crawler.Spider
import com.loong.longtv.data.bean.Site
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpiderRegistry @Inject constructor() {

    private val siteFactories: Map<String, (Site) -> Spider> = mapOf(
        "local" to { LocalMediaSpider() },
        "tw_meipa" to { MeipaSpider() },
        "app_v2_template" to { AppV2Spider() },
        "app_rj_template" to { AppRJSpider() },
        "app_get_template" to { AppGetSpider() },
        "app_qi_template" to { AppQiSpider() },
        "tw_feimao_appget" to { FeiMaoAppGetSpider() },
        "tw_jiafei_appqi" to { JiaFeiAppQiSpider() },
        "tw_yunyun_appqi" to { YunYunAppQiSpider() },
        "tw_guangying_appget" to { GuangYingAppGetSpider() },
        "tw_rbotv" to { ReBoAppRJSpider() },
        "tw_lkuys" to { ZhuiJuAppRJSpider() }
        ,
        "tw_ysdqjs" to { TianTianAppRJSpider() }
    )

    private val apiFactories: Map<String, (Site) -> Spider> = mapOf(
        "native_app_v2" to { AppV2Spider() },
        "native_meipa" to { MeipaSpider() },
        "native_app_rj" to { AppRJSpider() },
        "native_app_get" to { AppGetSpider() },
        "native_app_qi" to { AppQiSpider() }
    )

    fun create(site: Site): Spider? {
        siteFactories[site.key]?.let { return it(site) }
        apiFactories[site.api]?.let { return it(site) }
        return when (site.type) {
            0, 1, 2 -> CmsApiSpider(site.api)
            else -> null
        }
    }
}
