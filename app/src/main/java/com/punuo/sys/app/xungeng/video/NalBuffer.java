package com.punuo.sys.app.xungeng.video;

/**
 * Author chzjy
 * Date 2016/12/19.
 */
public class NalBuffer {
    private byte[] nalBuf;
    private boolean isReadable;
    private boolean isWriteable;
    private int nalLen;

    public NalBuffer() {
        nalBuf = new byte[100000];
        isReadable = false;
        isWriteable = true;
        nalLen = 0;
    }

    public void isReadable() {
        if (!isReadable) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized byte[] getReadableNalBuf() {
        isReadable();
        if (this.nalLen == 0) {
            return null;
        } else {
            byte[] tmp_NalBuf = new byte[this.nalLen];
            System.arraycopy(this.nalBuf, 0, tmp_NalBuf, 0, this.nalLen);
            return tmp_NalBuf;
        }
    }

    public synchronized void readLock() {
        isReadable = false;
        isWriteable = true;
        notify();
    }

    public void isWriteable() {
        if (!isWriteable) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void setNalBuf(byte[] nalBuf, int nalLen) {
        isWriteable();
        this.nalBuf = nalBuf;
        this.nalLen = nalLen;
    }

    public synchronized void writeLock() {
        isReadable = true;
        isWriteable = false;
        notify();
    }

    public synchronized void cleanNalBuf() {
        nalBuf = new byte[100000];
        isReadable = false;
        isWriteable = true;
        nalLen = 0;
    }
}
