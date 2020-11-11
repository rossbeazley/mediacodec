package uk.co.rossbeazley.mediacodec

import org.hamcrest.CoreMatchers.*
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
        val resourceAsStream = classLoader.getResourceAsStream("redGreen/init.dash")
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


    @Test
    fun trakHasTwoBoxes() {
        val moov = extracter.boxes["moov"] as BoxOfBoxes
        val box = moov.boxes["trak"] as BoxOfBoxes
        assertThat(box.boxes.size, `is`(2))
        assertThat(box.boxes["tkhd"]!!.size, `is`(12+80))
        assertThat(box.boxes["mdia"]!!.size, `is`(8+359))
    }

    @Test
    fun mdiaHasThreeBoxes() {
        val moov = extracter.boxes["moov"] as BoxOfBoxes
        val trak = moov.boxes["trak"] as BoxOfBoxes
        val box = trak.boxes["mdia"] as BoxOfBoxes
        assertThat(box.boxes.size, `is`(3))
        assertThat(box.boxes["mdhd"]!!.size, `is`(12+20))
        assertThat(box.boxes["hdlr"]!!.size, `is`(12+38))
        assertThat(box.boxes["minf"]!!.size, `is`(8+269))
    }


    @Test
    fun minfHasThreeBoxes() {
        val moov = extracter.boxes["moov"] as BoxOfBoxes
        val trak = moov.boxes["trak"] as BoxOfBoxes
        val mdia = trak.boxes["mdia"] as BoxOfBoxes
        val box = mdia.boxes["minf"] as BoxOfBoxes
        assertThat(box.boxes.size, `is`(3))
        assertThat(box.boxes["vmhd"]!!.size, `is`(12+8))
        assertThat(box.boxes["dinf"]!!.size, `is`(8+28))
        assertThat(box.boxes["stbl"]!!.size, `is`(8+205))
    }


    @Test
    fun stblIsParsed() {
        val moov = extracter.boxes["moov"] as BoxOfBoxes
        val trak = moov.boxes["trak"] as BoxOfBoxes
        val mdia = trak.boxes["mdia"] as BoxOfBoxes
        val minf = mdia.boxes["minf"] as BoxOfBoxes
        val box = minf.boxes["stbl"] as BoxOfBoxes
        assertThat(box.boxes.size, `is`(5))
        assertThat(box.boxes["stsd"]!!.size, `is`(12+125))
    }


    /**
     *
     * [stsd] size=12+125
            entry_count = 1
            [avc3] size=8+113
              data_reference_index = 1
              width = 192
              height = 108
              compressor = Elemental H.264
              [avcC] size=8+7
                Configuration Version = 1
                Profile = Baseline
                Profile Compatibility = c0
                Level = 21
                NALU Length Size = 4
              [btrt] size=8+12

     */
    @Test
    fun stsd() {
        val moov = extracter.boxes["moov"] as BoxOfBoxes
        val trak = moov.boxes["trak"] as BoxOfBoxes
        val mdia = trak.boxes["mdia"] as BoxOfBoxes
        val minf = mdia.boxes["minf"] as BoxOfBoxes
        val stbl = minf.boxes["stbl"] as BoxOfBoxes
        val box = stbl.boxes["stsd"] as StsdBox
        assertThat(box.entryCount, `is`(1))
        assertThat(box.sampleEntries.size, `is`(box.entryCount))
        assertThat(box.sampleEntries["avc3"]!!.name, `is`("avc3"))
        assertThat(box.sampleEntries["avc3"]!!.size, `is`(8+113))

    }




    /**
     *
    [avc3] size=8+113
    data_reference_index = 1
    width = 192
    height = 108
    compressor = Elemental H.264
    [avcC] size=8+7
        Configuration Version = 1
        Profile = Baseline
        Profile Compatibility = c0
        Level = 21
        NALU Length Size = 4
    [btrt] size=8+12

     */
    @Test
    fun avc3() {
        val moov = extracter.boxes["moov"] as BoxOfBoxes
        val trak = moov.boxes["trak"] as BoxOfBoxes
        val mdia = trak.boxes["mdia"] as BoxOfBoxes
        val minf = mdia.boxes["minf"] as BoxOfBoxes
        val stbl = minf.boxes["stbl"] as BoxOfBoxes
        val stsd = stbl.boxes["stsd"] as StsdBox
        val box = stsd.sampleEntries["avc3"] as Avc3Box
        assertThat(box.name, `is`("avc3"))
        assertThat(box.dataReferenceIndex, `is`(1))
        assertThat(box.width, `is`(192))
        assertThat(box.height, `is`(108))
        assertThat(box.depth,`is`(24))
        assertThat(box.compressorName,equalTo("Elemental H.264"))
        assertThat(box.boxes.size, `is`(2))
        assertThat(box.boxes["avcC"]!!.name, `is`("avcC"))
        assertThat(box.boxes["btrt"]!!.name, `is`("btrt"))

        assertThat(box.boxes["avcC"]!!.payload.size, `is`(7))

    }

    @Test
    fun avcCBox()
    {
        val moov = extracter.boxes["moov"] as BoxOfBoxes
        val trak = moov.boxes["trak"] as BoxOfBoxes
        val mdia = trak.boxes["mdia"] as BoxOfBoxes
        val minf = mdia.boxes["minf"] as BoxOfBoxes
        val stbl = minf.boxes["stbl"] as BoxOfBoxes
        val stsd = stbl.boxes["stsd"] as StsdBox
        val box = stsd.sampleEntries["avc3"] as Avc3Box
        assertThat(box.avcCBox().payload.size, `is`(7))
        assertThat(box.avcCBox().payload[0], `is`(1.toByte()))
        assertThat(extracter.avcCBox(), `is`(box.avcCBox()))
    }




}

