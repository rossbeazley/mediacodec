package uk.co.rossbeazley.mediacodec

import android.app.Activity
import android.content.res.AssetFileDescriptor
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.nio.ByteBuffer

class MultiThreadedExtractorDecodingActivity : Activity(), SurfaceHolder.Callback {

    private lateinit var initExtractor: InitM4SExtractor
    var NOT_DONE = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sv = SurfaceView(this)
        sv.holder.addCallback(this)
        setContentView(sv)
        givenASegmentIsLoadedIntoMemory()
    }

    lateinit var bytes: ByteArray

    private fun readInitialisationData(assetFileDescriptor: AssetFileDescriptor): MediaFormat {
        bytes = assetFileDescriptor.createInputStream().use { it.readBytes() }


        val mediaFormat = MediaFormat.createVideoFormat("video/avc", 192,108)
        val CSD0 = ByteBuffer.wrap( bytes.sliceArray(0 until 27))
        val csd1 = ByteBuffer.wrap(bytes.sliceArray(27 until 31))
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatYUV420Flexible)
        //mediaFormat.setByteBuffer("csd-0", CSD0)
        //mediaFormat.setByteBuffer("csd-1", csd1)
        return mediaFormat
    }

    private fun feedCodecWithMedia(assetFileDescriptor: AssetFileDescriptor, decoder: MediaCodec) {

       Thread {

           val frameCount = extracter.frameCount
           for(frame in 0 until frameCount) {
               feedCodec(decoder, frame)
           }


           val inIndex = decoder.dequeueInputBuffer(10000)
           if (inIndex >= 0) {
               logInput("Got an input buffer to fill with EOS")
               val buffer = decoder.getInputBuffer(inIndex)
               //its the end of media
               logInput("Got EOF")
               decoder.queueInputBuffer(inIndex, 0, 0, System.nanoTime(), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
               //NOT_DONE = false
           } else {
               logInput("No buffers for EOS")
           }

       }.start()

    }

    private fun renderToScreen(decoder: MediaCodec, surface: Surface) {

        Thread() {
            //decoder.setOutputSurface(surface)
            val startTime = System.nanoTime()

            val info = MediaCodec.BufferInfo()

            while (NOT_DONE) {
                //logOutput("dequeueOutputBuffer ${decoder.outputFormat}")
                val outIndex = decoder.dequeueOutputBuffer(info, 10000)
                when (outIndex) {
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> logOutput("INFO_OUTPUT_BUFFERS_CHANGED")
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> logOutput("New format " + decoder.outputFormat)
                    MediaCodec.INFO_TRY_AGAIN_LATER -> logOutput("dequeueOutputBuffer timed out!")
                    else -> {
                        logOutput("Got a buffer at ${info.presentationTimeUs} and current presentation time ${System.nanoTime() - startTime}")
//                        while (info.presentationTimeUs * 1000 > System.nanoTime() - startTime) {
//                            try {
//                                Thread.sleep(10)
//                            } catch (e: InterruptedException) {
//                                e.printStackTrace()
//                                break
//                            }
//                        }
                        logOutput("OutputBuffer info flags " + Integer.toBinaryString(info.flags))
                        decoder.releaseOutputBuffer(outIndex, true)
                        logOutput("RELEASED A BUFFER!")
                    }
                }


                // All decoded frames have been rendered, we can stop playing now


                // All decoded frames have been rendered, we can stop playing now
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    logOutput("OutputBuffer BUFFER_FLAG_END_OF_STREAM")
                    decoder.stop()
                    decoder.release()
                } else {
                    logOutput("carry on")
                }
            }
        }.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceCreated(holder: SurfaceHolder) {

        logMain("surface created")

        val surface = holder.surface
        val decoder = cobbleTogetherACodec(surface)
        //        bitmapDecoded(decoder)
        //thread 1
        feedCodecWithMedia(getAssets().openFd("seg1.m4s"), decoder)
        //thread 2
        renderToScreen(decoder, holder.surface)
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) {}


    private fun logMain(message: String) {
        Log.d("DecodeActivityInput", message)
    }

    private fun logInput(message: String) {
        Log.d("DecodeActivityInput", message)
    }

    private fun logOutput(message: String) {
        Log.d("DecodeActivityOutput", message)
    }


    lateinit var extracter: VideoM4SExtractor

    fun givenASegmentIsLoadedIntoMemory() {
        val assetFileDescriptor = getAssets().openFd("seg1.m4s")
        val bytes: ByteArray = assetFileDescriptor.createInputStream().use { it.readBytes() }
        extracter = VideoM4SExtractor(bytes)

        initExtractor = InitM4SExtractor(getAssets().openFd("init.mp4").createInputStream().use { it.readBytes() })
    }

    private fun feedCodec(decoder: MediaCodec, i: Int) {
        logInput("Feeding ${i}")
        val sample = extracter.sample(i)

        feedByteArray(decoder, sample, i)
    }

    private fun feedByteArray(decoder: MediaCodec, sample: ByteArray, i: Int) {
        val inIndex = decoder.dequeueInputBuffer(10000)
        logInput("inIndex ${inIndex}")
        val buffer = decoder.getInputBuffer(inIndex)!!
//
//        if(i==0) {
//            val payload = initExtractor.avcCBox().payload
//            buffer.put(0x0.toByte())
//            buffer.put(0x0.toByte())
//            buffer.put(0x0.toByte())
//            buffer.put(0x1.toByte())
//            buffer.put(payload)
//
//        }
        val avccToAnnexB = extracter.avccToAnnexB(sample)
        buffer.put(avccToAnnexB)



        val sampleSize: Int = buffer.position()
        logInput("sample size ${sampleSize}")
        //BUFFER_FLAG_KEY_FRAME | BUFFER_FLAG_CODEC_CONFIG
        val flags = when (i) {
            0 -> 0.or(MediaCodec.BUFFER_FLAG_CODEC_CONFIG).or(MediaCodec.BUFFER_FLAG_KEY_FRAME)
            else -> 0
        }
        buffer.position(0)
        decoder.queueInputBuffer(inIndex, i, sampleSize, System.nanoTime(), flags)
        logInput("samqueueInputBuffer ${flags}")
    }


    private fun cobbleTogetherACodec(surface : Surface): MediaCodec {

        logInput("cobbleTogetherACodec")

        //val decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        logInput("CREATED")
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 192, 108)
        format.setLong(MediaFormat.KEY_DURATION, 96000)
        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, 25)
        format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
        format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AV1Level21)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 25)
        format.setInteger(MediaFormat.KEY_WIDTH, 192)
        format.setInteger(MediaFormat.KEY_HEIGHT, 108)
        format.setInteger(MediaFormat.KEY_MAX_WIDTH, 1920)
        format.setInteger(MediaFormat.KEY_MAX_HEIGHT, 1080)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatYUV420Flexible)
        val decoder = MediaCodec.createByCodecName( MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(format) )

        //format.setString(KEY_CODECS_STRING,"avc3.42C015") //api 30
        //need to work out the format proper like
        decoder.configure(format, surface, null, 0)

        logInput("CONFIGURED")
        decoder.start()
        logInput("STARTED")

        SystemClock.sleep(1000)
        return decoder
    }
}