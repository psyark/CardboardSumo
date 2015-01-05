package jp.co.pokemon.cardboardsumo;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
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

    private boolean running = false;
    private boolean attitudeInitialized = false;
    private float currentYaw;
    private float currentPitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
            }
        });
    }

    @Override
    public void onDrawFrame(HeadTransform headTransform, Eye eye, Eye eye1) {
        float[] quaternion = new float[4];
        headTransform.getQuaternion(quaternion, 0);

        float x = quaternion[0];
        float y = quaternion[1];
        float z = quaternion[2];
        float w = quaternion[3];
        float roll  = (float)Math.atan2(2*y*w - 2*x*z, 1 - 2*y*y - 2*z*z);
        float pitch = (float)Math.atan2(2*x*w - 2*y*z, 1 - 2*x*x - 2*z*z);
        float yaw   = (float)Math.asin(2*x*y + 2*z*w);

        Log.v(TAG, String.format("onNewFrame p=%f, y=%f", pitch, roll));

        if (!attitudeInitialized) {
            attitudeInitialized = true;
            currentYaw = roll;
            currentPitch = pitch;
        }
        float relYaw = currentYaw - roll;
        float rotSpeed = Math.max(-1, Math.min(+1, relYaw * 8.0f));
        float speed;
        if (pitch > 0) {
            speed = Math.min(+1.0f, pitch * 2.0f);
        } else {
            speed = Math.max(-1.0f, pitch * 1.0f);
        }
        synchronized (sumoClient.session.move) {
            if (running) {
                sumoClient.session.move.rotation = (byte) (rotSpeed * 127);
                sumoClient.session.move.speed = (byte) (speed * 127);
            } else {
                sumoClient.session.move.rotation = 0;
                sumoClient.session.move.speed = 0;
            }
        }
        currentYaw = roll;
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
        Log.i(TAG, String.format("onCardboardTrigger pitch: %f", currentPitch));

        // 停止状態で上を向いていたらジャンプ
        if (!running && currentPitch > 0.5) {
            sumoClient.performJump();
        } else {
            running = !running;
        }
    }
}
