package app.plainlight.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import app.plainlight.ble.LightProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppPreferencesTest {
    @get:Rule val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val store by lazy {
        PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("app.preferences_pb") }
    }
    private val preferences by lazy { AppPreferences(store) }

    @After fun tearDown() = scope.cancel()

    @Test
    fun `every key is either a setting that Restore removes or kept, never both or neither`() {
        // Kotlin note: an `object`'s `val`s compile to static fields on its class, so plain Java
        // reflection finds every key declared there without kotlin-reflect.
        val declared =
            AppPreferences.Keys::class.java.declaredFields
                .filter { Preferences.Key::class.java.isAssignableFrom(it.type) }
                .map { field ->
                    field.isAccessible = true
                    field.get(null) as Preferences.Key<*>
                }
        assertTrue(declared.isNotEmpty())
        for (key in declared) {
            val lists = listOf(AppPreferences.Keys.SETTINGS, AppPreferences.Keys.KEPT).count { key in it }
            assertEquals(key.name, 1, lists)
        }
        assertEquals(declared.toSet(), (AppPreferences.Keys.SETTINGS + AppPreferences.Keys.KEPT).toSet())
    }

    @Test
    fun `saving a light keeps a stored light this version cannot read`() =
        runBlocking {
            val unreadable = """{"id":"x","name":"Old","protocol":"ZHIMEI_V9","controllerId":1}"""
            store.edit { it[AppPreferences.Keys.LIGHTS] = "[$unreadable]" }
            val lamp = Light("a", "Salon", LightProtocol.ZHIMEI_V2, 0x1234L)
            preferences.saveLight(lamp)
            assertEquals(listOf(lamp), preferences.lights.first())
            assertTrue(store.data.first()[AppPreferences.Keys.LIGHTS]!!.startsWith("[$unreadable,"))
        }

    @Test
    fun `simple mode is on until it is switched off`() =
        runBlocking {
            assertTrue(preferences.simpleMode.first())
            preferences.setSimpleMode(false)
            assertFalse(preferences.simpleMode.first())
        }

    @Test
    fun `restoring resets every setting and keeps state`() =
        runBlocking {
            with(preferences) {
                setThemeMode(ThemeMode.DARK)
                setMaterialYou(true)
                setSimpleMode(false)
                setOnboardingDone(true)
                markTipAsked()

                restoreDefaults()

                assertEquals(ThemeMode.SYSTEM, themeMode.first())
                assertFalse(materialYou.first())
                assertTrue(simpleMode.first())
                assertTrue(onboardingDone.first())
                assertTrue(tipAsked.first())
            }
        }
}
