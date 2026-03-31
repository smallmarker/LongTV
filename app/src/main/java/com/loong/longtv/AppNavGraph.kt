package com.loong.longtv

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.loong.longtv.ui.detail.VideoDetailScreen
import com.loong.longtv.ui.detail.FullscreenPlayerScreen
import com.loong.longtv.ui.favorite.FavoriteScreen
import com.loong.longtv.ui.home.HomeScreen
import com.loong.longtv.ui.history.HistoryScreen
import com.loong.longtv.ui.search.SearchResultScreen
import com.loong.longtv.ui.search.SearchScreen
import com.loong.longtv.ui.settings.SettingsScreen

/**
 * @author zl
 * @Date 2025/10/21
 **/

sealed class Screen(val route: String) {
    object Home : Screen("home")
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

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(navController)
        }

        composable(Screen.Search.route) {
            SearchScreen(navController)
        }

        composable(Screen.History.route) {
            HistoryScreen(navController)
        }

        composable(Screen.Favorite.route) {
            FavoriteScreen(navController)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }

        composable(
            Screen.SearchResult.route,
            arguments = listOf(navArgument("keyword") {
                type = NavType.StringType
            })
        ) {
            val keyword = Uri.decode(it.arguments?.getString("keyword") ?: "")
            SearchResultScreen(navController, keyword)
        }

        composable(
            Screen.VideoDetail.route,
            arguments = listOf(
                navArgument("key") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType }
            )
        ) {
            val key = Uri.decode(it.arguments?.getString("key") ?: "")
            val id = Uri.decode(it.arguments?.getString("id") ?: "")
            VideoDetailScreen(navController, key, id)
        }

        composable(
            Screen.FullscreenPlayer.route,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType },
                navArgument("headers") { type = NavType.StringType },
                navArgument("position") { type = NavType.LongType },
                navArgument("episodeIndex") { type = NavType.IntType },
                navArgument("episodeCount") { type = NavType.IntType }
            )
        ) {
            val title = Uri.decode(it.arguments?.getString("title") ?: "")
            val url = Uri.decode(it.arguments?.getString("url") ?: "")
            val headersJson = Uri.decode(it.arguments?.getString("headers") ?: "{}")
            val headers = Gson().fromJson(headersJson, Map::class.java)
                ?.mapKeys { entry -> entry.key?.toString().orEmpty() }
                ?.mapValues { entry -> entry.value?.toString().orEmpty() }
                .orEmpty()
            val position = it.arguments?.getLong("position") ?: 0L
            val episodeIndex = it.arguments?.getInt("episodeIndex") ?: 0
            val episodeCount = it.arguments?.getInt("episodeCount") ?: 0
            FullscreenPlayerScreen(
                navController = navController,
                title = title,
                videoUrl = url,
                requestHeaders = headers,
                initialPositionMs = position,
                episodeIndex = episodeIndex,
                episodeCount = episodeCount
            )
        }
    }
}
