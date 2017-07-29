package com.punuo.sys.app.xungeng.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.Toast;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.db.DatabaseInfo;
import com.punuo.sys.app.xungeng.db.MyDatabaseHelper;
import com.punuo.sys.app.xungeng.db.SQLiteManager;
import com.punuo.sys.app.xungeng.sip.BodyFactory;
import com.punuo.sys.app.xungeng.sip.KeepAlive;
import com.punuo.sys.app.xungeng.sip.SipDev;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;
import com.punuo.sys.app.xungeng.sip.SipUser;
import com.punuo.sys.app.xungeng.tools.ActivityCollector;
import com.punuo.sys.app.xungeng.view.CustomProgressDialog;

import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.address.SipURL;
import org.zoolu.sip.message.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Random;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.punuo.sys.app.xungeng.sip.SipInfo.localSdCard;
import static java.lang.Thread.sleep;

/***
 * Author chzjy
 * Date 2016/12/19.
 * 登录界面
 */

public class Login extends Activity implements View.OnTouchListener {


    @Bind(R.id.login)
    Button login;
    @Bind(R.id.root)
    LinearLayout root;
    @Bind(R.id.setting)
    Button setting;
    @Bind(R.id.username)
    EditText username;
    @Bind(R.id.password)
    EditText password;
    @Bind(R.id.main)
    RelativeLayout main;

    private String SdCard;
    //配置文件路径
    private String configPath;

