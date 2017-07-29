package com.punuo.sys.app.xungeng.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.punuo.sys.app.xungeng.groupvoice.GroupInfo;
import com.punuo.sys.app.xungeng.groupvoice.GroupKeepAlive;
import com.punuo.sys.app.xungeng.groupvoice.GroupUdpThread;
import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.tools.MyToast;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.punuo.sys.app.xungeng.sip.SipInfo.newMail;
import static com.punuo.sys.app.xungeng.sip.SipInfo.serverIp;

/**
 * Author chzjy
 * Date 2016/12/19.
 * 集群呼叫频道更换
 */
public class ChsChange extends Activity {
    @Bind(R.id.btnCall)
    ImageButton btnCall;
    @Bind(R.id.title)
    TextView title;
    @Bind(R.id.btn1)
    Button btn1;
    @Bind(R.id.btn2)
    Button btn2;
    @Bind(R.id.btn3)
    Button btn3;
    @Bind(R.id.btn4)
    Button btn4;
    @Bind(R.id.btn5)
    Button btn5;
    @Bind(R.id.btn6)
    Button btn6;

    public Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        GroupInfo.rtpAudio.changeParticipant(serverIp, GroupInfo.port);
                        GroupInfo.groupUdpThread = new GroupUdpThread(serverIp, GroupInfo.port);
                        GroupInfo.groupUdpThread.startThread();
                        GroupInfo.groupKeepAlive = new GroupKeepAlive();
                        GroupInfo.groupKeepAlive.startThread();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                title.setText("频道更换(当前:频道" + (GroupInfo.port % 7000 + 1) + ")");
                                MyToast.show(ChsChange.this, "频道切换至" + (GroupInfo.port % 7000 + 1), Toast.LENGTH_SHORT);
                            }
                        });
                    }
                }
            }.start();
            return true;
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chschange);
        ButterKnife.bind(this);
        title.setText("频道更换(当前:频道" + (GroupInfo.port % 7000 + 1) + ")");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }

    @OnClick({R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6})
    public void onClick(View view) {
        GroupInfo.groupUdpThread.stopThread();
        GroupInfo.groupKeepAlive.stopThread();
        switch (view.getId()) {
            case R.id.btn1:
                GroupInfo.port = 7000;
                break;
            case R.id.btn2:
                GroupInfo.port = 7001;
                break;
            case R.id.btn3:
                GroupInfo.port = 7002;
                break;
            case R.id.btn4:
                GroupInfo.port = 7003;
                break;
            case R.id.btn5:
                GroupInfo.port = 7004;
                break;
            case R.id.btn6:
                GroupInfo.port = 7005;
                break;
        }
        handler.sendEmptyMessage(0x1111);
    }
}
