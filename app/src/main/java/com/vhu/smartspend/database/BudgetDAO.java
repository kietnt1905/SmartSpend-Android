package com.vhu.smartspend.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vhu.smartspend.model.Budget;
import com.vhu.smartspend.model.Category;
import com.vhu.smartspend.model.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn thống kê thu/chi và ngân sách từ SQLite.
 */
public class BudgetDAO {

    private static final String SELECT_TX =
            "SELECT t.*, c.name AS category_name, c.type AS category_type, " +
            "c.icon_name AS category_icon_name, c.color_code AS category_color_code " +
            "FROM transactions t LEFT JOIN categories c ON t.category_id = c.category_id ";

    private final DatabaseHelper dbHelper;

    public BudgetDAO(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    /**
     * Lấy hạn mức ngân sách tháng của người dùng (ưu tiên bảng budgets, nếu chưa có lấy từ users).
     */
    public Budget getTotalMonthlyBudget(int userId, String monthYear) {
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            try (Cursor c = db.rawQuery(
                    "SELECT budget_id, user_id, month_year, limit_amount, alert_percent " +
                    "FROM budgets WHERE user_id = ? AND month_year = ? AND category_id IS NULL",
                    new String[]{String.valueOf(userId), monthYear})) {
                if (c.moveToFirst()) {
                    return new Budget(c.getInt(0), c.getInt(1), null, c.getString(2), c.getDouble(3), c.getInt(4));
                }
            }
            try (Cursor c = db.rawQuery(
                    "SELECT monthly_budget_limit FROM users WHERE user_id = ?",
                    new String[]{String.valueOf(userId)})) {
                if (c.moveToFirst()) {
                    return new Budget(userId, null, monthYear, c.getDouble(0), 80);
                }
            }
        }
        return null;
    }

    public double getTotalIncomeByMonth(int userId, String monthYear) {
        return getTotalAmountByDatePrefix(userId, Category.TYPE_INCOME, monthYear);
    }

    public double getTotalExpenseByMonth(int userId, String monthYear) {
        return getTotalAmountByDatePrefix(userId, Category.TYPE_EXPENSE, monthYear);
    }

    /**
     * Tính tổng thu hoặc chi theo tiền tố ngày (yyyy-MM-dd, yyyy-MM hoặc yyyy).
     */
    public double getTotalAmountByDatePrefix(int userId, String type, String datePrefix) {
        String sql = "SELECT COALESCE(SUM(t.amount), 0) " +
                "FROM transactions t INNER JOIN categories c ON t.category_id = c.category_id " +
                "WHERE t.user_id = ? AND c.type = ? AND t.transaction_date LIKE ? || '%'";
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor c = db.rawQuery(sql, new String[]{String.valueOf(userId), type, datePrefix})) {
            return c.moveToFirst() ? c.getDouble(0) : 0;
        }
    }

    /**
     * Tính tổng số dư khả dụng (Tổng Thu - Tổng Chi toàn thời gian).
     */
    public double getTotalBalance(int userId) {
        String sql = "SELECT COALESCE(SUM(CASE WHEN c.type = 'INCOME' THEN t.amount ELSE -t.amount END), 0) " +
                "FROM transactions t INNER JOIN categories c ON t.category_id = c.category_id " +
                "WHERE t.user_id = ?";
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor c = db.rawQuery(sql, new String[]{String.valueOf(userId)})) {
            return c.moveToFirst() ? c.getDouble(0) : 0;
        }
    }

    /**
     * Thống kê tổng tiền và số lượng giao dịch theo từng danh mục.
     */
    public List<CategorySummary> getCategorySummaryByDatePrefix(int userId, String type, String datePrefix) {
        List<CategorySummary> list = new ArrayList<>();
        String sql = "SELECT c.category_id, c.name, c.icon_name, c.color_code, " +
                "COUNT(t.transaction_id) AS tx_count, COALESCE(SUM(t.amount), 0) AS total_amount " +
                "FROM transactions t INNER JOIN categories c ON t.category_id = c.category_id " +
                "WHERE t.user_id = ? AND c.type = ? AND t.transaction_date LIKE ? || '%' " +
                "GROUP BY c.category_id, c.name, c.icon_name, c.color_code " +
                "ORDER BY total_amount DESC";
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor c = db.rawQuery(sql, new String[]{String.valueOf(userId), type, datePrefix})) {
            while (c.moveToNext()) {
                CategorySummary item = new CategorySummary();
                item.categoryId = c.getInt(0);
                item.name = c.getString(1);
                item.iconName = c.getString(2);
                item.colorCode = c.getString(3);
                item.transactionCount = c.getInt(4);
                item.totalAmount = c.getDouble(5);
                list.add(item);
            }
        }
        return list;
    }

    /**
     * Lấy giao dịch của một danh mục theo khoảng thời gian (sắp xếp mới nhất lên đầu).
     */
    public List<Transaction> getTransactionsByCategoryAndDatePrefix(int userId, int categoryId, String datePrefix) {
        return queryTransactions(
                SELECT_TX + "WHERE t.user_id = ? AND t.category_id = ? AND t.transaction_date LIKE ? || '%' " +
                "ORDER BY t.transaction_date DESC, t.transaction_id DESC",
                new String[]{String.valueOf(userId), String.valueOf(categoryId), datePrefix});
    }

    /**
     * Lấy các giao dịch gần đây nhất cho màn hình Trang chủ.
     */
    public List<Transaction> getRecentTransactions(int userId, int limit) {
        return queryTransactions(
                SELECT_TX + "WHERE t.user_id = ? ORDER BY t.transaction_date DESC, t.transaction_id DESC LIMIT ?",
                new String[]{String.valueOf(userId), String.valueOf(limit)});
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

    public static class CategorySummary {
        public int categoryId;
        public String name;
        public String iconName;
        public String colorCode;
        public int transactionCount;
        public double totalAmount;
    }
}
