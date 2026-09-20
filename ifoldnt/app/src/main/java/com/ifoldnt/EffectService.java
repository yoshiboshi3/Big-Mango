package com.ifoldnt;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;

import java.util.Locale;

public class EffectService extends Service implements SensorEventListener {
    public static final String ACTION_REFRESH = "com.ifoldnt.REFRESH";
    public static final String ACTION_DISABLE = "com.ifoldnt.DISABLE";
    public static final String ACTION_PREVIEW = "com.ifoldnt.PREVIEW";
    private static final int NOTIFICATION_ID = 7619;
    private static final String CHANNEL_ID = "ifoldnt_effect";

    private WindowManager wm;
    private OverlayView overlay;
    private WindowManager.LayoutParams overlayParams;
    private float safeWindowAlpha = 0.79f;
    private SensorManager sensors;
    private Sensor hinge;
    private Sensor gyro;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean screenOn = true;
    private boolean overlayAdded = false;
    private boolean lastInner = false;
    private float hingeAngle = 180f;
    private float lastHingeAngle = 180f;
    private long lastHingeNs = 0L;
    private float hingeVelocity = 0f;
    private float gyroEma = 0f;
    private boolean gestureActive = false;
    private String lockedSide = null;
    private long fallbackStarted = 0L;
    private long previewUntil = 0L;

    private static volatile String diagnostics = "Service not running.";

