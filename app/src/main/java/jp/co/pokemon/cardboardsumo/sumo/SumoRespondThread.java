package jp.co.pokemon.cardboardsumo.sumo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Sumoから届いたUDPメッセージに応答するスレッド
 *
 * Created by psyark on 2014/12/20.
 */
public class SumoRespondThread implements Runnable {
    private final String TAG = SumoRespondThread.class.getSimpleName();
    private InetAddress sumoAddress;
    private int c2dPort;
    private SumoSession session;

    public SumoRespondThread(InetAddress sumoAddress, int c2dPort, SumoSession session) {
        this.sumoAddress = sumoAddress;
        this.c2dPort = c2dPort;
        this.session = session;
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(new byte[0], 0, sumoAddress, c2dPort);

            while (true) {
                Thread.yield();

                Response response = session.pollResponse();
                if (response != null) {
                    response.updatePacket(packet);
                    socket.send(packet);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
