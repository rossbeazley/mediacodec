package uk.co.rossbeazley.mediacodec

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level21
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileMain
import android.media.MediaFormat.*
import android.view.Surface
import android.view.TextureView
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import junit.framework.Assert
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class DecodesOneFrame {


    private lateinit var view: TextureView
    private lateinit var textureViewAvailableLatch: CountDownLatch
    lateinit private var surfaceTexture: SurfaceTexture

    @get:Rule
    var activityActivityTestRule = ActivityTestRule(Activity::class.java)
    lateinit var extracter: VideoM4SExtractor

    @Before
    fun givenASegmentIsLoadedIntoMemory() {

        slapUpATextureViewToRenderIn()

        val assetFileDescriptor = InstrumentationRegistry.getInstrumentation().targetContext.getAssets().openFd("seg1.m4s")
        val bytes: ByteArray = assetFileDescriptor.createInputStream().use { it.readBytes() }
        extracter = VideoM4SExtractor(bytes)
    }

    @Test
    fun bitmapDecoded() {

        val decoder = cobbleTogetherACodec()


        val inIndex = decoder.dequeueInputBuffer(10000)
        println("inIndex ${inIndex}")
        val buffer = decoder.getInputBuffer(inIndex)!!

        val sample = extracter.sample(0)
        buffer.put(sample)
        val sampleSize: Int = sample.size
        println("sample size ${sampleSize}")
        //BUFFER_FLAG_KEY_FRAME | BUFFER_FLAG_CODEC_CONFIG
        decoder.queueInputBuffer(inIndex, 0, sampleSize, 0, BUFFER_FLAG_KEY_FRAME)

        val info = MediaCodec.BufferInfo()
        var stillWaiting = 10
        while(stillWaiting > 0) {
            val outIndex = decoder.dequeueOutputBuffer(info, 10000)
            when (outIndex) {
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> println("INFO_OUTPUT_BUFFERS_CHANGED")
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> println("New format " + decoder.outputFormat)
                MediaCodec.INFO_TRY_AGAIN_LATER -> println("dequeueOutputBuffer timed out!")
                else -> {
                    print("info.presentationTimeUs ${info.presentationTimeUs}")
                    stillWaiting = -1
                    println("outIndex ${outIndex}")
                    decoder.releaseOutputBuffer(outIndex, true)
                }
            }
            stillWaiting--
            println("still waiting ${stillWaiting} flags ${info.flags} ${info}")
        }


        decoder.stop()
        decoder.release()

        val bitmap: Bitmap = view.getBitmap()
        val RED = -55807
        assertThat(bitmap.getPixel(100, 100), `is`(RED))

    }

    private fun slapUpATextureViewToRenderIn() {
        val context = InstrumentationRegistry.getInstrumentation().context
        textureViewAvailableLatch = CountDownLatch(1)
        view = TextureView(context)
        view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, p1: Int, p2: Int) {
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
                return true
            }

            override fun onSurfaceTextureAvailable(st: SurfaceTexture, i: Int, i1: Int) {
                surfaceTexture = st
                textureViewAvailableLatch.countDown()

            }
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync { activityActivityTestRule.getActivity().setContentView(view) }



        try {
            textureViewAvailableLatch.await(2, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
            Assert.fail()
        }
    }

    private fun cobbleTogetherACodec(): MediaCodec {
        val surface = Surface(surfaceTexture)
        val decoder = MediaCodec.createDecoderByType(MIMETYPE_VIDEO_AVC)
        val format = createVideoFormat(MIMETYPE_VIDEO_AVC, 192, 108)
        format.setLong(KEY_DURATION, 96000)
        format.setInteger(KEY_CAPTURE_RATE, 25)
        format.setInteger(KEY_PROFILE, AVCProfileMain)
        format.setInteger(KEY_LEVEL, AV1Level21)
        //format.setString(KEY_CODECS_STRING,"avc3.42C015") //api 30
        //need to work out the format proper like
        decoder.configure(format, surface, null, 0)

        decoder.start()

        return decoder
    }

}