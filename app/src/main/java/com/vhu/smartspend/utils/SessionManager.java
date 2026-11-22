package com.vhu.smartspend.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.vhu.smartspend.model.User;

public class SessionManager {
    private static final String PREF_NAME = "SmartSpendPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULL_NAME = "full_name";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    /**
     * Tạo phiên đăng nhập người dùng
     */
    public void createLoginSession(User user) {
        if (user == null) return;
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, user.getUserId());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_FULL_NAME, user.getFullName());
        editor.apply();
    }

    /**
     * Kiểm tra xem người dùng đã đăng nhập chưa
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Lấy ID người dùng hiện tại
     */
    public int getUserId() {
        return sharedPreferences.getInt(KEY_USER_ID, -1);
    }

    /**
     * Lấy Tên đăng nhập
     */
    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, "");
    }

    /**
     * Lấy Họ và tên
     */
    public String getFullName() {
        return sharedPreferences.getString(KEY_FULL_NAME, "");
    }

    /**
     * Đăng xuất người dùng (xóa dữ liệu phiên đăng nhập)
     */
    public void logout() {
        editor.clear();
        editor.apply();
    }
}
