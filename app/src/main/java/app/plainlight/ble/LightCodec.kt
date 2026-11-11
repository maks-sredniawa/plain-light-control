package app.plainlight.ble

import kotlin.random.Random

/**
 * The Zhi Mei advertising format the lamp's own app (智美灯控PRO) speaks, ported from
 * NicoIIT/ha-ble-adv `codecs/zhimei.py` (MIT), commit 7b40e85.
 */
object LightCodec {
    private const val INDEX = 4
    private const val TX_MAX = 125
    private const val SEED_MAX = 0xF5
    private const val LEVEL_SCALE = 1000

    private val V1_HEADER = bytes(0x48, 0x46, 0x4B, 0x4A)
    private val V1_FOOTER = bytes(0x10, 0x11, 0x12, 0x13, 0x14, 0x15)
    private val V1_MATRIX = intArrayOf(29, 4, 17, 32, 152, 117, 40, 70, 11, 175, 67, 172, 214, 190, 137, 142)
    private val V2_HEADER = bytes(0xF9, 0x08, 0x49)
    private val V2_PREFIX = bytes(0x33, 0xAA, 0x55)
    private val V2_FOOTER = bytes(0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19)

    internal data class Frame(
        val cmd: Int,
        val arg0: Int = 0,
        val arg1: Int = 0,
        val arg2: Int = 0,
    )

    fun newControllerId(random: Random = Random.Default): Long = random.nextLong(1, 0xFFFF)

    /** The advertising payload's AD structures, without the flags Android writes itself. */
    fun encode(
        address: LightAddress,
        command: LightCommand,
        sequence: Int,
    ): ByteArray {
        val tx = 1 + Math.floorMod(sequence, TX_MAX - 1)
        val seed = 1 + Math.floorMod(sequence * 101 + address.controllerId.toInt(), SEED_MAX)
        return encodeFrame(address.protocol, address.controllerId, INDEX, tx, seed, frame(address.protocol, command))
    }

    internal fun frame(
        protocol: LightProtocol,
        command: LightCommand,
    ): Frame =
        when (command) {
            LightCommand.Pair -> if (protocol == LightProtocol.ZHIMEI_V1) Frame(0xB4, 0xAA, 0x66, 0x55) else Frame(0xB4)
            LightCommand.On -> Frame(0xB3)
            LightCommand.Off -> Frame(0xB2)
            is LightCommand.Brightness -> level(0xB5, command.level)
            is LightCommand.ColourTemperature -> level(0xB7, command.coldness)
            is LightCommand.Timer -> Frame(0xA5, arg0 = command.hours)
        }

    private fun level(
        cmd: Int,
        fraction: Float,
    ): Frame {
        val value = (fraction.coerceIn(0f, 1f) * LEVEL_SCALE).toInt()
        return Frame(cmd, arg1 = value / 256, arg2 = value % 256)
    }

    internal fun encodeFrame(
        protocol: LightProtocol,
        id: Long,
        index: Int,
        tx: Int,
        seed: Int,
        frame: Frame,
    ): ByteArray {
        val data =
            when (protocol) {
                LightProtocol.ZHIMEI_V1 -> V1_HEADER + encryptV1(readableV1(id, index, tx, seed, frame)) + V1_FOOTER
                LightProtocol.ZHIMEI_V2 ->
                    V2_HEADER + encryptV2(V2_PREFIX + readableV2(id, index, tx, frame)) +
                        V2_FOOTER
            }
        return bytes(data.size + 1, AD_SERVICE_UUIDS_16) + data
    }

    private fun readableV1(
        id: Long,
        index: Int,
        tx: Int,
        seed: Int,
        f: Frame,
    ): ByteArray {
        val uid = (0 until 4).map { (id shr (8 * it)).toInt() and 0xFF }
        return bytes(0xFF, seed, tx, uid[0], uid[1], uid[2], uid[3], f.cmd, index, 0xFF, tx, f.arg0, f.arg1, f.arg2)
    }

    private fun encryptV1(readable: ByteArray): ByteArray {
        var buffer = readable
        if (buffer[7].toInt() and 0xFF != 0xB4) {
            buffer = buffer.copyOfRange(0, 9) + applyMatrix(buffer.copyOfRange(9, buffer.size), 10)
        }
        val crc = crcHqx(buffer.copyOfRange(0, buffer.size - 1), 0)
        buffer += bytes(crc and 0xFF, crc shr 8)
        return applyMatrix(buffer, 6)
    }

    private fun applyMatrix(
        buffer: ByteArray,
        key: Int,
    ): ByteArray {
        val b1 = buffer[1].toInt() and 0xFF
        val pivot = V1_MATRIX[((b1 shr 4) and 15) xor (b1 and 15)]
        return ByteArray(buffer.size) { i ->
            (((buffer[i].toInt() and 0xFF) xor pivot) + V1_MATRIX[(key + i) and 0xF]).toByte()
        }
    }

    private fun readableV2(
        id: Long,
        index: Int,
        tx: Int,
        f: Frame,
    ): ByteArray {
        val u0 = (id and 0xFF).toInt()
        val u1 = ((id shr 8) and 0xFF).toInt()
        val d = intArrayOf(u1, f.arg0, index, f.arg1, f.cmd, u0, tx xor u1, f.arg0 xor f.arg2)
        val pivot = d[0] xor d[1] xor d[6] xor d[7]
        return ByteArray(d.size) { (d[it] xor pivot).toByte() }
    }

    private fun encryptV2(buffer: ByteArray): ByteArray {
        val crc = revCrc16Ccitt(buffer)
        return whiten(buffer + bytes(crc and 0xFF, crc shr 8), 0x48)
    }

    internal fun crcHqx(
        data: ByteArray,
        init: Int,
    ): Int {
        var crc = init
        for (byte in data) {
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            repeat(8) { crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1 }
            crc = crc and 0xFFFF
        }
        return crc
    }

    private fun revCrc16Ccitt(buffer: ByteArray): Int {
        val reversed = ByteArray(buffer.size) { reverse8(buffer[it].toInt() and 0xFF).toByte() }
        val crc = crcHqx(reversed, 0xFFFF)
        return reverse16(crc xor 0xFFFF)
    }

    private fun reverse8(x: Int): Int = Integer.reverse(x) ushr 24

    private fun reverse16(x: Int): Int = Integer.reverse(x) ushr 16

    private fun whiten(
        buffer: ByteArray,
        seed: Int,
    ): ByteArray {
        var r = seed
        return ByteArray(buffer.size) { i ->
            var b = 0
            for (j in 0 until 8) {
                r = r shl 1
                if (r and 0x80 != 0) {
                    r = r xor 0x11
                    b = b or (1 shl j)
                }
                r = r and 0x7F
            }
            ((buffer[i].toInt() and 0xFF) xor b).toByte()
        }
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }
}
