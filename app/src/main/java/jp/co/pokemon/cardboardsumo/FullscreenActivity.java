package jp.co.pokemon.cardboardsumo;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import javax.microedition.khronos.egl.EGLConfig;

import jp.co.pokemon.cardboardsumo.sumo.SumoClient;
import jp.co.pokemon.cardboardsumo.sumo.SumoImageCallback;
import jp.co.pokemon.cardboardsumo.util.SystemUiHider;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class FullscreenActivity extends CardboardActivity implements CardboardView.Renderer {
    private final String TAG = FullscreenActivity.class.getSimpleName();

    private final String SUMO_ADDR = "192.168.2.1";
    private SumoClient sumoClient;
    private Bitmap prevBitmap = null;

    private AudioManager audioManager;
    private boolean running = false;
    private boolean attitudeInitialized = false;
    private float currentYaw;
    private float currentPitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        setContentView(R.layout.common_ui);
        CardboardView cardboardView = (CardboardView)findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        final ImageView imageView1 = (ImageView)findViewById(R.id.image_view1);
        final ImageView imageView2 = (ImageView)findViewById(R.id.image_view2);
        final TextView textView1 = (TextView)findViewById(R.id.text_view1);
        final TextView textView2 = (TextView)findViewById(R.id.text_view2);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

        sumoClient = new SumoClient(SUMO_ADDR, new SumoImageCallback() {
            @Override
            public void OnImage(final Bitmap bitmap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView1.setImageBitmap(bitmap);
                        imageView2.setImageBitmap(bitmap);
                        if (sumoClient.session.batteryPercentage >= 0) {
                            textView1.setText(String.format("%d%%", sumoClient.session.batteryPercentage));
                            textView2.setText(String.format("%d%%", sumoClient.session.batteryPercentage));
                        }
                    }
                });
//                    if (prevBitmap != null) {
//                        prevBitmap.recycle();
//                        prevBitmap = null;
//                    }
//                    prevBitmap = bitmap;
            }
        });
    }

    @Override
    public void onDrawFrame(HeadTransform headTransform, Eye eye, Eye eye1) {
        float[] forward = new float[3];
        float[] right = new float[3];

        headTransform.getForwardVector(forward, 0);
        headTransform.getRightVector(right, 0);
//        Log.v(TAG, String.format("onNewFrame %f, %f, %f", forward[0], forward[1], forward[2]));

        float pitch = (float) Math.atan2(-forward[1], -forward[2]);
        float yaw   = (float) Math.atan2(right[0], -right[2]);
        Log.v(TAG, String.format("onNewFrame p=%f, y=%f", pitch, yaw));

        if (!attitudeInitialized) {
            attitudeInitialized = true;
            currentYaw = yaw;
        }
        float relYaw = currentYaw - yaw;
        float rotSpeed = Math.max(-1, Math.min(+1, relYaw * 8.0f));
        synchronized (sumoClient.session.move) {
            sumoClient.session.move.rotation = (byte)(rotSpeed * 127);
        }
        currentYaw = yaw;
        currentPitch = pitch;
    }


    @Override
    public void onFinishFrame(Viewport viewport) {

    }


    @Override
    public void onSurfaceChanged(int i, int i1) {

    }


    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {

    }


    @Override
    public void onRendererShutdown() {

    }

    /**
     * Called when the Cardboard trigger is pulled.
     */
    @Override
    public void onCardboardTrigger() {
        Log.i(TAG, "onCardboardTrigger");
        Log.i(TAG, String.format("pitch: %f", currentPitch));

        // 停止状態で上を向いていたらジャンプ
        if (!running && currentPitch > 0.5) {
            sumoClient.performJump();
        } else {
            int streamType = AudioManager.STREAM_SYSTEM;
            float volume = (float)audioManager.getStreamVolume(streamType) / (float)audioManager.getStreamMaxVolume(streamType);
            Log.i(TAG, String.format("volume: %f", volume));
            int runSpeed = (int)(127 * (0.2 + volume * 0.8));

            // 下を向いていたらバック
            if (currentPitch < -0.3) {
                runSpeed *= -1;
            }

            running = !running;
            synchronized (sumoClient.session.move) {
                sumoClient.session.move.speed = (byte) (running ? runSpeed : 0);
            }
        }
    }
}
