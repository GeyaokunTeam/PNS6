package com.punuo.sys.app.xungeng.ui;


import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.punuo.sys.app.xungeng.adapter.FriendAdapter;
import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.sip.SipInfo;

import java.util.Collections;


public class FriendListFragment extends ListFragment {
    private FriendAdapter friendAdapter;
    private int lastPosition = -1;

    public FriendListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Collections.sort(SipInfo.friends);
        friendAdapter = new FriendAdapter(SipInfo.friends, getActivity());
        setListAdapter(friendAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends_list, container, false);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

    public int getLastPosition() {
        return lastPosition;
    }

    public void notifyFriendListChanged() {
        Collections.sort(SipInfo.friends);
        friendAdapter = new FriendAdapter( SipInfo.friends,getActivity());
        setListAdapter(friendAdapter);
    }
}
