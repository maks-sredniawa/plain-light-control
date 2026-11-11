package app.plainlight.data

import app.plainlight.ble.LightProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LightsJsonTest {
    private val lamp = Light("a", "Salon", LightProtocol.ZHIMEI_V2, 0x1234L, LightSetting(0.3f, 0.1f))
    private val unreadable = """{"id":"x","name":"Old","protocol":"ZHIMEI_V9","controllerId":1}"""

    private fun encodeLights(lights: List<Light>): String? =
        lights.fold(null as String?) { text, light -> putLight(text, light) }

    @Test
    fun `a list survives a round trip`() = assertEquals(listOf(lamp), decodeLights(encodeLights(listOf(lamp))))

    @Test
    fun `one unreadable entry is dropped and the rest kept`() {
        val text = """[$unreadable,${encodeLights(listOf(lamp))!!.removeSurrounding("[", "]")}]"""
        assertEquals(listOf(lamp), decodeLights(text))
    }

    @Test
    fun `an unreadable entry survives saving, changing and removing other lights`() {
        val other = lamp.copy(id = "b", name = "Hall")
        val saved = putLight(putLight("[$unreadable]", lamp), other)
        val changed = changeLight(saved, lamp.id) { it.copy(name = "Kitchen") }
        val removed = dropLight(changed, other.id)
        assertEquals(listOf(lamp.copy(name = "Kitchen")), decodeLights(removed))
        assertTrue(removed.startsWith("[$unreadable,"))
    }

    @Test
    fun `an unreadable entry is replaced by a save with its id and removed with it, never changed`() {
        assertEquals("[$unreadable]", changeLight("[$unreadable]", "x") { it.copy(name = "New") })
        assertEquals(listOf(lamp.copy(id = "x")), decodeLights(putLight("[$unreadable]", lamp.copy(id = "x"))))
        assertEquals("[]", dropLight("[$unreadable]", "x"))
    }

    @Test
    fun `a field added later reads its default and an unknown field is ignored`() {
        val text = """[{"id":"a","name":"Salon","protocol":"ZHIMEI_V2","controllerId":4660,"room":"living"}]"""
        assertEquals(listOf(lamp.copy(favourite = LightSetting())), decodeLights(text))
    }

    @Test
    fun `a light stored by 0_2_0, without scenes, reads the three presets`() {
        val text =
            """[{"id":"a","name":"Salon","protocol":"ZHIMEI_V2","controllerId":4660,""" +
                """"favourite":{"brightness":0.3,"coldness":0.1}}]"""
        val scenes = decodeLights(text).single().scenes
        assertEquals(
            listOf(ScenePreset.EVENING, ScenePreset.READING, ScenePreset.NIGHT_LIGHT),
            scenes.map { it.preset },
        )
        assertEquals(LightSetting(0.05f, 0f), scenes.last().setting)
    }

    @Test
    fun `a saved scene and a deleted preset both survive a round trip`() {
        val saved = Scene("s1", name = "Film", setting = LightSetting(0.2f, 0.8f))
        val edited = lamp.copy(scenes = presetScenes.drop(1) + saved)
        assertEquals(listOf(edited), decodeLights(encodeLights(listOf(edited))))
    }

    @Test
    fun `a light with every scene deleted keeps none`() {
        val bare = lamp.copy(scenes = emptyList())
        assertEquals(listOf(bare), decodeLights(encodeLights(listOf(bare))))
    }

    @Test
    fun `nothing stored and garbage both read as no lights`() {
        assertEquals(emptyList<Light>(), decodeLights(null))
        assertEquals(emptyList<Light>(), decodeLights("{not json"))
    }

    private val fromNewerVersion =
        """{"id":"a","name":"Salon","protocol":"ZHIMEI_V2","controllerId":4660,"room":"Kitchen",""" +
            """"favourite":{"brightness":0.3,"coldness":0.1,"hue":3},""" +
            """"scenes":[{"id":"film","name":"Film","setting":{"brightness":0.2,"coldness":0.4},"fade":2}]}"""

    @Test
    fun `a change keeps the fields a newer version stored, at every depth`() {
        val changed = changeLight("[$fromNewerVersion]", "a") { it.copy(name = "Kitchen") }
        assertEquals("Kitchen", decodeLights(changed).single().name)
        listOf(""""room":"Kitchen"""", """"hue":3""", """"fade":2""").forEach {
            assertTrue("$it lost from $changed", it in changed)
        }
    }

    @Test
    fun `a field reset to its default is not brought back from the stored entry`() {
        val changed =
            changeLight("[$fromNewerVersion]", "a") { it.copy(scenes = presetScenes, favourite = LightSetting()) }
        val light = decodeLights(changed).single()
        assertEquals(presetScenes, light.scenes)
        assertEquals(LightSetting(), light.favourite)
        assertTrue(""""room":"Kitchen"""" in changed)
    }

    @Test
    fun `a save over a stored light keeps the fields a newer version stored`() {
        val saved = putLight("[$fromNewerVersion]", decodeLights("[$fromNewerVersion]").single().copy(name = "Hall"))
        assertTrue(""""room":"Kitchen"""" in saved)
        assertEquals("Hall", decodeLights(saved).single().name)
    }
}