    private Handler handler = new Handler();
    //检查配置是否存在窗口
    private AlertDialog configDialog;
    //修改配置窗口
    private AlertDialog editConfigDialog;
    //网络连接失败窗口
    private AlertDialog newWorkConnectedDialog;
    //账号不存在
    private AlertDialog accountNotExistDialog;
    //登陆超时
    private AlertDialog timeOutDialog;
    //密码错误次数
    private int errorTime = 0;
    //前一次的账号
    private String lastUserAccount;
    //注册等待窗口
    private CustomProgressDialog registering;
    private String TAG = getClass().getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCollector.addActivity(this);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);//控件绑定
        //初始化
        init();
    }

    //初始化
    private void init() {
        localSdCard = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PNS9/";
        SdCard = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PNS9";
        configPath = SdCard + "/config.properties";
        //创建根目录文件夹
        createDirs(SdCard);
        root.setOnTouchListener(this);
        addLayoutListener(main,login);
        isNetworkreachable();
        loadProperties();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isNetworkreachable();
        loadProperties();
        closeKeyboard(Login.this, getWindow().getDecorView());
    }
    private void addLayoutListener(final View main, final View scroll) {
        main.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect rect=new Rect();
                main.getWindowVisibleDisplayFrame(rect);//rect为输出参数,因此rect不允许为null
                int mainInvisibleHeight=main.getRootView().getHeight()-rect.bottom;
                if (mainInvisibleHeight>100){
                    int[] location=new int[2];
                    scroll.getLocationOnScreen(location);
                    int scrollHeight=(location[1]+scroll.getHeight()-rect.bottom);
                    main.scrollTo(0,scrollHeight);
                }else{
                    main.scrollTo(0,0);
                }

            }
        });
    }

    //检查网络是否连接
    public boolean isNetworkreachable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info == null) {
            SipInfo.isNetworkConnected = false;
        } else {
            SipInfo.isNetworkConnected = info.getState() == NetworkInfo.State.CONNECTED;
        }
        return SipInfo.isNetworkConnected;
    }

    private void loadProperties() {
        //读取配置文件
        Properties properties;
        File config = new File(configPath);
        if (config.exists()) {
            properties = loadConfig(configPath);
            if (properties != null) {
                //配置信息
                SipInfo.serverIp = properties.getProperty("serverIp");
                SipInfo.devId = properties.getProperty("devId");
                SipInfo.centerPhoneNumber = properties.getProperty("centerPhone");
            }
        } else {
            handler.post(configIsNotExist);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
        ButterKnife.unbind(this);//空间解绑
    }

    @Override
    public void onBackPressed() {
        //屏蔽返回键
    }

    @OnClick({R.id.setting, R.id.login})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.setting:
                Intent mIntent = new Intent(Settings.ACTION_SETTINGS);
                startActivity(mIntent);
                break;
            case R.id.login:
                Log.i(TAG, "isNetworkConnected = " + SipInfo.isNetworkConnected);
                if (SipInfo.isNetworkConnected) {
                    SipInfo.userAccount = username.getText().toString();
                    SipInfo.passWord = password.getText().toString();
                    if (TextUtils.isEmpty(SipInfo.userAccount)) {
                        showTip("账号不能为空");
                        break;
                    }
                    if (TextUtils.isEmpty(SipInfo.passWord)) {
                        showTip("密码不能为空");
                        break;
                    }
                    if (SipInfo.userAccount.equals("0000") && SipInfo.passWord.equals("0")) {
                        EditConfig();
                        break;
                    }
                    if (!SipInfo.userAccount.equals(lastUserAccount)) {
                        errorTime = 0;
                    }
                    beforeLogin();
                    registering = new CustomProgressDialog(Login.this);
                    registering.setCancelable(false);
                    registering.setCanceledOnTouchOutside(false);
                    registering.show();

                    new Thread(connecting).start();

                } else {
                    //弹出网络连接失败窗口
                    handler.post(networkConnectedFailed);
                }
                break;
        }
    }


    Runnable connecting = new Runnable() {
        @Override
        public void run() {
            try {
                int hostPort = new Random().nextInt(5000) + 2000;
                SipInfo.sipUser = new SipUser(null, hostPort, Login.this);
                Message register = SipMessageFactory.createRegisterRequest(
                        SipInfo.sipUser, SipInfo.user_to, SipInfo.user_from);
                SipInfo.sipUser.sendMessage(register);
                sleep(1000);
                for (int i = 0; i < 2; i++) {
                    if (!SipInfo.isAccountExist) {
                        //用户账号不存在
                        break;
                    }
                    if (SipInfo.passwordError) {
                        //密码错误
                        break;
                    }
                    if (!SipInfo.loginTimeout) {
                        //没有超时
                        break;
                    }
                    SipInfo.sipUser.sendMessage(register);
                    sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {

                if (!SipInfo.isAccountExist) {
                    registering.dismiss();
                    /**账号不存在提示*/
                    handler.post(accountNotExist);
                } else if (SipInfo.passwordError) {
                    //密码错误提示
                    registering.dismiss();
                    showDialogTip(errorTime++);
                    lastUserAccount = SipInfo.userAccount;
                } else if (SipInfo.loginTimeout) {
                    registering.dismiss();
                    //超时
                    handler.post(timeOut);
                } else {

                    if (SipInfo.userLogined) {
                        Log.i(TAG, "用户登录成功!");
                        //开启用户保活心跳包
                        SipInfo.keepUserAlive = new KeepAlive();
                        SipInfo.keepUserAlive.setType(0);
                        SipInfo.keepUserAlive.startThread();
                        //数据库
                        String dbPath = SipInfo.userId + ".db";
                        deleteDatabase(dbPath);
                        MyDatabaseHelper myDatabaseHelper = new MyDatabaseHelper(Login.this, dbPath, null, 1);
                        DatabaseInfo.sqLiteManager = new SQLiteManager(myDatabaseHelper);

                        SipInfo.applist.clear();
                        //请求服务器上的app列表
                        SipInfo.sipUser.sendMessage(SipMessageFactory.createSubscribeRequest(SipInfo.sipUser,
                                SipInfo.user_to, SipInfo.user_from, BodyFactory.createAppsQueryBody()));
                        //启动设备注册线程
                        new Thread(devConnecting).start();
                    }
                }
            }
        }
    };
    //设备注册线程
    private Runnable devConnecting = new Runnable() {
        @Override
        public void run() {
            try {
                int hostPort = new Random().nextInt(5000) + 2000;
                SipInfo.sipDev = new SipDev(Login.this, null, hostPort);//无网络时在主线程操作会报异常
                Message register = SipMessageFactory.createRegisterRequest(
                        SipInfo.sipDev, SipInfo.dev_to, SipInfo.dev_from);
                for (int i = 0; i < 3; i++) {//如果没有回应,最多重发2次
                    SipInfo.sipDev.sendMessage(register);
                    sleep(2000);
                    if (!SipInfo.dev_loginTimeout) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                registering.dismiss();
                if (SipInfo.devLogined) {
                    Log.d(TAG, "设备注册成功!");
                    Log.d(TAG, "设备心跳包发送!");
                    startActivity(new Intent(Login.this, Main.class));
                    //启动设备心跳线程
                    SipInfo.keepDevAlive = new KeepAlive();
                    SipInfo.keepDevAlive.setType(1);
                    SipInfo.keepDevAlive.startThread();
                } else {
                    Log.e(TAG, "设备注册失败!");
                }
            }
        }
    };

    private void showDialogTip(final int errorTime) {
        if (errorTime < 2) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    AlertDialog dialog = new AlertDialog.Builder(Login.this)
                            .setTitle("密码输入错误/还有" + (2 - errorTime) + "次输入机会")
                            .setPositiveButton("确定", null)
                            .create();
                    dialog.show();
                    dialog.setCanceledOnTouchOutside(false);
                }
            });
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    AlertDialog dialog = new AlertDialog.Builder(Login.this)
                            .setTitle("由于密码输入错误过多,该账号已被冻结")
                            .setPositiveButton("确定", null)//锁账号暂未完成
                            .create();
                    dialog.show();
                    dialog.setCanceledOnTouchOutside(false);
                    Toast.makeText(getApplicationContext(), "该账号已被冻结", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    //登录前的准备
    private void beforeLogin() {
        SipInfo.isAccountExist = true;
        SipInfo.passwordError = false;
        SipInfo.userLogined = false;
        SipInfo.loginTimeout = true;
        SipURL local = new SipURL(SipInfo.REGISTER_ID, SipInfo.serverIp, SipInfo.SERVER_PORT_USER);
        SipURL remote = new SipURL(SipInfo.SERVER_ID, SipInfo.serverIp, SipInfo.SERVER_PORT_USER);
        SipInfo.user_from = new NameAddress(SipInfo.userAccount, local);
        SipInfo.user_to = new NameAddress(SipInfo.SERVER_NAME, remote);
        SipInfo.devLogined = false;
        SipInfo.dev_loginTimeout = true;
        SipURL local_dev = new SipURL(SipInfo.devId, SipInfo.serverIp, SipInfo.SERVER_PORT_DEV);
        SipURL remote_dev = new SipURL(SipInfo.SERVER_ID, SipInfo.serverIp, SipInfo.SERVER_PORT_DEV);
        SipInfo.dev_from = new NameAddress(SipInfo.devId, local_dev);
        SipInfo.dev_to = new NameAddress(SipInfo.SERVER_NAME, remote_dev);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        root.requestFocus();
        username.clearFocus();
        password.clearFocus();
        closeKeyboard(Login.this, getWindow().getDecorView());
        return false;
    }

    /**
     * 强制关闭键盘
     */
    public void closeKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * 读取配置文件
     */
    private Properties loadConfig(String file) {
        Properties properties = new Properties();
        try {
            FileInputStream s = new FileInputStream(file);
            properties.load(s);
        } catch (Exception e) {
            return null;
        }
        return properties;
    }

    //配置文件存在与否窗口
    private Runnable configIsNotExist = new Runnable() {
        @Override
        public void run() {
            if (configDialog == null || !configDialog.isShowing()) {
                configDialog = new AlertDialog.Builder(Login.this)
                        .setPositiveButton("配置", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                EditConfig();
                            }
                        })
                        .setTitle("配置文件不存在,请配置!")
                        .create();
                configDialog.setCancelable(false);
                configDialog.setCanceledOnTouchOutside(false);
                configDialog.show();
            }
        }
    };
    //修改配置
    private Runnable EditConfig = new Runnable() {
        @Override
        public void run() {
            TableLayout changeSetting = (TableLayout) getLayoutInflater().inflate(R.layout.edit_config, null);
            final EditText serverIP = (EditText) changeSetting.findViewById(R.id.serverip);
            final EditText devID = (EditText) changeSetting.findViewById(R.id.devid);
            final EditText centerPhoneNum = (EditText) changeSetting.findViewById(R.id.centerNum);
            serverIP.setText(SipInfo.serverIp);
            devID.setText(SipInfo.devId);
            centerPhoneNum.setText(SipInfo.centerPhoneNumber);
            editConfigDialog = new AlertDialog.Builder(Login.this)
                    .setView(changeSetting)
                    .setNegativeButton("取消", null)
                    .setPositiveButton("修改", null)
                    .create();
            editConfigDialog.setCanceledOnTouchOutside(false);
            editConfigDialog.setCancelable(false);
            editConfigDialog.show();
            editConfigDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String ip = serverIP.getText().toString();
                    String devId = devID.getText().toString();
                    String centerPhoneNumber = centerPhoneNum.getText().toString();
                    if (ip.isEmpty()) {
                        Toast.makeText(Login.this, "服务器IP不能为空", Toast.LENGTH_SHORT).show();
                    } else if (devId.isEmpty()) {
                        Toast.makeText(Login.this, "设备ID不能为空", Toast.LENGTH_SHORT).show();
                    } else if (centerPhoneNumber.isEmpty()) {
                        Toast.makeText(Login.this, "中心号码不能为空", Toast.LENGTH_SHORT).show();
                    } else {
                        Properties config = loadConfig(configPath);
                        if (config == null) {
                            config = new Properties();
                        }
                        config.put("serverIp", ip);
                        config.put("devId", devId);
                        config.put("centerPhone", centerPhoneNumber);
                        SipInfo.serverIp = ip;
                        SipInfo.devId = devId;
                        SipInfo.centerPhoneNumber = centerPhoneNumber;
                        boolean isSucceed = saveConfig(configPath, config);
                        if (isSucceed) {
                            editConfigDialog.dismiss();
                            showTip("修改配置成功");
                        } else {
                            showTip("修改失败,请重试");
                        }
                    }
                }
            });
        }
    };
    // 网络是否连接
    private Runnable networkConnectedFailed = new Runnable() {
        @Override
        public void run() {
            if (newWorkConnectedDialog == null || !newWorkConnectedDialog.isShowing()) {
                newWorkConnectedDialog = new AlertDialog.Builder(Login.this)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Intent mIntent = new Intent(Settings.ACTION_SETTINGS);
                                startActivity(mIntent);
                            }
                        })
                        .setTitle("当前无网络,请检查网络连接")
                        .create();
                newWorkConnectedDialog.setCancelable(false);
                newWorkConnectedDialog.setCanceledOnTouchOutside(false);
                newWorkConnectedDialog.show();
            }
        }
    };
    //账号不存在
    private Runnable accountNotExist = new Runnable() {
        @Override
        public void run() {
            if (accountNotExistDialog == null || !accountNotExistDialog.isShowing()) {
                accountNotExistDialog = new AlertDialog.Builder(Login.this)
                        .setTitle("不存在该账号")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .create();
                accountNotExistDialog.show();
                accountNotExistDialog.setCancelable(false);
                accountNotExistDialog.setCanceledOnTouchOutside(false);
            }
        }
    };
    private Runnable timeOut = new Runnable() {
        @Override
        public void run() {
            if (timeOutDialog == null || !timeOutDialog.isShowing()) {
                timeOutDialog = new AlertDialog.Builder(Login.this)
                        .setTitle("连接超时,请检查网络")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .create();
                timeOutDialog.show();
                timeOutDialog.setCancelable(false);
                timeOutDialog.setCanceledOnTouchOutside(false);
            }
        }
    };

    // 编辑配置
    private void EditConfig() {
        handler.post(EditConfig);
    }

    /**
     * 保存配置文件
     */
    public boolean saveConfig(String configPath, Properties properties) {
        try {
            File config = new File(configPath);
            if (!config.exists())
                config.createNewFile();
            FileOutputStream s = new FileOutputStream(config);
            properties.store(s, "");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void showTip(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Login.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //创建文件夹
    private boolean createDirs(String dir) {
        try {
            File dirPath = new File(dir);
            if (!dirPath.exists()) {
                dirPath.mkdirs();
            }
        } catch (Exception e) {
            showTip(e.getMessage());
            return false;
        }
        return true;
    }

}

