package app.plainlight.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * What this app last sent to each light, kept for the life of the process so a light's screen opens
 * on it rather than on the favourite. In memory only: after a restart the remote may have changed the
 * lamp, and the favourite is as good a guess as any.
 */
class LastSent {
    private val settings = MutableStateFlow<Map<String, LightSetting>>(emptyMap())

    fun of(lightId: String): Flow<LightSetting?> = settings.map { it[lightId] }

    fun now(lightId: String): LightSetting? = settings.value[lightId]

    fun update(
        light: Light,
        change: (LightSetting) -> LightSetting,
    ) {
        settings.update { it + (light.id to change(it[light.id] ?: light.favourite)) }
    }
}
