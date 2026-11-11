package app.plainlight.tip

import java.time.Duration
import java.time.Instant

/** Lights added before the tip is mentioned: one is enough to say the app is in use. */
const val TIP_ASK_AFTER_LIGHTS = 1

/** How long the app has to have been installed before the tip is mentioned. */
val TIP_ASK_AFTER: Duration = Duration.ofDays(30)

/**
 * Whether the home screen should mention the tip — **once, to someone who already uses the app**
 * (ADR-0007).
 *
 * [installedAt] is the package's `firstInstallTime`, which a restore onto a new phone does not carry,
 * so a moved install waits its own month. [asked] lives in preferences, which Auto Backup carries to
 * the next phone (ADR-0005), so an answered ask stays answered there.
 */
fun tipAskDue(
    asked: Boolean,
    lights: Int,
    installedAt: Instant,
    now: Instant,
): Boolean =
    !asked &&
        lights >= TIP_ASK_AFTER_LIGHTS &&
        Duration.between(installedAt, now) >= TIP_ASK_AFTER
