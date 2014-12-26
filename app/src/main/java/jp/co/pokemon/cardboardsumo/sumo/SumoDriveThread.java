package jp.co.pokemon.cardboardsumo.sumo;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * 連続的なUDPメッセージの送信でSumoを駆動するスレッド
 *
 * Created by psyark on 2014/12/20.
 */
public class SumoDriveThread implements Runnable {
    private final String TAG = SumoDriveThread.class.getSimpleName();
    private InetAddress sumoAddress;
    private int c2dPort;
    private DatagramSocket socket;
    private SumoSession session;

    public SumoDriveThread(InetAddress sumoAddress, int c2dPort, SumoSession session) {
        this.sumoAddress = sumoAddress;
        this.c2dPort = c2dPort;
        this.session = session;
    }

    @Override
    public void run() {
        if (configureSumo()) {
            driveSumo();
        }
    }

    private DatagramPacket createPacket(byte[] data) {
        return new DatagramPacket(data, data.length, sumoAddress, c2dPort);
    }

    /**
     * 設定のためのUDPメッセージを送る
     *
     * @return 成功したかどうか
     */
    private boolean configureSumo() {
        try {
            socket = new DatagramSocket();
            DatagramPacket packet;

            // 1 IOCTL:DATE:SET(2014-12-23)
            Log.i(TAG, "DATE");
            packet = createPacket(new byte[] { 0x04, 0x0b, 0x01, 0x16, 0x00, 0x00, 0x00, 0x00, 0x04, 0x01, 0x00, 0x32, 0x30, 0x31, 0x34, 0x2d, 0x31, 0x32, 0x2d, 0x32, 0x33, 0x00 });
            socket.send(packet);
            while (!session.getDateStatus()) {
            }
            Log.i(TAG, "DATE OK");

            // 2 IOCTL:TIME:SET(T152502+0900)
            Log.i(TAG, "TIME");
            packet = createPacket(new byte[] { 0x04, 0x0b, 0x02, 0x18, 0x00, 0x00, 0x00, 0x00, 0x04, 0x02, 0x00, 0x54, 0x31, 0x35, 0x32, 0x35, 0x30, 0x32, 0x2b, 0x30, 0x39, 0x30, 0x30, 0x00 });
            socket.send(packet);
            while (!session.getTimeStatus()) {
            }
            Log.i(TAG, "TIME OK");

            // 3 IOCTL:INFO:REQ
            Log.i(TAG, "INFO");
            packet = createPacket(new byte[] { 0x04, 0x0b, 0x03, 0x0b, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00 });
            socket.send(packet);
            while (session.getInfoStatus()) {
            }
            Log.i(TAG, "INFO OK");

            byte[] buffer = new byte[] {
                SumoPacketType.IOCTL,
                11, // 送信するIOCTLは常に11
                4, // seqno
                11, 0, // size
                0, 0, // unk
                0, 4, 0, 0 // body
            };

            Log.v(TAG, DebugUtil.hexDump(buffer));
            socket.send(new DatagramPacket(buffer, 11, sumoAddress, c2dPort));

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 操縦のためのUDPメッセージを送り続ける
     */
    private void driveSumo() {
        Log.i(TAG, "driveSumo start");
        int seqno = 1;

        byte[] buffer = new byte[] {
            SumoPacketType.SYNC,
            10, // 送信するSYNCは常に10
            0, // seqno
            14, 0, // size
            0, 0, // unk
            3, 0, 0, 0, // ???
            0, 0, 0 // active, speed, rotation
        };

        while (true) {
            Thread.yield();

            try {
                buffer[2] = (byte)(seqno++ & 0xFF);
                synchronized (session.move) {
                    buffer[11] = session.move.isActive();
                    buffer[12] = session.move.speed;
                    buffer[13] = session.move.rotation;
                }
                Log.v(TAG, DebugUtil.hexDump(buffer));
                socket.send(new DatagramPacket(buffer, 14, sumoAddress, c2dPort));
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
            }
        }
    }
}
