package app.plainlight.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.plainlight.ble.LightProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Her lights must survive the process going away: a lost controller ID means pairing every lamp again. */
@RunWith(AndroidJUnit4::class)
class LightsStoreTest {
    private val file =
        File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "lights-test.preferences_pb").apply {
            delete()
        }

    private suspend fun <T> withStore(block: suspend (AppPreferences) -> T): T {
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file }
        return try {
            block(AppPreferences(store))
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test
    fun lightsSurviveReopeningTheStore() =
        runBlocking {
            val salon = Light("a", "Salon", LightProtocol.ZHIMEI_V2, 0xC002L, LightSetting(0.3f, 0.2f))
            val kitchen = Light("b", "Kuchnia", LightProtocol.ZHIMEI_V1, 0x0101L)
            withStore { prefs ->
                prefs.saveLight(salon)
                prefs.saveLight(kitchen)
                prefs.saveLight(salon.copy(favourite = LightSetting(0.8f, 0.6f)))
            }
            val reread = withStore { it.lights.first() }
            assertEquals(listOf(salon.copy(favourite = LightSetting(0.8f, 0.6f)), kitchen), reread)

            withStore { it.removeLight("a") }
            assertEquals(listOf(kitchen), withStore { it.lights.first() })
        }
}
