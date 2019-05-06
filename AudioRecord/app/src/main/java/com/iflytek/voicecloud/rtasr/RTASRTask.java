package com.iflytek.voicecloud.rtasr;

import android.os.Environment;
import android.util.Log;

import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import org.java_websocket.WebSocket.READYSTATE;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iflytek.voicecloud.rtasr.util.EncryptUtil;

/**
 * 实时转写调用demo
 * 此demo只是一个简单的调用示例，不适合用到实际生产环境中
 * 
 * @author white
 *
 */
public class RTASRTask implements Runnable{

    // appid
    private static final String APPID = "5ccff492";

    // appid对应的secret_key
    private static final String SECRET_KEY = "bb896a9d80250cb982b0a0faa77ec4c4";

    // 请求地址
    private static final String HOST = "rtasr.xfyun.cn/v1/ws";

    private static final String BASE_URL = "ws://" + HOST;

    private static final String ORIGIN = "http://" + HOST;

    public static final String TAG = "RTASRTask ";

    // 音频文件路径
    private static final String AUDIO_PATH = Environment.getExternalStorageDirectory().getPath() + "/test/" +
            "test.pcm";

    // 每次发送的数据大小 1280 字节
    private static final int CHUNCKED_SIZE = 1280;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");


    public void realTimeASR() throws Exception {
        while (true) {
            String str = getHandShakeParams(APPID, SECRET_KEY);
            URI url = new URI(BASE_URL + str);
            DraftWithOrigin draft = new DraftWithOrigin(ORIGIN);
            CountDownLatch handshakeSuccess = new CountDownLatch(1);
            CountDownLatch connectClose = new CountDownLatch(1);
            MyWebSocketClient client = new MyWebSocketClient(url, draft, handshakeSuccess, connectClose);
            
            client.connect();
            
            while (!client.getReadyState().equals(READYSTATE.OPEN)) {
                Log.d(TAG , getCurrentTimeStr() + " connecting ");
                Thread.sleep(1000);
            }
            
            // 等待握手成功
            handshakeSuccess.await();
            Thread.sleep(8 * 1000);

            Log.d(TAG,sdf.format(new Date()) + " bgn send ");
            // 发送音频
            byte[] bytes = new byte[CHUNCKED_SIZE];
            try (RandomAccessFile raf = new RandomAccessFile(AUDIO_PATH, "r")) {
                int len = -1;
                long lastTs = 0;
                while ((len = raf.read(bytes)) != -1) {
                    if (len < CHUNCKED_SIZE) {
                        send(client, bytes = Arrays.copyOfRange(bytes, 0, len));
                        break;
                    }

                    long curTs = System.currentTimeMillis();
                    if (lastTs == 0) {
                        lastTs = System.currentTimeMillis();
                    } else {
                        long s = curTs - lastTs;
                        if (s < 40) {
                            Log.d(TAG , " error time interval: " + s + " ms");
                        }
                    }
                    send(client, bytes);
                    // 每隔40毫秒发送一次数据
                }
                
                // 发送结束标识
                send(client,"{\"end\": true}".getBytes());
                Log.d(TAG ,getCurrentTimeStr() + " send complete ");
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // 等待连接关闭
            connectClose.await();
            break;
        }
    }

    // 生成握手参数
    public static String getHandShakeParams(String appId, String secretKey) {
        String ts = System.currentTimeMillis()/1000 + "";
        String signa = "";
        try {
            signa = EncryptUtil.HmacSHA1Encrypt(EncryptUtil.MD5(appId + ts), secretKey);
            return "?appid=" + appId + "&ts=" + ts + "&signa=" + URLEncoder.encode(signa, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public static void send(WebSocketClient client, byte[] bytes) {
        if (client.isClosed()) {
            throw new RuntimeException("client connect closed!");
        }

        client.send(bytes);
    }

    public static String getCurrentTimeStr() {
        return sdf.format(new Date());
    }

    @Override
    public void run() {

        try {
            realTimeASR();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class MyWebSocketClient extends WebSocketClient {

        private CountDownLatch handshakeSuccess;
        private CountDownLatch connectClose;

        public MyWebSocketClient(URI serverUri, Draft protocolDraft, CountDownLatch handshakeSuccess, CountDownLatch connectClose) {
            super(serverUri, protocolDraft);
            this.handshakeSuccess = handshakeSuccess;
            this.connectClose = connectClose;
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            Log.d(TAG , getCurrentTimeStr() + " onOpen");
        }

        @Override
        public void onMessage(String msg) {
            JSONObject msgObj = JSON.parseObject(msg);
            String action = msgObj.getString("action");
            if (Objects.equals("started", action)) {
                // 握手成功
                Log.d(TAG , getCurrentTimeStr() + " onMessage sid: " + msgObj.getString("sid"));
                handshakeSuccess.countDown();
            } else if (Objects.equals("result", action)) {
                // 转写结果
                Log.d(TAG , getCurrentTimeStr() + "\tresult: " + getContent(msgObj.getString("data")));
            } else if (Objects.equals("error", action)) {
                // 连接发生错误
                Log.d(TAG , "Error: " + msg);
//                System.exit(0);
            }
        }

        @Override
        public void onError(Exception e) {
            Log.d(TAG , getCurrentTimeStr() + " onError" + e.getMessage() + ", " + new Date());
            e.printStackTrace();
//            System.exit(0);
        }

        @Override
        public void onClose(int arg0, String arg1, boolean arg2) {
            Log.d(TAG ,  getCurrentTimeStr() + " onClose");
            connectClose.countDown();
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            try {
                Log.d(TAG , getCurrentTimeStr() + " onMessage：" + new String(bytes.array(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    // 把转写结果解析为句子
    public static String getContent(String message) {
        StringBuffer resultBuilder = new StringBuffer();
        try {
            JSONObject messageObj = JSON.parseObject(message);
            JSONObject cn = messageObj.getJSONObject("cn");
            JSONObject st = cn.getJSONObject("st");
            JSONArray rtArr = st.getJSONArray("rt");
            for (int i = 0; i < rtArr.size(); i++) {
                JSONObject rtArrObj = rtArr.getJSONObject(i);
                JSONArray wsArr = rtArrObj.getJSONArray("ws");
                for (int j = 0; j < wsArr.size(); j++) {
                    JSONObject wsArrObj = wsArr.getJSONObject(j);
                    JSONArray cwArr = wsArrObj.getJSONArray("cw");
                    for (int k = 0; k < cwArr.size(); k++) {
                        JSONObject cwArrObj = cwArr.getJSONObject(k);
                        String wStr = cwArrObj.getString("w");
                        resultBuilder.append(wStr);
                    }
                }
            } 
        } catch (Exception e) {
            return message;
        }

        return resultBuilder.toString();
    }
}