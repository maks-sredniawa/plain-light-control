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
import app.plainlight.data.presetScenes
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
    private var answer: SendResult = SendResult.Sent
    private val transmitter =
        LightTransmitter { _, command ->
            sent += command
            answer
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

    @Test
    fun `applying a scene sends On, colour, then brightness and records it as last sent`() =
        runTest {
            preferences.saveLight(light)
            val viewModel = open()
            val evening = presetScenes.first()
            viewModel.applyScene(evening)
            lastSent.of(light.id).first { it == evening.setting }
            assertEquals(
                listOf(
                    LightCommand.On,
                    LightCommand.ColourTemperature(evening.setting.coldness),
                    LightCommand.Brightness(evening.setting.brightness),
                ),
                sent.toList(),
            )
            viewModel.state.first { it.setting == evening.setting }
        }

    @Test
    fun `a saved scene keeps the sliders' values, and a deleted preset stays deleted`() =
        runTest {
            preferences.saveLight(light)
            val viewModel = open()
            viewModel.previewBrightness(0.4f)
            viewModel.saveScene("  Film  ")
            viewModel.state.first { s -> s.light?.scenes?.any { it.name == "Film" } == true }
            viewModel.saveScene(" ")
            viewModel.deleteScene(presetScenes.first().id)
            val scenes =
                viewModel.state
                    .first { s -> s.light?.scenes?.size == 3 }
                    .light!!
                    .scenes
            assertEquals(presetScenes.drop(1), scenes.dropLast(1))
            assertEquals(LightSetting(0.4f, 0.5f), scenes.last().setting)
        }

    @Test
    fun `a rename is trimmed and a blank one is refused`() =
        runTest {
            preferences.saveLight(light)
            val viewModel = open()
            viewModel.rename(" ")
            // DataStore applies edits in order, so a blank name the rename had written lands before this one returns.
            preferences.updateLight(light.id) { it }
            assertEquals(
                "Lamp",
                preferences.lights
                    .first()
                    .single()
                    .name,
            )
            viewModel.rename("  Kitchen ")
            viewModel.state.first { it.light?.name == "Kitchen" }
        }

    @Test
    fun `the timer is reported as asked until the next command`() =
        runTest {
            preferences.saveLight(light)
            val viewModel = open()
            viewModel.switchOffIn(2)
            viewModel.state.first { it.timerHours == 2 }
            assertEquals(listOf<LightCommand>(LightCommand.Timer(2)), sent.toList())
            viewModel.switchOff()
            viewModel.state.first { it.timerHours == null }
        }

    @Test
    fun `after a failed send the sliders go back to what was last sent`() =
        runTest {
            preferences.saveLight(light)
            val viewModel = open()
            viewModel.previewBrightness(0.3f)
            viewModel.sendBrightness()
            lastSent.of(light.id).first { it?.brightness == 0.3f }

            answer = SendResult.BluetoothOff
            viewModel.previewBrightness(0.8f)
            viewModel.sendBrightness()
            viewModel.state.first { it.problem == SendResult.BluetoothOff }
            assertEquals(0.3f, viewModel.state.value.setting.brightness)

            viewModel.previewColdness(0.1f)
            viewModel.sendColdness()
            viewModel.state.first { it.setting == LightSetting(brightness = 0.3f, coldness = 0.5f) }
        }

    @Test
    fun `the screen's first state is the stored light, mode and last sent setting, not the defaults`() =
        runTest {
            preferences.saveLight(light)
            preferences.setSimpleMode(false)
            lastSent.update(light) { it.copy(brightness = 0.2f) }
            val first = LightViewModel(light.id, preferences, transmitter, lastSent).state.value
            assertEquals(light, first.light)
            assertEquals(false, first.simpleMode)
            assertEquals(0.2f, first.setting.brightness)
        }

    private suspend fun TestScope.open(): LightViewModel {
        val viewModel = LightViewModel(light.id, preferences, transmitter, lastSent)
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.state.first { it.light != null }
        return viewModel
    }
}
