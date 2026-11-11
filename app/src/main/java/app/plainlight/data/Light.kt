package app.plainlight.data

import app.plainlight.ble.LightAddress
import app.plainlight.ble.LightProtocol
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

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
    val scenes: List<Scene> = presetScenes,
) {
    val address: LightAddress get() = LightAddress(protocol, controllerId)
}

private val json =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

private fun storedEntries(text: String?): List<JsonElement> {
    if (text.isNullOrBlank()) return emptyList()
    return runCatching { json.decodeFromString<JsonArray>(text) }.getOrNull().orEmpty()
}

private fun JsonElement.lightId(): String? = ((this as? JsonObject)?.get("id") as? JsonPrimitive)?.contentOrNull

private fun decodeLight(entry: JsonElement): Light? =
    runCatching {
        json.decodeFromJsonElement(Light.serializer(), entry)
    }.getOrNull()

private fun encodeEntries(entries: List<JsonElement>): String = json.encodeToString(JsonArray(entries))

/** Skips an entry it cannot read; the writes below keep that entry as stored, so a later version can still read it. */
internal fun decodeLights(text: String?): List<Light> = storedEntries(text).mapNotNull(::decodeLight)

internal fun putLight(
    text: String?,
    light: Light,
): String {
    val entries = storedEntries(text)
    val encoded = json.encodeToJsonElement(Light.serializer(), light)
    return encodeEntries(
        if (entries.any { it.lightId() == light.id }) {
            entries.map { if (it.lightId() == light.id) encoded else it }
        } else {
            entries + encoded
        },
    )
}

/** Changes the light with [id]; an entry with that id it cannot read is left as stored. */
internal fun changeLight(
    text: String?,
    id: String,
    change: (Light) -> Light,
): String =
    encodeEntries(
        storedEntries(text).map { entry ->
            if (entry.lightId() != id) return@map entry
            decodeLight(entry)?.let { json.encodeToJsonElement(Light.serializer(), change(it)) } ?: entry
        },
    )

internal fun dropLight(
    text: String?,
    id: String,
): String = encodeEntries(storedEntries(text).filterNot { it.lightId() == id })
