package com.ifoldnt;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class DiagnosticsActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView report;
    private final Runnable refresh = new Runnable() {
        @Override public void run() { update(); handler.postDelayed(this, 500L); }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(30));
        scroll.addView(root);
        TextView title = new TextView(this); title.setText("iFoldn't Diagnostics"); title.setTextSize(25); title.setTextColor(Color.BLACK); title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);
        report = new TextView(this); report.setTextSize(14); report.setTextColor(Color.DKGRAY); report.setTextIsSelectable(true); report.setPadding(0, dp(14), 0, dp(14));
        root.addView(report);
        Button copy = new Button(this); copy.setText("Copy report"); copy.setAllCaps(false); copy.setOnClickListener(v -> copyReport()); root.addView(copy);
        Button close = new Button(this); close.setText("Back"); close.setAllCaps(false); close.setOnClickListener(v -> finish()); root.addView(close);
        setContentView(scroll);
    }

    private void update() {
        SensorManager sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        StringBuilder s = new StringBuilder();
        s.append(EffectService.diagnostics()).append("\n\nSensors:\n");
        List<Sensor> list = sm.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : list) {
            if (sensor.getType() == Sensor.TYPE_HINGE_ANGLE || sensor.getType() == Sensor.TYPE_GYROSCOPE || sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                s.append("• ").append(sensor.getName()).append(" [type ").append(sensor.getType()).append("]\n");
            }
        }
        s.append("\nOverlay permission: ").append(Settings.canDrawOverlays(this));
        s.append("\nMaster: ").append(Prefs.master(this));
        s.append("\nFade: ").append(Prefs.fade(this));
        s.append("\nBevel: ").append(Prefs.bevel(this));
        report.setText(s.toString());
    }

    private void copyReport() {
        ClipboardManager cb = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        cb.setPrimaryClip(ClipData.newPlainText("iFoldnt diagnostics", report.getText()));
        Toast.makeText(this, "Diagnostics copied.", Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
}
