package uk.co.rossbeazley.mediacodec

import android.app.Activity
import android.media.MediaCodec
import android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG
import android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaCodecList.REGULAR_CODECS
import android.media.MediaFormat
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView

class DIYDecodingActivity : Activity(), SurfaceHolder.Callback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sv = SurfaceView(this)
        sv.holder.addCallback(this)
        setContentView(sv)
        givenASegmentIsLoadedIntoMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val surface = holder.surface
        val decoder = cobbleTogetherACodec(surface)
        bitmapDecoded(decoder)
    }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    companion object {
        private fun log(message: String) {
            Log.d("DecodeActivity", message)
        }
    }

    lateinit var extracter: VideoM4SExtractor

    fun givenASegmentIsLoadedIntoMemory() {
        val assetFileDescriptor = getAssets().openFd("seg1.m4s")
        val bytes: ByteArray = assetFileDescriptor.createInputStream().use { it.readBytes() }
        extracter = VideoM4SExtractor(bytes)
    }

    fun bitmapDecoded(decoder : MediaCodec) {


        for(frame in 0 until 1) {
            feedCodec(decoder, frame)
        }


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
            println("still waiting ${stillWaiting} flags ${info.flags}")
        }


        decoder.stop()
        decoder.release()

    }

    private fun feedCodec(decoder: MediaCodec, i: Int) {
        println("Feeding ${i}")
        val inIndex = decoder.dequeueInputBuffer(10000)
        println("inIndex ${inIndex}")
        val buffer = decoder.getInputBuffer(inIndex)!!

        val sample = extracter.sample(i)
        buffer.put(sample)
        val sampleSize: Int = sample.size
        println("sample size ${sampleSize}")
        //BUFFER_FLAG_KEY_FRAME | BUFFER_FLAG_CODEC_CONFIG
        val flags = when(i) {
            0 -> 0.or(BUFFER_FLAG_CODEC_CONFIG).or(BUFFER_FLAG_KEY_FRAME)
            else -> 0
        }
        decoder.queueInputBuffer(inIndex, i, sampleSize, i.toLong(), flags)
        println("samqueueInputBuffer ${flags}")
    }


    private fun cobbleTogetherACodec(surface : Surface): MediaCodec {

        println("cobbleTogetherACodec")

        //val decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        println("CREATED")
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 192, 108)
        format.setLong(MediaFormat.KEY_DURATION, 96000)
        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, 25)
        format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
        format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AV1Level21)

        val decoder = MediaCodec.createByCodecName( MediaCodecList(REGULAR_CODECS).findDecoderForFormat(format) )

        //format.setString(KEY_CODECS_STRING,"avc3.42C015") //api 30
        //need to work out the format proper like
        decoder.configure(format, surface, null, 0)

        println("CONFIGURED")
        decoder.start()
        println("STARTED")

        SystemClock.sleep(1000)
        return decoder
    }
}