package com.loong.longtv.data.site

import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.loong.longtv.data.bean.Site

object BuiltInSites {

    val items = listOf(
        Site(
            key = "local",
            name = "本地媒体",
            type = 3,
            api = "local",
            searchable = 1,
            quickSearch = 1,
            changeable = 0,
            ext = null,
            playerType = 0
        ),
        Site(
            key = "cms_template",
            name = "CMS 模板站点",
            type = 1,
            api = "https://example.com/api.php/provide/vod/",
            searchable = 1,
            quickSearch = 1,
            changeable = 1,
            ext = null,
            playerType = 0,
            enabledByDefault = false
        ),
        Site(
            key = "tw_bfzy",
            name = "TVBoxTW | 暴风采集",
            type = 1,
            api = "https://bfzyapi.com/api.php/provide/vod",
            searchable = 1,
            quickSearch = 1,
            changeable = 1,
            ext = null,
            playerType = 0,
            enabledByDefault = true
        ),
        Site(
            key = "app_v2_template",
            name = "APPV2 模板站点",
            type = 3,
            api = "native_app_v2",
            searchable = 1,
            quickSearch = 1,
            changeable = 1,
            ext = JsonPrimitive("https://example.com"),
            playerType = 0,
            enabledByDefault = false
        ),
        Site(
            key = "app_rj_template",
            name = "AppRJ 模板站点",
            type = 3,
            api = "native_app_rj",
            searchable = 1,
            quickSearch = 1,
            changeable = 1,
            ext = JsonPrimitive("http://example.com"),
            playerType = 0,
            enabledByDefault = false
        ),
        Site(
            key = "app_get_template",
            name = "AppGet 模板站点",
            type = 3,
            api = "native_app_get",
            searchable = 1,
            quickSearch = 1,
            changeable = 1,
            ext = JsonParser.parseString(
                """{"url":"https://example.com","dataKey":"1234567890123456","dataIv":"1234567890123456","version":"119"}"""
            ),
            playerType = 0,
            enabledByDefault = false
        ),
        Site(
            key = "app_qi_template",
            name = "AppQi 模板站点",
            type = 3,
            api = "native_app_qi",
            searchable = 1,
            quickSearch = 1,
            changeable = 1,
            ext = JsonParser.parseString(
                """{"url":"","site":"https://example.com/host.txt","dataKey":"1234567890123456","dataIv":"1234567890123456","deviceId":"","version":"120","ua":""}"""
            ),
            playerType = 0,
            enabledByDefault = false
        ),
        Site(
            key = "tw_fuju",
            name = "TVBoxTW | 腐剧 APPV2",
            type = 3,
            api = "native_app_v2",
            searchable = 1,
            quickSearch = 1,
            changeable = 1,
            ext = JsonPrimitive("http://www.fuju2024.cc:8013/ruifenglb_api.php/v1.vod"),
            playerType = 0,
            enabledByDefault = false
        ),
        Site(
            key = "tw_rbotv",
            name = "TVBoxTW | 热播 AppRJ",
            type = 3,
            api = "native_app_rj",
            searchable = 1,
            quickSearch = 1,
            changeable = 1,
            ext = JsonPrimitive("http://v.rbotv.cn"),
            playerType = 0,
            enabledByDefault = false
        ),
        Site(
            key = "tw_lkuys",
            name = "TVBoxTW | 浪酷 AppRJ",
            type = 3,
            api = "native_app_rj",
            searchable = 1,
            quickSearch = 1,
            changeable = 1,
            ext = JsonPrimitive("http://v.lkuys.cn"),
            playerType = 0,
            enabledByDefault = false
        ),
        Site(
            key = "tw_ysdqjs",
            name = "TVBoxTW | 天天 AppRJ",
            type = 3,
            api = "native_app_rj",
            searchable = 1,
            quickSearch = 1,
            changeable = 1,
            ext = JsonPrimitive("http://tt.ysdqjs.cn"),
            playerType = 0,
            enabledByDefault = false
        ),
        Site(
            key = "tw_feimao_appget",
            name = "TVBoxTW | 肥猫 AppGet",
            type = 3,
            api = "native_app_get",
            searchable = 1,
            quickSearch = 1,
            changeable = 1,
            ext = JsonParser.parseString(
                """{"url":"https://mac.555618.xyz","dataKey":"#getapp@TMD@2025","siteApi":"get","version":"201"}"""
            ),
            playerType = 0,
            enabledByDefault = false
        ),
        Site(
            key = "tw_guangying_appget",
            name = "TVBoxTW | 光影 AppGet",
            type = 3,
            api = "native_app_get",
            searchable = 1,
            quickSearch = 1,
            changeable = 1,
            ext = JsonParser.parseString(
                """{"url":"https://www.guangyingmi.com","dataKey":"230fnsodfsdhfon2","dataIv":"230fnsodfsdhfon2","deviceId":"","version":""}"""
            ),
            playerType = 0,
            enabledByDefault = false
        ),
        Site(
            key = "tw_jiafei_appqi",
            name = "TVBoxTW | 加菲 AppQi",
            type = 3,
            api = "native_app_qi",
            searchable = 1,
            quickSearch = 1,
            changeable = 1,
            ext = JsonParser.parseString(
                """{"url":"","site":"https://aysappto.oss-cn-chengdu.aliyuncs.com/qj1.txt","dataKey":"dksvndkaAjekakcE","dataIv":"dksvndkaAjekakcE","deviceId":"","version":"","ua":""}"""
            ),
            playerType = 0,
            enabledByDefault = false
        ),
        Site(
            key = "tw_yunyun_appqi",
            name = "TVBoxTW | 云云 AppQi",
            type = 3,
            api = "native_app_qi",
            searchable = 1,
            quickSearch = 1,
            changeable = 1,
            ext = JsonParser.parseString(
                """{"url":"","site":"https://staraugust123456.oss-cn-hangzhou.aliyuncs.com/1.txt","dataKey":"staraugust123456","dataIv":"staraugust123456","deviceId":"","version":"120"}"""
            ),
            playerType = 0,
            enabledByDefault = false
        ),
        Site(
            key = "tw_lzzy",
            name = "TVBoxTW | 量子采集",
            type = 1,
            api = "http://cj.lziapi.com/api.php/provide/vod/",
            searchable = 1,
            quickSearch = 1,
            changeable = 1,
            ext = null,
            playerType = 0,
            enabledByDefault = true
        ),
        Site(
            key = "tw_360zy",
            name = "TVBoxTW | 360 资源",
            type = 1,
            api = "https://360zy.com/api.php/provide/vod",
            searchable = 1,
            quickSearch = 1,
            changeable = 1,
            ext = null,
            playerType = 0,
            enabledByDefault = true
        )
    )
}
