package com.loong.longtv.ui.shared.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loong.longtv.data.SourcePreference
import com.loong.longtv.di.DefaultSources
import com.loong.longtv.engine.TvBoxEngine
import com.loong.longtv.engine.config.ConfigSource
import com.loong.longtv.engine.source.model.SiteConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val currentSource: ConfigSource? = null,
    val availableSources: List<ConfigSource> = DefaultSources.all,
    val sites: List<SiteConfig> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

data class SiteToggleItem(
    val site: SiteConfig,
    val enabled: Boolean
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
            currentSource = engine.configManager.currentSource,
            availableSources = DefaultSources.all,
            sites = config?.sites?.filter { it.isSpiderSite() } ?: emptyList()
        )
    }

    fun switchSource(source: ConfigSource) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            val result = engine.switchSource(source)
            if (result.isSuccess) {
                sourcePreference.save(source)
                refresh()
                _uiState.value = _uiState.value.copy(loading = false)
            } else {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.message ?: "切换配置失败"
                )
            }
        }
    }
}
