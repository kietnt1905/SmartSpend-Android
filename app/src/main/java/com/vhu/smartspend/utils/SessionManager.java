package com.vhu.smartspend.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "SmartSpendPrefs";
    private final SharedPreferences sharedPreferences;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // TODO: Quản lý phiên đăng nhập và cài đặt Dark Mode bằng SharedPreferences
}
