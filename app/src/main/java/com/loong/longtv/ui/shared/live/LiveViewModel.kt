package com.loong.longtv.ui.shared.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loong.longtv.data.SourcePreference
import com.loong.longtv.di.DefaultSources
import com.loong.longtv.engine.TvBoxEngine
import com.loong.longtv.engine.source.model.LiveChannel
import com.loong.longtv.engine.source.model.LiveConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveUiState(
    val loading: Boolean = false,
    val sources: List<LiveConfig> = emptyList(),
    val channels: List<LiveChannel> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val engine: TvBoxEngine,
    private val sourcePreference: SourcePreference,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LiveUiState())
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, errorMessage = null) }
            val config = engine.currentConfig ?: engine.start(sourcePreference.load() ?: DefaultSources.default)
                .getOrElse { throwable ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            sources = emptyList(),
                            channels = emptyList(),
                            errorMessage = "加载直播源失败: ${throwable.message.orEmpty()}"
                        )
                    }
                    return@launch
                }

            if (config.lives.isEmpty()) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        sources = emptyList(),
                        channels = emptyList(),
                        errorMessage = null
                    )
                }
                return@launch
            }

            val channelsResult = engine.liveSpider.loadChannels(config.lives)
            _uiState.update {
                it.copy(
                    loading = false,
                    sources = config.lives,
                    channels = channelsResult.getOrNull().orEmpty(),
                    errorMessage = channelsResult.exceptionOrNull()?.message
                )
            }
        }
    }
}
