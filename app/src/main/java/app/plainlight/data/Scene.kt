package app.plainlight.data

import kotlinx.serialization.Serializable

@Serializable
enum class ScenePreset {
    EVENING,
    READING,
    NIGHT_LIGHT,
}

/** A preset has no [name], so its label is a string resource that follows the app's language; a saved scene has a name and no preset. */
@Serializable
data class Scene(
    val id: String,
    val name: String? = null,
    val preset: ScenePreset? = null,
    val setting: LightSetting,
)

val presetScenes: List<Scene> =
    listOf(
        Scene("evening", preset = ScenePreset.EVENING, setting = LightSetting(brightness = 0.30f, coldness = 0.10f)),
        Scene("reading", preset = ScenePreset.READING, setting = LightSetting(brightness = 1.00f, coldness = 0.50f)),
        Scene(
            "night_light",
            preset = ScenePreset.NIGHT_LIGHT,
            setting = LightSetting(brightness = 0.05f, coldness = 0.00f),
        ),
    )
