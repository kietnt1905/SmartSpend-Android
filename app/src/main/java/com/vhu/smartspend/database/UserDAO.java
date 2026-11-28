package com.vhu.smartspend.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vhu.smartspend.model.User;
import com.vhu.smartspend.utils.PasswordUtils;

public class UserDAO {
    private final DatabaseHelper dbHelper;

    public UserDAO(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    public boolean registerUser(User user) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("username", user.getUsername());
            values.put("password", PasswordUtils.hashPassword(user.getPassword()));
            values.put("full_name", user.getFullName());
            values.put("monthly_budget_limit", user.getMonthlyBudgetLimit() > 0 ? user.getMonthlyBudgetLimit() : 5000000);
            if (user.getAvatarUri() != null) {
                values.put("avatar_uri", user.getAvatarUri());
            }

            long userId = db.insert("users", null, values);
            if (userId != -1) {
                DatabaseHelper.seedDefaultCategoriesForUser(db, userId);
                return true;
            }
        }
        return false;
    }

    public User checkLogin(String username, String password) {
        User matchedUser = null;
        boolean needsHashUpgrade = false;
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor c = db.rawQuery(
                     "SELECT user_id, username, password, full_name, monthly_budget_limit, avatar_uri FROM users WHERE username = ?",
                     new String[]{username})) {
            if (c.moveToFirst()) {
                String storedPassword = c.getString(2);
                if (PasswordUtils.verifyPassword(password, storedPassword)) {
                    matchedUser = new User(c.getInt(0), c.getString(1), storedPassword, c.getString(3), c.getDouble(4));
                    matchedUser.setAvatarUri(c.getString(5));
                    needsHashUpgrade = !PasswordUtils.isHashed(storedPassword);
                }
            }
        }
        if (matchedUser != null && needsHashUpgrade) {
            String newHash = PasswordUtils.hashPassword(password);
            updatePasswordHash(matchedUser.getUserId(), newHash);
            matchedUser.setPassword(newHash);
        }
        return matchedUser;
    }

    public boolean checkUsernameExists(String username) {
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor c = db.rawQuery("SELECT user_id FROM users WHERE username = ?", new String[]{username})) {
            return c.getCount() > 0;
        }
    }

    public User getUserById(int userId) {
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor c = db.rawQuery(
                     "SELECT user_id, username, password, full_name, monthly_budget_limit, avatar_uri FROM users WHERE user_id = ?",
                     new String[]{String.valueOf(userId)})) {
            if (c.moveToFirst()) {
                User user = new User(c.getInt(0), c.getString(1), c.getString(2), c.getString(3), c.getDouble(4));
                user.setAvatarUri(c.getString(5));
                return user;
            }
        }
        return null;
    }

    public void updateFullName(int userId, String fullName) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues cv = new ContentValues();
            cv.put("full_name", fullName);
            db.update("users", cv, "user_id = ?", new String[]{String.valueOf(userId)});
        }
    }

    public void updateMonthlyLimit(int userId, double limit) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues cv = new ContentValues();
            cv.put("monthly_budget_limit", limit);
            db.update("users", cv, "user_id = ?", new String[]{String.valueOf(userId)});
        }
    }

    public void updateAvatarUri(int userId, String avatarUri) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues cv = new ContentValues();
            cv.put("avatar_uri", avatarUri);
            db.update("users", cv, "user_id = ?", new String[]{String.valueOf(userId)});
        }
    }

    public boolean checkPassword(int userId, String password) {
        String storedPassword = null;
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor c = db.rawQuery("SELECT password FROM users WHERE user_id = ?",
                     new String[]{String.valueOf(userId)})) {
            if (c.moveToFirst()) {
                storedPassword = c.getString(0);
            }
        }
        if (!PasswordUtils.verifyPassword(password, storedPassword)) {
            return false;
        }
        if (!PasswordUtils.isHashed(storedPassword)) {
            updatePassword(userId, password);
        }
        return true;
    }

    public boolean updatePassword(int userId, String newPassword) {
        return updatePasswordHash(userId, PasswordUtils.hashPassword(newPassword));
    }

    private boolean updatePasswordHash(int userId, String hashedPassword) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues cv = new ContentValues();
            cv.put("password", hashedPassword);
            int rows = db.update("users", cv, "user_id = ?", new String[]{String.valueOf(userId)});
            return rows > 0;
        }
    }
}
