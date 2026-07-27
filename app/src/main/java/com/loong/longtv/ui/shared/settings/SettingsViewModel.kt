package com.loong.longtv.ui.shared.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loong.longtv.data.cache.CacheStorage
import com.loong.longtv.data.SourcePreference
import com.loong.longtv.engine.TvBoxEngine
import com.loong.longtv.engine.source.model.SiteConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    @ApplicationContext private val context: Context,
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
            sites = config?.sites?.filter { it.isSpiderSite() && it.key != CHILD_EDUCATION_SITE_KEY } ?: emptyList()
        )
        viewModelScope.launch(Dispatchers.IO) {
            CacheStorage.enforceBudget(context)
        }
    }

    fun selectPopularSite(siteKey: String) {
        sourcePreference.savePopularSiteKey(siteKey)
        _uiState.value = _uiState.value.copy(popularSiteKey = siteKey, error = null)
    }

    companion object {
        private const val CHILD_EDUCATION_SITE_KEY = "少儿教育"
    }
}
