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
import app.plainlight.data.LastSent
import app.plainlight.data.Light
import app.plainlight.data.LightSetting
import app.plainlight.data.Scene
import app.plainlight.ui.lights.ifSent
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
import java.util.UUID

data class LightUiState(
    val light: Light? = null,
    /** What the sliders show: a drag in progress, else what this app last sent, else the favourite — never a reading from the lamp. */
    val setting: LightSetting = LightSetting(),
    val problem: SendResult? = null,
    val removed: Boolean = false,
)

class LightViewModel(
    private val lightId: String,
    private val preferences: AppPreferences,
    private val transmitter: LightTransmitter,
    private val lastSent: LastSent,
) : ViewModel() {
    private val preview = MutableStateFlow<LightSetting?>(null)
    private val problem = MutableStateFlow<SendResult?>(null)
    private val removed = MutableStateFlow(false)

    val state: StateFlow<LightUiState> =
        combine(
            preferences.lights.map { all ->
                all.firstOrNull { it.id == lightId }
            },
            lastSent.of(lightId),
            preview,
            problem,
            removed,
        ) { light, sent, preview, problem, removed ->
            LightUiState(light, preview ?: sent ?: light?.favourite ?: LightSetting(), problem, removed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LightUiState())

    fun switchOn() =
        withLight { light ->
            preview.value = null
            transmitter.switchOn(light, lastSent)
        }

    fun switchOff() = withLight { transmitter.send(it.address, LightCommand.Off) }

    fun previewBrightness(level: Float) {
        preview.value = current().copy(brightness = level)
    }

    fun previewColdness(coldness: Float) {
        preview.value = current().copy(coldness = coldness)
    }

    fun sendBrightness() =
        withLight { light ->
            val level = current().brightness
            transmitter
                .send(light.address, LightCommand.Brightness(level))
                .ifSent { lastSent.update(light) { it.copy(brightness = level) } }
        }

    fun sendColdness() =
        withLight { light ->
            val setting = current()
            transmitter
                .sendAll(
                    light,
                    LightCommand.ColourTemperature(setting.coldness),
                    LightCommand.Brightness(setting.brightness),
                ).ifSent { lastSent.update(light) { setting } }
        }

    fun remember() {
        val setting = current()
        viewModelScope.launch { preferences.updateLight(lightId) { it.copy(favourite = setting) } }
    }

    fun applyScene(scene: Scene) =
        withLight { light ->
            preview.value = null
            transmitter.switchOn(light, scene.setting, lastSent)
        }

    fun saveScene(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val scene = Scene(UUID.randomUUID().toString(), name = trimmed, setting = current())
        viewModelScope.launch { preferences.updateLight(lightId) { it.copy(scenes = it.scenes + scene) } }
    }

    fun deleteScene(sceneId: String) {
        viewModelScope.launch {
            preferences.updateLight(
                lightId,
            ) { light -> light.copy(scenes = light.scenes.filterNot { it.id == sceneId }) }
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
            return LightViewModel(
                lightId,
                app.container.preferences,
                app.container.transmitter,
                app.container.lastSent,
            ) as T
        }
    }
}
