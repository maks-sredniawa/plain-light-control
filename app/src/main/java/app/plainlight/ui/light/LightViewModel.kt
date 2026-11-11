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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class LightUiState(
    val light: Light? = null,
    /** What the sliders show: a drag in progress, else what this app last sent, else the favourite — never a reading from the lamp. */
    val setting: LightSetting = LightSetting(),
    val problem: SendResult? = null,
    val removed: Boolean = false,
    /** What the light was last asked to switch off in. The lamp keeps the time and cannot say whether it is counting. */
    val timerHours: Int? = null,
)

private data class ScreenState(
    val preview: LightSetting? = null,
    val problem: SendResult? = null,
    val removed: Boolean = false,
    val timerHours: Int? = null,
)

class LightViewModel(
    private val lightId: String,
    private val preferences: AppPreferences,
    private val transmitter: LightTransmitter,
    private val lastSent: LastSent,
) : ViewModel() {
    private val screen = MutableStateFlow(ScreenState())

    val state: StateFlow<LightUiState> =
        combine(
            preferences.lights.map { all -> all.firstOrNull { it.id == lightId } },
            lastSent.of(lightId),
            screen,
        ) { light, sent, screen ->
            LightUiState(
                light = light,
                setting = screen.preview ?: sent ?: light?.favourite ?: LightSetting(),
                problem = screen.problem,
                removed = screen.removed,
                timerHours = screen.timerHours,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LightUiState())

    fun switchOn() =
        withLight { light ->
            screen.update { it.copy(preview = null) }
            transmitter.switchOn(light, lastSent)
        }

    fun switchOff() = withLight { transmitter.send(it.address, LightCommand.Off) }

    fun switchOffIn(hours: Int) =
        withLight { light ->
            transmitter
                .send(light.address, LightCommand.Timer(hours))
                .ifSent { screen.update { it.copy(timerHours = hours) } }
        }

    fun previewBrightness(level: Float) {
        screen.update { it.copy(preview = current().copy(brightness = level)) }
    }

    fun previewColdness(coldness: Float) {
        screen.update { it.copy(preview = current().copy(coldness = coldness)) }
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

    fun rename(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { preferences.updateLight(lightId) { it.copy(name = trimmed) } }
    }

    fun applyScene(scene: Scene) =
        withLight { light ->
            screen.update { it.copy(preview = null) }
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
            screen.update { it.copy(removed = true) }
        }
    }

    fun dismissProblem() {
        screen.update { it.copy(problem = null) }
    }

    private fun current(): LightSetting = state.value.setting

    private suspend fun light(): Light? = preferences.lights.first().firstOrNull { it.id == lightId }

    private fun withLight(action: suspend (Light) -> SendResult) {
        viewModelScope.launch {
            val light = light() ?: return@launch
            screen.update { it.copy(timerHours = null) }
            val result = action(light).takeIf { it != SendResult.Sent }
            screen.update { it.copy(problem = result) }
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
