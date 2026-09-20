package com.ifoldnt;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static final String FILE = "ifoldnt";
    static final String MASTER = "master";
    static final String FADE = "fade";
    static final String BEVEL = "bevel";
    static final String REBOOT = "reboot";
    static final String SIDE = "side";
    static final String DARKNESS = "darkness";
    static final String START = "start";
    static final String FINISH = "finish";
    static final String BEVEL_PRESET = "bevel_preset";
    static final String CAL_RIGHT_MOVED = "cal_right_moved";
    static final String CAL_LEFT_MOVED = "cal_left_moved";
    static final String CAL_NOISE = "cal_noise";

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static boolean master(Context c) { return get(c).getBoolean(MASTER, false); }
    static boolean fade(Context c) { return get(c).getBoolean(FADE, true); }
    static boolean bevel(Context c) { return get(c).getBoolean(BEVEL, false); }
    static String side(Context c) { return get(c).getString(SIDE, "AUTO"); }
    static int darkness(Context c) { return get(c).getInt(DARKNESS, 65); }
    static int start(Context c) { return get(c).getInt(START, 90); }
    static int finish(Context c) { return get(c).getInt(FINISH, 178); }
    static String bevelPreset(Context c) { return get(c).getString(BEVEL_PRESET, "STUPID"); }

    static int bevelDp(Context c) {
        switch (bevelPreset(c)) {
            case "SUBTLE": return 18;
            case "MAXIMUM": return 52;
            default: return 32;
        }
    }

    private Prefs() {}
}
