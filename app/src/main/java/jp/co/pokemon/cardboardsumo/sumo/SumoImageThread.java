package jp.co.pokemon.cardboardsumo.sumo;

import android.graphics.Bitmap;

/**
 * Created by psyark on 2014/12/24.
 */
public class SumoImageThread implements Runnable {
    private final String TAG = SumoImageThread.class.getSimpleName();
    private SumoSession session;
    private SumoImageCallback callback;

    public SumoImageThread(SumoSession session, SumoImageCallback callback) {
        this.session = session;
        this.callback = callback;
    }

    @Override
    public void run() {
        while (true) {
            Thread.yield();

            Bitmap bitmap = session.imageData.getBitmap();
            if (bitmap != null) {
                callback.OnImage(bitmap);
            }
        }
    }
}
