package com.punuo.sys.app.xungeng.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.punuo.sys.app.xungeng.receiver.MyReceiver;


/**
 * Created by acer on 2016/7/1.
 */
public class PTTService extends Service {
    private static final String TAG = "PTTService";
    //    private static final String ACTION_PTT_DOWN = "android.intent.action.PTT.down";
//    private static final String ACTION_PTT_UP = "android.intent.action.PTT.up";
    private static final String ACTION_PTT_DOWN = "com.chivin.action.MEDIA_PTT_DOWN";
    private static final String ACTION_PTT_UP = "com.chivin.action.MEDIA_PTT_UP";
    private static final String ACTION_FN_DOWN = "com.android.action.PTT1_DOWN";
    private static final String ACTION_FN_UP = "com.android.action.PTT1_UP";
    private static final String ACTION_SOS = "android.intent.action.kkst.SOS";
    private MyReceiver pttReceiver1;
    private MyReceiver pttReceiver2;
    private MyReceiver pttReceiver3;
    private MyReceiver pttReceiver4;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerPTTReceiver(this);
        return START_STICKY;
    }

    private void registerPTTReceiver(Context context) {
        Log.d(TAG, "registerPTTReceiver");
        pttReceiver1 = new MyReceiver();
        pttReceiver2 = new MyReceiver();
        pttReceiver3 = new MyReceiver();
        pttReceiver4 = new MyReceiver();
        IntentFilter PttFilter_Down = new IntentFilter(ACTION_PTT_DOWN);
        IntentFilter PttFilter_Up = new IntentFilter(ACTION_PTT_UP);
        context.registerReceiver(pttReceiver1, PttFilter_Down);
        context.registerReceiver(pttReceiver2, PttFilter_Up);
        context.registerReceiver(pttReceiver3, new IntentFilter(ACTION_FN_DOWN));
        context.registerReceiver(pttReceiver4, new IntentFilter(ACTION_FN_UP));
    }

    private void unregisterPTTReceiver(Context context) {
        Log.d(TAG, "unregisterPTTReceiver");
        if (null != pttReceiver1) {
            context.unregisterReceiver(pttReceiver1);
        }
        if (null != pttReceiver2) {
            context.unregisterReceiver(pttReceiver2);
        }
        if (pttReceiver3 != null) {
            context.unregisterReceiver(pttReceiver3);
        }
        if (pttReceiver4 != null) {
            context.unregisterReceiver(pttReceiver4);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterPTTReceiver(this);
    }
}
