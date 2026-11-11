package app.plainlight.ui.lights

import app.plainlight.ble.LightCommand
import app.plainlight.ble.LightTransmitter
import app.plainlight.ble.SendResult
import app.plainlight.data.LastSent
import app.plainlight.data.Light

/** Some lamps reset brightness on a colour change, so brightness always goes last. */
suspend fun LightTransmitter.switchOn(
    light: Light,
    lastSent: LastSent,
): SendResult =
    sendAll(
        light,
        LightCommand.On,
        LightCommand.ColourTemperature(light.favourite.coldness),
        LightCommand.Brightness(light.favourite.brightness),
    ).ifSent { lastSent.update(light) { light.favourite } }

inline fun SendResult.ifSent(record: () -> Unit): SendResult = also { if (it == SendResult.Sent) record() }

suspend fun LightTransmitter.sendAll(
    light: Light,
    vararg commands: LightCommand,
): SendResult {
    for (command in commands) {
        val result = send(light.address, command)
        if (result != SendResult.Sent) return result
    }
    return SendResult.Sent
}
