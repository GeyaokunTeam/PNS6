package com.punuo.sys.app.xungeng.manager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.punuo.sys.app.xungeng.camera.FileOperateUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Author chzjy
 * Date 2016/12/19.
 */

public class MakeSmallVideoManager {
    private final String TAG = "MakeSmallVideoManager";
    private static MakeSmallVideoManager mInstance;
    private Context mContext;

    private Camera mCamera;
    public boolean isRecording;
    private MediaRecorder mRecorder;
    public String smallMoviePath;
    private File recordOutput;

    private MakeSmallVideoManager(Context context) {
        mContext = context;
        mRecorder = new MediaRecorder();
    }

    public static MakeSmallVideoManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new MakeSmallVideoManager(context.getApplicationContext());
        }
        return mInstance;
    }

    public void destory() {
        mInstance = null;
    }

    public void openCamera(SurfaceHolder mSurfaceHolder) {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraId_back = -1;
        int cameraId_out = -1;
        int cameraId_front = -1;
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId_back = i;     //获取后置摄像头的Id
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId_front = i;    //获取前置摄像头的Id
            } else {
                cameraId_out = i;
            }
        }
        try {
            mCamera = Camera.open(cameraId_out);
        } catch (Exception e) {
            try {
                mCamera = Camera.open(cameraId_back);
            } catch (Exception e1) {
                Log.e(TAG, "openCamera: ", e1);
            }
        }
        try {
            if (mCamera == null) return;
            mCamera.setPreviewDisplay(mSurfaceHolder);
            initCamera();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initCamera() {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
        parameters.setPreviewSize(1280, 720);  // 部分定制手机，无法正常识别该方法// 。
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);//1连续对焦
        setDispaly(parameters, mCamera);
        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

    public void closeCamera() {
        release();
    }

    private void release() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);//停止接收回调信号
            mCamera.stopPreview();//停止捕获和绘图
            mCamera.release();
            mCamera = null;
        }
    }

    public void startRecording(SurfaceView view) {
        if (!isRecording) {
            try {
                initializeRecorder(view.getHolder().getSurface());
                isRecording = true;
                mRecorder.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stopRecording(SurfaceView view) {
        if (isRecording) {
//            timer.cancel();
            mRecorder.stop();
            mRecorder.reset();
            mCamera.lock();
            isRecording = false;
            try {
                saveThumbnail(view);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initializeRecorder(Surface surface) throws IOException {
        mCamera.unlock();
        mRecorder.setCamera(mCamera);
        mRecorder.setOrientationHint(90);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        String path = FileOperateUtil.getFolderPath(mContext, FileOperateUtil.TYPE_VIDEO, "Camera");
        File directory = new File(path);
        if (!directory.exists()) {
            boolean mkdirs = directory.mkdirs();
        }
        smallMoviePath = path + File.separator + "video" + FileOperateUtil.createFileNmae(".mp4");
        recordOutput = new File(smallMoviePath);
        if (recordOutput.exists()) {
            boolean delete = recordOutput.delete();
        }
        CamcorderProfile cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        mRecorder.setProfile(cpHigh);
        mRecorder.setOutputFile(recordOutput.getAbsolutePath());
        mRecorder.setPreviewDisplay(surface);
        mRecorder.setMaxDuration(50000);
        mRecorder.prepare();
    }

    //控制图像的正确显示方向
    private void setDispaly(Camera.Parameters parameters, Camera camera) {
        if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
            setDisplayOrientation(camera, 90);
        } else {
            parameters.setRotation(90);
        }
    }

    //实现的图像的正确显示
    private void setDisplayOrientation(Camera camera, int i) {
        Method downPolymorphic;
        try {
            downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", int.class);
            if (downPolymorphic != null) {
                downPolymorphic.invoke(camera, i);
            }
        } catch (Exception e) {
            Log.e(TAG, "图像出错");
        }
    }


    private Bitmap saveThumbnail(View v) throws FileNotFoundException, IOException {
        if (smallMoviePath != null) {
            //创建缩略图,该方法只能获取384X512的缩略图，舍弃，使用源码中的获取缩略图方法
            //			Bitmap bitmap=ThumbnailUtils.createVideoThumbnail(mRecordPath, Thumbnails.MINI_KIND);
            Bitmap bitmap = getVideoThumbnail(smallMoviePath, v);

            if (bitmap != null) {
                String mThumbnailFolder = FileOperateUtil.getFolderPath(mContext, FileOperateUtil.TYPE_THUMBNAIL, "Camera");
                File folder = new File(mThumbnailFolder);
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                File file = new File(smallMoviePath);
                file = new File(folder + File.separator + file.getName().replace(".mp4", ".jpg"));
                //存图片小图
                BufferedOutputStream bufferos = new BufferedOutputStream(new FileOutputStream(file));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bufferos);
                bufferos.flush();
                bufferos.close();
                return bitmap;
            }
        }
        return null;
    }

    /**
     * 获取帧缩略图，根据容器的高宽进行缩放
     *
     * @param filePath
     * @return
     */
    public Bitmap getVideoThumbnail(String filePath, View v) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime(-1);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        if (bitmap == null)
            return null;
        // Scale down the bitmap if it's too large.
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int pWidth = v.getWidth();// 容器宽度
        int pHeight = v.getHeight();//容器高度
        //获取宽高跟容器宽高相比较小的倍数，以此为标准进行缩放
        float scale = Math.min((float) width / pWidth, (float) height / pHeight);
        int w = Math.round(scale * pWidth);
        int h = Math.round(scale * pHeight);
        bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
        return bitmap;
    }
}
