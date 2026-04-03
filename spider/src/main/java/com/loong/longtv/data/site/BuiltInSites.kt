package com.loong.longtv.data.site

import com.google.gson.JsonPrimitive
import com.loong.longtv.data.bean.Site

object BuiltInSites {

    val items = listOf(
        Site(
            key = "ddys",
            name = "低端影视",
            type = 3,
            api = "native_ddys",
            searchable = 1,
            quickSearch = 1,
            changeable = 1,
            ext = JsonPrimitive("https://ddys.io"),
            playerType = 0,
            enabledByDefault = true
        )
    )
}
