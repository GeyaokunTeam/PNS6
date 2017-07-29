package com.punuo.sys.app.xungeng.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.punuo.sys.app.R;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Author chzjy
 * Date 2016/12/19.
 */

public class DialFragment extends Fragment {
    @Bind(R.id.phoneNum)
    TextView phoneNum;
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
    @Bind(R.id.btn7)
    Button btn7;
    @Bind(R.id.btn8)
    Button btn8;
    @Bind(R.id.btn9)
    Button btn9;
    @Bind(R.id.btnClear)
    ImageButton btnClear;
    @Bind(R.id.btn0)
    Button btn0;
    @Bind(R.id.btnDel)
    ImageButton btnDel;

    private StringBuilder inputNum = new StringBuilder();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_dial, container, false);
        ButterKnife.bind(this, view);

        phoneNum.setText(inputNum);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @OnClick({R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnClear, R.id.btn0, R.id.btnDel})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn1:
                inputNum.append('1');
                break;
            case R.id.btn2:
                inputNum.append('2');
                break;
            case R.id.btn3:
                inputNum.append('3');
                break;
            case R.id.btn4:
                inputNum.append('4');
                break;
            case R.id.btn5:
                inputNum.append('5');
                break;
            case R.id.btn6:
                inputNum.append('6');
                break;
            case R.id.btn7:
                inputNum.append('7');
                break;
            case R.id.btn8:
                inputNum.append('8');
                break;
            case R.id.btn9:
                inputNum.append('9');
                break;
            case R.id.btnClear:
                inputNum.delete(0, inputNum.length());
                break;
            case R.id.btn0:
                inputNum.append('0');
                break;
            case R.id.btnDel:
                if (inputNum.length() > 0) {
                    inputNum.deleteCharAt(inputNum.length() - 1);
                }
                break;
            default:
                break;
        }
        if (inputNum.length() > 6) {
            inputNum.deleteCharAt(inputNum.length() - 1);
        }
        phoneNum.setText(inputNum);
    }

    public String getPhoneNum() {
        return inputNum.toString();
    }
}
