package com.loong.longtv.nav

import android.net.Uri
import com.google.gson.Gson

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Popular : Screen("popular")
    object Search : Screen("search")
    object History : Screen("history")
    object Favorite : Screen("favorite")
    object Settings : Screen("settings")
    object SearchResult : Screen("search_result/{keyword}") {
        fun createRoute(keyword: String) = "search_result/${Uri.encode(keyword)}"
    }

    object VideoDetail : Screen("video_detail/{key}/{id}") {
        fun createRoute(key: String, id: String) = "video_detail/${Uri.encode(key)}/${Uri.encode(id)}"
    }

    object FullscreenPlayer : Screen("fullscreen_player/{title}/{url}/{headers}/{position}/{episodeIndex}/{episodeCount}") {
        fun createRoute(
            title: String,
            url: String,
            headers: Map<String, String>,
            position: Long,
            episodeIndex: Int,
            episodeCount: Int
        ): String {
            val headersJson = Gson().toJson(headers)
            return "fullscreen_player/${Uri.encode(title)}/${Uri.encode(url)}/${Uri.encode(headersJson)}/$position/$episodeIndex/$episodeCount"
        }
    }
}
