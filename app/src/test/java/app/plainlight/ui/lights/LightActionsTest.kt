package app.plainlight.ui.lights

import app.plainlight.ble.LightCommand
import app.plainlight.ble.LightProtocol
import app.plainlight.ble.LightTransmitter
import app.plainlight.ble.SendResult
import app.plainlight.data.Light
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LightActionsTest {
    private val lights = (1L..3L).map { Light("l$it", "Light $it", LightProtocol.ZHIMEI_V2, it) }

    @Test
    fun `All off reaches every light and reports the first problem`() =
        runBlocking {
            val reached = mutableListOf<Long>()
            val transmitter =
                LightTransmitter { address, command ->
                    assertEquals(LightCommand.Off, command)
                    reached += address.controllerId
                    if (address.controllerId == 2L) SendResult.Failed(7) else SendResult.Sent
                }
            assertEquals(SendResult.Failed(7), transmitter.switchAllOff(lights))
            assertEquals(listOf(1L, 2L, 3L), reached)
        }

    @Test
    fun `All off with nothing wrong reports sent`() =
        runBlocking {
            assertEquals(SendResult.Sent, LightTransmitter { _, _ -> SendResult.Sent }.switchAllOff(lights))
        }
}
