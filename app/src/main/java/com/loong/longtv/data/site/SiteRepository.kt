package com.loong.longtv.data.site

import android.content.Context
import android.content.SharedPreferences
import com.loong.longtv.data.bean.Site
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface SiteRepository {
    fun getAllSites(): List<Site>
    fun getEnabledSites(): List<Site>
    fun getSite(key: String): Site?
    fun getDefaultSite(): Site
    fun setDefaultSite(key: String)
    fun setSiteEnabled(key: String, enabled: Boolean)
}

@Singleton
class SiteRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : SiteRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("longtv_site_prefs", Context.MODE_PRIVATE)

    override fun getAllSites(): List<Site> = BuiltInSites.items

    override fun getEnabledSites(): List<Site> =
        getAllSites().filter { prefs.getBoolean(enabledKey(it.key), it.enabledByDefault) }

    override fun getSite(key: String): Site? =
        getAllSites().firstOrNull { it.key == key }

    override fun getDefaultSite(): Site {
        val key = prefs.getString(KEY_DEFAULT_SITE, null)
        return getSite(key.orEmpty()) ?: getEnabledSites().firstOrNull() ?: getAllSites().first()
    }

    override fun setDefaultSite(key: String) {
        prefs.edit().putString(KEY_DEFAULT_SITE, key).apply()
    }

    override fun setSiteEnabled(key: String, enabled: Boolean) {
        prefs.edit().putBoolean(enabledKey(key), enabled).apply()
    }

    private fun enabledKey(key: String) = "site_enabled_$key"

    companion object {
        private const val KEY_DEFAULT_SITE = "default_site"
    }
}