    public static String diagnostics() { return diagnostics; }

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String a = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(a)) {
                screenOn = false;
                hideOverlay();
            } else if (Intent.ACTION_SCREEN_ON.equals(a) || Intent.ACTION_USER_PRESENT.equals(a)) {
                screenOn = true;
                render();
            }
        }
    };

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            render();
            handler.postDelayed(this, 120L);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        sensors = (SensorManager) getSystemService(SENSOR_SERVICE);
        hinge = sensors.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE);
        gyro = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (hinge != null) sensors.registerListener(this, hinge, SensorManager.SENSOR_DELAY_GAME);
        if (gyro != null) sensors.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME);

        if (Build.VERSION.SDK_INT >= 31) {
            InputManager input = (InputManager) getSystemService(INPUT_SERVICE);
            if (input != null) {
                float max = input.getMaximumObscuringOpacityForTouch();
                safeWindowAlpha = Math.max(0.05f, Math.min(0.79f, max - 0.01f));
            }
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(screenReceiver, filter);

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        handler.post(ticker);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_DISABLE.equals(action)) {
                Prefs.get(this).edit().putBoolean(Prefs.MASTER, false).apply();
                hideOverlay();
                stopSelf();
                return START_NOT_STICKY;
            }
            if (ACTION_PREVIEW.equals(action)) {
                previewUntil = SystemClock.uptimeMillis() + 5000L;
            }
        }
        if (!Prefs.master(this) || (!Prefs.fade(this) && !Prefs.bevel(this))) {
            hideOverlay();
            stopSelf();
            return START_NOT_STICKY;
        }
        render();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel c = new NotificationChannel(CHANNEL_ID, "iFoldn't effects", NotificationManager.IMPORTANCE_LOW);
        c.setDescription("Keeps the unfold fade and screen bevel effects active.");
        nm.createNotificationChannel(c);
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(this, 1, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent disable = new Intent(this, EffectService.class).setAction(ACTION_DISABLE);
        PendingIntent disablePi = PendingIntent.getService(this, 2, disable, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String active = (Prefs.fade(this) ? "Fade " : "") + (Prefs.bevel(this) ? "Bevels" : "");
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("iFoldn't is active")
                .setContentText(active.trim().isEmpty() ? "Effects enabled" : active.trim())
                .setContentIntent(openPi)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Disable all effects", disablePi).build())
                .build();
    }

    private boolean isUnlocked() {
        KeyguardManager kg = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return kg == null || !kg.isKeyguardLocked();
    }

    private Rect currentBounds() {
        try {
            if (wm == null) return null;
            Rect b = wm.getCurrentWindowMetrics().getBounds();
            if (b.width() <= 0 || b.height() <= 0) return null;
            return new Rect(b);
        } catch (Throwable t) {
            return null;
        }
    }

    private boolean isInner(Rect b) {
        if (b == null) return false;
        float ratio = Math.min(b.width(), b.height()) / (float)Math.max(b.width(), b.height());
        return ratio >= 0.66f;
    }

    private void ensureOverlay() {
        if (overlayAdded || !Settings.canDrawOverlays(this) || wm == null) return;
        try {
            overlay = new OverlayView(this);
            overlayParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            overlayParams.gravity = Gravity.TOP | Gravity.START;
            overlayParams.alpha = safeWindowAlpha;
            if (Build.VERSION.SDK_INT >= 28) {
                overlayParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            }
            wm.addView(overlay, overlayParams);
            overlayAdded = true;
        } catch (Throwable t) {
            overlayAdded = false;
            overlay = null;
            overlayParams = null;
        }
    }

    private void hideOverlay() {
        if (overlayAdded && overlay != null && wm != null) {
            try { wm.removeViewImmediate(overlay); } catch (Throwable ignored) {}
        }
        overlayAdded = false;
        overlay = null;
        overlayParams = null;
    }

    private float opacityFor(float angle) {
        float max = Math.max(0f, Math.min(0.75f, Prefs.darkness(this) / 100f));
        float start = Math.max(0f, Math.min(175f, Prefs.start(this)));
        float finish = Math.max(start + 5f, Math.min(179f, Prefs.finish(this)));
        if (angle <= start) return max;
        if (angle >= finish) return 0f;
        float t = (angle - start) / (finish - start);
        float eased = t * t * (3f - 2f * t);
        return max * (1f - eased);
    }

    private String chooseSide() {
        String mode = Prefs.side(this);
        if ("LEFT".equals(mode) || "RIGHT".equals(mode)) return mode;
        float rightMoved = Prefs.get(this).getFloat(Prefs.CAL_RIGHT_MOVED, Float.NaN);
        float leftMoved = Prefs.get(this).getFloat(Prefs.CAL_LEFT_MOVED, Float.NaN);
        float noise = Math.max(0.03f, Prefs.get(this).getFloat(Prefs.CAL_NOISE, 0.12f));
        if (Float.isNaN(rightMoved) || Float.isNaN(leftMoved)) return "RIGHT";
        float dr = Math.abs(gyroEma - rightMoved);
        float dl = Math.abs(gyroEma - leftMoved);
        float separation = Math.abs(rightMoved - leftMoved);
        float confidence = separation <= noise ? 0f : Math.min(1f, Math.abs(dl - dr) / (separation + noise));
        if (confidence < 0.62f) return "RIGHT";
        return dr < dl ? "RIGHT" : "LEFT";
    }

    private void render() {
        boolean master = Prefs.master(this);
        boolean fadeEnabled = Prefs.fade(this);
        boolean bevelEnabled = Prefs.bevel(this);
        boolean permission = Settings.canDrawOverlays(this);
        Rect bounds = currentBounds();
        boolean inner = isInner(bounds);
        boolean unlocked = screenOn && isUnlocked();
        long now = SystemClock.uptimeMillis();

        if (!master || (!fadeEnabled && !bevelEnabled) || !permission || !unlocked || bounds == null) {
            hideOverlay();
            if (!permission && master) diagnostics = "Overlay permission missing; effects suppressed.";
            return;
        }

        if (hinge == null) {
            if (inner && !lastInner) fallbackStarted = now;
            if (!inner) fallbackStarted = 0L;
        }
        lastInner = inner;

        boolean preview = now < previewUntil;
        boolean showFade = false;
        float effectiveFade = 0f;
        String side = lockedSide == null ? Prefs.side(this) : lockedSide;
        if ("AUTO".equals(side)) side = "RIGHT";

        if (preview) {
            showFade = true;
            effectiveFade = Math.min(0.65f, Prefs.darkness(this) / 100f);
            if (lockedSide == null) side = chooseSide();
        } else if (fadeEnabled && inner) {
            if (hinge != null) {
                if (hingeVelocity > 3f && hingeAngle < Prefs.finish(this)) {
                    if (!gestureActive) {
                        gestureActive = true;
                        lockedSide = chooseSide();
                    }
                    side = lockedSide;
                    effectiveFade = opacityFor(hingeAngle);
                    showFade = effectiveFade > 0.002f;
                } else if (hingeAngle >= Prefs.finish(this) || hingeVelocity < -3f) {
                    if (hingeAngle >= Prefs.finish(this)) {
                        gestureActive = false;
                        lockedSide = null;
                    }
                    showFade = false;
                } else if (gestureActive && lockedSide != null && hingeAngle < Prefs.finish(this)) {
                    side = lockedSide;
                    effectiveFade = opacityFor(hingeAngle);
                    showFade = effectiveFade > 0.002f;
                }
            } else if (fallbackStarted > 0L) {
                float t = Math.min(1f, (now - fallbackStarted) / 650f);
                float eased = t * t * (3f - 2f * t);
                effectiveFade = (Prefs.darkness(this) / 100f) * (1f - eased);
                showFade = t < 1f;
                side = chooseSide();
            }
        }

        boolean showBevel = bevelEnabled;
        if (!showFade && !showBevel) {
            hideOverlay();
        } else {
            ensureOverlay();
            if (overlay != null) {
                float internalFade = safeWindowAlpha <= 0f ? 0f : Math.min(1f, effectiveFade / safeWindowAlpha);
                int cutPx = Math.round(Prefs.bevelDp(this) * getResources().getDisplayMetrics().density);
                overlay.setEffect(showFade, "LEFT".equals(side), internalFade, showBevel, cutPx);
            }
        }

        float ratio = bounds == null ? 0f : Math.min(bounds.width(), bounds.height()) / (float)Math.max(bounds.width(), bounds.height());
        diagnostics = String.format(Locale.US,
                "Mode: %s\nHinge: %.1f°  velocity: %.1f°/s\nGyro EMA: %.3f\nSelected: %s  locked: %s\nDisplay: %dx%d ratio %.3f (%s)\nOverlay: %s  window alpha: %.3f\nFade: %s  Bevel: %s\nPermission: %s  unlocked: %s",
                hinge != null ? "Live hinge" : "Timed fallback",
                hingeAngle, hingeVelocity, gyroEma,
                Prefs.side(this), lockedSide == null ? "—" : lockedSide,
                bounds == null ? 0 : bounds.width(), bounds == null ? 0 : bounds.height(), ratio, inner ? "inner" : "cover",
                overlayAdded ? "attached" : "absent", safeWindowAlpha,
                showFade ? "visible" : "absent", showBevel ? "visible" : "absent",
                permission, unlocked);
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE && event.values.length >= 3) {
            float x = event.values[0], y = event.values[1], z = event.values[2];
            float mag = (float)Math.sqrt(x*x + y*y + z*z);
            gyroEma = gyroEma == 0f ? mag : gyroEma * 0.82f + mag * 0.18f;
        } else if (event.sensor.getType() == Sensor.TYPE_HINGE_ANGLE && event.values.length > 0) {
            float a = Math.max(0f, Math.min(180f, event.values[0]));
            long ns = event.timestamp;
            if (lastHingeNs != 0L && ns > lastHingeNs) {
                float dt = (ns - lastHingeNs) / 1_000_000_000f;
                if (dt > 0.001f) hingeVelocity = (a - lastHingeAngle) / dt;
            }
            lastHingeAngle = a;
            lastHingeNs = ns;
            hingeAngle = hingeAngle * 0.25f + a * 0.75f;
            render();
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (sensors != null) sensors.unregisterListener(this);
        try { unregisterReceiver(screenReceiver); } catch (Throwable ignored) {}
        hideOverlay();
        diagnostics = "Service stopped; overlays removed.";
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
