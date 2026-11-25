package com.vhu.smartspend.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vhu.smartspend.model.Category;
import com.vhu.smartspend.model.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    private static final String SELECT_TX =
            "SELECT t.*, c.name AS category_name, c.type AS category_type, " +
            "c.icon_name AS category_icon_name, c.color_code AS category_color_code " +
            "FROM transactions t LEFT JOIN categories c ON t.category_id = c.category_id ";

    private final DatabaseHelper dbHelper;

    public TransactionDAO(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    public long insertTransaction(Transaction t) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues cv = toValues(t);
            cv.put("user_id", t.getUserId());
            return db.insert("transactions", null, cv);
        }
    }

    public int updateTransaction(Transaction t) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            return db.update("transactions", toValues(t), "transaction_id = ?",
                    new String[]{String.valueOf(t.getTransactionId())});
        }
    }

    public int deleteTransaction(int transactionId) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            return db.delete("transactions", "transaction_id = ?", new String[]{String.valueOf(transactionId)});
        }
    }

    public Transaction getTransactionById(int transactionId) {
        List<Transaction> list = queryTransactions(
                SELECT_TX + "WHERE t.transaction_id = ?",
                new String[]{String.valueOf(transactionId)});
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Transaction> getTransactionsByUser(int userId) {
        return queryTransactions(
                SELECT_TX + "WHERE t.user_id = ? ORDER BY t.transaction_date DESC, t.transaction_id DESC",
                new String[]{String.valueOf(userId)});
    }

    public List<Transaction> searchTransactions(int userId, String keyword) {
        String like = "%" + (keyword != null ? keyword.trim() : "") + "%";
        return queryTransactions(
                SELECT_TX + "WHERE t.user_id = ? AND (t.title LIKE ? OR t.note LIKE ? OR c.name LIKE ?) " +
                "ORDER BY t.transaction_date DESC, t.transaction_id DESC",
                new String[]{String.valueOf(userId), like, like, like});
    }

    public List<Category> getCategoriesByType(int userId, String type) {
        return queryCategories(
                "SELECT category_id, name, type, icon_name, color_code FROM categories WHERE user_id = ? AND type = ? ORDER BY category_id ASC",
                new String[]{String.valueOf(userId), type});
    }

    public List<Category> getAllCategories(int userId) {
        return queryCategories(
                "SELECT category_id, name, type, icon_name, color_code FROM categories WHERE user_id = ? ORDER BY category_id ASC",
                new String[]{String.valueOf(userId)});
    }

    private ContentValues toValues(Transaction t) {
        ContentValues cv = new ContentValues();
        cv.put("category_id", t.getCategoryId());
        cv.put("title", t.getTitle());
        cv.put("amount", t.getAmount());
        cv.put("transaction_date", t.getTransactionDate());
        cv.put("note", t.getNote());
        cv.put("receipt_uri", t.getReceiptUri());
        return cv;
    }

    private List<Transaction> queryTransactions(String sql, String[] args) {
        List<Transaction> list = new ArrayList<>();
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor c = db.rawQuery(sql, args)) {
            while (c.moveToNext()) {
                Transaction t = new Transaction(
                        c.getInt(c.getColumnIndexOrThrow("transaction_id")),
                        c.getInt(c.getColumnIndexOrThrow("user_id")),
                        c.getInt(c.getColumnIndexOrThrow("category_id")),
                        c.getString(c.getColumnIndexOrThrow("title")),
                        c.getDouble(c.getColumnIndexOrThrow("amount")),
                        c.getString(c.getColumnIndexOrThrow("transaction_date")),
                        c.getString(c.getColumnIndexOrThrow("note")),
                        c.getString(c.getColumnIndexOrThrow("receipt_uri"))
                );
                t.setCategoryName(c.getString(c.getColumnIndexOrThrow("category_name")));
                t.setCategoryType(c.getString(c.getColumnIndexOrThrow("category_type")));
                t.setCategoryIconName(c.getString(c.getColumnIndexOrThrow("category_icon_name")));
                t.setCategoryColorCode(c.getString(c.getColumnIndexOrThrow("category_color_code")));
                list.add(t);
            }
        }
        return list;
    }

    private List<Category> queryCategories(String sql, String[] args) {
        List<Category> list = new ArrayList<>();
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor c = db.rawQuery(sql, args)) {
            while (c.moveToNext()) {
                list.add(new Category(c.getInt(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4)));
            }
        }
        return list;
    }
}
