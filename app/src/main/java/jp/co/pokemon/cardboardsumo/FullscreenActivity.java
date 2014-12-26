package jp.co.pokemon.cardboardsumo;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

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

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    private final String SUMO_ADDR = "192.168.2.1";
    private SumoClient sumoClient;
    private ImageView imageView1;
    private ImageView imageView2;
    private Bitmap prevBitmap = null;

    private AudioManager audioManager;
    private boolean running = false;
    private boolean attitudeInitialized = false;
    private float currentYaw;
    private float currentPitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        getActionBar().hide();

        setContentView(R.layout.activity_fullscreen);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        setContentView(R.layout.common_ui);
        CardboardView cardboardView = (CardboardView)findViewById(R.id.cardboard_view);
//        CardboardView cardboardView = new CardboardView(this);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);
//        setContentView(cardboardView);

        imageView1 = (ImageView)findViewById(R.id.image_view1);
        imageView2 = (ImageView)findViewById(R.id.image_view2);
//        imageView = new ImageView(this);
//        setContentView(imageView);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

        sumoClient = new SumoClient(SUMO_ADDR, new SumoImageCallback() {
            @Override
            public void OnImage(final Bitmap bitmap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView1.setImageBitmap(bitmap);
                        imageView2.setImageBitmap(bitmap);
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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                synchronized (sumoClient.session.move) {
//                    sumoClient.session.move.speed = 127;
//                }
//                break;
//            case MotionEvent.ACTION_UP:
//                synchronized (sumoClient.session.move) {
//                    sumoClient.session.move.speed = 0;
//                }
//                break;
//        }
        return false;
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

            running = !running;
            synchronized (sumoClient.session.move) {
                sumoClient.session.move.speed = (byte) (running ? runSpeed : 0);
            }
        }
    }
}
