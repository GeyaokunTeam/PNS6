package com.punuo.sys.app.xungeng.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.adapter.DevAdapter;
import com.punuo.sys.app.xungeng.sip.BodyFactory;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;

import java.util.Collections;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.punuo.sys.app.xungeng.sip.SipInfo.devCount;
import static com.punuo.sys.app.xungeng.sip.SipInfo.devList;
import static com.punuo.sys.app.xungeng.sip.SipInfo.sipUser;
import static com.punuo.sys.app.xungeng.sip.SipInfo.user_from;
import static com.punuo.sys.app.xungeng.sip.SipInfo.user_to;

/**
 * Author chzjy
 * Date 2016/12/19.
 */

public class VideoFragment extends Fragment {
    @Bind(R.id.title)
    TextView title;
    @Bind(R.id.video_list)
    ListView videoList;

    public Handler handler = new Handler();

    private String TAG = "Video";

    private DevAdapter devAdapter;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);
        ButterKnife.bind(this, view);
        title.setText("视频浏览");
        view.setClickable(true);
        title.setText("视频浏览("+ SipInfo.devName+")");
        devCount = 0;
        devList.clear();
        /*请求设备列表*/
        sipUser.sendMessage(SipMessageFactory.createSubscribeRequest(
                sipUser, user_to, user_from, BodyFactory.createDevsQueryBody()));
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    public void devNotify() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Collections.sort(devList);
                devAdapter = new DevAdapter(getActivity(), devList);
                videoList.setAdapter(devAdapter);
                devAdapter.notifyDataSetChanged();
            }
        });
    }
}
