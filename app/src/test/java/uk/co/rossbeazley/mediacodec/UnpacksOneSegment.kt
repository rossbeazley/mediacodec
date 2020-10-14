package uk.co.rossbeazley.mediacodec

import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class UnpacksOneSegment {


    /**
     *
    [moof] size=8+480
      [mfhd] size=12+4
        sequence number = 1
      [traf] size=8+456
        [tfhd] size=12+16, flags=2002a
          track ID = 1
          sample description index = 1
          default sample duration = 1000
          default sample flags = 10100c0
        [tfdt] size=12+8, version=1
          base media decode time = 0
        [trun] size=12+396, flags=205
          sample count = 96
          data offset = 496
          first sample flags = 2400040
    [mdat] size=8+2178



    https://w3c.github.io/media-source/byte-stream-format-registry.html
    https://dvb.org/wp-content/uploads/2019/12/a168_dvb_mpeg-dash_oct_2019.pdf
    https://uvcentral.com/files/CFFMediaFormat-2_1.pdf
    https://b.goeswhere.com/ISO_IEC_14496-12_2015.pdf


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

    aligned(8)
    class TrackRunBox extends FullBox(‘trun’, version, tr_flags)
    {
    unsigned   int(32)   sample_count;

    // the following are optional fields
    signed int(32) data_offset;
    unsigned   int(32)   first_sample_flags;

    // all fields in the following array are optional
    {
    unsigned   int(32)   sample_duration;
    unsigned   int(32)   sample_size;
    unsigned   int(32)   sample_flags

    if (version == 0)
    {
    unsigned   int(32)   sample_composition_time_offset;
    }
    else
    {
    signed   int(32)      sample_composition_time_offset;
    }

    }[ sample_count ]
    }


    section 4 of https://b.goeswhere.com/ISO_IEC_14496-12_2015.pdf

    aligned(8) class Box (unsigned int(32) boxtype,           optional unsigned int(8)[16] extended_type) {    unsigned int(32) size;    unsigned int(32) type = boxtype;    if (size==1) {       unsigned   int(64)   largesize;      } else if (size==0) {       // box extends to end of file    }      if (boxtype==‘uuid’) {       unsigned int(8)[16] usertype = extended_type;    }   }
    aligned(8) class FullBox(unsigned int(32) boxtype, unsigned int(8) v, bit(24) f)     extends Box(boxtype) {    unsigned int(8)   version = v;    bit(24)               flags   =   f;   }

    aligned(8) class MovieFragmentBox extends Box(‘moof’){ }
    aligned(8) class MovieFragmentHeaderBox extends FullBox(‘mfhd’, 0, 0){ unsigned   int(32)   sequence_number;   }
    aligned(8) class TrackFragmentBox extends Box(‘traf’){ }
    aligned(8) class TrackFragmentHeaderBox           extends   FullBox(‘tfhd’,   0,   tf_flags){      unsigned   int(32)   track_ID;      // all the following are optional fields     unsigned   int(64)   base_data_offset;      unsigned   int(32)   sample_description_index;      unsigned   int(32)   default_sample_duration;      unsigned   int(32)   default_sample_size;      unsigned   int(32)   default_sample_flags   }
    aligned(8) class TrackFragmentBaseMediaDecodeTimeBox    extends FullBox(‘tfdt’, version, 0) {    if (version==1) {       unsigned   int(64)   baseMediaDecodeTime;      } else { // version==0       unsigned   int(32)   baseMediaDecodeTime;      }   }
    aligned(8) class TrackRunBox           extends FullBox(‘trun’, version, tr_flags) {    unsigned   int(32)   sample_count;      // the following are optional fields     signed int(32) data_offset;    unsigned   int(32)   first_sample_flags;      // all fields in the following array are optional    {         unsigned   int(32)   sample_duration;         unsigned   int(32)   sample_size;         unsigned   int(32)   sample_flags         if (version == 0)           {   unsigned   int(32)   sample_composition_time_offset;   }         else            {   signed   int(32)      sample_composition_time_offset;   }      }[ sample_count ] }


    aligned(8) class MediaDataBox extends Box(‘mdat’) {    bit(8)   data[];   }
     */

    lateinit var extracter: VideoM4SExtractor

    @Before
    fun givenASegmentIsLoadedIntoMemory() {
        val classLoader = this::class.java.classLoader!!
        val resourceAsStream = classLoader.getResourceAsStream("seg1.m4s")
        val bytes: ByteArray = resourceAsStream.use { it.readBytes() }
        extracter = VideoM4SExtractor(bytes)
    }

    @Test
    fun segmentCanParseTheBoxes() {

        assertThat(extracter.boxes.size, `is`(2))
    }

    @Test
    fun theFirstBoxIsMoof() {
        assertThat(extracter.boxes["moof"]?.name, `is`("moof"))
    }

    @Test
    fun theFirstBoxHas480BytesOfPayload() {
        assertThat(extracter.boxes["moof"]!!.payload.size, `is`(480))
    }

    @Test
    fun theSecondBoxIsMDATAndHas2178BytesOfPayload() {
        assertThat(extracter.boxes["mdat"]?.name, `is`("mdat"))
        assertThat(extracter.boxes["mdat"]?.payload?.size, `is`(2178))
    }

    /////

    @Test
    fun theMoofHasTwoBoxes() {
        assertThat(extracter.boxes["moof"]!!.boxes.size, `is`(2))
    }

    @Test
    fun moofFirstBoxIsMFHD() {
        val box = extracter.boxes["moof"]!!.boxes["mfhd"]!!
        assertThat(box.name, `is`("mfhd"))
        assertThat(box.payload.size, `is`(4))

        when (box) {
            is MfhdBox -> {
                assertThat(box.sequenceNumber, `is`(1))
            }
            else -> {
                fail("Expected a MFHD Box")
            }
        }
    }


    @Test
    fun moofHasAtraf() {
        val box = extracter.boxes["moof"]!!.boxes["traf"]!!
        assertThat(box.name, `is`("traf"))
        assertThat(box.payload.size, `is`(456))
        assertThat(box.boxes.size, `is`(3))
    }

    @Test
    fun trafHasATFHD()
    {
        val box = extracter.boxes["moof"]!!.boxes["traf"]!!.boxes["tfhd"]!!
        assertThat(box.name, `is`("tfhd"))
        assertThat(box.payload.size, `is`(16))
        when (box) {
            is TfhdBox -> {
                assertThat(box.trackID, `is`(1))
            }
            else -> {
                fail("Expected a MFHD Box")
            }
        }

        /**
         * [tfhd] size=12+16, flags=2002a
        track ID = 1   ASSERTED, rest ignored
        sample description index = 1
        default sample duration = 1000
        default sample flags = 10100c0
         */
    }

    @Test
    fun trafHasATFDT()
    {
        val box = extracter.boxes["moof"]!!.boxes["traf"]!!.boxes["tfdt"]!!
        assertThat(box.name, `is`("tfdt"))
        assertThat(box.payload.size, `is`(8))
        when (box) {
            is TfdtBox -> {
                assertThat(box.mediaDecodeTime, `is`(0L))
            }
            else -> {
                fail("Expected a MFHD Box")
            }
        }

        /**
         * [tfdt] size=12+8, version=1
              base media decode time = 0
         */
    }


    @Test
    fun trafHasATrun()
    {
        /**
         * [trun] size=12+396, flags=205
             sample count = 96
             data offset = 496
             first sample flags = 2400040
         */
        val box : TrunBox = extracter.boxes["moof"]!!.boxes["traf"]!!.boxes["trun"]!! as TrunBox
        assertThat(box.name, `is`("trun"))
        assertThat(box.payload.size, `is`(396))
        assertThat(box.sampleCount, `is`(96))
        assertThat(box.dataOffset, `is`(496))
    }
}
