package app.plainlight.ui.addlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.plainlight.MainApplication
import app.plainlight.ble.LightAddress
import app.plainlight.ble.LightCodec
import app.plainlight.ble.LightCommand
import app.plainlight.ble.LightProtocol
import app.plainlight.ble.LightTransmitter
import app.plainlight.ble.SendResult
import app.plainlight.data.AppPreferences
import app.plainlight.data.Light
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class AddLightStep {
    INSTRUCTIONS,
    PAIRING,
    CHECK,
    SAVING,
    FAILED,
    DONE,
}

data class AddLightUiState(
    val step: AddLightStep = AddLightStep.INSTRUCTIONS,
    val attempt: Int = 0,
    val problem: SendResult? = null,
    val permissionJustGranted: Boolean = false,
)

/**
 * The lamp cannot say which protocol it speaks or whether pairing worked, so each protocol is tried
 * in turn and the person beside the lamp is asked whether it blinked off and on.
 */
class AddLightViewModel(
    private val preferences: AppPreferences,
    private val transmitter: LightTransmitter,
) : ViewModel() {
    private val controllerId = LightCodec.newControllerId()
    private val _state = MutableStateFlow(AddLightUiState())
    val state: StateFlow<AddLightUiState> = _state.asStateFlow()

    private val address get() = LightAddress(LightProtocol.entries[_state.value.attempt], controllerId)

    fun pair() {
        viewModelScope.launch {
            _state.update { it.copy(step = AddLightStep.PAIRING, problem = null, permissionJustGranted = false) }
            val result = sendBlink()
            _state.update {
                if (result ==
                    SendResult.Sent
                ) {
                    it.copy(step = AddLightStep.CHECK)
                } else {
                    it.copy(step = AddLightStep.INSTRUCTIONS, problem = result)
                }
            }
        }
    }

    /** The permission dialog outlasts the lamp's pairing window, so the wall-switch step comes again rather than a pair now. */
    fun permissionGranted() {
        _state.update { it.copy(problem = null, permissionJustGranted = true) }
    }

    fun permissionDenied() {
        _state.update { it.copy(problem = SendResult.PermissionMissing) }
    }

    fun confirm(name: String) {
        if (_state.value.step != AddLightStep.CHECK) return
        _state.update { it.copy(step = AddLightStep.SAVING) }
        viewModelScope.launch {
            preferences.saveLight(Light(UUID.randomUUID().toString(), name, address.protocol, controllerId))
            _state.update { it.copy(step = AddLightStep.DONE) }
        }
    }

    fun deny() {
        _state.update {
            val next = it.attempt + 1
            if (next <
                LightProtocol.entries.size
            ) {
                it.copy(step = AddLightStep.INSTRUCTIONS, attempt = next)
            } else {
                it.copy(step = AddLightStep.FAILED)
            }
        }
    }

    fun restart() {
        _state.value = AddLightUiState()
    }

    fun dismissProblem() {
        _state.update { it.copy(problem = null) }
    }

    private suspend fun sendBlink(): SendResult {
        for (command in listOf(LightCommand.Pair, LightCommand.Off)) {
            val result = transmitter.send(address, command)
            if (result != SendResult.Sent) return result
        }
        delay(BLINK_MILLIS)
        return transmitter.send(address, LightCommand.On)
    }

    companion object {
        private const val BLINK_MILLIS = 1_500L

        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    AddLightViewModel(app.container.preferences, app.container.transmitter)
                }
            }
    }
}
