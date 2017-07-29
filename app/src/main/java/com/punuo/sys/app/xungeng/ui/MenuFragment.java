package com.punuo.sys.app.xungeng.ui;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.adapter.AppGridViewAdapter;
import com.punuo.sys.app.xungeng.adapter.ApplicationAdapter;
import com.punuo.sys.app.xungeng.ftp.Ftp;
import com.punuo.sys.app.xungeng.ftp.FtpListener;
import com.punuo.sys.app.xungeng.model.Constant;
import com.punuo.sys.app.xungeng.model.MyApplicationInfo;
import com.punuo.sys.app.xungeng.sip.BodyFactory;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;
import com.punuo.sys.app.xungeng.tools.ActivityCollector;
import com.punuo.sys.app.xungeng.tools.VersionXmlParse;
import com.punuo.sys.app.xungeng.view.CustomProgressDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

import static android.app.Activity.DEFAULT_KEYS_SEARCH_LOCAL;

/**
 * Author chzjy
 * Date 2016/12/19.
 * 菜单
 */

public class MenuFragment extends Fragment {
    String TAG = getClass().getName();
    @Bind(R.id.userAccount)
    TextView account;
    @Bind(R.id.realname)
    TextView realname;
    @Bind(R.id.viewpager)
    ViewPager viewPager;
    public final int CALLCENTER = 0;
    public final int CHANGEPWD = 1;
    public final int GALLER = 2;
    public final int CHSCHANGE = 3;
    public final int UPDATE = 4;
    public final int ADDAPP = 5;

    private BroadcastReceiver AppReceiver = new ApplicationsIntentReceiver();

    private List<MyApplicationInfo> mApplications;
    // 程序所占的总屏数
    private int screenCount;
    //每个屏幕最大程序数量
    public static final int NUMBER_PER_SCREEN = 16;

    LayoutInflater inflater;

    private ApplicationAdapter applicationAdapter;

    private AppGridViewAdapter gridViewAdapter;

    private List<GridView> gridViewList;

    int[] icon = new int[]{
            R.drawable.menu_call_center,
            R.drawable.menu_change_psd,
            R.drawable.menu_album,
            R.drawable.menu_chs_change,
            R.drawable.menu_soft_update

    };

    List<String> titlelist = new ArrayList<>();

    String[] title = new String[]{
            "呼叫平台",
            "修改密码",
            "相册",
            "集群频道更换",
            "软件更新",
            "添加应用"
    };

    //手机内存卡路径
    String SdCard;
    //当前版本
    String version;
    //FTP上的版本
    String FtpVersion;
    //用于版本xml解析
    HashMap<String, String> versionHashMap = new HashMap<>();
    //进度条
    CustomProgressDialog loading;
    //进度条消失类型
    String result;
    //下载进度条
    ProgressDialog downloadDialog;
    //apk路径
    String apkPath;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        view.setClickable(true);
        ButterKnife.bind(this, view);

