package com.vhu.smartspend.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.vhu.smartspend.model.User;

/** Lưu phiên đăng nhập vào SharedPreferences. */
public class SessionManager {
    private static final String PREFS = "smartspend_session";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveSession(User user) {
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putInt(KEY_USER_ID, user.getId())
                .putString(KEY_USERNAME, user.getUsername())
                .putString(KEY_ROLE, user.getRole())
                .apply();
    }

    public boolean isLoggedIn() { return prefs.getBoolean(KEY_LOGGED_IN, false); }
    public int getUserId() { return prefs.getInt(KEY_USER_ID, -1); }
    public String getUsername() { return prefs.getString(KEY_USERNAME, ""); }
    public String getRole() { return prefs.getString(KEY_ROLE, User.ROLE_USER); }
    public boolean isAdmin() { return User.ROLE_ADMIN.equals(getRole()); }

    public void logout() { prefs.edit().clear().apply(); }
}
