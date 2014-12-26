package jp.co.pokemon.cardboardsumo.sumo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by psyark on 2014/12/24.
 */
public class SumoSession {
    private boolean dateStatus = false;
    private boolean timeStatus = false;
    private boolean infoStatus = false;

    private BlockingQueue<Response> responseQueue;

    public final Move move = new Move();
    public final ImageData imageData = new ImageData();

    public SumoSession() {
        responseQueue = new ArrayBlockingQueue<>(1000);
    }

    public void setDateStatus() {
        dateStatus = true;
    }
    public void setTimeStatus() {
        timeStatus = true;
    }
    public void setInfoStatus() {
        infoStatus = true;
    }

    public boolean getDateStatus() {
        return dateStatus;
    }
    public boolean getTimeStatus() {
        return timeStatus;
    }
    public boolean getInfoStatus() {
        return infoStatus;
    }

    public boolean offerResponse(Response response) {
        return responseQueue.offer(response);
    }

    public Response pollResponse() {
        return responseQueue.poll();
    }

    public class Move {
        public byte speed = 0;
        public byte rotation = 0;

        public byte isActive() {
            return speed != 0 || rotation != 0 ? (byte)1 : (byte)0;
        }
    }

    public class ImageData {
        public boolean changed = false;
        public byte[] bytes = new byte[65536 * 4];
        public int size = 0;

        public synchronized void put(ByteBuffer src) {
            changed = true;
            size = src.position();
            src.position(0);
            src.get(bytes, 0, size);
        }

        public Bitmap getBitmap() {
            byte[] bytes2;

            synchronized (this) {
                if (changed) {
                    changed = false;
                    bytes2 = new byte[size];
                    System.arraycopy(bytes, 0, bytes2, 0, size);
                } else {
                    return null;
                }
            }

            if ((bytes2[0] & 0xFF) == 0xFF && (bytes2[1] & 0xFF) == 0xD8) {
                return BitmapFactory.decodeByteArray(bytes2, 0, bytes2.length);
            } else {
                return null;
            }
        }
    }
}
