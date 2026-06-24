package com.loong.longtv.ui.shared.settings

import androidx.lifecycle.ViewModel
import com.loong.longtv.data.SourcePreference
import com.loong.longtv.engine.TvBoxEngine
import com.loong.longtv.engine.source.model.SiteConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SettingsUiState(
    val popularSiteKey: String? = null,
    val sites: List<SiteConfig> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val engine: TvBoxEngine,
    private val sourcePreference: SourcePreference,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val config = engine.currentConfig
        _uiState.value = SettingsUiState(
            popularSiteKey = sourcePreference.loadPopularSiteKey(),
            sites = config?.sites?.filter { it.isSpiderSite() } ?: emptyList()
        )
    }

    fun selectPopularSite(siteKey: String) {
        sourcePreference.savePopularSiteKey(siteKey)
        _uiState.value = _uiState.value.copy(popularSiteKey = siteKey, error = null)
    }
}
