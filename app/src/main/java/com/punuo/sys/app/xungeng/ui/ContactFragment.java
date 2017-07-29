package com.punuo.sys.app.xungeng.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.punuo.sys.app.xungeng.adapter.ContactAdapter;
import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.model.Friend;

import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.punuo.sys.app.xungeng.sip.SipInfo.friendList;

/**
 * Author chzjy
 * Date 2016/12/19.
 */
public class ContactFragment extends Fragment {


    @Bind(R.id.title)
    TextView title;
    @Bind(R.id.contact_list)
    ExpandableListView contactList;

    private ContactAdapter contactAdapter;
    private Handler handler = new Handler();
    private String TAG = "ContactFragment";
    private int[] groupState;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact, container, false);
        ButterKnife.bind(this, view);
        title.setText("联系人");
        Set keyname = friendList.keySet();
        groupState = new int[keyname.size()];
        contactAdapter = new ContactAdapter(getActivity(), friendList);
        contactList.setAdapter(contactAdapter);
        view.setClickable(true);
        contactList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Friend friend = (Friend) contactAdapter.getChild(groupPosition, childPosition);
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("friend", friend);
                startActivity(intent);
                return false;
            }
        });
        contactList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                if (groupState[groupPosition] == 0) {
                    groupState[groupPosition] = 1;
                } else {
                    groupState[groupPosition] = 0;
                }
                return false;
            }
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    public void notifyFriendListChanged() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Set keyname = friendList.keySet();
                groupState = new int[keyname.size()];
                contactAdapter = new ContactAdapter(getActivity(), friendList);
                try {
                    contactList.setAdapter(contactAdapter);
                } catch (NullPointerException e) {
                    Log.e(TAG, "contactAdapter is null");
                }
                for (int i = 0; i < groupState.length; i++) {
                    if (groupState[i] == 1) {
                        contactList.expandGroup(i);
                    }
                }
            }
        });
    }
}
