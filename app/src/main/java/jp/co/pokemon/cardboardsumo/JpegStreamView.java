package jp.co.pokemon.cardboardsumo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Random;

/**
 * Created by psyark on 2014/12/20.
 */
public class JpegStreamView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder holder;

    public JpegStreamView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
    }

    public void drawBitmap(Bitmap bitmap) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Canvas canvas = holder.lockCanvas();
        canvas.drawBitmap(bitmap, 0, 0, paint);
        holder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i("Fullscreen", String.format("f=%d, w=%d, h=%d", format, width, height));
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }
}
