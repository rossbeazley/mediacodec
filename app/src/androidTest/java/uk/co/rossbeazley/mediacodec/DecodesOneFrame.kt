package uk.co.rossbeazley.mediacodec

import android.content.res.AssetFileDescriptor
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class DecodesOneFrame {


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


    @Test
    @Ignore
    fun numberOfSamplesKnown() {
        assertThat(extracter.frameCount, `is`(96))
    }


    /////////

    lateinit var extracter: VideoM4SExtractor

    @Before
    fun givenASegmentIsLoadedIntoMemory() {
        val assetFileDescriptor = InstrumentationRegistry.getInstrumentation().targetContext.getAssets().openFd("seg1.m4s")
        extracter = VideoM4SExtractor(assetFileDescriptor)

    }

    /////////

    @Test @Ignore
    fun bitmapDecoded() {

        val surfaceTexture = SurfaceTexture(1)
        val surface = Surface(surfaceTexture)
        val decoder = MediaCodec.createDecoderByType("video/avc")
        val format = MediaFormat.createVideoFormat("video/avc", 192, 108)
        //need to work out the format proper like
        decoder.configure(format, null, null, 0)


    }

    @Test @Ignore
    fun everythingINeed() {
        // i need to know the codec profile?
        // i need to know the number of samples (frames)
        // i need to know the index of those frames in the mdat (and where the mdat is)

    }

    /////////

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
    @Ignore
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



    open class Box(val name: String, val payload: ByteArray) {
        var boxes: MutableMap<String,Box> = mutableMapOf()
    }

    class TfdtBox(val mediaDecodeTime: Long, boxBytes: ByteArray) : Box("tfdt", boxBytes)
    {
        /*     class FullBox(unsigned int(32) boxtype, unsigned int(8) v, bit(24) f)     extends Box(boxtype)
               {
                   unsigned int(8)   version = v;
                   bit(24)  flags   =   f;
               }
               aligned(8) class TrackFragmentBaseMediaDecodeTimeBox    extends FullBox(‘tfdt’, version, 0)
               {
                  if (version==1)
                  {
                         unsigned   int(64)   baseMediaDecodeTime;
                  }
                  else
                  { // version==0
                         unsigned   int(32)   baseMediaDecodeTime;
                  }
               }
     */
        companion object {
            fun from(bytes: ByteArray) : TfdtBox {
                //currently ignoring full box header hence 8
                //ignore box name 4 bytes
                val versionInt = bytes[4]

                val boxBytes = bytes.sliceArray(8 until bytes.size)

                val byteOffset = 0

                val byteM4 = (0xffL and (boxBytes[byteOffset + 0].toLong())).shl(56)
                val byteM3 = (0xffL and (boxBytes[byteOffset + 0].toLong())).shl(48)
                val byteM2 = (0xffL and (boxBytes[byteOffset + 0].toLong())).shl(40)
                val byteM1 = (0xffL and (boxBytes[byteOffset + 0].toLong())).shl(32)
                val byte0 = (0xffL and (boxBytes[byteOffset + 0].toLong())).shl(24)
                val byte1 = (0xffL and (boxBytes[byteOffset + 1].toLong())).shl(16)
                val byte2 = (0xffL and (boxBytes[byteOffset + 2].toLong())).shl(8)
                val byte3 = 0xffL and boxBytes[byteOffset + 3].toLong()
                val mediaDecodeTime = byteM4.or(byteM3).or(byteM2).or(byteM1).or(byte0).or(byte1).or(byte2).or(byte3)

                val tfhdBox = TfdtBox(mediaDecodeTime, boxBytes)

                return tfhdBox
            }
        }
    }

    class TfhdBox(val trackID: Int, boxBytes: ByteArray) : Box("tfhd", boxBytes)
    {
 /*     class FullBox(unsigned int(32) boxtype, unsigned int(8) v, bit(24) f)     extends Box(boxtype)
        {
            unsigned int(8)   version = v;
            bit(24)  flags   =   f;
        }
        aligned(8) class TrackFragmentHeaderBox  extends   FullBox(‘tfhd’,   0,   tf_flags)
        {
             unsigned   int(32)   track_ID;

             // all the following are optional fields
             unsigned   int(64)   base_data_offset;
             unsigned   int(32)   sample_description_index;
             unsigned   int(32)   default_sample_duration;
             unsigned   int(32)   default_sample_size;
             unsigned   int(32)   default_sample_flags
         } */
        companion object {
            fun from(bytes: ByteArray) : TfhdBox {
                //currently ignoring full box header hence 8
                val boxBytes = bytes.sliceArray(8 until bytes.size)

                val byteOffset = 0
                val byte0 = (0xffL and (boxBytes[byteOffset + 0].toLong())).shl(24)
                val byte1 = (0xffL and (boxBytes[byteOffset + 1].toLong())).shl(16)
                val byte2 = (0xffL and (boxBytes[byteOffset + 2].toLong())).shl(8)
                val byte3 = 0xffL and boxBytes[byteOffset + 3].toLong()
                val trackID =  byte0.or(byte1).or(byte2).or(byte3)

                val tfhdBox = TfhdBox(trackID.toInt(), boxBytes)

                return tfhdBox
            }
        }
    }

    class TrafBox(boxBytes: ByteArray) : Box("traf", boxBytes)
    {
        companion object {
            fun from(bytes: ByteArray) : TrafBox {
                val trafBox = TrafBox(bytes.sliceArray(4 until bytes.size))

                var bytesToParse = trafBox.payload
                val tfhdSize = boxSize(bytesToParse, 0)
                val sizeHeader = 4
                val tfhdBytes = bytesToParse.sliceArray(sizeHeader until tfhdSize.toInt())
                val tfhdBox = parseBox(tfhdBytes)

                trafBox.boxes.addBox(tfhdBox)
                var remainingBytes = bytesToParse.sliceArray((tfhdSize).toInt() until bytesToParse.size)


                bytesToParse = remainingBytes
                val tfdtSize = boxSize(bytesToParse, 0)
                val tfdtBytes = bytesToParse.sliceArray(sizeHeader until tfdtSize.toInt())
                trafBox.boxes.addBox( parseBox(tfdtBytes) )
                remainingBytes = bytesToParse.sliceArray((tfdtSize).toInt() until bytesToParse.size)


                bytesToParse = remainingBytes
                val trunSize = boxSize(bytesToParse, 0)
                val trunBytes = bytesToParse.sliceArray(sizeHeader until trunSize.toInt())
                trafBox.boxes.addBox( parseBox(trunBytes) )
                remainingBytes = bytesToParse.sliceArray((trunSize).toInt() until bytesToParse.size)

                return trafBox
            }
        }
    }

    class MoofBox(boxByes: ByteArray) : Box("moof", boxByes) {
        companion object {
            fun from(bytes: ByteArray): MoofBox {

                val moofBox = MoofBox(bytes.sliceArray(4 until bytes.size))

                var bytesToParse = moofBox.payload
                val mfhdSize = boxSize(bytesToParse, 0)
                val sizeHeader = 4
                val mfhdBytes = bytesToParse.sliceArray(sizeHeader until mfhdSize.toInt()) //slice off mfhd worth of bytes
                val mfhdBox = parseBox(mfhdBytes)

                moofBox.boxes.addBox(mfhdBox)

                val remainingBytes = bytesToParse.sliceArray((mfhdSize).toInt() until bytesToParse.size)

                bytesToParse = remainingBytes
                val trafSize = boxSize(bytesToParse, 0)
                val trafBytes = bytesToParse.sliceArray(sizeHeader until trafSize.toInt())
                val trafBox = parseBox(trafBytes)

                moofBox.boxes.addBox(trafBox)

                return moofBox
            }
        }
    }

    class MfhdBox(val sequenceNumber: Int, boxByes: ByteArray) : Box("mfhd", boxByes) {


        companion object {
            fun from(bytes: ByteArray): MfhdBox {
                // (size 4 bytees  already sliced)
                // name 4 bytes
                // version 1 bytes;
                // flags 3 bytes
                // PAYLOAD
                // sequence_number 4 bytes
                val byteOffset = 8
                val byte0 = (0xffL and (bytes[byteOffset + 0].toLong())).shl(24)
                val byte1 = (0xffL and (bytes[byteOffset + 1].toLong())).shl(16)
                val byte2 = (0xffL and (bytes[byteOffset + 2].toLong())).shl(8)
                val byte3 = 0xffL and bytes[byteOffset + 3].toLong()
                val sequenceNumber =  byte0.or(byte1).or(byte2).or(byte3)
                return MfhdBox(sequenceNumber.toInt(), bytes.sliceArray(byteOffset until bytes.size))
            }
        }
    }

    class MdatBox(boxByes: ByteArray) : Box("mdat", boxByes) {
        companion object {
            fun from(bytes: ByteArray): MdatBox {
                //4 bytes for the box name
                return MdatBox(bytes.sliceArray(4 until bytes.size))
            }
        }
    }



    class VideoM4SExtractor(val assetFileDescriptor: AssetFileDescriptor) {

        val boxes: MutableMap<String,Box> = mutableMapOf()

        var frameCount: Int = 0

        init {

            val bytes: ByteArray = assetFileDescriptor.createInputStream().use { it.readBytes() }
            //moof, it has boxes in it
            // unsigned int(32) size;
            // unsigned int(32) type = boxtype;

            var byteOffset = 0
            val moofSize = boxSize(bytes, byteOffset)
            //slice off Moof worth of bytes
            val sizeHeader = 4
            val moofBytes = bytes.sliceArray(byteOffset + sizeHeader until moofSize.toInt())
            val moofBox = parseBox(moofBytes)
            boxes.addBox(moofBox)


            val remainingBytes = bytes.sliceArray((moofSize).toInt() until bytes.size)
            val mdatSize = boxSize(remainingBytes, 0)
            //slice off Moof worth of bytes
            val mdatBytes = remainingBytes.sliceArray(0 + sizeHeader until mdatSize.toInt())
            val mdatBox = parseBox(mdatBytes)
            boxes.addBox(mdatBox)
        }

    }


}

