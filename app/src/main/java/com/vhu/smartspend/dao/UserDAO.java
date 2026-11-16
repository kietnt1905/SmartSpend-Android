package com.vhu.smartspend.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vhu.smartspend.db.DatabaseHelper;
import com.vhu.smartspend.model.User;
import com.vhu.smartspend.utils.PasswordUtils;

public class UserDAO {
    private final DatabaseHelper helper;

    public UserDAO(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    public boolean isUsernameExists(String username) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT 1 FROM users WHERE username = ? COLLATE NOCASE",
                new String[]{username})) {
            return c.moveToFirst();
        }
    }

    /** Đăng ký tài khoản mới (role mặc định "user"). Trả về id, hoặc -1 nếu lỗi. */
    public long register(String username, String password) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("username", username);
        v.put("password_hash", PasswordUtils.hash(password));
        v.put("role", User.ROLE_USER);
        return db.insert("users", null, v);
    }

    /** Trả về User nếu đúng tài khoản và mật khẩu, ngược lại trả về null. */
    public User login(String username, String password) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, username, password_hash, role FROM users "
                        + "WHERE username = ? COLLATE NOCASE AND password_hash = ?",
                new String[]{username, PasswordUtils.hash(password)})) {
            if (c.moveToFirst()) {
                return new User(c.getInt(0), c.getString(1), c.getString(2), c.getString(3));
            }
            return null;
        }
    }
}
