package app.plainlight.ui.lights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.plainlight.MainApplication
import app.plainlight.data.AppPreferences
import app.plainlight.tip.TipJar
import app.plainlight.tip.tipAskDue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

data class LightsUiState(
    /** The tip's one ask (ADR-0007). False wherever Play will not sell a tip: nothing is asked for that cannot be bought. */
    val showTipAsk: Boolean = false,
)

class LightsViewModel(
    lightCount: Flow<Int>,
    private val preferences: AppPreferences,
    tipJar: TipJar,
    installedAt: Instant,
) : ViewModel() {
    val state: StateFlow<LightsUiState> =
        combine(lightCount, preferences.tipAsked, tipJar.offers) { lights, asked, offers ->
            LightsUiState(showTipAsk = offers.isNotEmpty() && tipAskDue(asked, lights, installedAt, Instant.now()))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LightsUiState())

    init {
        // Prices are asked for only once the ask could otherwise be due — one product query saved
        // on every other visit. The binding itself happens on every foreground anyway, to settle tips.
        viewModelScope.launch {
            val due = tipAskDue(preferences.tipAsked.first(), lightCount.first(), installedAt, Instant.now())
            if (due) tipJar.refresh()
        }
    }

    /** Either answer — *Leave a tip* or *No thanks* — ends the ask on this install. */
    fun answerTipAsk() {
        viewModelScope.launch { preferences.markTipAsked() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    LightsViewModel(
                        // No light can be added yet, so the ask cannot come due.
                        lightCount = flowOf(0),
                        preferences = app.container.preferences,
                        tipJar = app.container.tipJar,
                        installedAt = app.container.installedAt,
                    )
                }
            }
    }
}
