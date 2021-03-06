package com.punuo.sys.app.xungeng.model;

/**
 * Author chzjy
 * Date 2016/12/19.
 */

public class Device implements Comparable<Device> {
    private String devId;
    private String name;
    private String phoneNum;
    private String devType;
    private boolean isLive;
    private boolean isSelect;

    public Device() {
    }

    public Device(String devId, String name, String phoneNum, String devType, boolean isLive) {
        this.devId = devId;
        this.name = name;
        this.phoneNum = phoneNum;
        this.devType = devType;
        this.isLive = isLive;
        this.isSelect = false;
    }

    public String getDevId() {
        return devId;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public String getDevType() {
        return devType;
    }

    public boolean isLive() {
        return isLive;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setDevId(String devId) {
        this.devId = devId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public void setDevType(String devType) {
        this.devType = devType;
    }

    public void setLive(boolean live) {
        isLive = live;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    @Override
    public int compareTo(Device another) throws ClassCastException, NullPointerException {
        if (another != null) {
            Device dev = another;
            if (this.isLive && !dev.isLive()) {
                return -1;
            } else if (!this.isLive && dev.isLive()) {
                return 1;
            } else if (this.isLive == dev.isLive()) {
                return this.name.compareTo(dev.getName());
            }
        } else {
            throw new NullPointerException("比较对象为空");
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o != null && o.getClass() == Device.class) {
            Device device = (Device) o;
            if (this.getDevId().equals(device.getDevId())) {
                return true;
            }
        }
        return false;
    }
}
