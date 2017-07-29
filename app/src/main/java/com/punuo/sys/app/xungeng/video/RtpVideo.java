package com.punuo.sys.app.xungeng.video;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.punuo.sys.app.xungeng.groupvoice.G711;

import java.net.DatagramSocket;
import java.net.SocketException;

import jlibrtp.DataFrame;
import jlibrtp.Participant;
import jlibrtp.RTPAppIntf;
import jlibrtp.RTPSession;

/**
 * Author chzjy
 * Date 2016/12/19.
 */

public class RtpVideo implements RTPAppIntf {
    private final byte H264_STREAM_HEAD[] = {0x00, 0x00, 0x00, 0x01};
    private RTPSession rtpSession;
    private DatagramSocket rtpSocket;
    private StreamBuf streamBuf;
    private byte tempNal[] = new byte[100000];
    private int tempNalLen = 0;
    private int putNum;
    private int preSeq;
    private boolean isPacketLost = true;
    private int frameSizeG711 = 160;
    private int samp_rate = 8000;
    private int maxjitter = AudioTrack.getMinBufferSize(samp_rate,
            AudioFormat.CHANNEL_CONFIGURATION_MONO,
            AudioFormat.ENCODING_PCM_16BIT);
    Participant p;

    public RtpVideo(String networkAddress, int remoteRtpPort) throws SocketException {
        rtpSocket = new DatagramSocket();
        rtpSession = new RTPSession(rtpSocket, null);
        rtpSession.RTPSessionRegister(this, null, null);
        p = new Participant(networkAddress, remoteRtpPort, remoteRtpPort + 1);
        rtpSession.addParticipant(p);
        rtpSession.naivePktReception(false);
        rtpSession.frameReconstruction(false);
        streamBuf = new StreamBuf(100, 5);
        for (int i = 0; i < VideoInfo.nalBuffers.length; i++) {
            VideoInfo.nalBuffers[i] = new NalBuffer();
        }
        putNum = 0;
        VideoInfo.track = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                samp_rate,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxjitter,
                AudioTrack.MODE_STREAM
        );
        VideoInfo.track.play();
    }

    @Override
    public void receiveData(DataFrame frame, Participant participant) {
        if (frame.payloadType() == 98) {
            StreamBufNode rtpFrameNode = new StreamBufNode(frame);
            streamBuf.addToBufBySeq(rtpFrameNode);
            if (streamBuf.isReady()) {
                StreamBufNode streamBufNode = streamBuf.getFromBuf();
                int seqNum = streamBufNode.getSeqNum();
                byte[] data = streamBufNode.getDataFrame().getConcatenatedData();

                int len = streamBufNode.getDataFrame().getTotalLength();
                Log.d("Rtp", "getNalDm365");
                Log.d("Rtp", "len:" + len + "  seqNum:" + seqNum);
                getNalDm365(data, seqNum, len);
                VideoInfo.isrec = 2;
            }
        } else if (frame.payloadType() == 69) {
            byte[] audioBuffer = new byte[frameSizeG711];
            short[] audioData = new short[frameSizeG711];
            audioBuffer = frame.getConcatenatedData();
            G711.ulaw2linear(audioBuffer, audioData, frameSizeG711);
            VideoInfo.track.write(audioData, 0, frameSizeG711);

        }
    }

    public void endSession() {
        rtpSession.endSession();
    }

    @Override
    public void userEvent(int type, Participant[] participant) {

    }

    //移除当前监听的端口
    public void removeParticipant() {
        rtpSession.removeParticipant(p);
    }

    @Override
    public int frameSize(int payloadType) {
        return 1;
    }

    public void sendActivePacket(byte[] msg) {
        rtpSession.payloadType(0x7a);
        for (int i = 0; i < 2; i++) {
            rtpSession.sendData(msg);
        }
    }

    public void getNalDm365(byte[] data, int seqNum, int len) {
        switch (frameParseDm365(data)) {
            case 0:
                addCompleteRtpPacketToTemp(data, seqNum, len);
                Log.d("Rtp", "addCompleteRtpPacketToTemp");
                copyFromTempToNal();
                isPacketLost = false;
                break;
            case 1:
                addFirstRtpPacketToTemp(data, seqNum, len);
                Log.d("Rtp", "addFirstRtpPacketToTemp");
                isPacketLost = false;
                break;
            case 2:
                if (!isPacketLost) {
                    if (preSeq + 1 == seqNum) {
                        addMiddleRtpPacketToTemp(data, seqNum, len);
                        Log.d("Rtp", "addMiddleRtpPacketToTemp");
                    } else {
                        jumpNal(seqNum);
                    }
                }
                break;
            case 3:
                if (!isPacketLost) {
                    if (preSeq + 1 == seqNum) {
                        addLastRtpPacketToTemp(data, seqNum, len);
                        Log.d("Rtp", "addLastRtpPacketToTemp");
                        copyFromTempToNal();
                    } else {
                        jumpNal(seqNum);
                    }
                } else {
                    isPacketLost = false;
                }
                break;
        }
    }

    public int frameParseDm365(byte[] data) {
        if ((data[0] & 0x1f) == 28 || (data[0] & 0x1f) == 29) {//先判断是否是分片
            if ((data[1] & 0xe0) == 0x80) {
                return 1;//分片首包
            } else if ((data[1] & 0xe0) == 0x00) {
                return 2;//分片中包
            } else {
                return 3;//分片末包
            }
        } else {//不是分片
            return 0;//单包
        }
    }

    private void addCompleteRtpPacketToTemp(byte[] data, int seqNum, int len) {
        tempNal = new byte[100000];
        tempNal[0] = H264_STREAM_HEAD[0];
        tempNal[1] = H264_STREAM_HEAD[1];
        tempNal[2] = H264_STREAM_HEAD[2];
        tempNal[3] = H264_STREAM_HEAD[3];
        System.arraycopy(data, 0, tempNal, 4, len);
        tempNalLen = len + 4;
        preSeq = seqNum;
    }

    private void addFirstRtpPacketToTemp(byte[] data, int seqNum, int len) {
        tempNal = new byte[100000];
        tempNal[0] = H264_STREAM_HEAD[0];
        tempNal[1] = H264_STREAM_HEAD[1];
        tempNal[2] = H264_STREAM_HEAD[2];
        tempNal[3] = H264_STREAM_HEAD[3];
        System.arraycopy(data, 2, tempNal, 4, len - 2);
        tempNalLen = len + 2;
        preSeq = seqNum;
    }

    private void addMiddleRtpPacketToTemp(byte[] data, int seqNum, int len) {
        System.arraycopy(data, 2, tempNal, tempNalLen, len - 2);
        tempNalLen += len - 2;
        preSeq = seqNum;
    }

    private void addLastRtpPacketToTemp(byte[] data, int seqNum, int len) {
        System.arraycopy(data, 2, tempNal, tempNalLen, len - 2);
        tempNalLen += len - 2;
        preSeq = seqNum;
    }

    private void copyFromTempToNal() {
        VideoInfo.nalBuffers[putNum].setNalBuf(tempNal, tempNalLen);
        VideoInfo.nalBuffers[putNum].writeLock();
        Log.d("rtp", "nalLen:" + tempNalLen);
        putNum++;
        if (putNum == 200) {
            putNum = 0;
        }
    }

    private void jumpNal(int seqNum) {
        VideoInfo.nalBuffers[putNum].writeLock();
        preSeq = seqNum;
        isPacketLost = true;
        putNum++;
        if (putNum == 200) {
            putNum = 0;
        }
    }
}
