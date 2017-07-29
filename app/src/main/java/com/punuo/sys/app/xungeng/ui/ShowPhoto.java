package com.punuo.sys.app.xungeng.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import com.punuo.sys.app.xungeng.db.DatabaseInfo;
import com.punuo.sys.app.xungeng.ftp.Ftp;
import com.punuo.sys.app.xungeng.ftp.FtpListener;
import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.view.CustomProgressDialog;
import com.punuo.sys.app.xungeng.model.Constant;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by acer on 2016/11/11.
 */
public class ShowPhoto extends Activity {
    @Bind(R.id.photo)
    ImageView photo;
    private String mPhotoPath;
    private int type;
    private CustomProgressDialog dialog;
    String ftppath;
    private String localPath;
    private String msgid;
    private Handler handler=new Handler();
    Ftp mFtp;

    private Bitmap currentBitmap=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.showphoto);
        ButterKnife.bind(this);
        Intent intent=getIntent();
        mPhotoPath=intent.getStringExtra("path");
        type=intent.getIntExtra("type",0);
        switch (type){
            case 0:
                if (new File(mPhotoPath).exists()) {
                    currentBitmap=BitmapFactory.decodeFile(mPhotoPath);
                    photo.setImageBitmap(currentBitmap);
                }else{
                    photo.setImageDrawable(getDrawable(R.drawable.ic_error));
                }
                break;
            case 1:
                final File file=new File(mPhotoPath);
                if (file.exists()) {
                    ftppath = intent.getStringExtra("ftppath");
                    ftppath = ftppath.replace("/Thumbnail/", "/");
                    Log.d("111", ftppath);
                    msgid = intent.getStringExtra("msgid");
                    localPath = SipInfo.localSdCard+"Files/Camera/Image/";
                    final String localphotoPath = localPath + file.getName();
                    FtpListener download=new FtpListener() {
                        @Override
                        public void onStateChange(String currentStep) {

                        }

                        @Override
                        public void onUploadProgress(String currentStep, long uploadSize, File targetFile) {

                        }

                        @Override
                        public void onDownLoadProgress(String currentStep, long downProcess, File targetFile) {
                            if (currentStep.equals(Constant.FTP_DOWN_SUCCESS)) {
                                DatabaseInfo.sqLiteManager.updateFileDownload(msgid, 1);
                                DatabaseInfo.sqLiteManager.updateLocalPath(msgid, localphotoPath);
                            }
                        }

                        @Override
                        public void onDeleteProgress(String currentStep) {

                        }
                    };
                    mFtp=new Ftp(SipInfo.serverIp,21,"ftpall","123456",download);
                    if (!new File(localphotoPath).exists()) {
                        currentBitmap=BitmapFactory.decodeFile(mPhotoPath);
                        photo.setImageBitmap(currentBitmap);
                        dialog = new CustomProgressDialog(this);
                        dialog.setCanceledOnTouchOutside(false);
                        dialog.setCancelable(false);
                        dialog.show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    mFtp.download(ftppath,localPath);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    dialog.dismiss();
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            currentBitmap=BitmapFactory.decodeFile(localphotoPath);
                                            photo.setImageBitmap(currentBitmap);
                                        }
                                    });

                                }
                            }
                        }.start();
                    } else {
                        photo.setImageBitmap(BitmapFactory.decodeFile(localphotoPath));
                    }
                }else{
                    photo.setImageDrawable(getDrawable(R.drawable.ic_error));
                }
                break;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentBitmap!=null) {
            currentBitmap.recycle();
            currentBitmap=null;
        }
        System.gc();
    }
}
