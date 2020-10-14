package uk.co.rossbeazley.mediacodec

import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test
import java.nio.ByteBuffer

class ShiftsBits
{


    @Test
    fun shifts32Bits() {
        val bytes: ByteBuffer = ByteBuffer.allocate(4).putInt( Integer.parseInt("0000000000000000000010011010010",2) )
        val int1234 : ByteArray = bytes.array()
        assertThat(int1234.size, `is`(4))
        assertThat(readBitsAsLong(32, int1234), `is`(1234L))
    }


    @Test
    fun shifts64Bits() {
        val bytes: ByteBuffer = ByteBuffer.allocate(8).putLong( -71777214294589696 )
        val longff00Repeats : ByteArray = bytes.array()
        assertThat(longff00Repeats.size, `is`(8))
        assertThat(readBitsAsLong(64, longff00Repeats), `is`(-71777214294589696))
    }


    @Test
    fun shifts32BitsWithOFfset() {
        val bytes: ByteBuffer = ByteBuffer.allocate(8)
                                            .putInt( Integer.parseInt("0000000000000000000011111111111",2) )
                                            .putInt( Integer.parseInt("0000000000000000000010011010010",2) )
        val int1234 : ByteArray = bytes.array()
        assertThat(int1234.size, `is`(8))
        assertThat(readBitsAsLong(32, int1234, 4), `is`(1234L))
    }

}