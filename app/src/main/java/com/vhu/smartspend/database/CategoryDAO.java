package com.vhu.smartspend.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vhu.smartspend.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {
    private final DatabaseHelper dbHelper;

    public CategoryDAO(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    /**
     * Thêm danh mục mới vào SQLite
     */
    public long insertCategory(Category category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", category.getName());
        values.put("type", category.getType());
        values.put("icon_name", category.getIconName());
        values.put("color_code", category.getColorCode());
        return db.insert("categories", null, values);
    }

    /**
     * Cập nhật thông tin danh mục
     */
    public int updateCategory(Category category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", category.getName());
        values.put("type", category.getType());
        values.put("icon_name", category.getIconName());
        values.put("color_code", category.getColorCode());

        return db.update("categories", values, "category_id = ?", new String[]{String.valueOf(category.getCategoryId())});
    }

    /**
     * Xóa danh mục theo ID
     */
    public int deleteCategory(int categoryId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete("categories", "category_id = ?", new String[]{String.valueOf(categoryId)});
    }

    /**
     * Lấy toàn bộ danh sách danh mục
     */
    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT category_id, name, type, icon_name, color_code FROM categories ORDER BY category_id DESC", null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("category_id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                String iconName = cursor.getString(cursor.getColumnIndexOrThrow("icon_name"));
                String colorCode = cursor.getString(cursor.getColumnIndexOrThrow("color_code"));

                list.add(new Category(id, name, type, iconName, colorCode));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Lấy danh sách danh mục theo loại (EXPENSE / INCOME)
     */
    public List<Category> getCategoriesByType(String type) {
        List<Category> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT category_id, name, type, icon_name, color_code FROM categories WHERE type = ? ORDER BY category_id DESC",
                new String[]{type}
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("category_id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String cType = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                String iconName = cursor.getString(cursor.getColumnIndexOrThrow("icon_name"));
                String colorCode = cursor.getString(cursor.getColumnIndexOrThrow("color_code"));

                list.add(new Category(id, name, cType, iconName, colorCode));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Lấy danh mục theo ID
     */
    public Category getCategoryById(int categoryId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Category category = null;
        Cursor cursor = db.rawQuery(
                "SELECT category_id, name, type, icon_name, color_code FROM categories WHERE category_id = ?",
                new String[]{String.valueOf(categoryId)}
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                String iconName = cursor.getString(cursor.getColumnIndexOrThrow("icon_name"));
                String colorCode = cursor.getString(cursor.getColumnIndexOrThrow("color_code"));

                category = new Category(categoryId, name, type, iconName, colorCode);
            }
            cursor.close();
        }
        return category;
    }
}