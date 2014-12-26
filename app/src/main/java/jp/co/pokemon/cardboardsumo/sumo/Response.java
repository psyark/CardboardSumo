package jp.co.pokemon.cardboardsumo.sumo;

import java.net.DatagramPacket;

/**
 * Created by psyark on 2014/12/25.
 */
public class Response {
    private byte[] data;

    private Response() {
    }

    public static Response createJump(byte seqno) {
        Response resp = new Response();
        resp.data = new byte[15];
        resp.data[0] = SumoPacketType.IOCTL;
        resp.data[1] = 11;
        resp.data[2] = seqno;
        resp.data[3] = 15; resp.data[4] = 0;
        resp.data[5] = 0; resp.data[6] = 0;
        resp.data[7] = 3; // flags == 3
        resp.data[8] = 2; // type == 2
        resp.data[9] = 3; // func == 3
        resp.data[10] = 0;
        resp.data[11] = 1;
        resp.data[12] = 0;
        resp.data[13] = 0;
        resp.data[14] = 0;
        return resp;
    }

    /**
     * SYNCパケット用の応答を作成
     * @param data
     */
    public static Response createSyncResponse(byte[] data) {
        Response resp = new Response();
        resp.data = new byte[15];
        System.arraycopy(data, 0, resp.data, 0, resp.data.length);
        resp.data[1] = 1;
        return resp;
    }

    /**
     * IOCTLパケット用の応答を作成
     * @param ext
     * @param new_seqno
     * @param src_seqno
     */
    public static Response createIoctlResponse(byte ext, byte new_seqno, byte src_seqno) {
        Response resp = new Response();
        resp.data = new byte[8];
        resp.data[0] = SumoPacketType.ACK;
        resp.data[1] = (byte)(ext | 0x80);
        resp.data[2] = new_seqno;
        resp.data[3] = 8; resp.data[4] = 0;
        resp.data[5] = 0; resp.data[6] = 0;
        resp.data[7] = src_seqno;
        return resp;
    }

    public void updatePacket(DatagramPacket packet) {
        packet.setData(data);
        packet.setLength(data.length);
    }
}
