package app.royalreader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.royalreader.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val reducedAnimations: Boolean = false,
    val autoBrightness: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = settingsStore.settings
        .map { SettingsUiState(reducedAnimations = it.reducedAnimations, autoBrightness = it.autoBrightness) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState(),
        )

    fun setReducedAnimations(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setReducedAnimations(enabled) }
    }

    fun setAutoBrightness(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAutoBrightness(enabled) }
    }
}
