package com.punuo.sys.app.xungeng.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.punuo.sys.app.xungeng.audiorecord.AudioRecorderButton;
import com.punuo.sys.app.xungeng.db.DatabaseInfo;
import com.punuo.sys.app.xungeng.ftp.Ftp;
import com.punuo.sys.app.xungeng.ftp.FtpListener;
import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.adapter.MsgAdapter;
import com.punuo.sys.app.xungeng.model.FileInfo;
import com.punuo.sys.app.xungeng.model.Friend;
import com.punuo.sys.app.xungeng.model.Msg;
import com.punuo.sys.app.xungeng.model.MyFile;
import com.punuo.sys.app.xungeng.sip.BodyFactory;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;
import com.punuo.sys.app.xungeng.sip.SipUser;
import com.punuo.sys.app.xungeng.tools.MD5Util;
import com.punuo.sys.app.xungeng.model.Constant;
import com.tb.emoji.Emoji;
import com.tb.emoji.EmojiUtil;
import com.tb.emoji.FaceFragment;

import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.address.SipURL;
import org.zoolu.sip.message.Message;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Author chzjy
 * Date 2016/12/19.
 */
public class ChatActivity extends Activity implements FaceFragment.OnEmojiClickListener, SipUser.MessageListener {
    @Bind(R.id.btnCall)
    ImageButton btnCall;
    @Bind(R.id.title)
    TextView title;
    @Bind(R.id.content_list)
    ListView contentList;
    @Bind(R.id.input_type)
    ImageButton inputType;
    @Bind(R.id.content)
    EditText content;
    @Bind(R.id.recorder_button)
    AudioRecorderButton recorderButton;
    @Bind(R.id.express)
    ImageButton express;
    @Bind(R.id.send)
    Button send;
    @Bind(R.id.addmore)
    ImageButton addmore;
    @Bind(R.id.expresslayout)
    FrameLayout expresslayout;
    @Bind(R.id.addfolder)
    ImageButton addfolder;
    @Bind(R.id.small_video)
    ImageButton smallVideo;
    @Bind(R.id.pic)
    ImageButton pic;
    @Bind(R.id.loc)
    ImageButton loc;
    @Bind(R.id.more)
    RelativeLayout more;
    private static String TAG = "ChatActivity";
    //表情界面
    FaceFragment faceFragment = FaceFragment.Instance();
    //当前聊天的好友对象
    private Friend currentFriend;
    //当前聊天好友id
    private String currentFriendId;

    private boolean inputStyle;//true语音按钮显示,false语音按钮消失
    private boolean anState;//true更多选项框显示,false更多选项框消失
    private boolean expState;//true表情选择框显示,false表情选择框消失
    //文件选择
    private final int FILE_CHOOSE = 1;
    //录制小视频
    private final int MAKE_SMALL_VIDEO = 3;
    //拍照
    private final int TAKE_PHOTO = 4;
    //选择地理位置
    private final int CHOOSE_LOCATION = 5;
    //选择照片
    private final int CHOOSE_PHOTO = 6;
    //消息列表
    private List<Msg> msgList = new ArrayList<>();
    private MsgAdapter msgAdapter;
    //消息列表是否停止上下滑动
    private boolean istop = false;
    //下载百分比结果
    private int result;

