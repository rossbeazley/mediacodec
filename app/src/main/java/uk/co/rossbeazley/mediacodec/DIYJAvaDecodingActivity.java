package uk.co.rossbeazley.mediacodec;

import android.app.Activity;
import android.media.MediaCodec;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
import static android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME;
import static android.media.MediaCodecList.REGULAR_CODECS;

public class DIYJAvaDecodingActivity extends Activity implements SurfaceHolder.Callback {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SurfaceView sv = new SurfaceView(this);
        sv.getHolder().addCallback(this);
        setContentView(sv);
        givenASegmentIsLoadedIntoMemory();
    }



    VideoM4SExtractor extracter;

    private void givenASegmentIsLoadedIntoMemory() {

        extracter = new VideoM4SExtractor(AssetPathAsBytesKt.asBytes("seg1.m4s", getAssets()));
    }

    public void bitmapDecoded(MediaCodec decoder) {


        //for(frame in 0 until 1) {
            feedCodec(decoder, 0);
        //}


        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int stillWaiting = 10;
        while(stillWaiting > 0) {
            int outIndex = decoder.dequeueOutputBuffer(info, 10000);
            switch (outIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED: System.out.println("INFO_OUTPUT_BUFFERS_CHANGED"); break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED: System.out.println("New format " + decoder.getOutputFormat()); break;
                case MediaCodec.INFO_TRY_AGAIN_LATER : System.out.println("dequeueOutputBuffer timed out!"); break;
                default: {
                    System.out.println("info.presentationTimeUs ${info.presentationTimeUs}");
                    stillWaiting = -1;
                    System.out.println("outIndex ${outIndex}");
                    decoder.releaseOutputBuffer(outIndex, true);
                }
            }
            stillWaiting--;
            System.out.println("still waiting ${stillWaiting} flags ${info.flags}");
        }


        decoder.stop();
        decoder.release();

    }

    private void feedCodec(MediaCodec decoder, int i) {
        System.out.println("Feeding ${i}");
        int inIndex = decoder.dequeueInputBuffer(10000);
        System.out.println("inIndex ${inIndex}");
        ByteBuffer buffer = decoder.getInputBuffer(inIndex);

        byte[] sample = extracter.sample(i);
        buffer.put(sample);
        System.out.println("sample size ${sampleSize}");
        //BUFFER_FLAG_KEY_FRAME | BUFFER_FLAG_CODEC_CONFIG
        int flags = i==0?BUFFER_FLAG_CODEC_CONFIG|BUFFER_FLAG_KEY_FRAME:0;
        decoder.queueInputBuffer(inIndex, i, sample.length, i, flags);
        System.out.println("samqueueInputBuffer ${flags}");
    }


    private MediaCodec cobbleTogetherACodec(Surface surface) {

        System.out.println("cobbleTogetherACodec");

        //val decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        System.out.println("CREATED");
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 192, 108);
        format.setLong(MediaFormat.KEY_DURATION, 96000);
        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, 25);
        format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
        format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AV1Level21);

        MediaCodec decoder = null;
        try {
            decoder = MediaCodec.createByCodecName( new MediaCodecList(REGULAR_CODECS).findDecoderForFormat(format) );
        } catch (IOException e) {
            e.printStackTrace();
        }

        //format.setString(KEY_CODECS_STRING,"avc3.42C015") //api 30
        //need to work out the format proper like
        decoder.configure(format, surface, null, 0);

        System.out.println("CONFIGURED");
        decoder.start();
        System.out.println("STARTED");

        SystemClock.sleep(1000);
        return decoder;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Surface surface = surfaceHolder.getSurface();
        MediaCodec decoder = cobbleTogetherACodec(surface);
        bitmapDecoded(decoder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }
}