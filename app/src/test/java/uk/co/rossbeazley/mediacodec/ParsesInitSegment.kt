package uk.co.rossbeazley.mediacodec

import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

class ParsesInitSegment {


    /**
     *
    aligned(8)
    class Box( unsigned int(32) boxtype, optional unsigned int(8)[16] extended_type)
    {
    unsigned int(32) size;
    unsigned int(32) type = boxtype;
    if (size==1) {
    unsigned   int(64)   largesize;
    }
    else if (size==0)
    {       // box extends to end of file    }
    if (boxtype==‘uuid’) {
    unsigned int(8)[16] usertype = extended_type;
    }
    }

    class FullBox(unsigned int(32) boxtype, unsigned int(8) v, bit(24) f)     extends Box(boxtype)
    {
    unsigned int(8)   version = v;
    bit(24)  flags   =   f;
    }


     */

    lateinit var extracter: InitM4SExtractor

    @Before
    fun givenASegmentIsLoadedIntoMemory() {
        val classLoader = this::class.java.classLoader!!
        val resourceAsStream = classLoader.getResourceAsStream("init.dash")
        val bytes: ByteArray = resourceAsStream.use { it.readBytes() }
        extracter = InitM4SExtractor(bytes)
    }

    @Test
    fun segmentCanParseTheBoxes() {
        assertThat(extracter.boxes.size, `is`(3))
    }

    @Test
    fun theFirstBoxIsFtyp() {
        assertThat(extracter.boxes["ftyp"]?.name, `is`("ftyp"))
    }

    @Test
    fun theFirstBoxHas480BytesOfPayload() {
        assertThat(extracter.boxes["ftyp"]!!.payload.size, `is`(16))
        assertThat(extracter.boxes["ftyp"]!!.size, `is`(24))
    }

    @Test
    fun moovHasThreeBoxes() {
        val moovBox = extracter.boxes["moov"] as BoxOfBoxes
        assertThat(moovBox.boxes.size, `is`(3))
        assertThat(moovBox.boxes["mvhd"]!!.size, `is`(108))
        assertThat(moovBox.boxes["trak"]!!.size, `is`(8+459))
        assertThat(moovBox.boxes["mvex"]!!.size, `is`(8+32))

    }
}