    private Handler handler = new Handler();
    //返回的文件路径
    private String filePath;
    //返回的小视频路径
    private String smallvideopath;
    //返回的图片路径
    private String picPath;
    //对应的缩略图路径
    private String thumbnailPath;
    //地理位置
    private String address;
    //图片集
    private ArrayList<String> picPaths;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        init();

    }

    private void init() {
        //获取当前聊天的好友对象
        currentFriend = (Friend) getIntent().getParcelableExtra("friend");
        //当前好友id
        currentFriendId = currentFriend.getUserId();
        //设置当前聊天好友名字
        title.setText(currentFriend.getRealName());
        //设置表情框监听
        faceFragment.setListener(this);
        //设置文字输入框内容改变监听
        content.addTextChangedListener(watcher);
        //输入框监听
        content.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                more.setVisibility(View.GONE);
                content.requestFocus();
                openKeyboard(ChatActivity.this, content);
                express.setImageDrawable(getResources().getDrawable(R.drawable.chat_smile));
                getFragmentManager().beginTransaction().remove(faceFragment).commitAllowingStateLoss();
                anState = false;
                expState = false;
                inputStyle = false;
                return true;
            }
        });
        msgAdapter = new MsgAdapter(this, msgList, downloadListener, openFileListener);
        contentList.setAdapter(msgAdapter);
        msgList.clear();
        //从本地数据库里取出最近10条
        msgList.addAll(DatabaseInfo.sqLiteManager.queryMessage(SipInfo.userId, currentFriendId, "2147483647", 10));
        //刷新
        msgAdapter.notifyDataSetChanged();
        //设置消息监听(SipUser类的自定义接口)
        SipInfo.sipUser.setMessageListener(this);
        //设置当前位置为消息列表中的最后一个
        contentList.setSelection(msgList.size());
        //触摸之后,关闭键盘,关闭表情框,隐藏更多选择框,重置所有标志位
        contentList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                more.setVisibility(View.GONE);
                content.requestFocus();
                closeKeyboard(ChatActivity.this, content);
                express.setImageDrawable(getResources().getDrawable(R.drawable.chat_smile));
                getFragmentManager().beginTransaction().remove(faceFragment).commitAllowingStateLoss();
                anState = false;
                expState = false;
                inputStyle = false;
                return false;
            }
        });
        //录音按钮结束录音回调函数
        recorderButton.setFinishRecorderCallBack(new AudioRecorderButton.AudioFinishRecorderCallBack() {
            @Override
            public void onFinish(final float seconds, final String filePath) {
                Log.d(TAG, filePath);
                final String content = "[语音消息]";
                final int time = (int) (System.currentTimeMillis() / 1000);
                final int isTimeShow;
                final String id = SipInfo.userId + System.currentTimeMillis();
                final String ftpPath = "/" + SipInfo.userAccount + "/";
                final File file = new File(filePath);
                final long size = file.length();
                final String md5 = MD5Util.getFileMD5String(file);
                FileInfo fileInfo = new FileInfo(file.getPath(), file.getName(), file.isDirectory());
                final String filetype = getType(fileInfo);
                FtpListener ftpListener = new FtpListener() {
                    @Override
                    public void onStateChange(String currentStep) {

                    }

                    @Override
                    public void onUploadProgress(String currentStep, long uploadSize, File targetFile) {
                        Log.d(TAG, currentStep);
                        android.os.Message msg = new android.os.Message();
                        msg.obj = id;
                        if (currentStep.equals(Constant.FTP_UPLOAD_SUCCESS)) {
                            Log.d(TAG, "-----上传成功--");
                            msg.what = 0x1112;
                            myhandler.sendMessage(msg);
                        } else if (currentStep.equals(Constant.FTP_UPLOAD_LOADING)) {
                            long fize = file.length();
                            float num = (float) uploadSize / (float) fize;
                            result = (int) (num * 100);
                            msg.arg1 = result;
                            msg.what = 0x1111;
                            //将消息发送给主线程的Handler
                            myhandler.sendMessage(msg);
                        }
                    }

                    @Override
                    public void onDownLoadProgress(String currentStep, long downProcess, File targetFile) {

                    }

                    @Override
                    public void onDeleteProgress(String currentStep) {

                    }
                };
                final Ftp mFtp = new Ftp(SipInfo.serverIp, 21, "ftpall", "123456", ftpListener);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mFtp.upload(filePath, ftpPath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            SipURL remote = new SipURL(currentFriendId, SipInfo.serverIp, SipInfo.SERVER_PORT_USER);
                            SipInfo.toUser = new NameAddress(currentFriend.getUsername(), remote);
                            SipInfo.sipUser.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipUser, SipInfo.toUser
                                    , SipInfo.user_from, BodyFactory.createFileTransferBody(SipInfo.userId, currentFriendId, id,
                                            file.getName(), filetype, "", "/" + SipInfo.userAccount + "/" + file.getName(), (long) seconds, md5, 0)));
                        }
                    }
                }).start();
                if (time - DatabaseInfo.sqLiteManager.queryLastTime(SipInfo.userId, currentFriendId) > 300) {
                    isTimeShow = 1;
                } else {
                    isTimeShow = 0;
                }
                //插入消息到数据库
                DatabaseInfo.sqLiteManager.insertMessage(id, SipInfo.userId, currentFriendId,
                        time, isTimeShow, content, 1, seconds);
                //插入文件信息到数据库
                DatabaseInfo.sqLiteManager.insertFile(id, file.getName(), SipInfo.userAccount,
                        filetype, time, filePath, ftpPath + "/" + file.getName(), size, md5, 0, 0, 0);
                Msg msg = new Msg();
                msg.setMsgId(id);
                msg.setFromUserId(SipInfo.userId);
                msg.setToUserId(currentFriendId);
                msg.setTime(time);
                msg.setIsTimeShow(isTimeShow);
                msg.setContent(content);
                msg.setType(1);
                msg.setRecordtime(seconds);
                msgList.add(msg);
                msgAdapter.notifyDataSetChanged();//刷新
                contentList.setSelection(msgList.size());
                //更新最新聊天消息数据库
                DatabaseInfo.sqLiteManager.insertLastestMsgTo(0, currentFriendId, content, time);
            }
        });
        //列表滑动加载更多本地聊天记录
        contentList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (istop && scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    istop = false;
                    int time = msgList.get(0).getTime();
                    int before = msgList.size();
                    msgList.addAll(0, DatabaseInfo.sqLiteManager.queryMessage(SipInfo.userId, currentFriendId, "" + time, 10));
                    int after = msgList.size();
                    msgAdapter.notifyDataSetChanged();
                    contentList.setSelection(after - before);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem == 0) {
                    istop = true;
                } else {
                    istop = false;
                }
            }
        });
    }
    //返回,更新未读聊天数量,更新最近聊天信息,关闭键盘
    @Override
    public void onBackPressed() {
        int i = DatabaseInfo.sqLiteManager.queryLastestMsgCount(0, currentFriendId);
        DatabaseInfo.sqLiteManager.updateLastestMsg(currentFriendId);
        closeKeyboard(ChatActivity.this, getWindow().getDecorView());
        super.onBackPressed();

    }
    //打开键盘
    private void openKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
    }
    //关闭键盘
    private void closeKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    //按钮监听事件
    @OnClick({R.id.input_type, R.id.express, R.id.send, R.id.addmore, R.id.addfolder, R.id.small_video, R.id.pic, R.id.loc})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.input_type://切换输入模式,文字或者语音聊天
                //重置图标
                express.setImageDrawable(getResources().getDrawable(R.drawable.chat_smile));
                //移除表情选择框
                getFragmentManager().beginTransaction().remove(faceFragment).commitAllowingStateLoss();
                //隐藏更多选项框
                more.setVisibility(View.GONE);
                if (!inputStyle) {//切换为语音模式
                    inputType.setImageDrawable(getResources().getDrawable(R.drawable.chat_keyboard));
                    recorderButton.setVisibility(View.VISIBLE);
                    content.setVisibility(View.GONE);
                    content.clearFocus();
                    closeKeyboard(ChatActivity.this, getWindow().getDecorView());
                    inputStyle = true;
                } else {//切换为文字模式
                    inputType.setImageDrawable(getResources().getDrawable(R.drawable.chat_audio));
                    recorderButton.setVisibility(View.GONE);
                    content.setVisibility(View.VISIBLE);
                    content.requestFocus();
                    openKeyboard(ChatActivity.this, content);
                    inputStyle = false;
                }
                anState = false;
                expState = false;
                break;
            case R.id.express:
                //语音按钮隐藏
                recorderButton.setVisibility(View.GONE);
                //图标变语音
                inputType.setImageDrawable(getResources().getDrawable(R.drawable.chat_audio));
                //显示文字输入框
                content.setVisibility(View.VISIBLE);
                //清除文字输入框的焦点
                content.clearFocus();
                //更多选项框隐藏
                more.setVisibility(View.GONE);
                //重置更多选项框状态
                anState = false;
                //重置输入类型状态
                inputStyle = false;
                if (!expState) {//显示表情选择框
                    express.setImageDrawable(getResources().getDrawable(R.drawable.chat_keyboard));
                    //显示表情选择框
                    getFragmentManager().beginTransaction().add(R.id.expresslayout, faceFragment).commitAllowingStateLoss();
                    closeKeyboard(ChatActivity.this, getWindow().getDecorView());
                    expState = true;
                } else {//隐藏表情选择框
                    express.setImageDrawable(getResources().getDrawable(R.drawable.chat_smile));
                    //文字输入框获取焦点
                    content.requestFocus();
                    openKeyboard(ChatActivity.this, content);
                    //移除表情选择框
                    getFragmentManager().beginTransaction().remove(faceFragment).commitAllowingStateLoss();
                    expState = false;
                }
                anState = false;
                inputStyle = false;
                break;
            case R.id.send://发送消息
                final String con = content.getText().toString();
                if (!TextUtils.isEmpty(con)) {
                    final int time = (int) (System.currentTimeMillis() / 1000);
                    final int isTimeShow;
                    final String id = SipInfo.userId + System.currentTimeMillis();
                    SipURL remote = new SipURL(currentFriendId, SipInfo.serverIp, SipInfo.SERVER_PORT_USER);
                    SipInfo.toUser = new NameAddress(currentFriend.getUsername(), remote);
                    Message message = SipMessageFactory.createNotifyRequest(SipInfo.sipUser, SipInfo.toUser,
                            SipInfo.user_from, BodyFactory.createMessageBody(id, SipInfo.userId, currentFriendId, con, "", 0));
                    SipInfo.sipUser.sendMessage(message);
                    if (time - DatabaseInfo.sqLiteManager.queryLastTime(SipInfo.userId, currentFriendId) > 300) {
                        isTimeShow = 1;
                    } else {
                        isTimeShow = 0;
                    }
                    DatabaseInfo.sqLiteManager.insertMessage(id, SipInfo.userId, currentFriendId,
                            time, isTimeShow, con, 0, 0);
                    Msg msg = new Msg();
                    msg.setMsgId(id);
                    msg.setFromUserId(SipInfo.userId);
                    msg.setToUserId(currentFriendId);
                    msg.setTime(time);
                    msg.setIsTimeShow(isTimeShow);
                    msg.setContent(con);
                    msg.setType(0);
                    msgList.add(msg);
                    msgAdapter.notifyDataSetChanged();
                    contentList.setSelection(msgList.size());
                    content.setText("");
                    DatabaseInfo.sqLiteManager.insertLastestMsgTo(0, currentFriendId, con, time);
                }
                break;
            case R.id.addmore:
                //弹出更多选项框
                recorderButton.setVisibility(View.GONE);
                content.setVisibility(View.VISIBLE);
                express.setImageDrawable(getResources().getDrawable(R.drawable.chat_smile));
                getFragmentManager().beginTransaction().remove(faceFragment).commitAllowingStateLoss();
                if (!anState) {
                    more.setVisibility(View.VISIBLE);
                    content.clearFocus();
                    inputType.setImageDrawable(getResources().getDrawable(R.drawable.chat_audio));
                    closeKeyboard(ChatActivity.this, getWindow().getDecorView());
                    inputStyle = false;
                    anState = true;
                } else {
                    more.setVisibility(View.GONE);
                    content.requestFocus();
                    openKeyboard(ChatActivity.this, content);
                    anState = false;
                }
                expState = false;
                inputStyle = false;
                break;
            case R.id.addfolder://选择文件
                Intent intent = new Intent(ChatActivity.this, FileChooserActivity.class);
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                    startActivityForResult(intent, FILE_CHOOSE);
                else
                    Toast.makeText(this, getText(R.string.sdcard_unmonted_hint), Toast.LENGTH_SHORT).show();
                break;
            case R.id.small_video://小视频
                Intent videointent = new Intent(ChatActivity.this, MakeSmallVideo.class);
                startActivityForResult(videointent, MAKE_SMALL_VIDEO);
                break;
            case R.id.pic://图片
                LinearLayout linearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.popup, null);
                Button camera = (Button) linearLayout.findViewById(R.id.camera);
                Button album = (Button) linearLayout.findViewById(R.id.album);
                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setView(linearLayout)
                        .create();
                dialog.show();
                camera.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent openCamera = new Intent(ChatActivity.this, MyCamera.class);
                        openCamera.putExtra("type", 1);
                        startActivityForResult(openCamera, TAKE_PHOTO);
                        dialog.dismiss();
                    }
                });
                album.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent openAlbum = new Intent(ChatActivity.this, AlbumAty.class);
                        openAlbum.putExtra("type", 1);
                        startActivityForResult(openAlbum, CHOOSE_PHOTO);
                        dialog.dismiss();
                    }
                });
                break;
            case R.id.loc://位置信息
                Intent openSend = new Intent(ChatActivity.this, SendLocation.class);
                startActivityForResult(openSend, CHOOSE_LOCATION);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_CHOOSE://文件选择返回
                if (resultCode == RESULT_OK) {
                    filePath = data.getStringExtra("FilePath");
                    final String id = SipInfo.userId + System.currentTimeMillis();
                    final String content = "[文件]";
                    final int time = (int) (System.currentTimeMillis() / 1000);
                    final String ftpPath = "/" + SipInfo.userAccount + "/";
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final File file = new File(filePath);
                            final long size = file.length();
                            final String md5 = MD5Util.getFileMD5String(file);
                            FileInfo fileInfo = new FileInfo(file.getPath(), file.getName(), file.isDirectory());
                            final String filetype = getType(fileInfo);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    final int isTimeShow;
                                    if (time - DatabaseInfo.sqLiteManager.queryLastTime(SipInfo.userId, currentFriendId) > 300) {
                                        isTimeShow = 1;
                                    } else {
                                        isTimeShow = 0;
                                    }
                                    Log.d(TAG, "run: " + filePath);
                                    DatabaseInfo.sqLiteManager.insertMessage(id, SipInfo.userId, currentFriendId,
                                            time, isTimeShow, content, 1, 0);
                                    DatabaseInfo.sqLiteManager.insertFile(id, file.getName(), SipInfo.userAccount, filetype, time, filePath, ftpPath + file.getName(), size, md5, 2, 0, 0);
                                    DatabaseInfo.sqLiteManager.insertLastestMsgTo(0, currentFriendId, content, time);
                                    final Msg msgfile = new Msg();
                                    msgfile.setMsgId(id);
                                    msgfile.setContent(content);
                                    msgfile.setFromUserId(SipInfo.userId);
                                    msgfile.setToUserId(currentFriendId);
                                    msgfile.setTime(time);
                                    msgfile.setIsTimeShow(isTimeShow);
                                    msgfile.setType(1);
                                    msgList.add(msgfile);
                                    msgAdapter.notifyDataSetChanged(); // 当有新消息时，刷新ListView中的显示
                                    contentList.setSelection(msgList.size()); // 将ListView定位到最后一行

                                }
                            });
                            FtpListener ftpListener = new FtpListener() {
                                @Override
                                public void onStateChange(String currentStep) {

                                }

                                @Override
                                public void onUploadProgress(String currentStep, long uploadSize, File targetFile) {
                                    Log.d(TAG, currentStep);
                                    android.os.Message msg = new android.os.Message();
                                    msg.obj = id;
                                    if (currentStep.equals(Constant.FTP_UPLOAD_SUCCESS)) {
                                        msg.what = 0x1112;
                                        myhandler.sendMessage(msg);
                                    } else if (currentStep.equals(Constant.FTP_UPLOAD_LOADING)) {
                                        long fize = file.length();
                                        float num = (float) uploadSize / (float) fize;
                                        result = (int) (num * 100);
                                        msg.arg1 = result;
                                        msg.what = 0x1111;
                                        //将消息发送给主线程的Handler
                                        myhandler.sendMessage(msg);
                                    }
                                }

                                @Override
                                public void onDownLoadProgress(String currentStep, long downProcess, File targetFile) {

                                }

                                @Override
                                public void onDeleteProgress(String currentStep) {

                                }
                            };
                            Ftp mFtp = new Ftp(SipInfo.serverIp, 21, "ftpall", "123456", ftpListener);
                            try {
                                mFtp.upload(filePath, ftpPath);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                SipURL remote = new SipURL(currentFriendId, SipInfo.serverIp, SipInfo.SERVER_PORT_USER);
                                SipInfo.toUser = new NameAddress(currentFriend.getUsername(), remote);
                                SipInfo.sipUser.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipUser, SipInfo.toUser
                                        , SipInfo.user_from, BodyFactory.createFileTransferBody(SipInfo.userId, currentFriendId, id,
                                                file.getName(), filetype, "", "/" + SipInfo.userAccount + "/" + file.getName(), size, md5, 2)));
                                DatabaseInfo.sqLiteManager.insertLastestMsgTo(0, currentFriendId, "[文件]", time);
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ChatActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }).start();
                }
                break;
            case CHOOSE_LOCATION:
                if (resultCode == RESULT_OK) {
                    address = data.getStringExtra("location");
                    Log.d(TAG, address);
                    final int time = (int) (System.currentTimeMillis() / 1000);
                    final int isTimeShow;
                    final String id = SipInfo.userId + System.currentTimeMillis();
                    SipURL remote = new SipURL(currentFriend.getUserId(), SipInfo.serverIp, SipInfo.SERVER_PORT_USER);
                    SipInfo.toUser = new NameAddress(currentFriend.getUsername(), remote);
                    Message message = SipMessageFactory.createNotifyRequest(SipInfo.sipUser, SipInfo.toUser,
                            SipInfo.user_from, BodyFactory.createMessageBody(id, SipInfo.userId, currentFriendId, address, "", 2));
                    SipInfo.sipUser.sendMessage(message);
                    if (time - DatabaseInfo.sqLiteManager.queryLastTime(SipInfo.userId, currentFriendId) > 300) {
                        isTimeShow = 1;
                    } else {
                        isTimeShow = 0;
                    }
                    DatabaseInfo.sqLiteManager.insertMessage(id, SipInfo.userId, currentFriendId,
                            time, isTimeShow, address, 2, 0);
                    Msg msg = new Msg();
                    msg.setMsgId(id);
                    msg.setFromUserId(SipInfo.userId);
                    msg.setToUserId(currentFriendId);
                    msg.setTime(time);
                    msg.setIsTimeShow(isTimeShow);
                    msg.setContent(address);
                    msg.setType(2);
                    msgList.add(msg);
                    msgAdapter.notifyDataSetChanged();
                    contentList.setSelection(msgList.size());
                    content.setText("");
                    DatabaseInfo.sqLiteManager.insertLastestMsgTo(0, currentFriendId, "[位置信息]", time);
                }
                break;
            case CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    picPaths = data.getStringArrayListExtra("picpaths");
                    for (final String Path : picPaths) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String thumbnailPath = Path;
                                final String picPath = thumbnailPath.replace(getString(R.string.Thumbnail), getString(R.string.Image));
                                Log.d(TAG, picPath);
                                Log.d(TAG, thumbnailPath);
                                final String id = SipInfo.userId + System.currentTimeMillis();
                                final String content = "图片";
                                final int time = (int) (System.currentTimeMillis() / 1000);
                                final String ftpPath = "/" + SipInfo.userAccount + "/";
                                final String thumbnailftpPath = ftpPath + "/Thumbnail/";
                                final File file = new File(picPath);
                                final File thumbnailFile = new File(thumbnailPath);
                                final long size = file.length();
                                final String md5 = MD5Util.getFileMD5String(file);
                                FileInfo fileInfo = new FileInfo(file.getPath(), file.getName(), file.isDirectory());
                                final String filetype = getType(fileInfo);
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {

                                        final int isTimeShow;
                                        if (time - DatabaseInfo.sqLiteManager.queryLastTime(SipInfo.userId, currentFriendId) > 300) {
                                            isTimeShow = 1;
                                        } else {
                                            isTimeShow = 0;
                                        }
                                        Log.d(TAG, "run: " + picPath);
                                        DatabaseInfo.sqLiteManager.insertMessage(id, SipInfo.userId, currentFriendId,
                                                time, isTimeShow, content, 1, 0);
                                        DatabaseInfo.sqLiteManager.insertFile(id, file.getName(), SipInfo.userAccount, filetype, time, picPath, ftpPath + file.getName(), size, md5, 3, 0, 0);
                                        final Msg msgfile = new Msg();
                                        msgfile.setMsgId(id);
                                        msgfile.setContent(content);
                                        msgfile.setFromUserId(SipInfo.userId);
                                        msgfile.setToUserId(currentFriendId);
                                        msgfile.setTime(time);
                                        msgfile.setIsTimeShow(isTimeShow);
                                        msgfile.setType(1);
                                        msgList.add(msgfile);
                                        msgAdapter.notifyDataSetChanged(); // 当有新消息时，刷新ListView中的显示
                                        contentList.setSelection(msgList.size()); // 将ListView定位到最后一行

                                    }
                                });
                                FtpListener ftpListener1 = new FtpListener() {
                                    @Override
                                    public void onStateChange(String currentStep) {

                                    }

                                    @Override
                                    public void onUploadProgress(String currentStep, long uploadSize, File targetFile) {
                                        Log.d(TAG, currentStep);
                                        android.os.Message msg = new android.os.Message();
                                        msg.obj = id;
                                        if (currentStep.equals(Constant.FTP_UPLOAD_SUCCESS)) {
                                            msg.what = 0x1112;
                                            myhandler.sendMessage(msg);
                                        } else if (currentStep.equals(Constant.FTP_UPLOAD_LOADING)) {
                                            long fize = file.length();
                                            float num = (float) uploadSize / (float) fize;
                                            result = (int) (num * 100);
                                            msg.arg1 = result;
                                            msg.what = 0x1111;
                                            //将消息发送给主线程的Handler
                                            myhandler.sendMessage(msg);
                                        }
                                    }

                                    @Override
                                    public void onDownLoadProgress(String currentStep, long downProcess, File targetFile) {

                                    }

                                    @Override
                                    public void onDeleteProgress(String currentStep) {

                                    }
                                };
                                FtpListener ftpListener2 = new FtpListener() {
                                    @Override
                                    public void onStateChange(String currentStep) {

                                    }

                                    @Override
                                    public void onUploadProgress(String currentStep, long uploadSize, File targetFile) {
                                        Log.d(TAG, currentStep);
                                    }

                                    @Override
                                    public void onDownLoadProgress(String currentStep, long downProcess, File targetFile) {

                                    }

                                    @Override
                                    public void onDeleteProgress(String currentStep) {

                                    }
                                };
                                Ftp mFtp1 = new Ftp(SipInfo.serverIp, 21, "ftpall", "123456", ftpListener1);
                                Ftp mFtp2 = new Ftp(SipInfo.serverIp, 21, "ftpall", "123456", ftpListener2);
                                try {
                                    mFtp1.upload(picPath, ftpPath);
                                    mFtp2.upload(thumbnailPath, thumbnailftpPath);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } finally {
                                    SipURL remote = new SipURL(currentFriendId, SipInfo.serverIp, SipInfo.SERVER_PORT_USER);
                                    SipInfo.toUser = new NameAddress(currentFriend.getUsername(), remote);
                                    SipInfo.sipUser.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipUser, SipInfo.toUser
                                            , SipInfo.user_from, BodyFactory.createFileTransferBody(SipInfo.userId, currentFriendId, id,
                                                    file.getName(), filetype, "", thumbnailftpPath + file.getName(), size, md5, 3)));
                                    DatabaseInfo.sqLiteManager.insertLastestMsgTo(0, currentFriendId, "[图片]", time);
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(ChatActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                }
                break;
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    picPath = data.getStringExtra("picpath");
                    thumbnailPath = picPath.replace(getString(R.string.Image), getString(R.string.Thumbnail));
                    Log.d(TAG, picPath);
                    Log.d(TAG, thumbnailPath);
                    final String id = SipInfo.userId + System.currentTimeMillis();
                    final String content = "图片";
                    final int time = (int) (System.currentTimeMillis() / 1000);
                    final String ftpPath = "/" + SipInfo.userAccount + "/";
                    final String thumbnailftpPath = ftpPath + "/Thumbnail/";
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final File file = new File(picPath);
                            final File thumbnailFile = new File(thumbnailPath);
                            final long size = file.length();
                            final String md5 = MD5Util.getFileMD5String(file);
                            FileInfo fileInfo = new FileInfo(file.getPath(), file.getName(), file.isDirectory());
                            final String filetype = getType(fileInfo);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {

                                    final int isTimeShow;
                                    if (time - DatabaseInfo.sqLiteManager.queryLastTime(SipInfo.userId, currentFriendId) > 300) {
                                        isTimeShow = 1;
                                    } else {
                                        isTimeShow = 0;
                                    }
                                    Log.d(TAG, "run: " + picPath);
                                    DatabaseInfo.sqLiteManager.insertMessage(id, SipInfo.userId, currentFriendId,
                                            time, isTimeShow, content, 1, 0);
                                    DatabaseInfo.sqLiteManager.insertFile(id, file.getName(), SipInfo.userAccount, filetype, time, picPath, ftpPath + file.getName(), size, md5, 3, 0, 0);
                                    final Msg msgfile = new Msg();
                                    msgfile.setMsgId(id);
                                    msgfile.setContent(content);
                                    msgfile.setFromUserId(SipInfo.userId);
                                    msgfile.setToUserId(currentFriendId);
                                    msgfile.setTime(time);
                                    msgfile.setIsTimeShow(isTimeShow);
                                    msgfile.setType(1);
                                    msgList.add(msgfile);
                                    msgAdapter.notifyDataSetChanged(); // 当有新消息时，刷新ListView中的显示
                                    contentList.setSelection(msgList.size()); // 将ListView定位到最后一行

                                }
                            });
                            FtpListener ftpListener1 = new FtpListener() {
                                @Override
                                public void onStateChange(String currentStep) {

                                }

                                @Override
                                public void onUploadProgress(String currentStep, long uploadSize, File targetFile) {
                                    Log.d(TAG, currentStep);
                                    android.os.Message msg = new android.os.Message();
                                    msg.obj = id;
                                    if (currentStep.equals(Constant.FTP_UPLOAD_SUCCESS)) {
                                        msg.what = 0x1112;
                                        myhandler.sendMessage(msg);
                                    } else if (currentStep.equals(Constant.FTP_UPLOAD_LOADING)) {
                                        long fize = file.length();
                                        float num = (float) uploadSize / (float) fize;
                                        result = (int) (num * 100);
                                        msg.arg1 = result;
                                        msg.what = 0x1111;
                                        //将消息发送给主线程的Handler
                                        myhandler.sendMessage(msg);
                                    }
                                }

                                @Override
                                public void onDownLoadProgress(String currentStep, long downProcess, File targetFile) {

                                }

                                @Override
                                public void onDeleteProgress(String currentStep) {

                                }
                            };
                            FtpListener ftpListener2 = new FtpListener() {
                                @Override
                                public void onStateChange(String currentStep) {

                                }

                                @Override
                                public void onUploadProgress(String currentStep, long uploadSize, File targetFile) {
                                    Log.d(TAG, currentStep);
                                }

                                @Override
                                public void onDownLoadProgress(String currentStep, long downProcess, File targetFile) {

                                }

                                @Override
                                public void onDeleteProgress(String currentStep) {

                                }
                            };
                            Ftp mFtp1 = new Ftp(SipInfo.serverIp, 21, "ftpall", "123456", ftpListener1);
                            Ftp mFtp2 = new Ftp(SipInfo.serverIp, 21, "ftpall", "123456", ftpListener2);
                            try {
                                mFtp1.upload(picPath, ftpPath);
                                mFtp2.upload(thumbnailPath, thumbnailftpPath);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                SipURL remote = new SipURL(currentFriendId, SipInfo.serverIp, SipInfo.SERVER_PORT_USER);
                                SipInfo.toUser = new NameAddress(currentFriend.getUsername(), remote);
                                SipInfo.sipUser.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipUser, SipInfo.toUser
                                        , SipInfo.user_from, BodyFactory.createFileTransferBody(SipInfo.userId, currentFriendId, id,
                                                file.getName(), filetype, "", thumbnailftpPath + file.getName(), size, md5, 3)));
                                DatabaseInfo.sqLiteManager.insertLastestMsgTo(0, currentFriendId, "[图片]", time);
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ChatActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }).start();
                }
                break;
            case MAKE_SMALL_VIDEO:
                if (resultCode == RESULT_OK) {
                    smallvideopath = data.getStringExtra("smallvideopath");
                    Log.i(TAG, smallvideopath);
                    final String id = SipInfo.userId + System.currentTimeMillis();
                    final String content = "小视频";
                    final int time = (int) (System.currentTimeMillis() / 1000);
                    final String ftpPath = "/" + SipInfo.userAccount + "/";
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final File file = new File(smallvideopath);
                            final long size = file.length();
                            final String md5 = MD5Util.getFileMD5String(file);
                            FileInfo fileInfo = new FileInfo(file.getPath(), file.getName(), file.isDirectory());
                            final String filetype = getType(fileInfo);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {

                                    final int isTimeShow;
                                    if (time - DatabaseInfo.sqLiteManager.queryLastTime(SipInfo.userId, currentFriendId) > 300) {
                                        isTimeShow = 1;
                                    } else {
                                        isTimeShow = 0;
                                    }
                                    Log.d(TAG, "run: " + smallvideopath);
                                    DatabaseInfo.sqLiteManager.insertMessage(id, SipInfo.userId, currentFriendId,
                                            time, isTimeShow, content, 1, 0);
                                    DatabaseInfo.sqLiteManager.insertFile(id, file.getName(), SipInfo.userAccount, filetype, time, smallvideopath, ftpPath + file.getName(), size, md5, 1, 0, 0);
                                    final Msg msgfile = new Msg();
                                    msgfile.setMsgId(id);
                                    msgfile.setContent(content);
                                    msgfile.setFromUserId(SipInfo.userId);
                                    msgfile.setToUserId(currentFriendId);
                                    msgfile.setTime(time);
                                    msgfile.setIsTimeShow(isTimeShow);
                                    msgfile.setType(1);
                                    msgList.add(msgfile);
                                    msgAdapter.notifyDataSetChanged(); // 当有新消息时，刷新ListView中的显示
                                    contentList.setSelection(msgList.size()); // 将ListView定位到最后一行

                                }
                            });
                            FtpListener ftpListener = new FtpListener() {
                                @Override
                                public void onStateChange(String currentStep) {

                                }

                                @Override
                                public void onUploadProgress(String currentStep, long uploadSize, File targetFile) {
                                    Log.d(TAG, currentStep);
                                    android.os.Message msg = new android.os.Message();
                                    msg.obj = id;
                                    if (currentStep.equals(Constant.FTP_UPLOAD_SUCCESS)) {
                                        msg.what = 0x1112;
                                        myhandler.sendMessage(msg);
                                    } else if (currentStep.equals(Constant.FTP_UPLOAD_LOADING)) {
                                        long fize = file.length();
                                        float num = (float) uploadSize / (float) fize;
                                        result = (int) (num * 100);
                                        msg.arg1 = result;
                                        msg.what = 0x1111;
                                        //将消息发送给主线程的Handler
                                        myhandler.sendMessage(msg);
                                    }
                                }

                                @Override
                                public void onDownLoadProgress(String currentStep, long downProcess, File targetFile) {

                                }

                                @Override
                                public void onDeleteProgress(String currentStep) {

                                }
                            };
                            Ftp mFtp = new Ftp(SipInfo.serverIp, 21, "ftpall", "123456", ftpListener);
                            try {
                                mFtp.upload(smallvideopath, ftpPath);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                SipURL remote = new SipURL(currentFriendId, SipInfo.serverIp, SipInfo.SERVER_PORT_USER);
                                SipInfo.toUser = new NameAddress(currentFriend.getUsername(), remote);
                                SipInfo.sipUser.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipUser, SipInfo.toUser
                                        , SipInfo.user_from, BodyFactory.createFileTransferBody(SipInfo.userId, currentFriendId, id,
                                                file.getName(), filetype, "", "/" + SipInfo.userAccount + "/" + file.getName(), size, md5, 1)));
                                DatabaseInfo.sqLiteManager.insertLastestMsgTo(0, currentFriendId, "[小视频]", time);
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ChatActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                        }
                    }).start();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    //文字输入监听
    private TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (s.length() > 0) {
                send.setVisibility(View.VISIBLE);
                addmore.setVisibility(View.GONE);
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() == 0) {
                send.setVisibility(View.GONE);
                addmore.setVisibility(View.VISIBLE);
            } else if (s.length() > 0) {
                send.setVisibility(View.VISIBLE);
                addmore.setVisibility(View.GONE);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    //Emoji接口
    @Override
    public void onEmojiDelete() {
        String text = content.getText().toString();
        if (text.isEmpty()) {
            return;
        }
        if ("]".equals(text.substring(text.length() - 1, text.length()))) {
            int index = text.lastIndexOf("[");
            if (index == -1) {
                int action = KeyEvent.ACTION_DOWN;
                int code = KeyEvent.KEYCODE_DEL;
                KeyEvent event = new KeyEvent(action, code);
                content.onKeyDown(KeyEvent.KEYCODE_DEL, event);
                displayTextView();
                return;
            }
            content.getText().delete(index, text.length());
            displayTextView();
            return;
        }
        int action = KeyEvent.ACTION_DOWN;
        int code = KeyEvent.KEYCODE_DEL;
        KeyEvent event = new KeyEvent(action, code);
        content.onKeyDown(KeyEvent.KEYCODE_DEL, event);
        displayTextView();
    }

    @Override
    public void onEmojiClick(Emoji emoji) {
        if (emoji != null) {
            int index = content.getSelectionStart();
            Editable editable = content.getEditableText();
            if (index < 0) {
                editable.append(emoji.getContent());
            } else {
                editable.insert(index, emoji.getContent());
            }
        }
        displayTextView();
    }

    private void displayTextView() {
        try {
            EmojiUtil.handlerEmojiText(content, content.getText().toString(), this);
            content.setSelection(content.getText().length());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getType(FileInfo fileInfo) {
        switch (fileInfo.whichtype()) {
            case DOC:
                return "doc";
            case DOCX:
                return "docx";
            case PPT:
                return "ppt";
            case PPTX:
                return "pptx";
            case UNKNOWN:
                return "unknown";
            case XLS:
                return "xls";
            case XLXS:
                return "xlxs";
            case PDF:
                return "pdf";
            case PNG:
                return "png";
            case TXT:
                return "txt";
            case MP3:
                return "mp3";
            case MP4:
                return "mp4";
            case BMP:
                return "bmp";
            case GIF:
                return "gif";
            case AVI:
                return "avi";
            case WMA:
                return "wma";
            case RAR:
                return "rar";
            case ZIP:
                return "zip";
            case WAV:
                return "wav";
            case JPG:
                return "jpg";
            case NULL:
                return "";
            default:
                return "";
        }
    }

    private Handler myhandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(android.os.Message message) {
            Msg msg = new Msg();
            msg.setMsgId((String) message.obj);
            int index = msgList.indexOf(msg);
            if (index != -1) {
                if (message.what == 0x1111) {
                    msgList.get(index).setProgress(message.arg1);
                    msgAdapter.notifyDataSetChanged();
                }
                if (message.what == 0x1112) {
                    DatabaseInfo.sqLiteManager.updateIsFileTransferFinish(msg.getMsgId(), 1);
                    msgAdapter.notifyDataSetChanged();
                }
            }
            return true;
        }
    });
    /**
     * 下载监听
     */
    private MsgAdapter.DownloadListener downloadListener = new MsgAdapter.DownloadListener() {
        @Override
        public void onDownload(final String msgId, final String filePath, final String fileName) {
            Thread downlistener = new Thread(new Runnable() {
                @Override
                public void run() {
                    String FilePath = SipInfo.localSdCard + "download/" + SipInfo.userAccount + "/personal/";
                    File file = new File(FilePath + fileName);
                    if (file.exists()) {
                        file.delete();
                        Log.d(TAG, "删除成功");
                    }
                    /**开始下载*/
                    FtpListener ftpListener = new FtpListener() {
                        @Override
                        public void onStateChange(String currentStep) {

                        }

                        @Override
                        public void onUploadProgress(String currentStep, long uploadSize, File targetFile) {
                        }

                        @Override
                        public void onDownLoadProgress(String currentStep, long downProcess, File targetFile) {
                            Log.d(TAG, currentStep);
                            android.os.Message message = new android.os.Message();
                            message.obj = msgId;
                            if (currentStep.equals(Constant.FTP_DOWN_SUCCESS)) {
                                Log.d(TAG, "-----下载--successful");
                                message.what = 0x2222;
                                dhandler.sendMessage(message);
                            } else if (currentStep.equals(Constant.FTP_DOWN_LOADING)) {
                                Log.d(TAG, "-----下载---" + downProcess + "%");
                                message.arg1 = (int) downProcess;
                                message.what = 0x2221;
                                dhandler.sendMessage(message);
                            }
                        }

                        @Override
                        public void onDeleteProgress(String currentStep) {

                        }
                    };
                    final Ftp mFtp = new Ftp(SipInfo.serverIp, 21, "ftpall", "123456", ftpListener);
                    try {
                        /**单文件下载*/
                        mFtp.download(filePath, FilePath);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            downlistener.start();
        }
    };


    private Handler dhandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(android.os.Message message) {
            Msg msg = new Msg();
            msg.setMsgId((String) message.obj);
            MyFile myFile = DatabaseInfo.sqLiteManager.queryFile(msg.getMsgId());
            int index = msgList.indexOf(msg);
            System.out.println("index = " + index);
            if (index != -1) {
                if (message.what == 0x2221) {
                    msgList.get(index).setProgress(message.arg1);
                    msgAdapter.notifyDataSetChanged();
                }
                if (message.what == 0x2222) {
                    DatabaseInfo.sqLiteManager.updateFileDownload(msg.getMsgId(), 1);
                    DatabaseInfo.sqLiteManager.updateLocalPath(msg.getMsgId(), SipInfo.localSdCard + "download/" + SipInfo.userAccount + "/personal/");
                    msgAdapter.notifyDataSetChanged();
                    if (myFile.getType() == 0) {
                        android.os.Message openmessage = new android.os.Message();
                        openmessage.what = 0x1;
                        openmessage.obj = msg.getMsgId();
                        msgAdapter.openhandler.sendMessage(openmessage);
                    }
                }
            }
            return true;
        }
    });

    /**
     * 打开文件监听
     */
    private MsgAdapter.OpenFileListener openFileListener = new MsgAdapter.OpenFileListener() {
        @Override
        public void OpenFile(File file) {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //设置intent的Action属性
            intent.setAction(Intent.ACTION_VIEW);
            //获取文件file的MIME类型
            String type = getMIMEType(file);
            //设置intent的data和Type属性。
            intent.setDataAndType(/*uri*/Uri.fromFile(file), type);
            //跳转
            startActivity(intent);
        }
    };

    /**
     * 根据文件后缀名获得对应的MIME类型。
     *
     * @param file
     */
    private String getMIMEType(File file) {

        String type = "*/*";
        String fName = file.getName();
        //获取后缀名前的分隔符"."在fName中的位置。
        int dotIndex = fName.lastIndexOf(".");
        if (dotIndex < 0) {
            return type;
        }
        /**获取文件的后缀名*/
        String end = fName.substring(dotIndex, fName.length()).toLowerCase();
        if (end == "") return type;
        /**在MIME和文件类型的匹配表中找到对应的MIME类型。*/
        for (int i = 0; i < MIME_MapTable.length; i++) { //MIME_MapTable??在这里你一定有疑问，这个MIME_MapTable是什么？
            if (end.equals(MIME_MapTable[i][0]))
                type = MIME_MapTable[i][1];
        }
        return type;
    }

    private final String[][] MIME_MapTable = {
            /**{后缀名，MIME类型}*/
            {".3gp", "video/3gpp"},
            {".apk", "application/vnd.android.package-archive"},
            {".asf", "video/x-ms-asf"},
            {".avi", "video/x-msvideo"},
            {".bin", "application/octet-stream"},
            {".bmp", "image/bmp"},
            {".c", "text/plain"},
            {".class", "application/octet-stream"},
            {".conf", "text/plain"},
            {".cpp", "text/plain"},
            {".doc", "application/msword"},
            {".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
            {".xls", "application/vnd.ms-excel"},
            {".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
            {".exe", "application/octet-stream"},
            {".gif", "image/gif"},
            {".gtar", "application/x-gtar"},
            {".gz", "application/x-gzip"},
            {".h", "text/plain"},
            {".htm", "text/html"},
            {".html", "text/html"},
            {".jar", "application/java-archive"},
            {".java", "text/plain"},
            {".jpeg", "image/jpeg"},
            {".jpg", "image/jpeg"},
            {".js", "application/x-javascript"},
            {".log", "text/plain"},
            {".m3u", "audio/x-mpegurl"},
            {".m4a", "audio/mp4a-latm"},
            {".m4b", "audio/mp4a-latm"},
            {".m4p", "audio/mp4a-latm"},
            {".m4u", "video/vnd.mpegurl"},
            {".m4v", "video/x-m4v"},
            {".mov", "video/quicktime"},
            {".mp2", "audio/x-mpeg"},
            {".mp3", "audio/x-mpeg"},
            {".mp4", "video/mp4"},
            {".mpc", "application/vnd.mpohun.certificate"},
            {".mpe", "video/mpeg"},
            {".mpeg", "video/mpeg"},
            {".mpg", "video/mpeg"},
            {".mpg4", "video/mp4"},
            {".mpga", "audio/mpeg"},
            {".msg", "application/vnd.ms-outlook"},
            {".ogg", "audio/ogg"},
            {".pdf", "application/pdf"},
            {".png", "image/png"},
            {".pps", "application/vnd.ms-powerpoint"},
            {".ppt", "application/vnd.ms-powerpoint"},
            {".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
            {".prop", "text/plain"},
            {".rc", "text/plain"},
            {".rmvb", "audio/x-pn-realaudio"},
            {".rtf", "application/rtf"},
            {".sh", "text/plain"},
            {".tar", "application/x-tar"},
            {".tgz", "application/x-compressed"},
            {".txt", "text/plain"},
            {".wav", "audio/x-wav"},
            {".wma", "audio/x-ms-wma"},
            {".wmv", "audio/x-ms-wmv"},
            {".wps", "application/vnd.ms-works"},
            {".xml", "text/plain"},
            {".z", "application/x-compress"},
            {".zip", "application/x-zip-compressed"},
            {"", "*/*"}
    };

    @Override
    public void onReceivedMessage(final Msg msg) {
        if (msg.getFromUserId().equals(currentFriendId)) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    msgList.add(msg);
                    msgAdapter.notifyDataSetChanged(); // 当有新消息时，刷新ListView中的显示
                    contentList.setSelection(msgList.size()); // 将ListView定位到最后一行
                }
            });
        }
    }
}
