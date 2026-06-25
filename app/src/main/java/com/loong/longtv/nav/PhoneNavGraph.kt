package com.loong.longtv.nav

import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.loong.longtv.ui.phone.detail.PhoneFullscreenPlayerScreen
import com.loong.longtv.ui.phone.detail.VideoDetailPhoneScreen
import com.loong.longtv.ui.phone.favorite.FavoritePhoneScreen
import com.loong.longtv.ui.phone.history.HistoryPhoneScreen
import com.loong.longtv.ui.phone.home.HomePhoneScreen
import com.loong.longtv.ui.phone.popular.PopularPhoneScreen
import com.loong.longtv.ui.phone.search.SearchPhoneScreen
import com.loong.longtv.ui.phone.search.SearchResultPhoneScreen
import com.loong.longtv.ui.phone.settings.SettingsPhoneScreen

@Composable
fun PhoneNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val navModifier = if (route == Screen.FullscreenPlayer.route) {
        Modifier
    } else {
        Modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal
            )
        )
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = navModifier
    ) {
        composable(Screen.Home.route) {
            HomePhoneScreen(navController = navController)
        }

        composable(Screen.Popular.route) {
            PopularPhoneScreen(navController = navController)
        }

        composable(Screen.Search.route) {
            SearchPhoneScreen(navController = navController)
        }

        composable(Screen.History.route) {
            HistoryPhoneScreen(navController = navController)
        }

        composable(Screen.Favorite.route) {
            FavoritePhoneScreen(navController = navController)
        }

        composable(Screen.Settings.route) {
            SettingsPhoneScreen(navController = navController)
        }

        composable(
            Screen.SearchResult.route,
            arguments = listOf(navArgument("keyword") { type = NavType.StringType })
        ) {
            val keyword = Uri.decode(it.arguments?.getString("keyword") ?: "")
            SearchResultPhoneScreen(
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
            VideoDetailPhoneScreen(
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
            PhoneFullscreenPlayerScreen(
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
