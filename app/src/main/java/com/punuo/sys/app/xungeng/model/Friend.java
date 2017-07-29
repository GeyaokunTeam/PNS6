package com.punuo.sys.app.xungeng.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Author chzjy
 * Date 2016/12/19.
 */

public class Friend implements Comparable<Friend>, Parcelable {
    private String userId;
    private String username;
    private String phoneNum;
    private String telNum;
    private String realName;
    private String unit;
    private boolean isLive;
    private int newMsgCount = 0;
    private boolean isSelect = false;
    private String sortLetters;  //显示数据拼音的首字母

    public Friend() {
    }

    protected Friend(Parcel in) {
        userId = in.readString();
        username = in.readString();
        phoneNum = in.readString();
        telNum = in.readString();
        realName = in.readString();
        unit = in.readString();
        isLive = in.readByte() != 0;
        newMsgCount = in.readInt();
        isSelect = in.readByte() != 0;
        sortLetters = in.readString();
    }

    public static final Creator<Friend> CREATOR = new Creator<Friend>() {
        @Override
        public Friend createFromParcel(Parcel in) {
            return new Friend(in);
        }

        @Override
        public Friend[] newArray(int size) {
            return new Friend[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeString(username);
        dest.writeString(phoneNum);
        dest.writeString(telNum);
        dest.writeString(realName);
        dest.writeString(unit);
        dest.writeByte((byte) (isLive ? 1 : 0));
        dest.writeInt(newMsgCount);
        dest.writeByte((byte) (isSelect ? 1 : 0));
        dest.writeString(sortLetters);
    }

    @Override
    public int compareTo(Friend another) {
        if (another != null) {
            Friend dev = another;
            if (this.isLive && !dev.isLive()) {
                return -1;
            } else if (!this.isLive && dev.isLive()) {
                return 1;
            } else if (this.isLive == dev.isLive()) {
                return this.username.compareTo(dev.getUsername());
            }
        } else {
            throw new NullPointerException("比较对象为空");
        }
        return 0;
    }

    public String getSortLetters() {
        return sortLetters;
    }

    public void setSortLetters(String sortLetters) {
        this.sortLetters = sortLetters;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public String getTelNum() {
        return telNum;
    }

    public boolean isLive() {
        return isLive;
    }

    public int getNewMsgCount() {
        return newMsgCount;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public void setTelNum(String telNum) {
        this.telNum = telNum;
    }

    public void setLive(boolean live) {
        isLive = live;
    }

    public void setNewMsgCount(int newMsgCount) {
        this.newMsgCount = newMsgCount;
    }

    public void addMsgCount(int newMsgCount) {
        this.newMsgCount += newMsgCount;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getRealName() {
        return realName;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o != null && o.getClass() == Friend.class) {
            Friend friend = (Friend) o;
            if (this.getUserId().equals(friend.getUserId())) {
                return true;
            }
        }
        return false;
    }
}
