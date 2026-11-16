package com.vhu.smartspend.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.vhu.smartspend.model.Category;
import com.vhu.smartspend.model.User;
import com.vhu.smartspend.utils.PasswordUtils;

/**
 * Nếu nhóm đã có DatabaseHelper, hãy gộp 3 câu CREATE TABLE và phần seed vào file của nhóm.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "smartspend.db";
    public static final int DB_VERSION = 1;

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "username TEXT NOT NULL UNIQUE,"
                + "password_hash TEXT NOT NULL,"
                + "role TEXT NOT NULL DEFAULT 'user')");

        db.execSQL("CREATE TABLE categories ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "type TEXT NOT NULL,"
                + "icon TEXT,"
                + "color TEXT)");

        db.execSQL("CREATE TABLE transactions ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER NOT NULL,"
                + "category_id INTEGER,"
                + "type TEXT NOT NULL,"
                + "amount REAL NOT NULL,"
                + "note TEXT,"
                + "date TEXT NOT NULL,"
                + "FOREIGN KEY(user_id) REFERENCES users(id),"
                + "FOREIGN KEY(category_id) REFERENCES categories(id))");

        // Tài khoản admin mặc định: admin / admin123
        ContentValues u = new ContentValues();
        u.put("username", "admin");
        u.put("password_hash", PasswordUtils.hash("admin123"));
        u.put("role", User.ROLE_ADMIN);
        db.insert("users", null, u);

        // Danh mục mẫu
        seed(db, "Ăn uống", Category.TYPE_EXPENSE, "🍜", "#FF9800");
        seed(db, "Học phí", Category.TYPE_EXPENSE, "🎓", "#3F51B5");
        seed(db, "Mua sắm", Category.TYPE_EXPENSE, "🛍️", "#E91E63");
        seed(db, "Di chuyển", Category.TYPE_EXPENSE, "🚌", "#009688");
        seed(db, "Lương", Category.TYPE_INCOME, "💰", "#4CAF50");
        seed(db, "Học bổng", Category.TYPE_INCOME, "🎁", "#9C27B0");
    }

    private void seed(SQLiteDatabase db, String name, String type, String icon, String color) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("type", type);
        v.put("icon", icon);
        v.put("color", color);
        db.insert("categories", null, v);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS transactions");
        db.execSQL("DROP TABLE IF EXISTS categories");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }
}
