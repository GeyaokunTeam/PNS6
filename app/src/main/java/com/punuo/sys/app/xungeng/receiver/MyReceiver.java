package com.punuo.sys.app.xungeng.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.punuo.sys.app.xungeng.groupvoice.GroupInfo;
import com.punuo.sys.app.xungeng.groupvoice.GroupSignaling;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.tools.MyToast;
import com.punuo.sys.app.xungeng.ui.MovieRecord;
import com.punuo.sys.app.xungeng.ui.MyCamera;
import com.punuo.sys.app.xungeng.video.H264Sending;
import com.punuo.sys.app.xungeng.video.VideoInfo;


public class MyReceiver extends BroadcastReceiver {
    public MyReceiver() {
    }

    private static final String ACTION_PTT_DOWN = "com.chivin.action.MEDIA_PTT_DOWN";
    private static final String ACTION_PTT_UP = "com.chivin.action.MEDIA_PTT_UP";
    private static final String ACTION_FN_DOWN = "com.android.action.PTT1_DOWN";
    private static final String ACTION_FN_UP = "com.android.action.PTT1_UP";
    private static final String ACTION_SOS = "android.intent.action.kkst.SOS";
    private static final String TAG = "PTTReceiver";

    @Override
    public synchronized void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        String action = intent.getAction();
        Log.d(TAG, "onReceive: " + action);
        if (action.equals(ACTION_FN_UP)) {
            Log.d(TAG, "SOS键弹起");
        } else if (action.equals(ACTION_FN_DOWN)) {
            Log.d(TAG, "SOS键按下");
//            PhoneCall.actionStart(context, SipInfo.centerPhone, 1);
            Intent intent1 = new Intent(context, MovieRecord.class);
            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent1);
        }
        if (action.equals(ACTION_PTT_DOWN)) {
            Log.i(TAG, "onReceive: " + "PTT按下");
            MyToast.show(context, "正在说话...", Toast.LENGTH_LONG);
            if (GroupInfo.rtpAudio != null) {
                GroupInfo.rtpAudio.pttChanged(true);
                GroupSignaling groupSignaling = new GroupSignaling();
                groupSignaling.setStart(SipInfo.devId);
                groupSignaling.setLevel(GroupInfo.level);
                String start = JSON.toJSONString(groupSignaling);
                GroupInfo.groupUdpThread.sendMsg(start.getBytes());

            }
        }
        if (action.equals(ACTION_PTT_UP)) {
            Log.i(TAG, "onReceive: " + "PTT弹起");
            MyToast.show(context, "结束说话...", Toast.LENGTH_LONG);
            if (GroupInfo.rtpAudio != null) {
                GroupInfo.rtpAudio.pttChanged(false);
                if (GroupInfo.isSpeak) {
                    GroupSignaling groupSignaling = new GroupSignaling();
                    groupSignaling.setEnd(SipInfo.devId);
                    String end = JSON.toJSONString(groupSignaling);
                    GroupInfo.groupUdpThread.sendMsg(end.getBytes());
                }
            }
        }
    }
}
