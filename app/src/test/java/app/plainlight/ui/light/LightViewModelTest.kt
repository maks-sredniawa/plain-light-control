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
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val main = UnconfinedTestDispatcher()

    /** On Dispatchers.IO a collection that starts mid-write can miss that write for good (DataStore 1.2.1), hanging a wait on it. */
    private val scope = CoroutineScope(main + SupervisorJob())
    private val preferences by lazy {
        AppPreferences(PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("app.preferences_pb") })
    }
    private val sent = CopyOnWriteArrayList<LightCommand>()
    private val sentCount = MutableStateFlow(0)
    private var answer: SendResult = SendResult.Sent
    private val transmitter =
        LightTransmitter { _, command ->
            sent += command
            sentCount.value = sent.size
            answer
        }
    private val lastSent by lazy { LastSent(preferences) }
    private val light =
        Light("lamp", "Lamp", LightProtocol.ZHIMEI_V2, 42, LightSetting(brightness = 1f, coldness = 0.5f))

    @Before fun setUp() = Dispatchers.setMain(main)

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
    fun `On brings back what was last sent, even after a restart`() =
        runTest {
            preferences.saveLight(light)
            val first = open()
            first.previewBrightness(0.2f)
            first.sendBrightness()
            lastSent.of(light.id).first { it?.brightness == 0.2f }

            val restarted = LightViewModel(light.id, preferences, transmitter, LastSent(preferences))
            backgroundScope.launch { restarted.state.collect {} }
            restarted.state.first { it.light != null }
            sent.clear()
            restarted.switchOn()
            sentCount.first { it == 3 }
            assertEquals(
                listOf(LightCommand.On, LightCommand.ColourTemperature(0.5f), LightCommand.Brightness(0.2f)),
                sent.toList(),
            )
        }

    @Test
    fun `once a favourite is remembered On brings it back rather than the last sent`() =
        runTest {
            preferences.saveLight(light)
            val viewModel = open()
            viewModel.previewBrightness(0.6f)
            viewModel.remember()
            viewModel.state.first { it.light?.favouriteRemembered == true }
            viewModel.previewBrightness(0.1f)
            viewModel.sendBrightness()
            lastSent.of(light.id).first { it?.brightness == 0.1f }

            sent.clear()
            viewModel.switchOn()
            viewModel.state.first { it.setting.brightness == 0.6f }
            assertEquals(LightCommand.Brightness(0.6f), sent.last())
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

            answer = SendResult.PermissionMissing
            viewModel.previewColdness(0.1f)
            viewModel.sendColdness()
            viewModel.state.first { it.problem == SendResult.PermissionMissing }
            assertEquals(LightSetting(brightness = 0.3f, coldness = 0.5f), viewModel.state.value.setting)
        }

    @Test
    fun `a send carries the slider's value before the screen state has caught up with it`() =
        runTest {
            preferences.saveLight(light)
            val unwatched = LightViewModel(light.id, preferences, transmitter, lastSent)
            unwatched.previewColdness(0.9f)
            unwatched.sendColdness()
            lastSent.of(light.id).first { it?.coldness == 0.9f }
            assertEquals(LightCommand.ColourTemperature(0.9f), sent.first())
        }

    private suspend fun TestScope.open(): LightViewModel {
        val viewModel = LightViewModel(light.id, preferences, transmitter, lastSent)
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.state.first { it.light != null }
        return viewModel
    }
}
