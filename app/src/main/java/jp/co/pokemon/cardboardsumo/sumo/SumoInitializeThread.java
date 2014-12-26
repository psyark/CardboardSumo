package jp.co.pokemon.cardboardsumo.sumo;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * 一回のTCPメッセージの往復でSumoを初期化するスレッド
 *
 * Created by psyark on 2014/12/20.
 */
public class SumoInitializeThread implements Runnable {
    private final String TAG = SumoInitializeThread.class.getSimpleName();

    private InetAddress sumoAddress;
    private SumoImageCallback imageCallback;
    private int c2dPort;

    private int currentImageFrame = -1;
    private ByteBuffer imageBuffer;
    private SumoSession session;

    public SumoInitializeThread(InetAddress sumoAddress, SumoImageCallback imageCallback, SumoSession session) {
        this.sumoAddress = sumoAddress;
        this.imageCallback = imageCallback;
        this.session = session;

        imageBuffer = ByteBuffer.allocate(65536 * 4);
    }

    @Override
    public void run() {
        Log.i(TAG, "run start");

        if (initializeSumo()) {
            new Thread(new SumoRespondThread(sumoAddress, c2dPort, session)).start();
            new Thread(new SumoDriveThread(sumoAddress, c2dPort, session)).start();
            new Thread(new SumoImageThread(session, imageCallback)).start();
            controlSumo();
        }

        Log.i(TAG, "run complete");
    }

    /**
     * 一回のTCPメッセージの往復でSumoを初期化する
     *
     * @return 成功したかどうか
     */
    private boolean initializeSumo() {
        Map<String, Object> map = new HashMap<>();
        map.put("controller_name", "com.parrot.freeflight3");
        map.put("controller_type", "iPad");
        map.put("d2c_port", 54321);
        JSONObject json = new JSONObject(map);
        Log.i("SumoClientThread", json.toString());

        try {
            Socket socket = new Socket(sumoAddress, 44444);

            OutputStreamWriter osw = new OutputStreamWriter(socket.getOutputStream());
            PrintWriter out = new PrintWriter(new BufferedWriter(osw), true);
            out.println(json.toString());
            out.flush();

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response = in.readLine();
            Log.i(TAG, response);

            json = new JSONObject(response);
            c2dPort = json.getInt("c2d_port");
            return true;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void controlSumo() {
        try {
            byte[] buffer = new byte[65536];
            DatagramSocket socket = new DatagramSocket(c2dPort);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            int my_seqno = 1;

            while (true) {
                Thread.yield();

                socket.receive(packet);

                ByteBuffer bb = ByteBuffer.wrap(buffer);
                bb.order(ByteOrder.LITTLE_ENDIAN);

                byte type  = bb.get();
                byte ext   = bb.get();
                byte seqno = bb.get();
                bb.getShort();
                bb.getShort();

                if (type == SumoPacketType.ACK) {
                    byte ack_seqno = bb.get();
                    Log.v(TAG, String.format("ACK(%d)", ack_seqno));
                } else if (type == SumoPacketType.SYNC && ext == 0) {
                    int sec = bb.getInt();
                    int nano = bb.getInt();
                    if (ext == 0) {
                        Log.v(TAG, String.format("SYNC:DTIME(%d.%d)", sec, nano));
                        // SYNC かつ ext=0 なら ext=1 にして返信
                        if (seqno % 5 == 0) { // 毎回だとIMAGEが壊れるので
                            session.offerResponse(Response.createSyncResponse(buffer));
                        }
                    } else if (ext == 1) {
                        Log.v(TAG, String.format("SYNC:CTIME:ACK(%d.%d)", sec, nano));
                    }
                } else if (type == SumoPacketType.IMAGE) {
                    short frame_number = bb.getShort();
                    bb.get(); // always 1
                    byte index = bb.get();
                    byte count = bb.get();
                    Log.v(TAG, String.format("IMAGE(frame_number=%d, %d/%d, packet.length=%d)", frame_number, index, count, packet.getLength()));

                    // フレーム番号が違うならイメージバッファをクリア
                    if (currentImageFrame != frame_number) {
                        currentImageFrame = frame_number;
                        imageBuffer.clear();
                    }
                    // イメージバッファに追加
                    imageBuffer.put(buffer, 7 + 5, packet.getLength() - (7 + 5));
                    // 最後のチャンクなら通知
                    if (index == count - 1) {
                        session.imageData.put(imageBuffer);
                    }
                } else if (type == SumoPacketType.IOCTL) {
                    byte ioc_flags = bb.get();
                    byte ioc_type = bb.get();
                    byte ioc_func = bb.get();
                    bb.get();
                    Log.v(TAG, String.format("IOCTL(ioc_flags=%d, ioc_type=%d, ioc_func=%d)", ioc_flags, ioc_type, ioc_func));

                    if (ioc_flags == 0) {
                        if (ioc_type == 3) {
                            if (ioc_func == 0) {
                                Log.i(TAG, "IOCTL:INFO:COMPLETE");
                                session.setInfoStatus();
                            }
                        } else if (ioc_type == 5) {
                            if (ioc_func == 4) {
                                Log.i(TAG, "IOCTL:INFO:DATE");
                                session.setDateStatus();
                            } else if (ioc_func == 5) {
                                Log.i(TAG, "IOCTL:INFO:TIME");
                                session.setTimeStatus();
                            }
                        }
                    }

                    session.offerResponse(Response.createIoctlResponse(ext, (byte)(my_seqno++ & 0xFF), seqno));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
