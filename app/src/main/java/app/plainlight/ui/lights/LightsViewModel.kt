package app.plainlight.ui.lights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.plainlight.MainApplication
import app.plainlight.ble.LightCommand
import app.plainlight.ble.LightTransmitter
import app.plainlight.ble.SendResult
import app.plainlight.data.AppPreferences
import app.plainlight.data.Light
import app.plainlight.tip.TipJar
import app.plainlight.tip.tipAskDue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

data class LightsUiState(
    val lights: List<Light> = emptyList(),
    /** The tip's one ask (ADR-0007). False wherever Play will not sell a tip: nothing is asked for that cannot be bought. */
    val showTipAsk: Boolean = false,
    val problem: SendResult? = null,
)

class LightsViewModel(
    private val preferences: AppPreferences,
    private val transmitter: LightTransmitter,
    tipJar: TipJar,
    installedAt: Instant,
) : ViewModel() {
    private val problem = MutableStateFlow<SendResult?>(null)

    val state: StateFlow<LightsUiState> =
        combine(preferences.lights, preferences.tipAsked, tipJar.offers, problem) { lights, asked, offers, problem ->
            LightsUiState(
                lights = lights,
                showTipAsk = offers.isNotEmpty() && tipAskDue(asked, lights.size, installedAt, Instant.now()),
                problem = problem,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LightsUiState())

    init {
        // Prices are asked for only once the ask could otherwise be due — one product query saved
        // on every other visit. The binding itself happens on every foreground anyway, to settle tips.
        viewModelScope.launch {
            val count = preferences.lights.map { it.size }.first()
            if (tipAskDue(preferences.tipAsked.first(), count, installedAt, Instant.now())) tipJar.refresh()
        }
    }

    fun switchOn(light: Light) = report { transmitter.switchOn(light) }

    fun switchOff(light: Light) = report { transmitter.send(light.address, LightCommand.Off) }

    fun dismissProblem() {
        problem.value = null
    }

    /** Either answer — *Leave a tip* or *No thanks* — ends the ask on this install. */
    fun answerTipAsk() {
        viewModelScope.launch { preferences.markTipAsked() }
    }

    private fun report(action: suspend () -> SendResult) {
        viewModelScope.launch { problem.value = action().takeIf { it != SendResult.Sent } }
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    LightsViewModel(
                        preferences = app.container.preferences,
                        transmitter = app.container.transmitter,
                        tipJar = app.container.tipJar,
                        installedAt = app.container.installedAt,
                    )
                }
            }
    }
}
