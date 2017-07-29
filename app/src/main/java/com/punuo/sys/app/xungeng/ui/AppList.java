package com.punuo.sys.app.xungeng.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.adapter.AppAdapter;
import com.punuo.sys.app.xungeng.db.DatabaseInfo;
import com.punuo.sys.app.xungeng.ftp.Ftp;
import com.punuo.sys.app.xungeng.ftp.FtpListener;
import com.punuo.sys.app.xungeng.model.App;
import com.punuo.sys.app.xungeng.sip.SipInfo;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.punuo.sys.app.xungeng.model.Constant.FTP_DOWN_LOADING;
import static com.punuo.sys.app.xungeng.model.Constant.FTP_DOWN_SUCCESS;

/**
 * Author chzjy
 * Date 2016/12/19.
 */

public class AppList extends Activity {
    @Bind(R.id.title)
    TextView title;
    @Bind(R.id.applist)
    ListView applist;
    @Bind(R.id.empty)
    TextView empty;
    private String TAG="Applist";
    private AppAdapter appAdapter;
    private String sdPath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.applist);
        ButterKnife.bind(this);

        init();
    }

    private void init() {
        title.setText("添加应用");
        if (SipInfo.applist.size()==0){
            empty.setVisibility(View.VISIBLE);
            applist.setVisibility(View.GONE);
        }else {
            empty.setVisibility(View.GONE);
            applist.setVisibility(View.VISIBLE);
        }
        sdPath= Environment.getExternalStorageDirectory().getAbsolutePath()+"/PNS9/download/apk/";
        appAdapter=new AppAdapter(AppList.this,downloadListener,openFileListener);
        applist.setAdapter(appAdapter);

    }
    private AppAdapter.DownloadListener downloadListener= new AppAdapter.DownloadListener() {
        @Override
        public void onDownload(final String appId, final String appPath, String appName) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FtpListener ftpListener=new FtpListener() {
                        @Override
                        public void onStateChange(String currentStep) {

                        }

                        @Override
                        public void onUploadProgress(String currentStep, long uploadSize, File targetFile) {
                        }

                        @Override
                        public void onDownLoadProgress(String currentStep, long downProcess, File targetFile) {
                            Log.d(TAG, currentStep);
                            Message message = new Message();
                            message.obj = appId;
                            if (currentStep.equals(FTP_DOWN_SUCCESS)) {
                                Log.d(TAG, "-----下载--successful");
                                message.what = 0x3332;
                                handler.sendMessage(message);
                            } else if (currentStep.equals(FTP_DOWN_LOADING)) {
                                Log.d(TAG, "-----下载---" + downProcess + "%");
                                message.arg1 = (int) downProcess;
                                message.what = 0x3331;
                                handler.sendMessage(message);
                            }
                        }

                        @Override
                        public void onDeleteProgress(String currentStep) {

                        }
                    };
                    final Ftp mFtp=new Ftp(SipInfo.serverIp,21,"ftpall","123456",ftpListener);
                    try {
                        //单文件下载
                        mFtp.download(appPath,sdPath);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } finally {
                        DatabaseInfo.sqLiteManager.updateAppState(appId, 1);
                    }

                }
            }).start();
        }
    };
    private AppAdapter.OpenFileListener openFileListener = new AppAdapter.OpenFileListener() {
        @Override
        public void OpenFile(File file) {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //设置intent的Action属性
            intent.setAction(Intent.ACTION_VIEW);
            //设置intent的data和Type属性。
            intent.setDataAndType(/*uri*/Uri.fromFile(file),
                    "application/vnd.android.package-archive");
            startActivity(intent);
        }
    };
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            App app = new App();
            app.setAppid((String) msg.obj);
            int index = SipInfo.applist.indexOf(app);
            if (index != -1) {
                if (msg.what == 0x3331) {
                    SipInfo.applist.get(index).setProgress(msg.arg1);
                    appAdapter.notifyDataSetChanged();
                }
                if (msg.what == 0x3332) {
                    DatabaseInfo.sqLiteManager.updateAppState(app.getAppid(), 1);
                    DatabaseInfo.sqLiteManager.updateAppLocalPath(app.getAppid(), sdPath+SipInfo.applist.get(index).getApkname());
                    appAdapter.notifyDataSetChanged();
                    SipInfo.applist.get(index).setProgress(0);
                }
            }
            return true;
        }
    });
}
