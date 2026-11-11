package app.plainlight.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Vectors: advertisements captured from the vendor app, from NicoIIT/ha-ble-adv `tests/codecs/test_zhimei.py` at 7b40e85. */
class LightCodecTest {
    private data class Vector(
        val protocol: LightProtocol,
        val id: Long,
        val index: Int,
        val tx: Int,
        val seed: Int,
        val frame: LightCodec.Frame,
        val hex: String,
    )

    private val vectors =
        listOf(
            Vector(
                LightProtocol.ZHIMEI_V2,
                0x000002C0L,
                4,
                31,
                0x0000,
                LightCodec.Frame(0xB4, 0, 0, 0),
                "1B03F90849B2CE2C9A20758B15D5EF26C13510111213141516171819",
            ),
            Vector(
                LightProtocol.ZHIMEI_V1,
                0x0000C002L,
                4,
                31,
                0x006E,
                LightCodec.Frame(0xB4, 170, 102, 85),
                "1B0348464B4A1CAB1FB80EB7E17D988231A57E7EDB68101112131415",
            ),
            Vector(
                LightProtocol.ZHIMEI_V1,
                0x0000C002L,
                4,
                32,
                0x0008,
                LightCodec.Frame(0xA5, 2, 0, 0),
                "1B0348464B4A1C4936B80EB7E16C98C8F3E6D5B190BA101112131415",
            ),
            Vector(
                LightProtocol.ZHIMEI_V2,
                0x000002C0L,
                4,
                32,
                0x0000,
                LightCodec.Frame(0xA5, 2, 0, 0),
                "1B03F90849B2CE2CA51D4AB43BEAEF1B94D210111213141516171819",
            ),
            Vector(
                LightProtocol.ZHIMEI_V2,
                0x000002C0L,
                4,
                34,
                0x0000,
                LightCodec.Frame(0xB2, 0, 0, 0),
                "1B03F90849B2CE2CA71D48B62EE8EF1B6BDF10111213141516171819",
            ),
            Vector(
                LightProtocol.ZHIMEI_V1,
                0x0000C002L,
                4,
                34,
                0x00F2,
                LightCodec.Frame(0xB2, 0, 0, 0),
                "1B0348464B4A6992A76BC16A94CA432972517638CE1E101112131415",
            ),
            Vector(
                LightProtocol.ZHIMEI_V1,
                0x0000C002L,
                4,
                33,
                0x00D3,
                LightCodec.Frame(0xB3, 0, 0, 0),
                "1B0348464B4A9EA0B33A8C355FF81639418368409614101112131415",
            ),
            Vector(
                LightProtocol.ZHIMEI_V2,
                0x000002C0L,
                4,
                33,
                0x0000,
                LightCodec.Frame(0xB3, 0, 0, 0),
                "1B03F90849B2CE2CA41E4BB52CEBEF18ED0810111213141516171819",
            ),
            Vector(
                LightProtocol.ZHIMEI_V1,
                0x0000C002L,
                4,
                35,
                0x00DA,
                LightCodec.Frame(0xB5, 0, 0, 0),
                "1B0348464B4AE1E270F3C9F21CB1CB06B2A095EBACD7101112131415",
            ),
            Vector(
                LightProtocol.ZHIMEI_V2,
                0x000002C0L,
                4,
                35,
                0x0000,
                LightCodec.Frame(0xB5, 0, 0, 0),
                "1B03F90849B2CE2CA61C49B728E9EF1AA1CE10111213141516171819",
            ),
            Vector(
                LightProtocol.ZHIMEI_V1,
                0x0000C002L,
                4,
                36,
                0x004A,
                LightCodec.Frame(0xB5, 0, 3, 232),
                "1B0348464B4A9E09B83A8C355FFA16214E7B71E03145101112131415",
            ),
            Vector(
                LightProtocol.ZHIMEI_V2,
                0x000002C0L,
                4,
                36,
                0x0000,
                LightCodec.Frame(0xB5, 0, 3, 232),
                "1B03F90849B2CE2C49F3A65BC706071D23A910111213141516171819",
            ),
            Vector(
                LightProtocol.ZHIMEI_V2,
                0x000002C0L,
                4,
                37,
                0x0000,
                LightCodec.Frame(0xB7, 0, 3, 232),
                "1B03F90849B2CE2C48F2A75AC407071CBED610111213141516171819",
            ),
            Vector(
                LightProtocol.ZHIMEI_V1,
                0x0000C002L,
                4,
                37,
                0x000A,
                LightCodec.Frame(0xB7, 0, 3, 232),
                "1B0348464B4AE48F71F0C6EF19B2D04D696351944ADB101112131415",
            ),
            Vector(
                LightProtocol.ZHIMEI_V1,
                0x0000C002L,
                4,
                38,
                0x003C,
                LightCodec.Frame(0xB7, 0, 0, 0),
                "1B0348464B4A99F8B33B913A64F713B201E4E9CFB13C101112131415",
            ),
            Vector(
                LightProtocol.ZHIMEI_V2,
                0x000002C0L,
                4,
                38,
                0x0000,
                LightCodec.Frame(0xB7, 0, 0, 0),
                "1B03F90849B2CE2CA3194CB22FECEF1F81A210111213141516171819",
            ),
            Vector(
                LightProtocol.ZHIMEI_V2,
                0x000002C0L,
                4,
                30,
                0x0000,
                LightCodec.Frame(0xCA, 255, 19, 0),
                "1B03F90849B2CE2C9BDE74996AD4EFD87CAF10111213141516171819",
            ),
            Vector(
                LightProtocol.ZHIMEI_V1,
                0x0000C002L,
                4,
                30,
                0x000C,
                LightCodec.Frame(0xCA, 255, 19, 0),
                "1B0348464B4A5120D3835982ACDA5BF007959EE17F1D101112131415",
            ),
            Vector(
                LightProtocol.ZHIMEI_V1,
                0x0000C002L,
                4,
                40,
                0x008B,
                LightCodec.Frame(0xA6, 2, 0, 0),
                "1B0348464B4A07F113D123CCF644AD6D543B320C9FCA101112131415",
            ),
            Vector(
                LightProtocol.ZHIMEI_V2,
                0x000002C0L,
                4,
                40,
                0x0000,
                LightCodec.Frame(0xA6, 2, 0, 0),
                "1B03F90849B2CE2CAD1542BC30E2EF1323D010111213141516171819",
            ),
            Vector(
                LightProtocol.ZHIMEI_V1,
                0x0000C002L,
                4,
                39,
                0x00F5,
                LightCodec.Frame(0xA6, 1, 0, 0),
                "1B0348464B4AE4FC6FF0C6EF19A3D01CDA0D81DD3A2D101112131415",
            ),
            Vector(
                LightProtocol.ZHIMEI_V2,
                0x000002C0L,
                4,
                39,
                0x0000,
                LightCodec.Frame(0xA6, 1, 0, 0),
                "1B03F90849B2CE2CA2194DB33FEDEF1F2CB510111213141516171819",
            ),
            Vector(
                LightProtocol.ZHIMEI_V2,
                0x000002C0L,
                1,
                51,
                0x0000,
                LightCodec.Frame(0xA1, 25, 25, 0),
                "1B03F90849B2CE2CB6155CBE2CF9EF130BB910111213141516171819",
            ),
            Vector(
                LightProtocol.ZHIMEI_V1,
                0x0000C002L,
                1,
                51,
                0x0042,
                LightCodec.Frame(0xA1, 25, 25, 0),
                "1B0348464B4AFFB026D92BD4FE47B29B0FF6FBAEDDF0101112131415",
            ),
            Vector(
                LightProtocol.ZHIMEI_V1,
                0x0000C002L,
                1,
                47,
                0x000B,
                LightCodec.Frame(0xA7, 1, 0, 0),
                "1B0348464B4A7BED8E5DAF5882C936B6AE3DE10B6568101112131415",
            ),
            Vector(
                LightProtocol.ZHIMEI_V2,
                0x000002C0L,
                1,
                47,
                0x0000,
                LightCodec.Frame(0xA7, 1, 0, 0),
                "1B03F90849B2CE2CAA1140BB36E5EF176A9A10111213141516171819",
            ),
            Vector(
                LightProtocol.ZHIMEI_V2,
                0x000002C0L,
                1,
                48,
                0x0000,
                LightCodec.Frame(0xA7, 2, 0, 0),
                "1B03F90849B2CE2CB50D5FA429FAEF0B91B110111213141516171819",
            ),
            Vector(
                LightProtocol.ZHIMEI_V1,
                0x0000C002L,
                1,
                48,
                0x0022,
                LightCodec.Frame(0xA7, 2, 0, 0),
                "1B0348464B4A0A8538CE20C9F378A5CDBEE9D4D4EC38101112131415",
            ),
            Vector(
                LightProtocol.ZHIMEI_V2,
                0x000002C0L,
                1,
                49,
                0x0000,
                LightCodec.Frame(0xA7, 3, 0, 0),
                "1B03F90849B2CE2CB40D5EA528FBEF0B267910111213141516171819",
            ),
            Vector(
                LightProtocol.ZHIMEI_V1,
                0x0000C002L,
                1,
                49,
                0x00A8,
                LightCodec.Frame(0xA7, 3, 0, 0),
                "1B0348464B4A16FF2BC214BDE77499AEFAFDEFAB7A6E101112131415",
            ),
        )

