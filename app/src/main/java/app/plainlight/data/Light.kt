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
    val lastUsed: LightSetting? = null,
    val favouriteRemembered: Boolean = false,
) {
    val address: LightAddress get() = LightAddress(protocol, controllerId)

    /** What On brings back: a favourite the user remembered, else what was last sent, since the lamp forgets it at the wall switch. */
    val onSetting: LightSetting get() = if (favouriteRemembered) favourite else lastUsed ?: favourite
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

private fun encode(light: Light): JsonElement = json.encodeToJsonElement(Light.serializer(), light)

private fun encodeOver(
    stored: JsonElement,
    before: Light?,
    after: Light,
): JsonElement = keepUnknown(stored, before?.let(::encode), encode(after), Light.serializer().descriptor)

/** Keeps [stored]'s unknown names, and a known name's stored value while [before] (null if unreadable) and [after] agree on it. */
private fun keepUnknown(
    stored: JsonElement,
    before: JsonElement?,
    after: JsonElement,
    descriptor: SerialDescriptor,
): JsonElement =
    when {
        stored is JsonObject && after is JsonObject && descriptor.kind == StructureKind.CLASS -> {
            val known = (0 until descriptor.elementsCount).associateBy(descriptor::getElementName)
            val beforeObject = before as? JsonObject
            val unknown = stored.filterKeys { it !in known }
            val merged =
                known.mapNotNull { (name, index) ->
                    val now = after[name]
                    val kept = stored[name]
                    val value =
                        when {
                            beforeObject != null && beforeObject[name] == now -> kept ?: now
                            now == null -> null
                            else ->
                                kept?.let {
                                    keepUnknown(
                                        it,
                                        beforeObject?.get(name),
                                        now,
                                        descriptor.getElementDescriptor(index),
                                    )
                                } ?: now
                        }
                    value?.let { name to it }
                }
            JsonObject(unknown + merged)
        }
        stored is JsonArray && after is JsonArray && descriptor.kind == StructureKind.LIST -> {
            val storedById = stored.associateBy { it.id() }
            val beforeById = (before as? JsonArray)?.associateBy { it.id() }
            JsonArray(
                after.map { item ->
                    storedById[item.id()]?.let {
                        keepUnknown(it, beforeById?.get(item.id()), item, descriptor.getElementDescriptor(0))
                    } ?: item
                },
            )
        }
        else -> after
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
            entries.map { if (it.id() == light.id) encodeOver(it, decodeLight(it), light) else it }
        } else {
            entries + encode(light)
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
            decodeLight(entry)?.let { encodeOver(entry, it, change(it)) } ?: entry
        },
    )

internal fun dropLight(
    text: String?,
    id: String,
): String = encodeEntries(storedEntries(text).filterNot { it.id() == id })
