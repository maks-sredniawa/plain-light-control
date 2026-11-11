package app.plainlight.ble

data class AdStructure(
    val type: Int,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        other is AdStructure && type == other.type && data.contentEquals(other.data)

    override fun hashCode(): Int = 31 * type + data.contentHashCode()
}

const val AD_FLAGS = 0x01
const val AD_SERVICE_UUIDS_16 = 0x03
const val AD_MANUFACTURER_DATA = 0xFF

/** Splits a raw advertising payload into its length-type-value structures; null if malformed. */
fun parseAdStructures(raw: ByteArray): List<AdStructure>? {
    val structures = mutableListOf<AdStructure>()
    var i = 0
    while (i < raw.size) {
        val length = raw[i].toInt() and 0xFF
        if (length == 0) break
        if (i + 1 + length > raw.size) return null
        structures += AdStructure(raw[i + 1].toInt() and 0xFF, raw.copyOfRange(i + 2, i + 1 + length))
        i += 1 + length
    }
    return structures
}

/** The 16-bit service UUIDs Android must be given, in order, to emit [data] as an AD type 0x03 body. */
fun serviceUuids16(data: ByteArray): List<Int>? {
    if (data.size % 2 != 0) return null
    return data.toList().chunked(2) { (lo, hi) -> (lo.toInt() and 0xFF) or ((hi.toInt() and 0xFF) shl 8) }
}
