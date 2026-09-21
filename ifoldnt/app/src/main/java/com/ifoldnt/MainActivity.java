package com.ifoldnt;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private TextView permissionStatus;
    private TextView darknessLabel;
    private TextView startLabel;
    private TextView finishLabel;
    private TextView calibrationStatus;
    private Button calibrationButton;

    private SensorManager sensorManager;
    private Sensor hingeSensor;
    private Sensor gyroSensor;
    private boolean recordingCalibration = false;
    private int calibrationStage = 0;
    private float calHingeMin = Float.MAX_VALUE;
    private float calHingeMax = -Float.MAX_VALUE;
    private double calGyroSum = 0;
    private double calGyroSqSum = 0;
    private int calGyroCount = 0;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = Prefs.get(this);
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        hingeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        setContentView(buildUi());
        requestNotificationPermissionIfUseful();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(40));
        scroll.addView(root);

        TextView title = text("iFoldn't", 30, true);
        root.addView(title);
        TextView subtitle = text("Make your Fold dramatically less practical.", 15, false);
        subtitle.setTextColor(Color.DKGRAY);
        root.addView(subtitle, margin(0, 4, 0, 18));

        Switch master = new Switch(this);
        master.setText("Enable iFoldn't");
        master.setTextSize(20);
        master.setChecked(Prefs.master(this));
        root.addView(master, margin(0, 4, 0, 14));

        permissionStatus = text("", 14, true);
        root.addView(permissionStatus);
        Button permission = button("Grant display-over-apps permission");
        permission.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
        root.addView(permission, margin(0, 6, 0, 18));

        root.addView(section("Effects"));
        Switch fade = new Switch(this);
        fade.setText("Unfold Fade");
        fade.setChecked(Prefs.fade(this));
        root.addView(fade);

        Switch bevel = new Switch(this);
        bevel.setText("Bevel Screen Corners");
        bevel.setChecked(Prefs.bevel(this));
        root.addView(bevel, margin(0, 6, 0, 12));

        root.addView(text("Moving side", 14, true));
        RadioGroup sides = new RadioGroup(this);
        sides.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton auto = radio("Witchcraft Auto", "AUTO".equals(Prefs.side(this)));
        RadioButton left = radio("Left", "LEFT".equals(Prefs.side(this)));
        RadioButton right = radio("Right", "RIGHT".equals(Prefs.side(this)));
        auto.setId(View.generateViewId()); left.setId(View.generateViewId()); right.setId(View.generateViewId());
        sides.addView(auto); sides.addView(left); sides.addView(right);
        root.addView(sides, margin(0, 3, 0, 14));

        darknessLabel = text("", 14, true);
        root.addView(darknessLabel);
        SeekBar darkness = seek(0, 75, Prefs.darkness(this));
        root.addView(darkness);

        startLabel = text("", 14, true);
        root.addView(startLabel, margin(0, 10, 0, 0));
        SeekBar start = seek(30, 160, Prefs.start(this));
        root.addView(start);

        finishLabel = text("", 14, true);
        root.addView(finishLabel, margin(0, 10, 0, 0));
        SeekBar finish = seek(100, 179, Prefs.finish(this));
        root.addView(finish);
        refreshSliderLabels(darkness.getProgress(), start.getProgress(), finish.getProgress());

        root.addView(text("Bevel preset", 14, true), margin(0, 14, 0, 0));
        RadioGroup presets = new RadioGroup(this);
        presets.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton subtle = radio("Subtle", "SUBTLE".equals(Prefs.bevelPreset(this)));
        RadioButton stupid = radio("Stupid", "STUPID".equals(Prefs.bevelPreset(this)));
        RadioButton maximum = radio("Maximum Courage", "MAXIMUM".equals(Prefs.bevelPreset(this)));
        subtle.setId(View.generateViewId()); stupid.setId(View.generateViewId()); maximum.setId(View.generateViewId());
        presets.addView(subtle); presets.addView(stupid); presets.addView(maximum);
        root.addView(presets, margin(0, 3, 0, 14));

        root.addView(section("Witchcraft calibration"));
        calibrationStatus = text(calibrationSummary(), 14, false);
        root.addView(calibrationStatus);
        calibrationButton = button("Calibrate Witchcraft");
        calibrationButton.setOnClickListener(v -> beginCalibration());
        root.addView(calibrationButton, margin(0, 8, 0, 6));
        TextView hint = text("Two recordings: hold LEFT and move RIGHT, then hold RIGHT and move LEFT. Each sample needs at least 25° of hinge travel.", 13, false);
        hint.setTextColor(Color.DKGRAY);
        root.addView(hint, margin(0, 0, 0, 16));

        root.addView(section("Tools"));
        Button preview = button("Preview Effect — 5 seconds");
        preview.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Grant display-over-apps permission first.", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putBoolean(Prefs.MASTER, true).apply();
            master.setChecked(true);
            startEffects(EffectService.ACTION_PREVIEW);
        });
        root.addView(preview);

        Button diagnostics = button("Diagnostics");
        diagnostics.setOnClickListener(v -> startActivity(new Intent(this, DiagnosticsActivity.class)));
        root.addView(diagnostics, margin(0, 6, 0, 10));

        Switch reboot = new Switch(this);
        reboot.setText("Start after reboot");
        reboot.setChecked(prefs.getBoolean(Prefs.REBOOT, false));
        root.addView(reboot);

        TextView privacy = text("No internet. No camera. No Accessibility service. No file access. Effects are a single non-touchable overlay surface so Android 12+ pass-through touch limits are respected.", 12, false);
        privacy.setTextColor(Color.GRAY);
        root.addView(privacy, margin(0, 18, 0, 0));

        master.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean(Prefs.MASTER, checked).apply();
            if (checked) {
                if (Settings.canDrawOverlays(this)) startEffects(EffectService.ACTION_REFRESH);
                else Toast.makeText(this, "Grant display-over-apps permission, then enable again.", Toast.LENGTH_LONG).show();
            } else stopService(new Intent(this, EffectService.class));
        });
        fade.setOnCheckedChangeListener((b, checked) -> { prefs.edit().putBoolean(Prefs.FADE, checked).apply(); refreshService(); });
        bevel.setOnCheckedChangeListener((b, checked) -> { prefs.edit().putBoolean(Prefs.BEVEL, checked).apply(); refreshService(); });
        reboot.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean(Prefs.REBOOT, checked).apply());
        sides.setOnCheckedChangeListener((g, id) -> {
            String v = id == left.getId() ? "LEFT" : id == right.getId() ? "RIGHT" : "AUTO";
            prefs.edit().putString(Prefs.SIDE, v).apply(); refreshService();
        });
        presets.setOnCheckedChangeListener((g, id) -> {
            String v = id == subtle.getId() ? "SUBTLE" : id == maximum.getId() ? "MAXIMUM" : "STUPID";
            prefs.edit().putString(Prefs.BEVEL_PRESET, v).apply(); refreshService();
        });

        darkness.setOnSeekBarChangeListener(listener(p -> {
            prefs.edit().putInt(Prefs.DARKNESS, p).apply();
            refreshSliderLabels(p, start.getProgress(), finish.getProgress()); refreshService();
        }));
        start.setOnSeekBarChangeListener(listener(p -> {
            int f = finish.getProgress();
            if (f < p + 5) { f = Math.min(179, p + 5); finish.setProgress(f); }
            prefs.edit().putInt(Prefs.START, p).putInt(Prefs.FINISH, f).apply();
            refreshSliderLabels(darkness.getProgress(), p, f); refreshService();
        }));
        finish.setOnSeekBarChangeListener(listener(p -> {
            int s = start.getProgress();
            if (p < s + 5) { p = Math.min(179, s + 5); finish.setProgress(p); }
            prefs.edit().putInt(Prefs.FINISH, p).apply();
            refreshSliderLabels(darkness.getProgress(), s, p); refreshService();
        }));

        return scroll;
    }

    private void refreshSliderLabels(int d, int s, int f) {
        darknessLabel.setText("Maximum darkness: " + d + "%");
        startLabel.setText("Fade starts: " + s + "°");
        finishLabel.setText("Fully revealed: " + f + "°");
    }

    private SeekBar.OnSeekBarChangeListener listener(java.util.function.IntConsumer onChange) {
        return new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int p, boolean fromUser) { if (fromUser) onChange.accept(p); }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        };
    }

    private SeekBar seek(int min, int max, int value) {
        SeekBar s = new SeekBar(this);
        s.setMin(min);
        s.setMax(max);
        s.setProgress(Math.max(min, Math.min(max, value)));
        return s;
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(Color.BLACK);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private TextView section(String s) {
        TextView t = text(s, 18, true);
        t.setPadding(0, dp(10), 0, dp(6));
        return t;
    }

    private Button button(String s) { Button b = new Button(this); b.setText(s); b.setAllCaps(false); return b; }
    private RadioButton radio(String s, boolean checked) { RadioButton b = new RadioButton(this); b.setText(s); b.setChecked(checked); return b; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private LinearLayout.LayoutParams margin(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(l), dp(t), dp(r), dp(b)); return p;
    }

    private void startEffects(String action) {
        Intent i = new Intent(this, EffectService.class).setAction(action);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }

    private void refreshService() {
        if (Prefs.master(this) && Settings.canDrawOverlays(this) && (Prefs.fade(this) || Prefs.bevel(this))) startEffects(EffectService.ACTION_REFRESH);
        else if (!Prefs.fade(this) && !Prefs.bevel(this)) stopService(new Intent(this, EffectService.class));
    }

    private void requestNotificationPermissionIfUseful() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 31);
        }
    }

    private void beginCalibration() {
        if (recordingCalibration) return;
        if (hingeSensor == null || gyroSensor == null) {
            Toast.makeText(this, "This device is missing the public hinge or gyroscope sensor. Manual Left/Right still works.", Toast.LENGTH_LONG).show();
            return;
        }
        calibrationStage = 1;
        calibrationButton.setText("Record 1: Hold LEFT, move RIGHT");
        calibrationStatus.setText("Ready for sample 1. Tap the button again, then unfold while holding the LEFT panel still.");
        calibrationButton.setOnClickListener(v -> startCalibrationSample());
    }

    private void startCalibrationSample() {
        if (recordingCalibration) return;
        recordingCalibration = true;
        calHingeMin = Float.MAX_VALUE; calHingeMax = -Float.MAX_VALUE;
        calGyroSum = 0; calGyroSqSum = 0; calGyroCount = 0;
        sensorManager.registerListener(this, hingeSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
        calibrationButton.setEnabled(false);
        calibrationStatus.setText(calibrationStage == 1 ? "Recording: HOLD LEFT, MOVE RIGHT…" : "Recording: HOLD RIGHT, MOVE LEFT…");
        handler.postDelayed(this::finishCalibrationSample, 4200L);
    }

    private void finishCalibrationSample() {
        sensorManager.unregisterListener(this);
        recordingCalibration = false;
        calibrationButton.setEnabled(true);
        float travel = (calHingeMin == Float.MAX_VALUE || calHingeMax == -Float.MAX_VALUE) ? 0f : calHingeMax - calHingeMin;
        if (travel < 25f || calGyroCount < 8) {
            calibrationStatus.setText(String.format(Locale.US, "Sample rejected: %.1f° hinge travel. Move through at least 25° and try again.", travel));
            calibrationButton.setText(calibrationStage == 1 ? "Retry 1: Hold LEFT, move RIGHT" : "Retry 2: Hold RIGHT, move LEFT");
            return;
        }
        float mean = (float)(calGyroSum / calGyroCount);
        float variance = (float)Math.max(0, calGyroSqSum / calGyroCount - mean * mean);
        float noise = (float)Math.sqrt(variance);
        SharedPreferences.Editor e = prefs.edit();
        if (calibrationStage == 1) {
            e.putFloat(Prefs.CAL_RIGHT_MOVED, mean).apply();
            calibrationStage = 2;
            calibrationStatus.setText(String.format(Locale.US, "Sample 1 saved (gyro %.3f). Now hold RIGHT and move LEFT.", mean));
            calibrationButton.setText("Record 2: Hold RIGHT, move LEFT");
        } else {
            float first = prefs.getFloat(Prefs.CAL_RIGHT_MOVED, Float.NaN);
            float separation = Float.isNaN(first) ? 0f : Math.abs(first - mean);
            e.putFloat(Prefs.CAL_LEFT_MOVED, mean).putFloat(Prefs.CAL_NOISE, Math.max(0.03f, noise)).apply();
            calibrationStage = 0;
            calibrationStatus.setText(String.format(Locale.US, "Calibration saved. Separation %.3f; %s", separation, separation > Math.max(0.10f, noise * 1.5f) ? "good signal." : "weak signal; Auto may fall back Right."));
            calibrationButton.setText("Calibrate Witchcraft again");
            calibrationButton.setOnClickListener(v -> beginCalibration());
            refreshService();
        }
    }

    private String calibrationSummary() {
        float r = prefs.getFloat(Prefs.CAL_RIGHT_MOVED, Float.NaN);
        float l = prefs.getFloat(Prefs.CAL_LEFT_MOVED, Float.NaN);
        if (Float.isNaN(r) || Float.isNaN(l)) return "Witchcraft Auto is not calibrated yet. It will safely fall back to Right.";
        return String.format(Locale.US, "Calibrated. Right-moved %.3f, left-moved %.3f, separation %.3f.", r, l, Math.abs(r-l));
    }

    @Override public void onSensorChanged(SensorEvent e) {
        if (!recordingCalibration) return;
        if (e.sensor.getType() == Sensor.TYPE_HINGE_ANGLE && e.values.length > 0) {
            float a = e.values[0]; calHingeMin = Math.min(calHingeMin, a); calHingeMax = Math.max(calHingeMax, a);
        } else if (e.sensor.getType() == Sensor.TYPE_GYROSCOPE && e.values.length >= 3) {
            float x=e.values[0], y=e.values[1], z=e.values[2];
            double m = Math.sqrt(x*x+y*y+z*z);
            calGyroSum += m; calGyroSqSum += m*m; calGyroCount++;
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override protected void onResume() {
        super.onResume();
        if (permissionStatus != null) permissionStatus.setText(Settings.canDrawOverlays(this) ? "✓ Display-over-apps permission granted" : "⚠ Display-over-apps permission required");
        if (calibrationStatus != null && !recordingCalibration && calibrationStage == 0) calibrationStatus.setText(calibrationSummary());
        if (Prefs.master(this) && Settings.canDrawOverlays(this)) refreshService();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onDestroy();
    }
}
