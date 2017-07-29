package com.punuo.sys.app.xungeng.tools;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.EventLogTags;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by asus on 2017/6/16.
 */
//硬编码
public class H264decoder {
    private static final String TAG = "H264decoder";
    private MediaCodec mCodec;
    private final static String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private final static int VIDEO_WIDTH = 1280;
    private final static int VIDEO_HEIGHT = 720;
//    private final static int VIDEO_WIDTH = 352;
//    private final static int VIDEO_HEIGHT = 288;
    private final static int TIME_INTERNAL = 10;
    private final static int HEAD_OFFSET=512;

    int mCount = 0;

    public void initDecoder(Surface surface, int w, int h) {

        try {
            mCodec = MediaCodec.createDecoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,
                w, h);
        mCodec.configure(mediaFormat, surface,
                null, 0);
        mCodec.start();
    }
    public void close(){
        mCodec.stop();
        mCodec.release();
    }


    public boolean onFrame(byte[] buf, int offset, int length) {
        Log.e("Media", "onFrame start");
        Log.e("Media", "onFrame Thread:" + Thread.currentThread().getName());
        // Get input buffer index
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        int inputBufferIndex = mCodec.dequeueInputBuffer(-1);

        Log.e("Media", "onFrame index:" + inputBufferIndex);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(buf, offset, length);
            mCodec.queueInputBuffer(inputBufferIndex, 0, length, mCount
                    * TIME_INTERNAL, 0);
            mCount++;
        } else {
            return false;
        }

        // Get output buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
        while (outputBufferIndex >= 0) {
            mCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
        Log.e("Media", "onFrame end");
        return true;
    }



}
