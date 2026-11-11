package app.plainlight.ble

import kotlinx.serialization.Serializable

/** The order a new light is tried in while pairing: the newest variant first. */
@Serializable
enum class LightProtocol {
    ZHIMEI_V2,
    ZHIMEI_V1,
}

data class LightAddress(
    val protocol: LightProtocol,
    val controllerId: Long,
)

sealed interface LightCommand {
    data object Pair : LightCommand

    data object On : LightCommand

    data object Off : LightCommand

    data class Brightness(
        val level: Float,
    ) : LightCommand

    data class ColourTemperature(
        val coldness: Float,
    ) : LightCommand
}

sealed interface SendResult {
    data object Sent : SendResult

    data object BluetoothOff : SendResult

    data object PermissionMissing : SendResult

    data object AdvertisingUnsupported : SendResult

    data class Failed(
        val reason: Int,
    ) : SendResult
}

fun interface LightTransmitter {
    suspend fun send(
        address: LightAddress,
        command: LightCommand,
    ): SendResult
}
