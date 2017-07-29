package com.punuo.sys.app.xungeng.sip;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import com.punuo.sys.app.xungeng.groupvoice.GroupInfo;
import com.punuo.sys.app.xungeng.groupvoice.GroupKeepAlive;
import com.punuo.sys.app.xungeng.groupvoice.GroupUdpThread;
import com.punuo.sys.app.xungeng.groupvoice.RtpAudio;
import com.punuo.sys.app.xungeng.model.TaskInfo;
import com.punuo.sys.app.xungeng.service.PTTService;
import com.punuo.sys.app.xungeng.tools.ActivityCollector;
import com.punuo.sys.app.xungeng.ui.MakeSmallVideo;
import com.punuo.sys.app.xungeng.ui.MovieRecord;
import com.punuo.sys.app.xungeng.video.VideoInfo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.zoolu.sip.address.SipURL;
import org.zoolu.sip.message.Message;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.Transport;
import org.zoolu.sip.provider.TransportConnId;

import java.io.IOException;
import java.io.StringReader;
import java.net.SocketException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


/**
 * Author chzjy
 * Date 2016/12/19.
 */

public class SipDev extends SipProvider {
    public static final String TAG = "SipDev";
    public static final String[] PROTOCOLS = {"udp"};
    private Context context;
    private ExecutorService pool = Executors.newFixedThreadPool(3);
    private WorkerLoginListener workerLoginListener;

    public SipDev(Context context, String viaAddr, int hostPort) {
        super(viaAddr, hostPort, PROTOCOLS, null);
        this.context = context;
    }

    public TransportConnId sendMessage(Message msg) {
        return sendMessage(msg, SipInfo.serverIp, SipInfo.SERVER_PORT_DEV);
    }

