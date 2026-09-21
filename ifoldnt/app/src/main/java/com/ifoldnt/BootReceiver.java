package com.ifoldnt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        if (!Prefs.get(context).getBoolean(Prefs.REBOOT, false) || !Prefs.master(context)) return;
        if (!Settings.canDrawOverlays(context) || (!Prefs.fade(context) && !Prefs.bevel(context))) return;
        Intent service = new Intent(context, EffectService.class).setAction(EffectService.ACTION_REFRESH);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service); else context.startService(service);
    }
}
