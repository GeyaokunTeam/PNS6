package com.punuo.sys.app.xungeng.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.manager.IMakeSmallVideo;
import com.punuo.sys.app.xungeng.manager.MakeSmallVideoManager;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.tools.ActivityCollector;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MakeSmallVideo extends Activity implements SurfaceHolder.Callback, IMakeSmallVideo {

    @Bind(R.id.surface_video)
    SurfaceView surfaceVideo;
    @Bind(R.id.time)
    Chronometer time;
    @Bind(R.id.record)
    ImageButton record;
    @Bind(R.id.returnback)
    Button returnback;

    public static boolean flag = false;
    private final String TAG = "MakeSmallVideo";
    private MakeSmallVideoManager mMakeSmallVideoManager;
    private Handler handler = new Handler();
    private int len = 8;
    /**
     * 计时器
     */
    private Timer timer = new Timer();
    private int isSend = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCollector.addActivity(this);
//        SipInfo.instance = this;
        setContentView(R.layout.activity_make_small_video);
        ButterKnife.bind(this);
        //添加回调函数
        surfaceVideo.getHolder().addCallback(this);
        surfaceVideo.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //设置分辨率
        surfaceVideo.getHolder().setFixedSize(1280, 720);
        surfaceVideo.setFocusable(true);
        SipInfo.flag = false;
        mMakeSmallVideoManager = MakeSmallVideoManager.getInstance(this);
        flag = true;

    }


    @OnClick({R.id.returnback, R.id.record})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.returnback:
                finish();
                break;
            case R.id.record:
                if (!mMakeSmallVideoManager.isRecording) {
                    record.setBackgroundResource(R.drawable.btn_shutter_recording);
                    returnback.setVisibility(View.GONE);
                    startRecording();
                    time.setBase(SystemClock.elapsedRealtime());
                    time.start();
                } else {
                    record.setBackgroundResource(R.drawable.btn_shutter_record);
                    stopRecording();
                    time.stop();
                    time.setBase(SystemClock.elapsedRealtime());
                    timer.cancel();
                    returnback.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            len--;
            if (len == -1) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        record.callOnClick();
                    }
                });
            }
        }
    };

    @Override
    public void startRecording() {
        //重置计时
        len = 8;
        timer.schedule(task, 1000, 1000);
        mMakeSmallVideoManager.startRecording(surfaceVideo);
    }

    @Override
    public void stopRecording() {
        mMakeSmallVideoManager.stopRecording(surfaceVideo);
        Intent videoShow = new Intent(MakeSmallVideo.this, VideoShow.class);
        videoShow.putExtra("smallVideoPath", mMakeSmallVideoManager.smallMoviePath);
        startActivityForResult(videoShow, isSend);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == isSend) {
            if (resultCode == RESULT_OK) {
                String smallVideoPath = data.getStringExtra("smallVideoPath");
                Intent intent = new Intent();
                intent.putExtra("smallvideopath", smallVideoPath);
                setResult(RESULT_OK, intent);
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e(TAG, "surfaceChanged: ");
        mMakeSmallVideoManager.openCamera(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mMakeSmallVideoManager.closeCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
        mMakeSmallVideoManager.destory();
        mMakeSmallVideoManager = null;
        SipInfo.flag = true;
        flag = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        flag=true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        flag=false;
    }
}