    public TransportConnId sendMessage(final Message msg, final String destAddr, final int destPort) {
        Log.d(TAG, "<----------send sip message---------->");
        Log.d(TAG, msg.toString());
        TransportConnId id = null;
        try {
            id = pool.submit(new Callable<TransportConnId>() {
                public TransportConnId call() {
                    return sendMessage(msg, "udp", destAddr, destPort, 0);
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return id;
    }
    //结束线程池
    public void shutdown(){
        pool.shutdown();
    }
    public synchronized void onReceivedMessage(Transport transport, Message msg) {
        Log.d(TAG, "<----------received sip message---------->");
        Log.d(TAG, msg.toString());
        int port = msg.getRemotePort();
        if (port == SipInfo.SERVER_PORT_DEV) {
            Log.e(TAG, "onReceivedMessage: " + port);
            String body = msg.getBody();
            if (msg.isRequest()) {// 请求消息
                if (!requestParse(msg)) {
                    int requestType = bodyParse(body);
                }
            } else { // 响应消息
                int code = msg.getStatusLine().getCode();
                Log.e(TAG, "onReceivedMessage: " + "code " + code);
                if (code == 200) {
                    if (!responseParse(msg)) {
                        Log.e(TAG, "onReceivedMessage: " + "bodyParse" );
                        bodyParse(body);
                    }
                } else if (code == 401) {
                    SipInfo.dev_loginTimeout = false;
                } else if (code == 402) {

                }
            }
        }
    }

    private int bodyParse(String body) {
        if (body != null) {
            StringReader sr = new StringReader(body);
            InputSource is = new InputSource(sr);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            Document document;
            try {
                builder = factory.newDocumentBuilder();
                document = builder.parse(is);
                Element root = document.getDocumentElement();
                String type = root.getTagName();
                switch (type) {
                    case "negotiate_response"://注册第一步响应
                        Element seedElement = (Element) root.getElementsByTagName("seed").item(0);
                        SipURL local = new SipURL(SipInfo.devId, SipInfo.serverIp, SipInfo.SERVER_PORT_DEV);
                        SipInfo.dev_from.setAddress(local);
                        Log.d(TAG, "收到设备注册第一步响应");
                        String password = "123456";
                        Message register = SipMessageFactory.createRegisterRequest(
                                SipInfo.sipDev, SipInfo.dev_to, SipInfo.dev_from,
                                BodyFactory.createRegisterBody(/*随便输*/password));
                        SipInfo.sipDev.sendMessage(register);
                        return 0;
                    case "login_response"://注册成功响应，心跳回复
                        if (SipInfo.devLogined) {
                            SipInfo.dev_heartbeatResponse = true;
                            Log.d(TAG, "设备收到心跳回复");
                        } else {
                            //获取电源锁,用于防止手机静默之后,心跳线程暂停
                            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                            GroupInfo.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, context.getClass().getCanonicalName());
                            GroupInfo.wakeLock.acquire();

                            SipInfo.devLogined = true;
                            SipInfo.dev_loginTimeout = false;
                            Log.d(TAG, "设备注册成功");
                            /*群组呼叫组别查询*/
                            SipInfo.sipDev.sendMessage(SipMessageFactory.createSubscribeRequest(SipInfo.sipDev,
                                    SipInfo.dev_to, SipInfo.dev_from, BodyFactory.createGroupSubscribeBody(SipInfo.devId)));

                        }
                        return 1;
                }
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            Log.d(TAG, "body is null");
        }
        return -1;
    }

    private boolean requestParse(Message msg) {
        String body = msg.getBody();
        if (body != null) {
            StringReader sr = new StringReader(body);
            InputSource is = new InputSource(sr);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            Document document;
            try {
                builder = factory.newDocumentBuilder();
                document = builder.parse(is);
                Element root = document.getDocumentElement();
                String type = root.getTagName();
                switch (type) {
                    case "query":
                        if (SipInfo.isFirst) {

                            if (MakeSmallVideo.flag) {
                                ActivityCollector.getCurrentActivity().finish();
                                ActivityCollector.removeActivity(ActivityCollector.getCurrentActivity());
                            }
                            if (SipInfo.myCamera != null) {
                                SipInfo.myCamera.finish();
                                SipInfo.myCamera = null;
                            }
                            if (MovieRecord.flag){
                                ActivityCollector.getCurrentActivity().onBackPressed();
                                ActivityCollector.removeActivity(ActivityCollector.getCurrentActivity());
                            }

//                            Element resolutionElement = (Element) root.getElementsByTagName("resolution").item(0);
//                            String resolutionType = resolutionElement.getFirstChild().getNodeValue();
//                            if (!resolutionType.equals(null)) {
//                                SipInfo.query = resolutionType;
//                                Message message = SipMessageFactory.createResponse(msg, 200, "OK",
//                                        BodyFactory.createOptionsBody(resolutionType));
//                                SipInfo.sipDev.sendMessage(message);
//                            } else {
                                Message message = SipMessageFactory.createResponse(msg, 200, "OK",
                                        BodyFactory.createOptionsBody("MOBILE_S6"));
                                SipInfo.sipDev.sendMessage(message);
//                            }
                            return true;
                        }else {
                            Message message = SipMessageFactory.createResponse(msg, 200, "OK",
                                    BodyFactory.createOptionsBody("false"));
                            SipInfo.sipDev.sendMessage(message);
                        }
                    case "media":
                        Element peerElement = (Element) root.getElementsByTagName("peer").item(0);
                        Element magicElement = (Element) root.getElementsByTagName("magic").item(0);
                        String peer = peerElement.getFirstChild().getNodeValue();
                        String magic = magicElement.getFirstChild().getNodeValue();
                        VideoInfo.media_info_ip = peer.substring(0, peer.indexOf("UDP")).trim();
                        VideoInfo.media_info_port = Integer.parseInt(peer.substring(peer.indexOf("UDP") + 3).trim());
                        VideoInfo.media_info_magic = new byte[magic.length() / 2 + magic.length() % 2];
                        for (int i = 0; i < VideoInfo.media_info_magic.length; i++) {
                            try {
                                VideoInfo.media_info_magic[i] = (byte) (0xff & Integer.parseInt(magic.substring(i * 2, i * 2 + 2), 16));
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        SipInfo.msg = msg;
                        SipInfo.notifymedia.sendEmptyMessage(0x1111);
                        return true;
                    case "recvaddr":
                        VideoInfo.endView = true;
                        SipInfo.sipDev.sendMessage(SipMessageFactory.createResponse(msg, 200, "Ok", ""));
                        SipInfo.queryResponse = true;
                        SipInfo.isFirst = true;
                        return true;
                    case "task": {
                        Element userIdElement = (Element) root.getElementsByTagName("user_id").item(0);
                        Element devIdElement = (Element) root.getElementsByTagName("dev_id").item(0);
                        Element receiverElement = (Element) root.getElementsByTagName("receiver").item(0);
                        Element receiveTimeElement = (Element) root.getElementsByTagName("receive_time").item(0);
                        Element infoSourceElement = (Element) root.getElementsByTagName("info_source").item(0);
                        Element taskTypeElement = (Element) root.getElementsByTagName("task_type").item(0);
                        Element taskIdElement = (Element) root.getElementsByTagName("task_id").item(0);
                        Element taskNameElement = (Element) root.getElementsByTagName("task_name").item(0);
                        Element orderElement = (Element) root.getElementsByTagName("order").item(0);
                        Element locationElement = (Element) root.getElementsByTagName("location").item(0);
                        Element eventIdElement = (Element) root.getElementsByTagName("event_id").item(0);
                        Element eventTimeElement = (Element) root.getElementsByTagName("event_time").item(0);
                        Element picPathDownElement = (Element) root.getElementsByTagName("pic_path_down").item(0);
                        Element vehicleFaultNumElement = (Element) root.getElementsByTagName("vehicle_fault_num").item(0);
                        Element vehicleFaultTypeElement = (Element) root.getElementsByTagName("vehicle_fault_type").item(0);
                        Element parkingPositionElement = (Element) root.getElementsByTagName("parking_position").item(0);
                        Element cargoElement = (Element) root.getElementsByTagName("cargo").item(0);
                        Element wreckerNumElement = (Element) root.getElementsByTagName("wrecker_num").item(0);
                        Element wreckerTypeElement = (Element) root.getElementsByTagName("wrecker_type").item(0);
                        Element driverElement = (Element) root.getElementsByTagName("driver").item(0);
                        Element coDriverElement = (Element) root.getElementsByTagName("co_driver").item(0);
                        Element roadOccupancyElement = (Element) root.getElementsByTagName("road_occupancy").item(0);
                        Element descriptionElement = (Element) root.getElementsByTagName("description").item(0);
                        TaskInfo taskInfo = new TaskInfo();
                        taskInfo.setDev_id(devIdElement.getFirstChild().getNodeValue());
                        taskInfo.setReceiver(receiverElement.getFirstChild().getNodeValue());
                        taskInfo.setReceive_time(receiveTimeElement.getFirstChild().getNodeValue());
                        taskInfo.setInfo_source(infoSourceElement.getFirstChild().getNodeValue());
                        taskInfo.setTask_type(taskTypeElement.getFirstChild().getNodeValue());
                        taskInfo.setTask_id(taskIdElement.getFirstChild().getNodeValue());
                        taskInfo.setTask_name(taskNameElement.getFirstChild().getNodeValue());
                        taskInfo.setOrder(orderElement.getFirstChild().getNodeValue());
                        taskInfo.setLocation(locationElement.getFirstChild().getNodeValue());
                        taskInfo.setEvent_time(eventTimeElement.getFirstChild().getNodeValue());
                        taskInfo.setPic_path_down(picPathDownElement.getFirstChild().getNodeValue());
                        taskInfo.setVehicle_fault_num(vehicleFaultNumElement.getFirstChild().getNodeValue());
                        taskInfo.setVehicle_fault_type(vehicleFaultTypeElement.getFirstChild().getNodeValue());
                        taskInfo.setParking_position(parkingPositionElement.getFirstChild().getNodeValue());
                        taskInfo.setCargo(cargoElement.getFirstChild().getNodeValue());
                        taskInfo.setWrecker_num(wreckerNumElement.getFirstChild().getNodeValue());
                        taskInfo.setWrecker_type(wreckerTypeElement.getFirstChild().getNodeValue());
                        taskInfo.setDriver(driverElement.getFirstChild().getNodeValue());
                        taskInfo.setCo_driver(coDriverElement.getFirstChild().getNodeValue());
                        taskInfo.setTxPath(roadOccupancyElement.getFirstChild().getNodeValue());
                        taskInfo.setState(descriptionElement.getFirstChild().getNodeValue());
                        SipInfo.newTask.sendMessage(new android.os.Message());
                        Intent intent = new Intent("com.punuo.sys.app.task_receive");
                        SipInfo.tasklist.add(taskInfo);
                        context.sendBroadcast(intent);
                        SipInfo.sipDev.sendMessage(SipMessageFactory.
                                createResponse(msg, 200, "OK", BodyFactory.createTaskResponse()));
                        return true;
                    }
                    case "videoParam": {
                        Element previewWidthElement = (Element) root.getElementsByTagName("preview_w").item(0);
                        Element previewHeightElement = (Element) root.getElementsByTagName("preview_h").item(0);
                        Element frameRateElement = (Element) root.getElementsByTagName("frame_rate").item(0);
                        Element timeElement = (Element) root.getElementsByTagName("time").item(0);
                        String time = timeElement.getFirstChild().getNodeValue();
                        int previewWidth = Integer.parseInt(previewWidthElement.getFirstChild().getNodeValue());
                        int previewHeight = Integer.parseInt(previewHeightElement.getFirstChild().getNodeValue());
                        int frameRate = Integer.parseInt(frameRateElement.getFirstChild().getNodeValue());
                        Log.e(TAG, "time" + time + "previewW" + previewWidth + "previewH" + previewHeight + "frameRate" + frameRate );

                        if (previewWidth != 0) {
                            resolution.onResolution(previewWidth, previewHeight, frameRate);
                        }
                        return true;
                    }
                    case "videoParam_response":
                        Element resultElement = (Element)root.getElementsByTagName("result").item(0);
                        Element timeElement = (Element)root.getElementsByTagName("time").item(0);
                        int result = Integer.parseInt(resultElement.getFirstChild().getNodeValue());
                        String time = timeElement.getFirstChild().getNodeValue();
                        Log.e(TAG, "result"+result);
                        if (result == 1){
                            response.onResponse();
                        }
                        return true;

                    default:
                        return false;
                }
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }


        } else {
            Log.d(TAG, "body is null");
            return true;
        }
        return false;
    }

    private boolean responseParse(Message msg) {
        Log.e(TAG, "responseParse: " + "responseParse");
        String body = msg.getBody();
        if (body != null) {
            StringReader sr = new StringReader(body);
            InputSource is = new InputSource(sr);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            Document document;
            try {
                builder = factory.newDocumentBuilder();
                document = builder.parse(is);
                Element root = document.getDocumentElement();
                String type = root.getTagName();
                switch (type) {
                    case "md_login_response":
                        Element worknameElement = (Element) root.getElementsByTagName("name").item(0);
                        if (worknameElement != null) {
                            String name = worknameElement.getFirstChild().getNodeValue();
                            if (workerLoginListener != null) {
                                workerLoginListener.loginRes(name);
                            }
                        }
                        return true;
                    case "md_login_ack_response": {
                        Element resultElement = (Element) root.getElementsByTagName("result").item(0);
                        if (resultElement != null) {
                            String result = resultElement.getFirstChild().getNodeValue();
                            if (workerLoginListener != null) {
                                workerLoginListener.loginAckRes(result);
                            }
                        }
                        return true;
                    }
                    case "subscribe_grouppn_response":
                        Element codeElement = (Element) root.getElementsByTagName("code").item(0);
                        String code = codeElement.getFirstChild().getNodeValue();
                        if (code.equals("200")) {
                            Element groupNumElement = (Element) root.getElementsByTagName("group_num").item(0);
                            Element peerElement = (Element) root.getElementsByTagName("peer").item(0);
                            Element levelElement = (Element) root.getElementsByTagName("level").item(0);
                            Element nameElement = (Element) root.getElementsByTagName("name").item(0);
                            GroupInfo.groupNum = groupNumElement.getFirstChild().getNodeValue();
                            String peer = peerElement.getFirstChild().getNodeValue();
                            GroupInfo.ip = peer.substring(0, peer.indexOf("UDP")).trim();
                            GroupInfo.port = Integer.parseInt(peer.substring(peer.indexOf("UDP") + 3).trim());
                            GroupInfo.level = levelElement.getFirstChild().getNodeValue();
                            SipInfo.devName = nameElement.getFirstChild().getNodeValue();
                            Thread groupVoice = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        GroupInfo.rtpAudio = new RtpAudio(SipInfo.serverIp, GroupInfo.port);
                                        GroupInfo.groupUdpThread = new GroupUdpThread(SipInfo.serverIp, GroupInfo.port);
                                        GroupInfo.groupUdpThread.startThread();
                                        GroupInfo.groupKeepAlive = new GroupKeepAlive();
                                        GroupInfo.groupKeepAlive.startThread();
                                        Intent PTTIntent = new Intent(context, PTTService.class);
                                        context.startService(PTTIntent);
                                    } catch (SocketException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, "groupVoice");
                            groupVoice.start();
                        }
                        return true;
                    default:
                        return false;
                }
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }


        } else {
            Log.d(TAG, "BODY IS NULL");
            return true;
        }
        return false;
    }

    public interface WorkerLoginListener {
        void loginRes(String name);

        void loginAckRes(String result);
    }

    public void setWorkerLoginListener(WorkerLoginListener workerLoginListener) {
        this.workerLoginListener = workerLoginListener;
    }

    public ChangeResolution resolution;
    public interface ChangeResolution{
        void onResolution(int w,int h,int f);
    }

    public void setResolution(ChangeResolution resolution){
        this.resolution = resolution;
    }

    public ChangeResolutionResponse response;
    public interface ChangeResolutionResponse{
        void onResponse();
    }
    public void setResponse(ChangeResolutionResponse response){
        this.response = response;
    }

}
