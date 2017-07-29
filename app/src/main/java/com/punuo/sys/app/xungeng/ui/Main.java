package com.punuo.sys.app.xungeng.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.KeyboardShortcutGroup;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.punuo.sys.app.xungeng.db.DatabaseInfo;
import com.punuo.sys.app.xungeng.groupvoice.GroupInfo;
import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.model.Constant;
import com.punuo.sys.app.xungeng.model.Msg;
import com.punuo.sys.app.xungeng.model.MyFile;
import com.punuo.sys.app.xungeng.service.BinderPoolService;
import com.punuo.sys.app.xungeng.service.NewsService;
import com.punuo.sys.app.xungeng.service.PTTService;
import com.punuo.sys.app.xungeng.service.SipService;
import com.punuo.sys.app.xungeng.sip.BodyFactory;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;
import com.punuo.sys.app.xungeng.sip.SipUser;
import com.punuo.sys.app.xungeng.tools.ActivityCollector;

import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.address.SipURL;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnTouch;

import static com.punuo.sys.app.xungeng.sip.SipInfo.sipDev;
import static com.punuo.sys.app.xungeng.sip.SipInfo.sipUser;

/**
 * Author chzjy
 * Date 2016/12/19.
 * 主界面
 */

public class Main extends Activity implements View.OnClickListener, SipUser.LoginNotifyListener, SipUser.BottomListener {
    private final String TAG = getClass().getSimpleName();
    @Bind(R.id.network_layout)
    LinearLayout networkLayout;
    @Bind(R.id.content_frame)
    FrameLayout contentFrame;
    @Bind(R.id.message)
    ImageButton message;
    @Bind(R.id.message_text)
    TextView messageText;
    @Bind(R.id.contacts)
    ImageButton contacts;
    @Bind(R.id.contacts_text)
    TextView contactsText;
    @Bind(R.id.menu)
    ImageButton menu;
    @Bind(R.id.menu_text)
    TextView menuText;
    @Bind(R.id.phone)
    ImageButton phone;
    @Bind(R.id.phone_text)
    TextView phoneText;
    @Bind(R.id.video)
    ImageButton video;
    @Bind(R.id.video_text)
    TextView videoText;
    @Bind(R.id.menu_layout)
    LinearLayout menuLayout;
    @Bind(R.id.count)
    TextView messageCount;


    private FragmentManager fm;
    private FragmentTransaction ft;
    //菜单界面
    private MenuFragment menuFragment;
    //视频浏览界面
    private VideoFragment videoFragment;
    //聊天界面
    private MessageFragment messageFragment;
    //语音呼叫界面
    private AudioFragment audioFragment;
    //联系人界面
    private ContactFragment contactFragment;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActivityCollector.addActivity(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        init();
    }




    @Override
    protected void onResume() {
        super.onResume();
        setButtonType(Constant.SAVE_FRAGMENT_SELECT_STATE);
        SipInfo.lastestMsgs = DatabaseInfo.sqLiteManager.queryLastestMsg();
        SipInfo.messageCount = 0;
        for (int i = 0; i < SipInfo.lastestMsgs.size(); i++) {
            if (SipInfo.lastestMsgs.get(i).getType() == 0) {
                SipInfo.messageCount += SipInfo.lastestMsgs.get(i).getNewMsgCount();
            }
        }
        if (SipInfo.messageCount != 0) {
            messageCount.setVisibility(View.VISIBLE);
            messageCount.setText(String.valueOf(SipInfo.messageCount));
        } else {
            messageCount.setVisibility(View.INVISIBLE);
        }

    }

