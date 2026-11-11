package app.plainlight.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What this app last sent to each light, stored with the light so On can bring it back after a restart. */
class LastSent(
    private val preferences: AppPreferences,
) {
    fun of(lightId: String): Flow<LightSetting?> =
        preferences.lights.map { all -> all.firstOrNull { it.id == lightId }?.lastSent }

    suspend fun update(
        light: Light,
        change: (LightSetting) -> LightSetting,
    ) {
        preferences.updateLight(light.id) { it.copy(lastSent = change(it.lastSent ?: it.favourite)) }
    }
}
