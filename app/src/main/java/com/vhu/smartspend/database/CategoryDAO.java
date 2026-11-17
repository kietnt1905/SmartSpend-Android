package com.vhu.smartspend.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vhu.smartspend.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {
    private final DatabaseHelper helper;

    public CategoryDAO(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    private ContentValues toValues(Category c) {
        ContentValues v = new ContentValues();
        v.put("name", c.getName());
        v.put("type", c.getType());
        v.put("icon", c.getIcon());
        v.put("color", c.getColor());
        return v;
    }

    public long insert(Category c) {
        return helper.getWritableDatabase().insert("categories", null, toValues(c));
    }

    public int update(Category c) {
        return helper.getWritableDatabase().update("categories", toValues(c),
                "id = ?", new String[]{String.valueOf(c.getId())});
    }

    public int delete(int id) {
        return helper.getWritableDatabase().delete("categories",
                "id = ?", new String[]{String.valueOf(id)});
    }

    public List<Category> getAll() {
        return query("SELECT id, name, type, icon, color FROM categories ORDER BY type DESC, name",
                null);
    }

    public List<Category> getByType(String type) {
        return query("SELECT id, name, type, icon, color FROM categories WHERE type = ? ORDER BY name",
                new String[]{type});
    }

    private List<Category> query(String sql, String[] args) {
        List<Category> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(sql, args)) {
            while (c.moveToNext()) {
                list.add(new Category(c.getInt(0), c.getString(1), c.getString(2),
                        c.getString(3), c.getString(4)));
            }
        }
        return list;
    }
}