        init(inflater);
        return view;
    }

    private void init(LayoutInflater layoutInflater) {
        SdCard = Environment.getExternalStorageDirectory().getAbsolutePath();
        apkPath = SdCard + "/PNS6/download/apk/";
        for (String aTitle : title) {
            titlelist.add(aTitle);
        }
        inflater = layoutInflater;
        getActivity().setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
        registerIntentReceivers();
        bindApplications();

        account.setText(SipInfo.userAccount);
        realname.setText(SipInfo.userRealname);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
        unregisterIntentReceivers();
    }


    //注册app监听
    private void registerIntentReceivers() {
        IntentFilter filter;
        filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        getActivity().registerReceiver(AppReceiver, filter);
    }

    private void unregisterIntentReceivers() {
        getActivity().unregisterReceiver(AppReceiver);
    }

    private class ApplicationsIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadApplications(false);
            bindApplications();
        }
    }

    /**
     * Loads the list of installed applications in mApplications.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void loadApplications(boolean isLaunching) {
        if (isLaunching && mApplications != null) {
            return;
        }
        //获取所有app的入口
        PackageManager manager = getActivity().getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));
        if (mApplications == null) {
            mApplications = new ArrayList<>();
        }
        mApplications.clear();
        for (int i = 0; i < icon.length; i++) {
            MyApplicationInfo application = new MyApplicationInfo();
            application.setType(MyApplicationInfo.TYPE_BUTTON);
            application.setTitle(title[i]);
            application.setIcon(getActivity().getDrawable(icon[i]));
            application.setSystemApp(false);
            mApplications.add(application);
        }
        int count = apps.size();
        for (int i = 0; i < count; i++) {
            MyApplicationInfo application = new MyApplicationInfo();
            ResolveInfo info = apps.get(i);

            if (info.loadLabel(manager).equals("设置") || info.loadLabel(manager).equals("文件管理")
                    || (info.activityInfo.applicationInfo.packageName.contains("com.punuo.sys.app")
                    && !info.activityInfo.applicationInfo.packageName.contains("com.punuo.sys.app.xungeng"))) {
                application.setType(MyApplicationInfo.TYPE_APP);
                application.setTitle(info.loadLabel(manager));
                application.packageName = info.activityInfo.applicationInfo.packageName;
                application.setActivity(new ComponentName(
                                info.activityInfo.applicationInfo.packageName,
                                info.activityInfo.name),
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                switch (info.loadLabel(manager).toString()) {
                    case "设置":
                        application.setIcon(getActivity().getDrawable(R.drawable.menu_setting));
                        break;
                    case "文件管理":
                        application.setIcon(getActivity().getDrawable(R.drawable.menu_filemanager));
                        break;
                    default:
                        application.setIcon(info.activityInfo.loadIcon(manager));
                        break;
                }
                application.setSystemApp((info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
                mApplications.add(application);
            }
        }
        MyApplicationInfo applicationInfo = new MyApplicationInfo();
        applicationInfo.setType(MyApplicationInfo.TYPE_BUTTON);
        applicationInfo.setTitle("添加应用");
        applicationInfo.setIcon(getActivity().getDrawable(R.drawable.menu_add));
        applicationInfo.setSystemApp(false);
        mApplications.add(applicationInfo);
    }

    /**
     * Creates a new appplications adapter for the grid view and registers it.
     */
    private void bindApplications() {
        loadApplications(true);
        screenCount = mApplications.size() % NUMBER_PER_SCREEN == 0 ?
                mApplications.size() / NUMBER_PER_SCREEN :
                mApplications.size() / NUMBER_PER_SCREEN + 1;
        viewPager.removeAllViews();
        gridViewList = new ArrayList<>();
        for (int i = 0; i < screenCount; i++) {
            GridView gv = new GridView(getActivity());
            gridViewAdapter = new AppGridViewAdapter(getActivity(), mApplications, i);
            gv.setAdapter(gridViewAdapter);
            gv.setGravity(Gravity.CENTER);
            gv.setNumColumns(4);
            gv.setClickable(true);
            gv.setFocusable(true);
            gv.setColumnWidth(120);
            gv.setVerticalSpacing(60);
            final int finalI = i;
            gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View vie, int position, long id) {
                    final MyApplicationInfo currentAppInfo = mApplications.get(position + finalI * NUMBER_PER_SCREEN);
                    if (currentAppInfo.getType() == MyApplicationInfo.TYPE_BUTTON) {
                        //按钮功能
                        switch (titlelist.indexOf(currentAppInfo.getTitle().toString())) {
                            case CALLCENTER:
                                PhoneCall.actionStart(getActivity(), SipInfo.centerPhoneNumber,1);
                                break;
                            case CHANGEPWD:
                                startActivity(new Intent(getActivity(), ChangePassword.class));
                                break;
                            case GALLER:
                                Intent gallerIntent = new Intent(getActivity(), AlbumAty.class);
                                startActivity(gallerIntent);
                                break;
                            case CHSCHANGE:
                                Intent chschange=new Intent(getActivity(),ChsChange.class);
                                startActivity(chschange);
                                break;
                            case UPDATE:
                                result = "Finished";
                                loading = new CustomProgressDialog(getActivity());
                                loading.setCancelable(false);
                                loading.setCanceledOnTouchOutside(false);
                                loading.show();
                                //初始化FTP
                                mFtp = new Ftp("101.69.255.132", 21, "ftpall", "123456", Dversion);
                                //获取当前版本号
                                PackageManager packageManager = getActivity().getPackageManager();
                                try {
                                    PackageInfo pi = packageManager.getPackageInfo(getActivity().getPackageName(), 0);
                                    version = pi.versionName;
                                } catch (PackageManager.NameNotFoundException e) {
                                    e.printStackTrace();
                                }
                                new Thread(checkVersion).start();
                                break;
                           case ADDAPP:
                                startActivity(new Intent(getActivity(),AppList.class));
                                break;
                        }
                    } else {
                        //第三方app或者系统app
                        startActivity(currentAppInfo.getIntent());
                    }
                }
            });
            gv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    final MyApplicationInfo currentAppInfo = mApplications.get(position + finalI * NUMBER_PER_SCREEN);
                    System.out.println(currentAppInfo.getTitle() + "" + currentAppInfo.isSystemApp());
                    if (!currentAppInfo.isSystemApp()) {//是否为系统app
                        if (currentAppInfo.getType() == MyApplicationInfo.TYPE_APP) {//是否为第三方app
                            Uri currentAppUri = Uri.parse("package:" + currentAppInfo.packageName);
                            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, currentAppUri);
                            startActivity(uninstallIntent);
                        }
                    } else {
                        //显示应用信息
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", currentAppInfo.packageName, null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                    return true;
                }
            });
            gridViewList.add(gv);
        }
        applicationAdapter = new ApplicationAdapter(gridViewList);
        viewPager.setAdapter(applicationAdapter);
    }

    //Ftp对象
    Ftp mFtp;
    //版本信息下载监听器
    FtpListener Dversion = new FtpListener() {
        @Override
        public void onStateChange(String currentStep) {
            Log.i(TAG, currentStep);
        }

        @Override
        public void onUploadProgress(String currentStep, long uploadSize, File targetFile) {

        }

        @Override
        public void onDownLoadProgress(String currentStep, long downProcess, File targetFile) {
            if (currentStep.equals(Constant.FTP_DOWN_SUCCESS)) {
                Log.i(TAG, currentStep);
            } else if (currentStep.equals(Constant.FTP_DOWN_LOADING)) {
                Log.i(TAG, "-----下载---" + downProcess + "%");
            }
        }

        @Override
        public void onDeleteProgress(String currentStep) {

        }
    };
    //版本apk下载监听器
    FtpListener Dapk = new FtpListener() {
        @Override
        public void onStateChange(String currentStep) {

        }

        @Override
        public void onUploadProgress(String currentStep, long uploadSize, File targetFile) {

        }

        @Override
        public void onDownLoadProgress(String currentStep, long downProcess, File targetFile) {
            if (currentStep.equals(Constant.FTP_DOWN_SUCCESS)) {
                Log.d(TAG, currentStep);
                downloadDialog.dismiss();
                Message message = new Message();
                message.what = 0x0002;
                handler.sendMessage(message);
            }
            if (currentStep.equals(Constant.FTP_DOWN_LOADING)) {
                downloadDialog.setProgress((int) downProcess);
                Log.i(TAG, "-----下载---" + downProcess + "%");
            }
        }

        @Override
        public void onDeleteProgress(String currentStep) {

        }
    };
    Runnable checkVersion = new Runnable() {
        @Override
        public void run() {
            try {
                //下载版本信息xml文件
                mFtp.download("/apk/version_PNS6.xml", SdCard + "/PNS6/version/");
                File xml = new File(SdCard + "/PNS6/version/version_PNS6.xml");
                InputStream inputStream = new FileInputStream(xml);
                //解析xml文件
                versionHashMap = VersionXmlParse.parseXml(inputStream);
            } catch (Exception e) {
                result = e.getMessage();
            }
            //获取ftp上的版本号
            FtpVersion = versionHashMap.get("version");
            //根据result显示相应的对话框
            showVersionDialog(version, FtpVersion, result);
        }
    };

    private void showVersionDialog(String currentVersion, final String FtpVersion, final String result) {
        //取消进度条
        loading.dismiss();
        if (result.equals("Finished")) {
            Log.i(TAG, "当前版本为 " + version + "FTP上版本为 " + FtpVersion);
            if (!currentVersion.equals(FtpVersion)) {
                //版本不一致
                Message message = new Message();
                message.what = 0x0001;
                handler.sendMessage(message);
            } else {
                //版本一致
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                                .setTitle("当前为最新版本")
                                .setPositiveButton("确定", null)
                                .create();
                        dialog.setCanceledOnTouchOutside(false);
                        dialog.show();
                    }
                });
            }
        } else {
            //失败
            showTip(result);
        }
    }

    //下载完成
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0x0001:
                    AlertDialog dialog = new AlertDialog.Builder(getActivity())
                            .setTitle("有新版本")
                            .setMessage("当前版本为" + version + ",新版本为" + FtpVersion)
                            .setPositiveButton("下载并安装", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    downloadDialog = new ProgressDialog(getActivity());
                                    downloadDialog.setTitle("下载进度");
                                    downloadDialog.setMessage("已经下载了");
                                    downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                    downloadDialog.setCancelable(false);
                                    downloadDialog.setIndeterminate(false);
                                    downloadDialog.setMax(100);
                                    downloadDialog.show();
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            mFtp.setListener(Dapk);
                                            try {
                                                mFtp.download(versionHashMap.get("path"), apkPath);
                                            } catch (final Exception e) {
                                                downloadDialog.dismiss();
                                                showTip("网络连接失败,请检查网络或重试");
                                            }
                                        }
                                    }.start();
                                }
                            })
                            .setNegativeButton("取消", null).create();
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();
                    break;
                case 0x0002:
                    //apk文件路径
                    String localApkPath = apkPath + versionHashMap.get("name");
                    File file = new File(localApkPath);
                    if (file.exists()) {
                        Intent intent = new Intent();
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        //设置intent的Action属性
                        intent.setAction(Intent.ACTION_VIEW);
                        //设置intent的data和Type属性。
                        intent.setDataAndType(Uri.fromFile(file),
                                "application/vnd.android.package-archive");
                        //注销
                        SipInfo.sipUser.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipUser, SipInfo.user_to,
                                SipInfo.user_from, BodyFactory.createLogoutBody()));
                        SipInfo.sipDev.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipDev, SipInfo.dev_to,
                                SipInfo.dev_from, BodyFactory.createLogoutBody()));
                        //界面回到登录状态
                        ActivityCollector.finishToFirstView();
                        //跳转到安装界面
                        startActivity(intent);
                    }
                    break;
            }
            return true;
        }
    });

    private void showTip(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