    private void init() {
        fm = getFragmentManager();


        setButtonType(Constant.PHONE);
        setButtonType(Constant.CONTACT);
        setButtonType(Constant.VIDEO);
        setButtonType(Constant.MESSAGE);
        setButtonType(Constant.MENU);
        setButtonType(Constant.SAVE_FRAGMENT_SELECT_STATE);

        message.setOnClickListener(this);
        contacts.setOnClickListener(this);
        menu.setOnClickListener(this);
        video.setOnClickListener(this);
        phone.setOnClickListener(this);

        sipUser.setLoginNotifyListener(this);
        sipUser.setBottomListener(this);
        //启动语音电话服务
        startService(new Intent(Main.this, SipService.class));
        //启动监听服务
        startService(new Intent(this, NewsService.class));
        //启动aidl接口服务
        startService(new Intent(this, BinderPoolService.class));
        SipInfo.loginReplace = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                sipUser.sendMessage(SipMessageFactory.createNotifyRequest(sipUser, SipInfo.user_to,
                        SipInfo.user_from, BodyFactory.createLogoutBody()));
                SipInfo.sipDev.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipDev, SipInfo.dev_to,
                        SipInfo.dev_from, BodyFactory.createLogoutBody()));
                //关闭语音电话服务
                stopService(new Intent(Main.this, SipService.class));
                //关闭监听服务
                stopService(new Intent(Main.this, NewsService.class));
                //关闭PTT监听服务
                stopService(new Intent(Main.this, PTTService.class));
                //关闭aidl接口服务
                stopService(new Intent(Main.this, BinderPoolService.class));
                //关闭用户心跳
                SipInfo.keepUserAlive.stopThread();
                //关闭设备心跳
                SipInfo.keepDevAlive.stopThread();
                //重置登录状态
                SipInfo.userLogined = false;
                SipInfo.devLogined = false;
                //关闭集群呼叫
                GroupInfo.rtpAudio.removeParticipant();
                GroupInfo.groupUdpThread.stopThread();
                GroupInfo.groupKeepAlive.stopThread();
                AlertDialog loginReplace = new AlertDialog.Builder(getApplicationContext())
                        .setTitle("账号异地登录")
                        .setMessage("请重新登录")
                        .setPositiveButton("确定", null)
                        .create();
                loginReplace.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                loginReplace.show();
                loginReplace.setCancelable(false);
                loginReplace.setCanceledOnTouchOutside(false);
                startActivity(new Intent(getApplicationContext(), Login.class));
                super.handleMessage(msg);
            }
        };
    }

    /**
     * 更改设置底部按钮样式
     */
    public void setButtonType(int id) {
        reSetButtonType();
        Constant.SAVE_FRAGMENT_SELECT_STATE = id;
        int color = getResources().getColor(R.color.select);
        switch (id) {
            case Constant.MESSAGE:
                message.setImageResource(R.drawable.icon_message_pressed);
                messageText.setTextColor(color);
                showFragment(Constant.MESSAGE);
                break;
            case Constant.CONTACT:
                contacts.setImageResource(R.drawable.icon_contact_pressed);
                contactsText.setTextColor(color);
                showFragment(Constant.CONTACT);
                break;
            case Constant.PHONE:
                phone.setImageResource(R.drawable.icon_phone_pressed);
                phoneText.setTextColor(color);
                showFragment(Constant.PHONE);
                break;
            case Constant.VIDEO:
                video.setImageResource(R.drawable.icon_video_pressed);
                videoText.setTextColor(color);
                showFragment(Constant.VIDEO);
                break;
            case Constant.MENU:
                menu.setImageResource(R.drawable.icon_menu_pressed);
                menuText.setTextColor(color);
                showFragment(Constant.MENU);
                break;
        }
    }

    /**
     * 重置底部按钮样式
     */
    public void reSetButtonType() {
        message.setImageResource(R.drawable.icon_message_normal);
        messageText.setTextColor(Color.WHITE);
        contacts.setImageResource(R.drawable.icon_contact_normal);
        contactsText.setTextColor(Color.WHITE);
        phone.setImageResource(R.drawable.icon_phone_normal);
        phoneText.setTextColor(Color.WHITE);
        video.setImageResource(R.drawable.icon_video_normal);
        videoText.setTextColor(Color.WHITE);
        menu.setImageResource(R.drawable.icon_menu_normal);
        menuText.setTextColor(Color.WHITE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
        ButterKnife.unbind(this);
        SipInfo.keepUserAlive.stopThread();
        SipInfo.keepDevAlive.stopThread();
        //关闭集群呼叫
        GroupInfo.wakeLock.release();
        GroupInfo.rtpAudio.removeParticipant();
        GroupInfo.groupUdpThread.stopThread();
        GroupInfo.groupKeepAlive.stopThread();
        SipInfo.userLogined = false;
        SipInfo.devLogined = false;
        SipInfo.loginReplace = null;
        //停止语音电话服务
        stopService(new Intent(Main.this, SipService.class));
        //关闭监听服务
        stopService(new Intent(Main.this, NewsService.class));
        //停止PPT监听服务
        stopService(new Intent(this, PTTService.class));
        //停止aidl接口服务
        stopService(new Intent(Main.this, BinderPoolService.class));
        sipUser.setLoginNotifyListener(null);
        sipUser.setBottomListener(null);
        //关闭线程池
        sipUser.shutdown();
        sipDev.shutdown();
        //关闭监听线程
        sipUser.halt();
        sipDev.halt();
        System.gc();
    }

    /**
     * 显示Fragment
     */
    public void showFragment(int index) {
        ft = fm.beginTransaction();
        hideFragment(ft);
        switch (index) {
            case Constant.MESSAGE:
                if (messageFragment != null)
                    ft.show(messageFragment);
                else {
                    messageFragment = new MessageFragment();
                    ft.add(R.id.content_frame, messageFragment);
                }
                break;
            case Constant.CONTACT:
                if (contactFragment != null) {
                    ft.show(contactFragment);
                } else {
                    contactFragment = new ContactFragment();
                    ft.add(R.id.content_frame, contactFragment);
                }
                break;
            case Constant.MENU:
                if (menuFragment != null)
                    ft.show(menuFragment);
                else {
                    menuFragment = new MenuFragment();
                    ft.add(R.id.content_frame, menuFragment);
                }
                menuLayout.setVisibility(View.VISIBLE);
                break;
            case Constant.PHONE:
                if (audioFragment != null)
                    ft.show(audioFragment);
                else {
                    audioFragment = new AudioFragment();
                    ft.add(R.id.content_frame, audioFragment);
                }
                break;
            case Constant.VIDEO:
                if (videoFragment != null)
                    ft.show(videoFragment);
                else {
                    videoFragment = new VideoFragment();
                    ft.add(R.id.content_frame, videoFragment);
                }
                break;
        }
        ft.commitAllowingStateLoss();
    }

    /**
     * 隐藏Fragment
     */
    public void hideFragment(FragmentTransaction ft) {
        if (messageFragment != null) {
            if (Constant.SAVE_FRAGMENT_SELECT_STATE != Constant.MESSAGE) {
                ft.hide(messageFragment);
            }
        }
        if (contactFragment != null) {
            if (Constant.SAVE_FRAGMENT_SELECT_STATE != Constant.CONTACT) {
                ft.hide(contactFragment);
            }
        }
        if (menuFragment != null) {
            if (Constant.SAVE_FRAGMENT_SELECT_STATE != Constant.MENU) {
                ft.hide(menuFragment);
            }
        }

        if (audioFragment != null) {
            if (Constant.SAVE_FRAGMENT_SELECT_STATE != Constant.PHONE) {
                ft.hide(audioFragment);
            }
        }
        if (videoFragment != null) {
            if (Constant.SAVE_FRAGMENT_SELECT_STATE != Constant.VIDEO) {
                ft.hide(videoFragment);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.message:
                setButtonType(Constant.MESSAGE);
                break;
            case R.id.contacts:
                setButtonType(Constant.CONTACT);
                break;
            case R.id.phone:
                setButtonType(Constant.PHONE);
                break;
            case R.id.video:
                setButtonType(Constant.VIDEO);
                break;
            case R.id.menu:
                setButtonType(Constant.MENU);
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 82) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage("注销账户?")
                    .setNegativeButton("否", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton("是", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sipUser.sendMessage(SipMessageFactory.createNotifyRequest(sipUser, SipInfo.user_to,
                                    SipInfo.user_from, BodyFactory.createLogoutBody()));
                            SipInfo.sipDev.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipDev, SipInfo.dev_to,
                                    SipInfo.dev_from, BodyFactory.createLogoutBody()));
                            dialog.dismiss();
                            ActivityCollector.finishToFirstView();
                        }
                    }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            return true;
        }
        if (keyCode == 4) {
            setButtonType(Constant.MENU);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void onDevNotify() {
        videoFragment.devNotify();
    }

    @Override
    public void onUserNotify() {
        audioFragment.userNotify();
        contactFragment.notifyFriendListChanged();
    }

    @Override
    public void onReceivedBottomMessage(Msg msg) {
        SipInfo.messageCount++;
        handler.post(new Runnable() {
            @Override
            public void run() {
                messageCount.setVisibility(View.VISIBLE);
                messageCount.setText(String.valueOf(SipInfo.messageCount));
            }
        });
    }

    @Override
    public void onReceivedBottomFileshare(MyFile myfile) {

    }

}
