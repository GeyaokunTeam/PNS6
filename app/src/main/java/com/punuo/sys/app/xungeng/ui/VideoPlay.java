package com.punuo.sys.app.xungeng.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.FFmpeg.ffmpeg;
import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.sip.BodyFactory;
import com.punuo.sys.app.xungeng.sip.SipDev;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;
import com.punuo.sys.app.xungeng.sip.SipUser;
import com.punuo.sys.app.xungeng.tools.H264decoder;
import com.punuo.sys.app.xungeng.video.VideoInfo;

import org.zoolu.sip.message.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.punuo.sys.app.xungeng.sip.SipInfo.changeSuccess;
import static com.punuo.sys.app.xungeng.sip.SipInfo.sipUser;

/**
 * Author chzjy
 * Date 2016/12/19.
 */

public class VideoPlay extends Activity {
    public static final String TAG = "VideoPlay";
    private final byte[] SPS_DM365_CIF = {0x00, 0x00, 0x00, 0x01, 0x27, 0x64, 0x00, 0x1f, (byte) 0xad, (byte) 0x84, 0x09, 0x26, 0x6e, 0x23, 0x34, (byte) 0x90, (byte) 0x81, 0x24, (byte) 0xcd, (byte) 0xc4, 0x66, (byte) 0x92, 0x10, 0x24, (byte) 0x99, (byte) 0xb8, (byte) 0x8c, (byte) 0xd2, 0x42, 0x04, (byte) 0x93, 0x37, 0x11, (byte) 0x9a, 0x48, 0x40, (byte) 0x92, 0x66, (byte) 0xe2, 0x33, 0x49, 0x08, 0x12, 0x4c, (byte) 0xdc, 0x46, 0x69, 0x21, 0x05, 0x5a, (byte) 0xeb, (byte) 0xd7, (byte) 0xd7, (byte) 0xe4, (byte) 0xfe, (byte) 0xbf, 0x27, (byte) 0xd7, (byte) 0xae, (byte) 0xb5, 0x50, (byte) 0x82, (byte) 0xad, 0x75, (byte) 0xeb, (byte) 0xeb, (byte) 0xf2, 0x7f, 0x5f, (byte) 0x93, (byte) 0xeb, (byte) 0xd7, 0x5a, (byte) 0xab, 0x40, (byte) 0xb0, 0x4b, 0x20};
    private final byte[] PPS_DM365_CIF = {0x00, 0x00, 0x00, 0x01, 0x28, (byte) 0xee, 0x3c, (byte) 0xb0};
    private final byte[] SPS_MOBILE_QCIF = {0x00, 0x00, 0x00, 0x01, 0x27, 0x42, 0x10, 0x09, (byte) 0x96, 0x35, 0x05, (byte) 0x89, (byte) 0xc8};
    private final byte[] PPS_MOBILE_QCIF = {0x00, 0x00, 0x00, 0x01, 0x28, (byte) 0xce, 0x02, (byte) 0xfc, (byte) 0x80};
//    private final byte[] SPS_MOBILE_CIF = {0x00, 0x00, 0x00, 0x01, 0x67, 0x42, 0x00, 0x29, (byte) 0x8d, (byte) 0x8d, 0x40, (byte) 0xb0, 0x4b, 0x40, 0x3c, 0x22, 0x11, 0x4e};
//    private final byte[] PPS_MOBILE_CIF = {0x00, 0x00, 0x00, 0x01, 0x68, (byte) 0xca, 0x43, (byte) 0xc8};
    private final byte[] SPS_MOBILE_S6 = {0x00, 0x00, 0x00, 0x01, 0x27, 0x42, 0x00, 0x33, (byte) 0xe3, 0x50, 0x28, (byte) 0x3f, (byte) 0x20};
    private final byte[] PPS_MOBILE_S6 = {0x00, 0x00, 0x00, 0x01, 0x28, (byte) 0xce, 0x02, (byte) 0xfc, (byte) 0x80};
    private final byte[] SPS_MOBILE_S9 = {0x00, 0x00, 0x00, 0x01, 0x27, 0x42, 0x10, 0x09, (byte) 0x96, 0x35, 0x02, (byte) 0x83, (byte) 0xf2};
    private final byte[] PPS_MOBILE_S9 = {0x00, 0x00, 0x00, 0x01, 0x28, (byte) 0xce, 0x02, (byte) 0xfc, (byte) 0x80};
  //  private ffmpeg mFFmpeg = new ffmpeg();
    private SurfaceHolder surfaceHolder;
    private byte[] mPixel = new byte[VideoInfo.width * VideoInfo.height * 2];
    private ByteBuffer buffer = ByteBuffer.wrap(mPixel);
    private Bitmap videoBit = Bitmap.createBitmap(VideoInfo.width, VideoInfo.height, Bitmap.Config.RGB_565);
    private int getNum = 0;
    Timer timer = new Timer();
    private H264decoder h264decoder;

