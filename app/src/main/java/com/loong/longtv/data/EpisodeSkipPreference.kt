package com.loong.longtv.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class EpisodeSkipSettings(
    val introEndMs: Long? = null,
    val outroRemainingMs: Long? = null
) {
    internal fun playbackRange(durationMs: Long, hasNextEpisode: Boolean): LongRange? {
        val start = introEndMs ?: 0L
        val end = durationMs - if (hasNextEpisode) outroRemainingMs ?: 0L else 0L
        return if (start >= 0 && end <= durationMs && end - start >= 1_000L) start..end else null
    }
}

class EpisodeSkipPreference @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("longtv_episode_skip", Context.MODE_PRIVATE)

    fun load(siteKey: String, videoId: String): EpisodeSkipSettings {
        val key = key(siteKey, videoId)
        return EpisodeSkipSettings(
            introEndMs = prefs.getLong("$key.introEnd", 0L).takeIf { it > 0L },
            outroRemainingMs = prefs.getLong("$key.outroRemaining", 0L).takeIf { it > 0L }
        )
    }

    fun save(siteKey: String, videoId: String, settings: EpisodeSkipSettings) {
        val key = key(siteKey, videoId)
        prefs.edit()
            .putLong("$key.introEnd", settings.introEndMs ?: 0L)
            .putLong("$key.outroRemaining", settings.outroRemainingMs ?: 0L)
            .apply()
    }

    private fun key(siteKey: String, videoId: String): String = "${siteKey.length}:$siteKey$videoId"
}
