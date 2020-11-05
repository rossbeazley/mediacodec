package uk.co.rossbeazley.mediacodec

import android.app.Activity
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

class MultiThreadedExtractorDecodingActivity : Activity(), SurfaceHolder.Callback {

    lateinit var extracter: VideoM4SExtractor
    private lateinit var initExtractor: InitM4SExtractor
    var NOT_DONE = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sv = SurfaceView(this)
        sv.holder.addCallback(this)
        setContentView(sv)
        givenASegmentIsLoadedIntoMemory()
    }

    fun givenASegmentIsLoadedIntoMemory() {
        val assetFileDescriptor = getAssets().openFd("seg1.m4s")
        val bytes: ByteArray = assetFileDescriptor.createInputStream().use { it.readBytes() }
        extracter = VideoM4SExtractor(bytes)

        initExtractor = InitM4SExtractor(getAssets().openFd("init.mp4").createInputStream().use { it.readBytes() })
    }

    private fun startPlayback(toSurface: Surface) {
        logMain("Start Playback")
        val decoder = cobbleTogetherACodec(toSurface)
        //thread 1
        feedCodecWithMedia(extracter, decoder)
        //thread 2
        renderToScreen(decoder, toSurface)
    }

    private fun cobbleTogetherACodec(surface : Surface): MediaCodec {

        logInput("cobbleTogetherACodec")

        //val decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        logInput("CREATED")
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 192, 108)
        format.setLong(MediaFormat.KEY_DURATION, 96000)
        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, 25)
        format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedBaseline)
        format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AV1Level21)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 25)
        format.setInteger(MediaFormat.KEY_WIDTH, 192)
        format.setInteger(MediaFormat.KEY_HEIGHT, 108)
        format.setInteger(MediaFormat.KEY_MAX_WIDTH, 1920)
        format.setInteger(MediaFormat.KEY_MAX_HEIGHT, 1080)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatYUV420Flexible)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16128)
        val decoder = MediaCodec.createByCodecName( MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(format) )
        //need to work out the format proper like
        decoder.configure(format, surface, null, 0)

        logInput("CONFIGURED")
        decoder.start()
        logInput("STARTED")

        SystemClock.sleep(1000)
        return decoder
    }


    private fun feedCodecWithMedia(extracter: VideoM4SExtractor, decoder: MediaCodec) {

       Thread {

           val frameCount = extracter.frameCount
           for(frame in 0 until 8) {
               feedCodec(extracter, decoder, frame)
           }

           val inIndex = decoder.dequeueInputBuffer(10000)
           if (inIndex >= 0) {
               logInput("Got an input buffer to fill with EOS")
               val buffer = decoder.getInputBuffer(inIndex)
               //its the end of media
               logInput("Got EOF")
               decoder.queueInputBuffer(inIndex, 0, 0, System.nanoTime(), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
           } else {
               logInput("No buffers for EOS")
           }

       }.start()

    }

    private fun feedCodec(extracter: VideoM4SExtractor, decoder: MediaCodec, frameIdx: Int) {
        logInput("Feeding           ===== ${frameIdx}")
        val sample = extracter.sample(frameIdx)
        val inIndex = decoder.dequeueInputBuffer(10000)
        logInput("inIndex ${inIndex}")
        val buffer = decoder.getInputBuffer(inIndex)!!
        buffer.clear()
        val avccToAnnexB = extracter.avccToAnnexB(sample)
        buffer.put(sample)
        val sampleSize: Int = buffer.position()
        logInput("sample size ${sampleSize}")
        val flags = when (frameIdx) {
            0 -> 0.or(MediaCodec.BUFFER_FLAG_CODEC_CONFIG).or(MediaCodec.BUFFER_FLAG_KEY_FRAME)
            else -> 0
        }
        buffer.flip()
        decoder.queueInputBuffer(inIndex, frameIdx, sampleSize, System.nanoTime(), flags)
        logInput("samqueueInputBuffer ${flags}")
    }

    private fun renderToScreen(decoder: MediaCodec, surface: Surface) {

        Thread() {
            val startTime = System.nanoTime()
            val info = MediaCodec.BufferInfo()

            while (NOT_DONE) {
                val outIndex = decoder.dequeueOutputBuffer(info, 10000)
                when (outIndex) {
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> logOutput("INFO_OUTPUT_BUFFERS_CHANGED")
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> logOutput("New format " + decoder.outputFormat)
                    MediaCodec.INFO_TRY_AGAIN_LATER -> logOutput("dequeueOutputBuffer timed out!")
                    else -> {
                        logOutput("Got a buffer at ${info.presentationTimeUs} and current presentation time ${System.nanoTime() - startTime}")
             //           while (info.presentationTimeUs * 1000 > System.nanoTime() - startTime) {
                            try {
                                Thread.sleep((1/25)*1000)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                                break
                            }
               //         }
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

    override fun surfaceCreated(holder: SurfaceHolder) = startPlayback(holder.surface)
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    private fun logMain(message: String) = Log.d("DecodeActivityInput", message)
    private fun logInput(message: String) =Log.d("DecodeActivityInput", message)
    private fun logOutput(message: String) =Log.d("DecodeActivityOutput", message)

}