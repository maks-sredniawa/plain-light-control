package app.plainlight.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.plainlight.data.AppPreferences
import app.plainlight.data.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val preferences by lazy {
        AppPreferences(PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("app.preferences_pb") })
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() {
        Dispatchers.resetMain()
        scope.cancel()
    }

    @Test
    fun `the screen's first state is the stored settings, not the defaults`() =
        runTest {
            preferences.setThemeMode(ThemeMode.DARK)
            preferences.setSimpleMode(false)
            preferences.setMaterialYou(true)
            assertEquals(
                SettingsUiState(themeMode = ThemeMode.DARK, materialYou = true, simpleMode = false),
                SettingsViewModel(preferences).state.value,
            )
        }
}
