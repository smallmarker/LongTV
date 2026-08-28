package com.loong.longtv.nav

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.loong.longtv.ui.tv.child.ChildEducationTvScreen
import com.loong.longtv.ui.tv.detail.TvFullscreenPlayerScreen
import com.loong.longtv.ui.tv.detail.VideoDetailTvScreen
import com.loong.longtv.ui.tv.favorite.FavoriteTvScreen
import com.loong.longtv.ui.tv.history.HistoryTvScreen
import com.loong.longtv.ui.tv.home.HomeTvScreen
import com.loong.longtv.ui.tv.live.LiveTvScreen
import com.loong.longtv.ui.tv.popular.PopularTvScreen
import com.loong.longtv.ui.tv.search.SearchResultTvScreen
import com.loong.longtv.ui.tv.search.SearchTvScreen
import com.loong.longtv.ui.tv.section.ContentSectionTvScreen
import com.loong.longtv.ui.tv.settings.SettingsTvScreen

@Composable
fun TvNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeTvScreen(navController = navController)
        }

        composable(Screen.Popular.route) {
            PopularTvScreen(navController = navController)
        }

        composable(Screen.ChildEducation.route) {
            ChildEducationTvScreen(navController = navController)
        }

        composable(
            Screen.ContentSection.route,
            arguments = listOf(navArgument("sectionId") { type = NavType.StringType })
        ) {
            ContentSectionTvScreen(navController = navController)
        }

        composable(Screen.Live.route) {
            LiveTvScreen(navController = navController)
        }

        composable(Screen.Search.route) {
            SearchTvScreen(navController = navController)
        }

        composable(Screen.History.route) {
            HistoryTvScreen(navController = navController)
        }

        composable(Screen.Favorite.route) {
            FavoriteTvScreen(navController = navController)
        }

        composable(Screen.Settings.route) {
            SettingsTvScreen(navController = navController)
        }

        composable(
            Screen.SearchResult.route,
            arguments = listOf(navArgument("keyword") { type = NavType.StringType })
        ) {
            val keyword = Uri.decode(it.arguments?.getString("keyword") ?: "")
            SearchResultTvScreen(
                navigator = navController,
                keyword = keyword
            )
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
            VideoDetailTvScreen(
                navigator = navController,
                key = key,
                id = id
            )
        }

        composable(
            Screen.FullscreenPlayer.route,
            arguments = fullscreenPlayerArguments()
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
            TvFullscreenPlayerScreen(
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