    public Thread decoder;

    private int previewWidth = 720;
    private int previewHeight =640;
    private int previewFrameRate = 10;

    @Bind(R.id.surfaceView)
    SurfaceView surfaceView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);
        ButterKnife.bind(this);
//        SipInfo.sipDev.setResponse(this);

        surfaceHolder = surfaceView.getHolder();
//        surfaceHolder.addCallback(this);
        h264decoder=new H264decoder();

        playVideo();
        timer.schedule(task, 0, 2000);

//        final Button changeResolution = (Button)findViewById(R.id.change_resolution);
//        changeResolution.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                SipInfo.decoding = false;
//                h264decoder.close();
//                Log.e(TAG, "结束解码" );
//                closeVideoThread();
//
//                if (previewWidth == 1280){
//                    previewWidth = 720;
//                    previewHeight = 480;
//                }else if (previewWidth == 720){
//                    previewWidth = 1280;
//                    previewHeight = 720;
//                }
//                sipUser.sendMessage(SipMessageFactory.createNotifyRequest(sipUser, SipInfo.toDev,
//                        SipInfo.user_from, BodyFactory.createResolutionChangeBody(previewWidth, previewHeight, previewFrameRate)));
//            }
//        });
    }
    TimerTask task = new TimerTask() {

        @Override
        public void run() {
            if (VideoInfo.isrec == 0) {
                closeVideo();
            } else if (VideoInfo.isrec == 2) {
                VideoInfo.isrec = 0;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
        VideoInfo.isrec = 1;
        SipInfo.decoding = false;
        VideoInfo.rtpVideo.removeParticipant();
        VideoInfo.sendActivePacket.stopThread();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
//                    mFFmpeg.Destroy();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        VideoInfo.rtpVideo.endSession();
        VideoInfo.track.stop();
        buffer.clear();
        ButterKnife.unbind(this);
        System.gc();
    }

    private void playVideo() {
//        decoder=new Thread(Video);
//        decoder.start();
        new Thread(Video).start();
    }
//    private void closeVideoThread(){
//        decoder.interrupt();
//        decoder=null;
//        Log.e(TAG,"解码线程已经关闭");
//    }
//    long a;
    Runnable Video = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "准备开始解码" );
            //软解码设置
//            mFFmpeg.init(VideoInfo.width, VideoInfo.height);
//            switch (VideoInfo.videoType) {
//                case 2:
//                    mFFmpeg.DecoderNal(SPS_DM365_CIF, 78, mPixel);
//                    mFFmpeg.DecoderNal(PPS_DM365_CIF, 8, mPixel);
//                    break;
//                case 3:
//                    mFFmpeg.DecoderNal(SPS_MOBILE_QCIF, 13, mPixel);
//                    mFFmpeg.DecoderNal(PPS_MOBILE_QCIF, 9, mPixel);
//                    break;
//                case 4:
//                    mFFmpeg.DecoderNal(SPS_MOBILE_S6, 13, mPixel);
//                    mFFmpeg.DecoderNal(PPS_MOBILE_S6, 9, mPixel);
//                    break;
//                case 5:
//                    mFFmpeg.DecoderNal(SPS_MOBILE_S9, 13, mPixel);
//                    mFFmpeg.DecoderNal(PPS_MOBILE_S9, 9, mPixel);
//                    break;
//                default:
//                    break;
//            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Surface surface=surfaceHolder.getSurface();
            System.out.println(surface);
            if (surface!=null) {
                h264decoder.initDecoder(surface, previewWidth, previewHeight);
                while (SipInfo.decoding) {
                    Log.e(TAG, "解码开始" );
                    if (SipInfo.isNetworkConnected) {
                        byte[] nal = VideoInfo.nalBuffers[getNum].getReadableNalBuf();
                        if (nal != null) {
                            Log.i(TAG, "nalLen:" + nal.length);
                            try {
                                //软解码
//                            int iTemp = mFFmpeg.DecoderNal(nal, nal.length, mPixel);
//
//                            if (iTemp > 0) {
//                                doSurfaceDraw();
//                            }

                              //硬解码
                                h264decoder.onFrame(nal, 0, nal.length);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        VideoInfo.nalBuffers[getNum].readLock();
                        VideoInfo.nalBuffers[getNum].cleanNalBuf();
                        getNum++;
                        if (getNum == 200) {
                            getNum = 0;
                        }
                    }
                }
            }
        }
    };
//      软解码呈现画面
//    private void doSurfaceDraw() {
//        videoBit.copyPixelsFromBuffer(buffer);
//        buffer.position(0);
//        Log.i(TAG, "doSurfaceDraw");
//
//        int surfaceViewWidth = surfaceView.getWidth();
//        int surfaceViewHeight = surfaceView.getHeight();
//        int bmpWidth = videoBit.getWidth();
//        int bmpHeight = videoBit.getHeight();
//        System.out.println("bmpHeight = " + bmpHeight);
//        System.out.println("bmpWidth = " + bmpWidth);
//        System.out.println("surfaceViewHeight = " + surfaceViewHeight);
//        System.out.println("surfaceViewWidth = " + surfaceViewWidth);
//        Matrix matrix = new Matrix();
//        matrix.postScale(4.3f, 4.3f);
//        Bitmap resizeBmp = Bitmap.createBitmap(videoBit, 0, 0, bmpWidth, bmpHeight, matrix, true);
//        if (VideoInfo.videoType==4||VideoInfo.videoType==5) {
//            resizeBmp = adjustPhotoRotation(resizeBmp, 90);
//        }
//        if (surfaceHolder!=null) {
//            Canvas canvas = surfaceHolder.lockCanvas();
//            canvas.drawBitmap(resizeBmp, 0, 0, null);
//            surfaceHolder.unlockCanvasAndPost(canvas);
//        }
//    }

    Bitmap adjustPhotoRotation(Bitmap bm, final int orientationDegree) {
        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        float targetX, targetY;
        if (orientationDegree == 90) {
            targetX = bm.getHeight();
            targetY = 0;
        } else {
            targetX = bm.getHeight();
            targetY = bm.getWidth();
        }
        final float[] values = new float[9];
        m.getValues(values);
        float x1 = values[Matrix.MTRANS_X];
        float y1 = values[Matrix.MTRANS_Y];
        m.postTranslate(targetX - x1, targetY - y1);
        Bitmap bm1 = Bitmap.createBitmap(bm.getHeight(), bm.getWidth(), Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        Canvas canvas = new Canvas(bm1);
        canvas.drawBitmap(bm, m, paint);
        return bm1;
    }

    @Override
    public void onBackPressed() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("是否结束监控?")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        closeVideo();
                    }
                })
                .setNegativeButton("否", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();
        dialog.setCanceledOnTouchOutside(false);
    }

    private void closeVideo() {
        Message bye = SipMessageFactory.createByeRequest(SipInfo.sipUser, SipInfo.toDev, SipInfo.user_from);
        SipInfo.sipUser.sendMessage(bye);
        finish();
    }


//    @Override
//    public void surfaceCreated(SurfaceHolder holder) {
//
//    }
//
//    @Override
//    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//
//    }
//
//    @Override
//    public void surfaceDestroyed(SurfaceHolder holder) {
//
//    }

//    @Override
//    public void onResponse() {
//        h264decoder.initDecoder(surfaceHolder.getSurface(), previewWidth, previewHeight);
//        SipInfo.decoding = true;
//        playVideo();
//    }
}

