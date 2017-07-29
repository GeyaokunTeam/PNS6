package com.punuo.sys.app.xungeng.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.model.Friend;
import com.punuo.sys.app.xungeng.ui.PhoneCall;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Author chzjy
 * Date 2016/12/19.
 */

public class FriendAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<Friend> list;

    public FriendAdapter(ArrayList<Friend> list, Context mContext) {
        this.list = list;
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.fragment_friendlist, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (list.get(position).isLive()) {
            holder.devIcon.setImageResource(R.drawable.icon_online);
            holder.check.setVisibility(View.VISIBLE);
            holder.check.setImageResource(R.drawable.icon_btncall);
        } else {
            holder.devIcon.setImageResource(R.drawable.icon_offline);
            holder.check.setVisibility(View.GONE);
        }
        holder.devName.setText(list.get(position).getRealName());
        holder.check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNum = list.get(position).getPhoneNum();
                PhoneCall.actionStart(mContext, phoneNum, 1);
            }
        });
        return convertView;
    }

    static class ViewHolder {
        @Bind(R.id.devIcon)
        ImageView devIcon;
        @Bind(R.id.devName)
        TextView devName;
        @Bind(R.id.check)
        ImageView check;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
