package app.plainlight.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/** Which colour scheme applies, regardless of what the phone is set to. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Everything the app remembers.
 *
 * **DataStore, not SharedPreferences.** SharedPreferences' `commit()` blocks and its `apply()` can
 * still write on the main thread at the wrong moment; DataStore is `suspend`-only and transactional,
 * so there is no API here that can stall a frame.
 *
 * Kotlin note: every read is a `Flow`, whose first emission arrives after a disk read. [lastRead]
 * is the synchronous value a first frame starts from: the last one read or written, null only
 * before the first read, which `MainApplication.onCreate` forces. [themeModeNow] stays for that one
 * caller, and it is `suspend` because reading a file is.
 *
 * ## Adding a preference
 *
 * A key, listed in `Keys.SETTINGS` or `Keys.KEPT`, a reader on [Preferences] that gives it a
 * default, a `Flow` and, if a screen shows it, a [StoredValues] field built from that reader, and a
 * `suspend` setter. `AppPreferencesTest` fails until the key is in exactly one of the two lists, so
 * whoever adds it decides whether *Restore to default* resets it. Keep the default in the `Flow`
 * rather than writing it on first launch — a default that has been written to disk cannot be
 * changed in a later version without a migration, and a default that has not can.
 */
class AppPreferences(
    private val store: DataStore<Preferences>,
) {
    internal object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MATERIAL_YOU = booleanPreferencesKey("material_you")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val TIP_ASKED = booleanPreferencesKey("tip_asked")
        val LIGHTS = stringPreferencesKey("lights")
        val SIMPLE_MODE = booleanPreferencesKey("simple_mode")

        /** What *Restore to default* removes: everything the user chose, so each reads its default again. */
        val SETTINGS: List<Preferences.Key<*>> =
            listOf(
                THEME_MODE,
                MATERIAL_YOU,
                SIMPLE_MODE,
            )

        /**
         * What *Restore to default* leaves alone: live state, one-time asks, and anything pointing at
         * a grant the system holds, which a reset would orphan rather than undo.
         */
        val KEPT: List<Preferences.Key<*>> =
            listOf(
                ONBOARDING_DONE,
                TIP_ASKED,
                LIGHTS,
            )
    }

    @Volatile private var latest: Preferences? = null

    private val data: Flow<Preferences> = store.data.onEach { latest = it }

    /** What was last read or written, so a screen's first frame shows it rather than the defaults; null before the first read. */
    val lastRead: StoredValues? get() = latest?.let(::StoredValues)

    /**
     * Light, dark or follow-the-system.
     *
     * Unknown stored values fall back to [ThemeMode.SYSTEM] rather than throwing: a preference file
     * survives a downgrade, and a crash loop at startup is a far worse outcome than the wrong theme.
     */
    val themeMode: Flow<ThemeMode> = data.map(::readThemeMode)

    /**
     * Whether to take the wallpaper's palette instead of the app's own (ADR-0006).
     *
     * Defaults **off**. With it on, nothing on Android 12+ reads the generated scheme at all, so the
     * app's identity would be invisible on almost every device that runs it.
     */
    val materialYou: Flow<Boolean> = data.map(::readMaterialYou)

    /** Whether first-run setup has been completed. */
    val onboardingDone: Flow<Boolean> = data.map { it[Keys.ONBOARDING_DONE] ?: false }

    /**
     * Whether the home screen's one tip ask has been answered, either way (ADR-0007). Auto Backup
     * carries it to the next phone (ADR-0005), so an answer given once is not asked for again there.
     */
    val tipAsked: Flow<Boolean> = data.map { it[Keys.TIP_ASKED] ?: false }

    /** Whether a light's screen shows only On, Off and brightness. */
    val simpleMode: Flow<Boolean> = data.map(::readSimpleMode)

    /** The user's lights, as a list rather than a table (`docs/optional-modules.md`, *How to decide*). */
    val lights: Flow<List<Light>> = data.map(::readLights)

    private suspend fun edit(change: suspend (MutablePreferences) -> Unit) {
        latest = store.edit(change)
    }

    /** Adds [light], or replaces the one with the same id. */
    suspend fun saveLight(light: Light) {
        edit { prefs -> prefs[Keys.LIGHTS] = putLight(prefs[Keys.LIGHTS], light) }
    }

    /** Changes the stored light inside one edit, so two changes made at once cannot overwrite each other. */
    suspend fun updateLight(
        id: String,
        change: (Light) -> Light,
    ) {
        edit { prefs -> prefs[Keys.LIGHTS] = changeLight(prefs[Keys.LIGHTS], id, change) }
    }

    suspend fun removeLight(id: String) {
        edit { prefs -> prefs[Keys.LIGHTS] = dropLight(prefs[Keys.LIGHTS], id) }
    }

    /**
     * The theme mode, read once, before any Activity exists.
     *
     * The one place a `suspend` read is worth its cost: `MainApplication.onCreate` needs the answer
     * to call `applyThemeMode` *before* the first window is created, and a `Flow` collected in a
     * composition arrives a frame too late — which is a light flash on every cold start for a user
     * who chose dark.
     */
    suspend fun themeModeNow(): ThemeMode = themeMode.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setMaterialYou(enabled: Boolean) {
        edit { it[Keys.MATERIAL_YOU] = enabled }
    }

    suspend fun setSimpleMode(enabled: Boolean) {
        edit { it[Keys.SIMPLE_MODE] = enabled }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun markTipAsked() {
        edit { it[Keys.TIP_ASKED] = true }
    }

    /**
     * Every setting back to how the app came, in one edit. The keys are removed rather than written,
     * so each reads its default again and stays free to change in a later version.
     */
    suspend fun restoreDefaults() {
        edit { prefs -> Keys.SETTINGS.forEach { prefs.remove(it) } }
    }
}

class StoredValues internal constructor(
    private val prefs: Preferences,
) {
    val themeMode: ThemeMode get() = readThemeMode(prefs)
    val materialYou: Boolean get() = readMaterialYou(prefs)
    val simpleMode: Boolean get() = readSimpleMode(prefs)
    val lights: List<Light> get() = readLights(prefs)
}

private fun readThemeMode(prefs: Preferences): ThemeMode =
    prefs[AppPreferences.Keys.THEME_MODE]?.let { name -> runCatching { enumValueOf<ThemeMode>(name) }.getOrNull() }
        ?: ThemeMode.SYSTEM

private fun readMaterialYou(prefs: Preferences): Boolean = prefs[AppPreferences.Keys.MATERIAL_YOU] ?: false

private fun readSimpleMode(prefs: Preferences): Boolean = prefs[AppPreferences.Keys.SIMPLE_MODE] ?: false

private fun readLights(prefs: Preferences): List<Light> = decodeLights(prefs[AppPreferences.Keys.LIGHTS])
