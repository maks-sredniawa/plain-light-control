package app.plainlight.ui.addlight

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.plainlight.ble.LightTransmitter
import app.plainlight.ble.SendResult
import app.plainlight.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class AddLightViewModelTest {
    @get:Rule val folder = TemporaryFolder()

    private val main = UnconfinedTestDispatcher()

    /** On Dispatchers.IO a collection that starts mid-write can miss that write for good (DataStore 1.2.1), hanging a wait on it. */
    private val scope = CoroutineScope(StandardTestDispatcher(main.scheduler) + SupervisorJob())
    private val preferences by lazy {
        AppPreferences(PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("app.preferences_pb") })
    }
    private val sends = AtomicInteger()
    private val transmitter =
        LightTransmitter { _, _ ->
            sends.incrementAndGet()
            SendResult.Sent
        }

    @Before fun setUp() = Dispatchers.setMain(main)

    @After fun tearDown() {
        Dispatchers.resetMain()
        scope.cancel()
    }

    @Test
    fun `a double tap on Yes saves the light once`() =
        runTest {
            val viewModel = AddLightViewModel(preferences, transmitter)
            viewModel.pair()
            viewModel.state.first { it.step == AddLightStep.CHECK }
            viewModel.confirm("Lamp")
            viewModel.confirm("Lamp")
            viewModel.state.first { it.step == AddLightStep.DONE }
            assertEquals(1, preferences.lights.first().size)
        }

    @Test
    fun `a permission grant asks for the wall switch again instead of pairing`() =
        runTest {
            val viewModel = AddLightViewModel(preferences, transmitter)
            viewModel.permissionDenied()
            viewModel.permissionGranted()
            val state = viewModel.state.value
            assertEquals(AddLightStep.INSTRUCTIONS, state.step)
            assertTrue(state.permissionJustGranted)
            assertNull(state.problem)
            assertEquals(0, sends.get())
        }
}
