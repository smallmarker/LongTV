package com.loong.longtv.nav

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

internal fun fullscreenPlayerArguments(): List<NamedNavArgument> {
    return listOf(
        navArgument("title") { type = NavType.StringType },
        navArgument("url") { type = NavType.StringType },
        navArgument("headers") { type = NavType.StringType },
        navArgument("position") { type = NavType.LongType },
        navArgument("episodeIndex") { type = NavType.IntType },
        navArgument("episodeCount") { type = NavType.IntType }
    )
}
