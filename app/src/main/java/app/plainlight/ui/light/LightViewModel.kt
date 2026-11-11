package app.plainlight.ui.light

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import app.plainlight.MainApplication
import app.plainlight.ble.LightCommand
import app.plainlight.ble.LightTransmitter
import app.plainlight.ble.SendResult
import app.plainlight.data.AppPreferences
import app.plainlight.data.Light
import app.plainlight.data.LightSetting
import app.plainlight.ui.lights.sendAll
import app.plainlight.ui.lights.switchOn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LightUiState(
    val light: Light? = null,
    /** What the sliders show: the last values this app sent, never a reading from the lamp. */
    val setting: LightSetting = LightSetting(),
    val problem: SendResult? = null,
    val removed: Boolean = false,
)

class LightViewModel(
    private val lightId: String,
    private val preferences: AppPreferences,
    private val transmitter: LightTransmitter,
) : ViewModel() {
    private val setting = MutableStateFlow<LightSetting?>(null)
    private val problem = MutableStateFlow<SendResult?>(null)
    private val removed = MutableStateFlow(false)

    val state: StateFlow<LightUiState> =
        combine(
            preferences.lights.map { all ->
                all.firstOrNull { it.id == lightId }
            },
            setting,
            problem,
            removed,
        ) { light, setting, problem, removed ->
            LightUiState(light, setting ?: light?.favourite ?: LightSetting(), problem, removed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LightUiState())

    fun switchOn() =
        withLight { light ->
            setting.value = light.favourite
            transmitter.switchOn(light)
        }

    fun switchOff() = withLight { transmitter.send(it.address, LightCommand.Off) }

    fun previewBrightness(level: Float) {
        setting.value = current().copy(brightness = level)
    }

    fun previewColdness(coldness: Float) {
        setting.value = current().copy(coldness = coldness)
    }

    fun sendBrightness() = withLight { transmitter.send(it.address, LightCommand.Brightness(current().brightness)) }

    fun sendColdness() =
        withLight {
            transmitter.sendAll(
                it,
                LightCommand.ColourTemperature(current().coldness),
                LightCommand.Brightness(current().brightness),
            )
        }

    fun remember() {
        viewModelScope.launch {
            val light = light() ?: return@launch
            preferences.saveLight(light.copy(favourite = current()))
        }
    }

    fun remove() {
        viewModelScope.launch {
            preferences.removeLight(lightId)
            removed.value = true
        }
    }

    fun dismissProblem() {
        problem.value = null
    }

    private fun current(): LightSetting = state.value.setting

    private suspend fun light(): Light? = preferences.lights.first().firstOrNull { it.id == lightId }

    private fun withLight(action: suspend (Light) -> SendResult) {
        viewModelScope.launch {
            val light = light() ?: return@launch
            problem.value = action(light).takeIf { it != SendResult.Sent }
        }
    }

    class Factory(
        private val lightId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras,
        ): T {
            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MainApplication
            return LightViewModel(lightId, app.container.preferences, app.container.transmitter) as T
        }
    }
}
