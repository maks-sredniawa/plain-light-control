package app.plainlight.data

import app.plainlight.ble.LightProtocol
import org.junit.Assert.assertEquals
import org.junit.Test

class LightsJsonTest {
    private val lamp = Light("a", "Salon", LightProtocol.ZHIMEI_V2, 0x1234L, LightSetting(0.3f, 0.1f))

    @Test
    fun `a list survives a round trip`() = assertEquals(listOf(lamp), decodeLights(encodeLights(listOf(lamp))))

    @Test
    fun `one unreadable entry is dropped and the rest kept`() {
        val text = """[{"id":"x","name":"Old","protocol":"ZHIMEI_V9","controllerId":1},${encodeLights(
            listOf(lamp),
        ).removeSurrounding("[", "]")}]"""
        assertEquals(listOf(lamp), decodeLights(text))
    }

    @Test
    fun `a field added later reads its default and an unknown field is ignored`() {
        val text = """[{"id":"a","name":"Salon","protocol":"ZHIMEI_V2","controllerId":4660,"room":"living"}]"""
        assertEquals(listOf(lamp.copy(favourite = LightSetting())), decodeLights(text))
    }

    @Test
    fun `nothing stored and garbage both read as no lights`() {
        assertEquals(emptyList<Light>(), decodeLights(null))
        assertEquals(emptyList<Light>(), decodeLights("{not json"))
    }
}
