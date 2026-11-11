package app.plainlight.ui.addlight

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.viewModelScope
import app.plainlight.ble.LightTransmitter
import app.plainlight.ble.SendResult
import app.plainlight.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
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

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val preferences by lazy {
        AppPreferences(PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("app.preferences_pb") })
    }
    private val sends = AtomicInteger()
    private val transmitter =
        LightTransmitter { _, _ ->
            sends.incrementAndGet()
            SendResult.Sent
        }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    private val created = mutableListOf<AddLightViewModel>()

    private fun track(viewModel: AddLightViewModel): AddLightViewModel = viewModel.also { created += it }

    /** A send or save still in flight would resume on Main after [Dispatchers.resetMain] and fail whichever test runs next. */
    @After fun tearDown() {
        runBlocking {
            created.forEach {
                it.viewModelScope.coroutineContext.job
                    .cancelAndJoin()
            }
        }
        Dispatchers.resetMain()
        scope.cancel()
    }

    @Test
    fun `a double tap on Yes saves the light once`() =
        runTest {
            val viewModel = track(AddLightViewModel(preferences, transmitter))
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
            val viewModel = track(AddLightViewModel(preferences, transmitter))
            viewModel.permissionDenied()
            viewModel.permissionGranted()
            val state = viewModel.state.value
            assertEquals(AddLightStep.INSTRUCTIONS, state.step)
            assertTrue(state.permissionJustGranted)
            assertNull(state.problem)
            assertEquals(0, sends.get())
        }
}