fun MutableMap<String, DecodesOneFrame.Box>.addBox(box : DecodesOneFrame.Box) {
    this += box.name to box
}

private fun parseBox(bytes: ByteArray): DecodesOneFrame.Box {
    val boxName = boxName(bytes, 0)
    return when (boxName) {
        "moof" -> DecodesOneFrame.MoofBox.from(bytes)
        "mdat" -> DecodesOneFrame.MdatBox.from(bytes)
        "mfhd" -> DecodesOneFrame.MfhdBox.from(bytes)
        "traf" -> DecodesOneFrame.TrafBox.from(bytes)
        "tfhd" -> DecodesOneFrame.TfhdBox.from(bytes)
        "tfdt" -> DecodesOneFrame.TfdtBox.from(bytes)
        else -> DecodesOneFrame.Box(boxName, bytes.sliceArray(4 until bytes.size))
    }
}

private fun boxName(bytes: ByteArray, byteOffset: Int): String {
    val charArray = CharArray(4)
    charArray[0] = bytes[byteOffset + 0].toChar()
    charArray[1] = bytes[byteOffset + 1].toChar()
    charArray[2] = bytes[byteOffset + 2].toChar()
    charArray[3] = bytes[byteOffset + 3].toChar()

    val name = String(charArray)
    println("boxName ${name}")
    return name
}

private fun boxSize(bytes: ByteArray, byteOffset: Int): Long {
    val byte0 = (0xffL and (bytes[byteOffset + 0].toLong())).shl(24)
    val byte1 = (0xffL and (bytes[byteOffset + 1].toLong())).shl(16)
    val byte2 = (0xffL and (bytes[byteOffset + 2].toLong())).shl(8)
    val byte3 = 0xffL and bytes[byteOffset + 3].toLong()
    val result = byte0.or(byte1).or(byte2).or(byte3)
    println("boxSize ${result}")
    return result
}