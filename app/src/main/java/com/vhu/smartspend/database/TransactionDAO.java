package com.vhu.smartspend.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vhu.smartspend.model.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {
    private final DatabaseHelper dbHelper;

    public TransactionDAO(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    /**
     * Thêm giao dịch mới
     */
    public long insertTransaction(Transaction transaction) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", transaction.getUserId());
        values.put("category_id", transaction.getCategoryId());
        values.put("title", transaction.getTitle());
        values.put("amount", transaction.getAmount());
        values.put("transaction_date", transaction.getTransactionDate());
        values.put("note", transaction.getNote());
        values.put("receipt_uri", transaction.getReceiptUri());
        return db.insert("transactions", null, values);
    }

    /**
     * Cập nhật giao dịch
     */
    public int updateTransaction(Transaction transaction) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", transaction.getUserId());
        values.put("category_id", transaction.getCategoryId());
        values.put("title", transaction.getTitle());
        values.put("amount", transaction.getAmount());
        values.put("transaction_date", transaction.getTransactionDate());
        values.put("note", transaction.getNote());
        values.put("receipt_uri", transaction.getReceiptUri());

        return db.update("transactions", values, "transaction_id = ?", new String[]{String.valueOf(transaction.getTransactionId())});
    }

    /**
     * Xóa giao dịch
     */
    public int deleteTransaction(int transactionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete("transactions", "transaction_id = ?", new String[]{String.valueOf(transactionId)});
    }

    /**
     * Lấy toàn bộ giao dịch (kèm tên và loại danh mục)
     */
    public List<Transaction> getAllTransactions() {
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT t.transaction_id, t.user_id, t.category_id, t.title, t.amount, " +
                "t.transaction_date, t.note, t.receipt_uri, c.name AS category_name, c.type AS category_type, " +
                "c.icon_name AS category_icon_name, c.color_code AS category_color_code " +
                "FROM transactions t " +
                "LEFT JOIN categories c ON t.category_id = c.category_id " +
                "ORDER BY t.transaction_date DESC, t.transaction_id DESC";

        Cursor cursor = db.rawQuery(sql, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Transaction t = mapCursorToTransaction(cursor);
                list.add(t);
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Lấy toàn bộ giao dịch theo User ID
     */
    public List<Transaction> getAllTransactionsByUserId(int userId) {
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT t.transaction_id, t.user_id, t.category_id, t.title, t.amount, " +
                "t.transaction_date, t.note, t.receipt_uri, c.name AS category_name, c.type AS category_type, " +
                "c.icon_name AS category_icon_name, c.color_code AS category_color_code " +
                "FROM transactions t " +
                "LEFT JOIN categories c ON t.category_id = c.category_id " +
                "WHERE t.user_id = ? " +
                "ORDER BY t.transaction_date DESC, t.transaction_id DESC";

        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Transaction t = mapCursorToTransaction(cursor);
                list.add(t);
            }
            cursor.close();
        }
        return list;
    }

    private Transaction mapCursorToTransaction(Cursor cursor) {
        int tId = cursor.getInt(cursor.getColumnIndexOrThrow("transaction_id"));
        int uId = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
        int cId = cursor.getInt(cursor.getColumnIndexOrThrow("category_id"));
        String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
        String date = cursor.getString(cursor.getColumnIndexOrThrow("transaction_date"));
        String note = cursor.getString(cursor.getColumnIndexOrThrow("note"));
        String receipt = cursor.getString(cursor.getColumnIndexOrThrow("receipt_uri"));

        Transaction t = new Transaction(tId, uId, cId, title, amount, date, note, receipt);
        t.setCategoryName(cursor.getString(cursor.getColumnIndexOrThrow("category_name")));
        t.setCategoryType(cursor.getString(cursor.getColumnIndexOrThrow("category_type")));
        t.setCategoryIconName(cursor.getString(cursor.getColumnIndexOrThrow("category_icon_name")));
        t.setCategoryColorCode(cursor.getString(cursor.getColumnIndexOrThrow("category_color_code")));
        return t;
    }
}