    private fun ByteArray.hex() = joinToString("") { "%02X".format(it) }

    @Test
    fun `every captured advertisement is reproduced byte for byte`() {
        for (v in vectors) {
            val encoded = LightCodec.encodeFrame(v.protocol, v.id, v.index, v.tx, v.seed, v.frame)
            assertEquals(v.toString(), v.hex, encoded.hex())
        }
    }

    @Test
    fun `commands map to the codes the vendor app sends`() {
        assertEquals(
            LightCodec.Frame(0xB4, 0xAA, 0x66, 0x55),
            LightCodec.frame(LightProtocol.ZHIMEI_V1, LightCommand.Pair),
        )
        assertEquals(LightCodec.Frame(0xB4), LightCodec.frame(LightProtocol.ZHIMEI_V2, LightCommand.Pair))
        assertEquals(
            LightCodec.Frame(0xB5, 0, 3, 232),
            LightCodec.frame(LightProtocol.ZHIMEI_V2, LightCommand.Brightness(1f)),
        )
        assertEquals(
            LightCodec.Frame(0xB7, 0, 1, 244),
            LightCodec.frame(LightProtocol.ZHIMEI_V1, LightCommand.ColourTemperature(0.5f)),
        )
    }

    @Test
    fun `the timer for 2 hours is the captured A5 frame for both variants`() {
        for (v in vectors.filter { it.frame.cmd == 0xA5 }) {
            val frame = LightCodec.frame(v.protocol, LightCommand.Timer(2))
            assertEquals(LightCodec.Frame(0xA5, 2, 0, 0), frame)
            assertEquals(v.hex, LightCodec.encodeFrame(v.protocol, v.id, v.index, v.tx, v.seed, frame).hex())
        }
        assertEquals(2, vectors.count { it.frame.cmd == 0xA5 })
    }

    @Test
    fun `every encoded payload is one service-UUID structure Android can express`() {
        for (protocol in LightProtocol.entries) {
            for (sequence in 0..300) {
                val raw = LightCodec.encode(LightAddress(protocol, 0xC002L), LightCommand.Brightness(0.3f), sequence)
                val structures = parseAdStructures(raw)!!
                assertEquals(1, structures.size)
                assertEquals(AD_SERVICE_UUIDS_16, structures[0].type)
                assertTrue(serviceUuids16(structures[0].data) != null)
                assertTrue(raw.size + 3 <= 31)
            }
        }
    }
}
