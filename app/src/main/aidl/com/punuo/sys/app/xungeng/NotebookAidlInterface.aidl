// NotebookAidlInterface.aidl
package com.punuo.sys.app.xungeng;

// Declare any non-default types here with import statements
import com.punuo.sys.app.xungeng.model.MailInfo;
import com.punuo.sys.app.xungeng.model.Friend;
import com.punuo.sys.app.xungeng.model.UserInfo;
interface NotebookAidlInterface {
    List<MailInfo> getMailInfo();
    List<Friend> getFriendList();
    UserInfo getUserInfo();
    void sendMail(String mailId,String fromId,String toId,String theme,String content);
    void deleteMail(String mailId);
}
