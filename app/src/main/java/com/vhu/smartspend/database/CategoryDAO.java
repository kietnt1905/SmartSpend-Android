package com.vhu.smartspend.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.vhu.smartspend.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {
    private final DatabaseHelper dbHelper;

    public CategoryDAO(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    public long insertCategory(Category category) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("user_id", category.getUserId());
            values.put("name", category.getName());
            values.put("type", category.getType());
            values.put("icon_name", category.getIconName());
            values.put("color_code", category.getColorCode());
            return db.insert("categories", null, values);
        }
    }

    public int updateCategory(Category category) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("name", category.getName());
            values.put("type", category.getType());
            values.put("icon_name", category.getIconName());
            values.put("color_code", category.getColorCode());
            return db.update("categories", values, "category_id = ? AND user_id = ?",
                    new String[]{String.valueOf(category.getCategoryId()), String.valueOf(category.getUserId())});
        }
    }

    public boolean existsCategoryName(int userId, String name, String type, int excludeCategoryId) {
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor c = db.rawQuery(
                     "SELECT COUNT(*) FROM categories WHERE user_id = ? AND LOWER(TRIM(name)) = LOWER(TRIM(?)) AND type = ? AND category_id != ?",
                     new String[]{String.valueOf(userId), name, type, String.valueOf(excludeCategoryId)})) {
            return c.moveToFirst() && c.getInt(0) > 0;
        }
    }

    public boolean isCategoryInUse(int categoryId) {
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor c = db.rawQuery("SELECT COUNT(*) FROM transactions WHERE category_id = ?",
                     new String[]{String.valueOf(categoryId)})) {
            return c.moveToFirst() && c.getInt(0) > 0;
        }
    }

    public int deleteCategory(int categoryId, int userId) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            return db.delete("categories", "category_id = ? AND user_id = ?",
                    new String[]{String.valueOf(categoryId), String.valueOf(userId)});
        } catch (SQLiteConstraintException e) {
            return -1;
        }
    }

    public List<Category> getAllCategories(int userId) {
        List<Category> list = new ArrayList<>();
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor c = db.rawQuery(
                     "SELECT category_id, user_id, name, type, icon_name, color_code FROM categories WHERE user_id = ? ORDER BY category_id DESC",
                     new String[]{String.valueOf(userId)})) {
            while (c.moveToNext()) {
                list.add(new Category(c.getInt(0), c.getInt(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5)));
            }
        }
        return list;
    }
}
