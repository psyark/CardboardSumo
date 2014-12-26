package jp.co.pokemon.cardboardsumo.sumo;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Jumping Sumo クライアント
 *
 * Created by psyark on 2014/12/20.
 */
public class SumoClient {
    private InetAddress sumoAddress;
    public final SumoSession session = new SumoSession();
    private int jumpSeqno = 5;

    public SumoClient(String addr, final SumoImageCallback imageCallback) {
        try {
            sumoAddress = InetAddress.getByName(addr);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }

        new Thread(new SumoInitializeThread(sumoAddress, imageCallback, session)).start();
    }

    public void performJump() {
        session.offerResponse(Response.createJump((byte)(jumpSeqno++ & 0xFF)));
    }
}
