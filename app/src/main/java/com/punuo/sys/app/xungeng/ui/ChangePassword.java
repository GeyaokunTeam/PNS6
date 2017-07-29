package com.punuo.sys.app.xungeng.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.sip.BodyFactory;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;
import com.punuo.sys.app.xungeng.sip.SipUser;
import com.punuo.sys.app.xungeng.tools.SHA1;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Author chzjy
 * Date 2016/12/19.
 */

public class ChangePassword extends Activity implements SipUser.ChangePWDListener {
    @Bind(R.id.title)
    TextView title;
    @Bind(R.id.old_password)
    EditText oldPassword;
    @Bind(R.id.new_password)
    EditText newPassword;
    @Bind(R.id.confirm_new_password)
    EditText confirmNewPassword;
    @Bind(R.id.change)
    Button change;

    Handler handler = new Handler();
    private String newPwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_changepassword);
        ButterKnife.bind(this);
        SipInfo.sipUser.setChangePWDListener(this);
        title.setText("修改密码");
    }

    @OnClick(R.id.change)
    public void onClick() {
        String oldPwd = oldPassword.getText().toString();
        newPwd = newPassword.getText().toString();
        String confrimNewPwd = confirmNewPassword.getText().toString();
        if (TextUtils.isEmpty(oldPwd)) {
            Toast.makeText(this, "旧密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(newPwd)) {
            Toast.makeText(this, "新密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(confrimNewPwd)) {
            Toast.makeText(this, "请再次输入新密码", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!oldPwd.equals(SipInfo.passWord)) {
            Toast.makeText(this, "旧密码输入错误", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPwd.equals(confrimNewPwd)) {
            Toast.makeText(this, "两个密码不一样", Toast.LENGTH_SHORT).show();
            return;
        }
        SHA1 sha1 = SHA1.getInstance();
        String oldpassword = sha1.hashData(SipInfo.salt + oldPwd);
        String newpassword = sha1.hashData(SipInfo.salt + newPwd);
        SipInfo.sipUser.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipUser,
                SipInfo.user_to, SipInfo.user_from, BodyFactory.cretePasswordChange(oldpassword, newpassword)));
    }

    @Override
    public void onChangePWD(int i) {
        if (i == 1) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "密码修改成功", Toast.LENGTH_SHORT).show();
                }
            });
            //重置当前密码
            SipInfo.passWord = newPwd;
            finish();
        } else if (i == 0) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "密码修改失败,请重试!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
