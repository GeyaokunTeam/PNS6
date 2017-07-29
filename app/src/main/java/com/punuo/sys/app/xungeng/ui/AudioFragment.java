package com.punuo.sys.app.xungeng.ui;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.sip.SipInfo;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.punuo.sys.app.xungeng.sip.SipInfo.userPhoneNumber;

/**
 * Author chzjy
 * Date 2016/12/19.
 */
public class AudioFragment extends Fragment {
    @Bind(R.id.title)
    TextView title;
    @Bind(R.id.iv1)
    ImageView iv1;
    @Bind(R.id.iv2)
    ImageView iv2;
    @Bind(R.id.tv1)
    TextView tv1;
    @Bind(R.id.tv2)
    TextView tv2;
    @Bind(R.id.btnCall)
    ImageButton btnCall;

    Main main;

    private DialFragment dialFragment;

    private FriendListFragment friendListFragment;

    private String TAG = "AudioFragment";

    private Handler handler = new Handler();

    private FragmentManager fm;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio, container, false);
        ButterKnife.bind(this, view);
        view.setClickable(true);
        title.setText("语音电话(" + userPhoneNumber + ")");
        initView();
        dialFragment = new DialFragment();
        friendListFragment = new FriendListFragment();
        fm = getFragmentManager();
        fm.beginTransaction()
                .add(R.id.frame, friendListFragment)
                .commitAllowingStateLoss();
        return view;
    }

    @TargetApi(22)
    private void initView() {
        Drawable dr1 = getResources().getDrawable(R.drawable.icon_list, null);
        Drawable dr2 = getResources().getDrawable(R.drawable.icon_phone, null);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density;//屏幕密度
        int len = (int) (19 * density);
        dr1.setBounds(0, 0, len, len);
        dr2.setBounds(0, 0, len, len);
        tv1.setCompoundDrawables(dr1, null, null, null);
        tv2.setCompoundDrawables(dr2, null, null, null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @OnClick({R.id.tv1, R.id.tv2, R.id.btnCall})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv1:
                if (iv1.getVisibility() == View.INVISIBLE && iv2.getVisibility() == View.VISIBLE) {
                    iv1.setVisibility(View.VISIBLE);
                    iv2.setVisibility(View.INVISIBLE);
                    btnCall.setVisibility(View.GONE);
                    fm.beginTransaction()
                            .replace(R.id.frame, friendListFragment)
                            .commitAllowingStateLoss();
                }
                break;
            case R.id.tv2:
                if (iv1.getVisibility() == View.VISIBLE && iv2.getVisibility() == View.INVISIBLE) {
                    iv1.setVisibility(View.INVISIBLE);
                    iv2.setVisibility(View.VISIBLE);
                    btnCall.setVisibility(View.VISIBLE);
                    fm.beginTransaction()
                            .replace(R.id.frame, dialFragment)
                            .commitAllowingStateLoss();
                }
                break;
            case R.id.btnCall:
                if (iv1.getVisibility() == View.INVISIBLE && iv2.getVisibility() == View.VISIBLE) {
                    String phoneNum = dialFragment.getPhoneNum();
                    if (TextUtils.isEmpty(phoneNum)) {
                        Toast.makeText(getActivity(), "请输入您要拨打语音号码", Toast.LENGTH_SHORT).show();
                    } else {
                        if (phoneNum.equals(SipInfo.userPhoneNumber)) {
                            Toast.makeText(getActivity(), "不能拨打自己的号码", Toast.LENGTH_SHORT).show();
                        } else {
                            PhoneCall.actionStart(getActivity(), phoneNum, 1);
                        }
                    }
                }
                break;
        }
    }

    public void userNotify() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                friendListFragment.notifyFriendListChanged();
            }
        });
    }
}
