package com.loong.longtv.data

import android.content.Context
import com.loong.longtv.di.DefaultSources
import com.loong.longtv.engine.config.ConfigSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val PREFS_NAME = "longtv_source_prefs"
private const val KEY_TYPE = "source_type"
private const val KEY_NAME = "source_name"
private const val KEY_DATA = "source_data"
private const val KEY_POPULAR_SITE = "popular_site_key"

class SourcePreference(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _popularSiteKey = MutableStateFlow(prefs.getString(KEY_POPULAR_SITE, null))
    val popularSiteKey: StateFlow<String?> = _popularSiteKey

    fun save(source: ConfigSource) {
        val (type, data) = when (source) {
            is ConfigSource.Asset -> "asset" to source.assetPath
        }
        prefs.edit()
            .putString(KEY_TYPE, type)
            .putString(KEY_NAME, source.displayName())
            .putString(KEY_DATA, data)
            .apply()
    }

    fun load(): ConfigSource? {
        val type = prefs.getString(KEY_TYPE, null) ?: return null
        val name = prefs.getString(KEY_NAME, null) ?: return null
        val data = prefs.getString(KEY_DATA, null) ?: return null
        return when (type) {
            "asset" -> ConfigSource.Asset(name = name, assetPath = data)
            else -> DefaultSources.default
        }
    }

    fun savePopularSiteKey(siteKey: String) {
        prefs.edit()
            .putString(KEY_POPULAR_SITE, siteKey)
            .apply()
        _popularSiteKey.value = siteKey
    }

    fun loadPopularSiteKey(): String? {
        return prefs.getString(KEY_POPULAR_SITE, null)
    }
}
