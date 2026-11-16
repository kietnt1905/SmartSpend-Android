package com.vhu.smartspend.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/** Lưu và áp dụng Dark Mode. */
public class ThemeHelper {
    private static final String PREFS = "smartspend_settings";
    private static final String KEY_DARK = "dark_mode";

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isDarkMode(Context c) {
        return prefs(c).getBoolean(KEY_DARK, false);
    }

    public static void setDarkMode(Context c, boolean on) {
        prefs(c).edit().putBoolean(KEY_DARK, on).apply();
        apply(on);
    }

    /** Gọi ở Application.onCreate() để giữ chế độ khi mở lại app. */
    public static void applySaved(Context c) {
        apply(isDarkMode(c));
    }

    private static void apply(boolean on) {
        AppCompatDelegate.setDefaultNightMode(
                on ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
