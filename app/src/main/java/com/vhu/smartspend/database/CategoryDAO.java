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

    /**
     * Thêm danh mục mới vào SQLite theo userId
     */
    public long insertCategory(Category category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", category.getUserId());
        values.put("name", category.getName());
        values.put("type", category.getType());
        values.put("icon_name", category.getIconName());
        values.put("color_code", category.getColorCode());
        return db.insert("categories", null, values);
    }

    /**
     * Cập nhật thông tin danh mục của đúng người dùng
     */
    public int updateCategory(Category category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", category.getName());
        values.put("type", category.getType());
        values.put("icon_name", category.getIconName());
        values.put("color_code", category.getColorCode());

        return db.update(
                "categories",
                values,
                "category_id = ? AND user_id = ?",
                new String[]{String.valueOf(category.getCategoryId()), String.valueOf(category.getUserId())}
        );
    }

    /**
     * Kiểm tra tên danh mục đã tồn tại trong cùng loại Thu/Chi của người dùng chưa
     */
    public boolean existsCategoryName(int userId, String name, String type, int excludeCategoryId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM categories WHERE user_id = ? AND LOWER(TRIM(name)) = LOWER(TRIM(?)) AND type = ? AND category_id != ?",
                new String[]{String.valueOf(userId), name, type, String.valueOf(excludeCategoryId)}
        );
        boolean exists = false;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0;
            }
            cursor.close();
        }
        return exists;
    }

    /**
     * Kiểm tra danh mục có đang được sử dụng trong bảng giao dịch không
     */
    public boolean isCategoryInUse(int categoryId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM transactions WHERE category_id = ?",
                new String[]{String.valueOf(categoryId)}
        );
        boolean inUse = false;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                inUse = cursor.getInt(0) > 0;
            }
            cursor.close();
        }
        return inUse;
    }

    /**
     * Xóa danh mục theo ID và userId (trả về -1 nếu bị ràng buộc khóa ngoại)
     */
    public int deleteCategory(int categoryId, int userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            return db.delete(
                    "categories",
                    "category_id = ? AND user_id = ?",
                    new String[]{String.valueOf(categoryId), String.valueOf(userId)}
            );
        } catch (SQLiteConstraintException e) {
            return -1;
        }
    }

    /**
     * Lấy toàn bộ danh sách danh mục của người dùng
     */
    public List<Category> getAllCategories(int userId) {
        List<Category> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT category_id, user_id, name, type, icon_name, color_code FROM categories WHERE user_id = ? ORDER BY category_id DESC",
                new String[]{String.valueOf(userId)}
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("category_id"));
                int uId = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                String iconName = cursor.getString(cursor.getColumnIndexOrThrow("icon_name"));
                String colorCode = cursor.getString(cursor.getColumnIndexOrThrow("color_code"));

                list.add(new Category(id, uId, name, type, iconName, colorCode));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Lấy danh sách danh mục theo loại (EXPENSE / INCOME) của người dùng
     */
    public List<Category> getCategoriesByType(int userId, String type) {
        List<Category> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT category_id, user_id, name, type, icon_name, color_code FROM categories WHERE user_id = ? AND type = ? ORDER BY category_id DESC",
                new String[]{String.valueOf(userId), type}
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("category_id"));
                int uId = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String cType = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                String iconName = cursor.getString(cursor.getColumnIndexOrThrow("icon_name"));
                String colorCode = cursor.getString(cursor.getColumnIndexOrThrow("color_code"));

                list.add(new Category(id, uId, name, cType, iconName, colorCode));
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
                "SELECT category_id, user_id, name, type, icon_name, color_code FROM categories WHERE category_id = ?",
                new String[]{String.valueOf(categoryId)}
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int uId = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                String iconName = cursor.getString(cursor.getColumnIndexOrThrow("icon_name"));
                String colorCode = cursor.getString(cursor.getColumnIndexOrThrow("color_code"));

                category = new Category(categoryId, uId, name, type, iconName, colorCode);
            }
            cursor.close();
        }
        return category;
    }
}
