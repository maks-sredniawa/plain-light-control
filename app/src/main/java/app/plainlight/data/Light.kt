package app.plainlight.data

import app.plainlight.ble.LightAddress
import app.plainlight.ble.LightProtocol
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

@Serializable
data class LightSetting(
    val brightness: Float = 1f,
    val coldness: Float = 0.5f,
)

@Serializable
data class Light(
    val id: String,
    val name: String,
    val protocol: LightProtocol,
    val controllerId: Long,
    val favourite: LightSetting = LightSetting(),
) {
    val address: LightAddress get() = LightAddress(protocol, controllerId)
}

private val json =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

/** One unreadable entry is dropped rather than losing the whole list. */
internal fun decodeLights(text: String?): List<Light> {
    if (text.isNullOrBlank()) return emptyList()
    val entries = runCatching { json.decodeFromString<JsonArray>(text) }.getOrNull() ?: return emptyList()
    return entries.mapNotNull { entry: JsonElement ->
        runCatching { json.decodeFromJsonElement(Light.serializer(), entry) }.getOrNull()
    }
}

internal fun encodeLights(lights: List<Light>): String = json.encodeToString(lights)
