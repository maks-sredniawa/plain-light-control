package app.plainlight.data

import app.plainlight.ble.LightAddress
import app.plainlight.ble.LightProtocol
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
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

private fun JsonElement.id(): String? = ((this as? JsonObject)?.get("id") as? JsonPrimitive)?.contentOrNull

private fun decodeLight(entry: JsonElement): Light? =
    runCatching {
        json.decodeFromJsonElement(Light.serializer(), entry)
    }.getOrNull()

private fun encodeEntries(entries: List<JsonElement>): String = json.encodeToString(JsonArray(entries))

private fun encodeOver(
    stored: JsonElement,
    light: Light,
): JsonElement = keepUnknown(stored, json.encodeToJsonElement(Light.serializer(), light), Light.serializer().descriptor)

/**
 * Keeps what a newer version stored under names this one does not know. A known name missing from
 * [encoded] is a default left out by the encoder, so it is never taken from [stored].
 */
private fun keepUnknown(
    stored: JsonElement,
    encoded: JsonElement,
    descriptor: SerialDescriptor,
): JsonElement =
    when {
        stored is JsonObject && encoded is JsonObject && descriptor.kind == StructureKind.CLASS -> {
            val known = (0 until descriptor.elementsCount).associateBy(descriptor::getElementName)
            val unknown = stored.filterKeys { it !in known }
            val merged =
                encoded.mapValues { (name, value) ->
                    val index = known[name] ?: return@mapValues value
                    stored[name]?.let { keepUnknown(it, value, descriptor.getElementDescriptor(index)) } ?: value
                }
            JsonObject(unknown + merged)
        }
        stored is JsonArray && encoded is JsonArray && descriptor.kind == StructureKind.LIST -> {
            val storedById = stored.associateBy { it.id() }
            JsonArray(
                encoded.map { item ->
                    storedById[item.id()]?.let { keepUnknown(it, item, descriptor.getElementDescriptor(0)) } ?: item
                },
            )
        }
        else -> encoded
    }

/** Skips an entry it cannot read; the writes below keep that entry as stored, so a later version can still read it. */
internal fun decodeLights(text: String?): List<Light> = storedEntries(text).mapNotNull(::decodeLight)

internal fun putLight(
    text: String?,
    light: Light,
): String {
    val entries = storedEntries(text)
    return encodeEntries(
        if (entries.any { it.id() == light.id }) {
            entries.map { if (it.id() == light.id) encodeOver(it, light) else it }
        } else {
            entries + json.encodeToJsonElement(Light.serializer(), light)
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
            if (entry.id() != id) return@map entry
            decodeLight(entry)?.let { encodeOver(entry, change(it)) } ?: entry
        },
    )

internal fun dropLight(
    text: String?,
    id: String,
): String = encodeEntries(storedEntries(text).filterNot { it.id() == id })
