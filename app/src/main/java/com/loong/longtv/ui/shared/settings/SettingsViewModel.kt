package com.loong.longtv.ui.shared.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loong.longtv.data.bean.Site
import com.loong.longtv.data.player.parser.LocalParserRegistry
import com.loong.longtv.data.site.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val defaultSiteKey: String = "",
    val sites: List<SiteToggleItem> = emptyList(),
    val parserNames: List<String> = emptyList()
)

data class SiteToggleItem(
    val site: Site,
    val enabled: Boolean
)

enum class SpiderFamily(val label: String) {
    PRIVATE("私有模板"),
    LOCAL("本地"),
    CMS("CMS"),
    APP_V2("AppV2"),
    APP_RJ("AppRJ"),
    APP_GET("AppGet"),
    CUSTOM("自定义")
}

fun Site.spiderFamily(): SpiderFamily {
    return when {
        api in setOf("native_ddys", "native_dyrs") || key in setOf("ddys", "dyrs") -> SpiderFamily.PRIVATE
        api == "local" -> SpiderFamily.LOCAL
        api == "native_app_v2" || key == "app_v2_template" -> SpiderFamily.APP_V2
        api == "native_meipa" -> SpiderFamily.APP_V2
        api == "native_app_rj" || key == "app_rj_template" -> SpiderFamily.APP_RJ
        api == "native_app_get" || key == "app_get_template" -> SpiderFamily.APP_GET
        type in listOf(0, 1, 2) -> SpiderFamily.CMS
        else -> SpiderFamily.CUSTOM
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val localParserRegistry: LocalParserRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val allSites = siteRepository.getAllSites()
        val enabledKeys = siteRepository.getEnabledSites().map { it.key }.toSet()
        val defaultSite = siteRepository.getDefaultSite()
        _uiState.value = SettingsUiState(
            defaultSiteKey = defaultSite.key,
            sites = allSites.map { SiteToggleItem(it, it.key in enabledKeys) },
            parserNames = localParserRegistry.parserNames()
        )
    }

    fun setDefaultSite(key: String) {
        viewModelScope.launch {
            siteRepository.setDefaultSite(key)
            refresh()
        }
    }

    fun toggleSite(key: String, enabled: Boolean) {
        viewModelScope.launch {
            siteRepository.setSiteEnabled(key, enabled)
            val defaultSite = siteRepository.getDefaultSite()
            if (!enabled && defaultSite.key == key) {
                siteRepository.getEnabledSites().firstOrNull()?.let { siteRepository.setDefaultSite(it.key) }
            }
            refresh()
        }
    }
}
