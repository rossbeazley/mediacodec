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


    lateinit var extracter: VideoM4SExtractor

    @Before
    fun givenASegmentIsLoadedIntoMemory() {
        val assetFileDescriptor = InstrumentationRegistry.getInstrumentation().targetContext.getAssets().openFd("seg1.m4s")
        val bytes: ByteArray = assetFileDescriptor.createInputStream().use { it.readBytes() }
        extracter = VideoM4SExtractor(bytes)

    }
    @Test
    @Ignore
    fun bitmapDecoded() {

        val surfaceTexture = SurfaceTexture(1)
        val surface = Surface(surfaceTexture)
        val decoder = MediaCodec.createDecoderByType("video/avc")
        val format = MediaFormat.createVideoFormat("video/avc", 192, 108)
        //need to work out the format proper like
        decoder.configure(format, null, null, 0)


    }

    @Test
    @Ignore
    fun everythingINeed() {
        // i need to know the codec profile?
        // i need to know the number of samples (frames)
        // i need to know the index of those frames in the mdat (and where the mdat is)

    }

}