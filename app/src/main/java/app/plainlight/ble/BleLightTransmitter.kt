package app.plainlight.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.random.Random

private const val ADVERTISE_MILLIS = 750L

/** Some Bluetooth stacks answer neither success nor failure; without a limit the send, and every send queued behind it, waits forever. */
private const val START_TIMEOUT_MILLIS = 3_000L

const val FAILED_START_TIMEOUT = -1

class BleLightTransmitter(
    context: Context,
    private val encode: (LightAddress, LightCommand, Int) -> ByteArray,
) : LightTransmitter {
    private val appContext = context.applicationContext
    private val mutex = Mutex()

    /** A lamp may drop a frame identical to the last one it heard, so a cold start must not replay the previous process's first number. */
    private var sequence = Random.nextInt(0x100)

    override suspend fun send(
        address: LightAddress,
        command: LightCommand,
    ): SendResult =
        mutex.withLock {
            if (!canAdvertise(appContext)) return SendResult.PermissionMissing
            val adapter =
                appContext.getSystemService(BluetoothManager::class.java)?.adapter
                    ?: return SendResult.AdvertisingUnsupported
            if (!adapter.isEnabled) return SendResult.BluetoothOff
            val advertiser = adapter.bluetoothLeAdvertiser ?: return SendResult.AdvertisingUnsupported
            val data = advertiseData(encode(address, command, sequence)) ?: return SendResult.AdvertisingUnsupported
            sequence = (sequence + 1) and 0xFF
            advertiseOnce(advertiser, data)
        }

    @SuppressLint("MissingPermission")
    private suspend fun advertiseOnce(
        advertiser: android.bluetooth.le.BluetoothLeAdvertiser,
        data: AdvertiseData,
    ): SendResult {
        val settings =
            AdvertiseSettings
                .Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                // Android writes the flags structure only into a connectable advertisement, and the lamp ignores a packet without it.
                .setConnectable(true)
                .build()
        var callback: AdvertiseCallback? = null
        val started =
            withTimeoutOrNull(START_TIMEOUT_MILLIS) {
                suspendCancellableCoroutine<SendResult> { continuation ->
                    callback =
                        object : AdvertiseCallback() {
                            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                                if (continuation.isActive) continuation.resume(SendResult.Sent)
                            }

                            override fun onStartFailure(errorCode: Int) {
                                if (continuation.isActive) continuation.resume(SendResult.Failed(errorCode))
                            }
                        }
                    advertiser.startAdvertising(settings, data, callback)
                    continuation.invokeOnCancellation { advertiser.stopAdvertising(callback) }
                }
            } ?: SendResult.Failed(FAILED_START_TIMEOUT)
        if (started == SendResult.Sent) {
            try {
                delay(ADVERTISE_MILLIS)
            } finally {
                advertiser.stopAdvertising(callback)
            }
        }
        return started
    }
}

fun canAdvertise(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED

private const val BASE_UUID_LOW = -0x7fffff7fa064cb05L

private fun advertiseData(raw: ByteArray): AdvertiseData? {
    val builder = AdvertiseData.Builder().setIncludeDeviceName(false).setIncludeTxPowerLevel(false)
    for (structure in parseAdStructures(raw) ?: return null) {
        when (structure.type) {
            AD_FLAGS -> Unit
            AD_SERVICE_UUIDS_16 ->
                serviceUuids16(structure.data)?.forEach { short ->
                    builder.addServiceUuid(ParcelUuid(UUID((short.toLong() shl 32) or 0x1000L, BASE_UUID_LOW)))
                } ?: return null
            AD_MANUFACTURER_DATA -> {
                if (structure.data.size < 2) return null
                val company = (structure.data[0].toInt() and 0xFF) or ((structure.data[1].toInt() and 0xFF) shl 8)
                builder.addManufacturerData(company, structure.data.copyOfRange(2, structure.data.size))
            }
            else -> return null
        }
    }
    return builder.build()
}
