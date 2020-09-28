package uk.co.rossbeazley.mediacodec

import android.content.res.AssetFileDescriptor
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
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

     */



    @Test
    fun moofSize()
    {
        val assetFileDescriptor = InstrumentationRegistry.getInstrumentation().targetContext.getAssets().openFd("seg1.m4s")
        val extracter : VideoM4SExtractor = VideoM4SExtractor(assetFileDescriptor)
        assertThat(extracter.moofSize, `is`(488L))
    }

    @Test
    fun moofName()
    {
        val assetFileDescriptor = InstrumentationRegistry.getInstrumentation().targetContext.getAssets().openFd("seg1.m4s")
        val extracter : VideoM4SExtractor = VideoM4SExtractor(assetFileDescriptor)
        assertThat(extracter.moofName, `is`("moof"))
    }

    @Test
    fun numberOfSamplesKnown()
    {
        val assetFileDescriptor = InstrumentationRegistry.getInstrumentation().targetContext.getAssets().openFd("seg1.m4s")
        val extracter : VideoM4SExtractor = VideoM4SExtractor(assetFileDescriptor)
        assertThat(extracter.frameCount, `is`(96))
    }



    @Test
    fun mfhdSize()
    {
        val assetFileDescriptor = InstrumentationRegistry.getInstrumentation().targetContext.getAssets().openFd("seg1.m4s")
        val extracter : VideoM4SExtractor = VideoM4SExtractor(assetFileDescriptor)
        assertThat(extracter.mfhdSize, `is`(16L))
    }

    @Test
    fun mfhdName()
    {
        val assetFileDescriptor = InstrumentationRegistry.getInstrumentation().targetContext.getAssets().openFd("seg1.m4s")
        val extracter : VideoM4SExtractor = VideoM4SExtractor(assetFileDescriptor)
        assertThat(extracter.mfhdName, `is`("mfhd"))
    }



    class VideoM4SExtractor(val assetFileDescriptor: AssetFileDescriptor) {

        val mfhdName: String
        val moofSize : Long
        var frameCount: Int = 0
        val moofName : String

        val mfhdSize : Long
        init {

            val bytes : ByteArray = assetFileDescriptor.createInputStream().use { it.readBytes() }
            //moof, it has boxes in it
            // unsigned int(32) size;
            // unsigned int(32) type = boxtype;

            var byteOffset = 0
            moofSize =  boxSize(bytes, byteOffset)

            byteOffset = 4
            moofName = boxName(bytes, byteOffset)
            //mfhd next, it has no boxes in it
            // size 4 bytees
            // name 4 bytes
            // version 1 bytes;
            // flags 3 bytes
            // sequence_number 4 bytes

            mfhdSize = boxSize(bytes, 8)
            mfhdName = boxName(bytes, 12)
        }

        private fun boxName(bytes: ByteArray, byteOffset: Int): String {
            val charArray = CharArray(4)
            charArray[0] = bytes[byteOffset + 0].toChar()
            charArray[1] = bytes[byteOffset + 1].toChar()
            charArray[2] = bytes[byteOffset + 2].toChar()
            charArray[3] = bytes[byteOffset + 3].toChar()

            val name = String(charArray)
            return name
        }

        private fun boxSize(bytes: ByteArray, byteOffset: Int): Long {
            val byte0 = (0xffL and (bytes[byteOffset + 0].toLong())).shl(24)
            val byte1 = (0xffL and (bytes[byteOffset + 1].toLong())).shl(16)
            val byte2 = (0xffL and (bytes[byteOffset + 2].toLong())).shl(8)
            val byte3 = 0xffL and bytes[byteOffset + 3].toLong()
            return byte0.or(byte1).or(byte2).or(byte3)
        }
    }

    @Test
    fun bitmapDecoded() {

        val surfaceTexture = SurfaceTexture(1)
        val surface = Surface(surfaceTexture)
            val decoder = MediaCodec.createDecoderByType("video/avc")
            val format = MediaFormat.createVideoFormat("video/avc",192, 108)
            //need to work out the format proper like
            decoder.configure(format, null, null, 0)


    }

    @Test
    fun everythingINeed()
    {
        // i need to know the codec profile?
        // i need to know the number of samples (frames)
        // i need to know the index of those frames in the mdat (and where the mdat is)

    }
}