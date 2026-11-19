package com.vhu.smartspend.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "smart_spend.db";
    public static final int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Bảng Users (Phân quyền & Hạn mức tháng)
        db.execSQL("CREATE TABLE users (" +
                "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL, " +
                "full_name TEXT NOT NULL, " +
                "role TEXT DEFAULT 'USER', " +
                "monthly_budget_limit REAL DEFAULT 5000000)");

        // 2. Bảng Categories (Danh mục Thu/Chi)
        db.execSQL("CREATE TABLE categories (" +
                "category_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "type TEXT NOT NULL, " +
                "icon_name TEXT, " +
                "color_code TEXT)");

        // 3. Bảng Transactions (Khoản Thu/Chi)
        db.execSQL("CREATE TABLE transactions (" +
                "transaction_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "category_id INTEGER NOT NULL, " +
                "title TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "transaction_date TEXT NOT NULL, " +
                "note TEXT, " +
                "receipt_uri TEXT, " +
                "FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE, " +
                "FOREIGN KEY(category_id) REFERENCES categories(category_id))");

        // 4. Bảng Budgets (Ngân sách theo danh mục & tháng)
        db.execSQL("CREATE TABLE budgets (" +
                "budget_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "category_id INTEGER, " +
                "month_year TEXT NOT NULL, " +
                "limit_amount REAL NOT NULL, " +
                "alert_percent INTEGER DEFAULT 80, " +
                "FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE, " +
                "FOREIGN KEY(category_id) REFERENCES categories(category_id) ON DELETE CASCADE)");

        // Chèn dữ liệu mẫu (Seed data) phục vụ test và demo ngay lập tức
        db.execSQL("INSERT INTO users (username, password, full_name, role, monthly_budget_limit) " +
                "VALUES ('kietvhu', '123456', 'Nguyễn Tuấn Kiệt', 'ADMIN', 4500000)");
        db.execSQL("INSERT INTO categories (name, type, icon_name, color_code) VALUES " +
                "('Ăn uống', 'EXPENSE', 'ic_food', '#FF5722'), " +
                "('Học phí', 'EXPENSE', 'ic_school', '#2196F3'), " +
                "('Mua sắm', 'EXPENSE', 'ic_shopping', '#9C27B0'), " +
                "('Di chuyển', 'EXPENSE', 'ic_transport', '#009688'), " +
                "('Học bổng', 'INCOME', 'ic_award', '#4CAF50'), " +
                "('Làm thêm', 'INCOME', 'ic_work', '#FFC107')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS budgets");
        db.execSQL("DROP TABLE IF EXISTS transactions");
        db.execSQL("DROP TABLE IF EXISTS categories");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }
}