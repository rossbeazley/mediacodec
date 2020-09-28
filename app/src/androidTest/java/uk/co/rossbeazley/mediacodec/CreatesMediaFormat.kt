package uk.co.rossbeazley.mediacodec

import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.test.espresso.internal.inject.TargetContext
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.startsWith
import org.junit.Test
import java.io.IOException

class CreatesMediaFormat {

    @Test
    fun canYouCreateMediaFormatFromTheSegments()
    {
        val assetFileDescriptor = context().getAssets().openFd("merged.mp4")
        val extractor = MediaExtractor()
        extractor.setDataSource(assetFileDescriptor)

        assertThat(extractor.trackCount,`is`(1))

        val format: MediaFormat = extractor.getTrackFormat(1)
        val mime = format.getString(MediaFormat.KEY_MIME)


        assertThat(mime, startsWith("video"))
    }

    private fun context() = InstrumentationRegistry.getInstrumentation().targetContext
}

/**

 think a box is three empty bytes then the size, maybe the first four bytes are the size?


 find the moov box, says how many tracks there are
    get the list of trak boxes
      the first trak box (will be able to read the number of bytes)
          tkhd (track header box), we can determine the resolution
          then find the mdia box (will also say how many bytes to read)
            it has a mdhd (its got a size amount) SKIP
            hdlr box (has a size amount) SKIP
            minf box (media info box)
            vmhd SKIP
            dinf  SKIP
            stb (sample table box)
                 stsd (sample description box)
                 then we have an unknown box avc3
                         avcC (AVC Configuration box) is where the magic is


                        AVC Level 21
                        AVC profile 66
        */