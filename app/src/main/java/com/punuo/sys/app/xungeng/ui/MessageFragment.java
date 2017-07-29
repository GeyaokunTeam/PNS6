package com.punuo.sys.app.xungeng.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.punuo.sys.app.xungeng.db.DatabaseInfo;
import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.adapter.MessageAdapter;
import com.punuo.sys.app.xungeng.model.Friend;
import com.punuo.sys.app.xungeng.model.LastestMsg;
import com.punuo.sys.app.xungeng.model.Msg;
import com.punuo.sys.app.xungeng.model.MyFile;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipUser;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Author chzjy
 * Date 2016/12/19.
 */
public class MessageFragment extends Fragment implements SipUser.TotalListener {

    @Bind(R.id.title)
    TextView title;
    @Bind(R.id.message_list)
    ListView messageList;
    private MessageAdapter messageAdapter;
    private Handler handler = new Handler();

    @Override
    public void onResume() {
        super.onResume();
        SipInfo.lastestMsgs = DatabaseInfo.sqLiteManager.queryLastestMsg();
        messageAdapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message, container, false);
        ButterKnife.bind(this, view);
        title.setText("消息");
        /** 防止点击穿透，底层的fragment响应上层点击触摸事件 */
        view.setClickable(true);
        SipInfo.sipUser.setTotalListener(this);
        SipInfo.lastestMsgs = DatabaseInfo.sqLiteManager.queryLastestMsg();
        messageAdapter = new MessageAdapter(getActivity());
        messageList.setAdapter(messageAdapter);
        messageList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LastestMsg lastestMsgselected = SipInfo.lastestMsgs.get(position);
                if (lastestMsgselected.getType() == 0) {
                    Friend friend = new Friend();
                    friend.setUserId(lastestMsgselected.getId());
                    int index = SipInfo.friends.indexOf(friend);
                    if (index != -1) {
                        Friend f = SipInfo.friends.get(index);
                        Intent intent = new Intent(getActivity(), ChatActivity.class);
                        intent.putExtra("friend", f);
                        startActivity(intent);
                    }
                } else {
//                    Group group = new Group();
//                    group.setGroupid(lastestMsgselected.getId());
//                    int index = SipInfo.groupList.indexOf(group);
//                    if (index != -1) {
//                        Intent intent = new Intent(getActivity(), GroupActivity.class);
//                        intent.putExtra("position", index);
//                        startActivity(intent);
//                    }
                }
            }
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @Override
    public void onReceivedTotalMessage(Msg msg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                SipInfo.lastestMsgs = DatabaseInfo.sqLiteManager.queryLastestMsg();
                messageAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onReceivedTotalFileshare(MyFile myfile) {

    }
}
