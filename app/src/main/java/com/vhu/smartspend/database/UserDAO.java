package com.vhu.smartspend.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vhu.smartspend.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private final DatabaseHelper dbHelper;

    public UserDAO(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    /**
     * Đăng ký người dùng mới và khởi tạo danh mục mặc định riêng cho người dùng đó
     */
    public boolean registerUser(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", user.getUsername());
        values.put("password", user.getPassword());
        values.put("full_name", user.getFullName());
        values.put("monthly_budget_limit", user.getMonthlyBudgetLimit() > 0 ? user.getMonthlyBudgetLimit() : 5000000);

        long userId = db.insert("users", null, values);
        if (userId != -1) {
            DatabaseHelper.seedDefaultCategoriesForUser(db, userId);
            return true;
        }
        return false;
    }

    /**
     * Kiểm tra đăng nhập người dùng
     */
    public User checkLogin(String username, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        User user = null;
        Cursor cursor = db.rawQuery(
                "SELECT user_id, username, password, full_name, monthly_budget_limit FROM users WHERE username = ? AND password = ?",
                new String[]{username, password}
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
                String uName = cursor.getString(cursor.getColumnIndexOrThrow("username"));
                String pwd = cursor.getString(cursor.getColumnIndexOrThrow("password"));
                String fullName = cursor.getString(cursor.getColumnIndexOrThrow("full_name"));
                double limit = cursor.getDouble(cursor.getColumnIndexOrThrow("monthly_budget_limit"));

                user = new User(id, uName, pwd, fullName, limit);
            }
            cursor.close();
        }
        return user;
    }

    /**
     * Kiểm tra username đã tồn tại hay chưa
     */
    public boolean checkUsernameExists(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT user_id FROM users WHERE username = ?", new String[]{username});
        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }
        return exists;
    }

    /**
     * Lấy thông tin user theo ID
     */
    public User getUserById(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        User user = null;
        Cursor cursor = db.rawQuery(
                "SELECT user_id, username, password, full_name, monthly_budget_limit FROM users WHERE user_id = ?",
                new String[]{String.valueOf(userId)}
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String uName = cursor.getString(cursor.getColumnIndexOrThrow("username"));
                String pwd = cursor.getString(cursor.getColumnIndexOrThrow("password"));
                String fullName = cursor.getString(cursor.getColumnIndexOrThrow("full_name"));
                double limit = cursor.getDouble(cursor.getColumnIndexOrThrow("monthly_budget_limit"));

                user = new User(userId, uName, pwd, fullName, limit);
            }
            cursor.close();
        }
        return user;
    }

    /**
     * Cập nhật thông tin user
     */
    public boolean updateUser(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("full_name", user.getFullName());
        values.put("password", user.getPassword());
        values.put("monthly_budget_limit", user.getMonthlyBudgetLimit());

        int rows = db.update("users", values, "user_id = ?", new String[]{String.valueOf(user.getUserId())});
        return rows > 0;
    }
}
