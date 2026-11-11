package app.plainlight.ui.light

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.plainlight.ble.LightCommand
import app.plainlight.ble.LightProtocol
import app.plainlight.ble.LightTransmitter
import app.plainlight.ble.SendResult
import app.plainlight.data.AppPreferences
import app.plainlight.data.LastSent
import app.plainlight.data.Light
import app.plainlight.data.LightSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(ExperimentalCoroutinesApi::class)
class LightViewModelTest {
    @get:Rule val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val preferences by lazy {
        AppPreferences(PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("app.preferences_pb") })
    }
    private val sent = CopyOnWriteArrayList<LightCommand>()
    private val transmitter =
        LightTransmitter { _, command ->
            sent += command
            SendResult.Sent
        }
    private val lastSent = LastSent()
    private val light =
        Light("lamp", "Lamp", LightProtocol.ZHIMEI_V2, 42, LightSetting(brightness = 1f, coldness = 0.5f))

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() {
        Dispatchers.resetMain()
        scope.cancel()
    }

    @Test
    fun `a reopened screen shows and re-sends what was last sent, not the favourite`() =
        runTest {
            preferences.saveLight(light)
            val first = open()
            first.previewBrightness(0.1f)
            first.sendBrightness()
            lastSent.of(light.id).first { it?.brightness == 0.1f }

            val second = open()
            assertEquals(0.1f, second.state.value.setting.brightness)
            sent.clear()
            second.previewColdness(0.9f)
            second.sendColdness()
            lastSent.of(light.id).first { it?.coldness == 0.9f }
            assertEquals(listOf(LightCommand.ColourTemperature(0.9f), LightCommand.Brightness(0.1f)), sent.toList())
        }

    @Test
    fun `a light nothing was sent to opens on its favourite`() =
        runTest {
            preferences.saveLight(light)
            assertEquals(light.favourite, open().state.value.setting)
        }

    private suspend fun TestScope.open(): LightViewModel {
        val viewModel = LightViewModel(light.id, preferences, transmitter, lastSent)
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.state.first { it.light != null }
        return viewModel
    }
}
