package app.plainlight.tip

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TipAskTest {
    private val installedAt = Instant.parse("2026-09-01T08:00:00Z")
    private val monthLater = installedAt + TIP_ASK_AFTER

    private fun due(
        asked: Boolean = false,
        lights: Int = TIP_ASK_AFTER_LIGHTS,
        now: Instant = monthLater,
    ) = tipAskDue(asked, lights, installedAt, now)

    @Test
    fun `due once installed a month with a light added`() {
        assertTrue(due())
    }

    @Test
    fun `not due a millisecond short of the month`() {
        assertFalse(due(lights = 1_000, now = monthLater.minusMillis(1)))
    }

    @Test
    fun `not due one light short`() {
        assertFalse(due(lights = TIP_ASK_AFTER_LIGHTS - 1, now = monthLater.plus(TIP_ASK_AFTER)))
    }

    @Test
    fun `never again once answered`() {
        assertFalse(due(asked = true, lights = 1_000, now = monthLater.plus(TIP_ASK_AFTER)))
    }
}
