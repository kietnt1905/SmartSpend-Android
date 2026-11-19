package com.vhu.smartspend.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.vhu.smartspend.model.Budget;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Quản lý Thao tác Dữ liệu Ngân sách (Budget Data Access Object)
 * Phụ trách các thao tác CRUD và thống kê thu chi với cơ sở dữ liệu SQLite.
 *
 * Tác giả: Lê Phúc Thịnh (MSSV: 231A290032)
 */
public class BudgetDAO {
    private static final String TAG = "BudgetDAO";
    private final DatabaseHelper dbHelper;

    public BudgetDAO(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    // =========================================================================
    // 1. CÁC PHƯƠNG THỨC THIẾT LẬP HẠN MỨC NGÂN SÁCH (THÁNG & DANH MỤC)
    // =========================================================================

    /**
     * Thiết lập hoặc Cập nhật hạn mức ngân sách tổng theo tháng của người dùng.
     * Lưu vào bảng 'budgets' với category_id = NULL.
     *
     * @param userId ID người dùng
     * @param monthYear Định dạng yyyy-MM (ví dụ: "2026-11")
     * @param limitAmount Số tiền ngân sách giới hạn
     * @param alertPercent Phần trăm cảnh báo (mặc định 80%)
     * @return ID dòng được chèn hoặc số dòng cập nhật (-1 nếu thất bại)
     */
    public long saveOrUpdateMonthlyBudget(int userId, String monthYear, double limitAmount, int alertPercent) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long result = -1;

        try {
            // Kiểm tra xem đã có ngân sách tổng tháng này chưa
            Cursor cursor = db.rawQuery(
                    "SELECT budget_id FROM budgets WHERE user_id = ? AND month_year = ? AND category_id IS NULL",
                    new String[]{String.valueOf(userId), monthYear});

            ContentValues values = new ContentValues();
            values.put("user_id", userId);
            values.putNull("category_id");
            values.put("month_year", monthYear);
            values.put("limit_amount", limitAmount);
            values.put("alert_percent", alertPercent);

            if (cursor != null && cursor.moveToFirst()) {
                // Đã tồn tại -> Thực hiện UPDATE
                int budgetId = cursor.getInt(cursor.getColumnIndexOrThrow("budget_id"));
                cursor.close();
                result = db.update("budgets", values, "budget_id = ?", new String[]{String.valueOf(budgetId)});
                Log.d(TAG, "Đã cập nhật ngân sách tổng tháng " + monthYear + " cho userId: " + userId);
            } else {
                // Chưa tồn tại -> Thực hiện INSERT
                if (cursor != null) cursor.close();
                result = db.insert("budgets", null, values);
                Log.d(TAG, "Đã thêm mới ngân sách tổng tháng " + monthYear + " cho userId: " + userId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi lưu/cập nhật ngân sách tổng tháng: " + e.getMessage(), e);
        } finally {
            db.close();
        }
        return result;
    }

    /**
     * Thiết lập hoặc Cập nhật hạn mức ngân sách cho từng danh mục cụ thể trong tháng.
     *
     * @param userId ID người dùng
     * @param categoryId ID danh mục chi tiêu
     * @param monthYear Định dạng yyyy-MM (ví dụ: "2026-11")
     * @param limitAmount Số tiền ngân sách giới hạn
     * @param alertPercent Phần trăm cảnh báo (mặc định 80%)
     * @return ID dòng được chèn hoặc số dòng cập nhật (-1 nếu thất bại)
     */
    public long saveOrUpdateCategoryBudget(int userId, int categoryId, String monthYear, double limitAmount, int alertPercent) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long result = -1;

        try {
            Cursor cursor = db.rawQuery(
                    "SELECT budget_id FROM budgets WHERE user_id = ? AND category_id = ? AND month_year = ?",
                    new String[]{String.valueOf(userId), String.valueOf(categoryId), monthYear});

            ContentValues values = new ContentValues();
            values.put("user_id", userId);
            values.put("category_id", categoryId);
            values.put("month_year", monthYear);
            values.put("limit_amount", limitAmount);
            values.put("alert_percent", alertPercent);

            if (cursor != null && cursor.moveToFirst()) {
                // Đã tồn tại -> UPDATE
                int budgetId = cursor.getInt(cursor.getColumnIndexOrThrow("budget_id"));
                cursor.close();
                result = db.update("budgets", values, "budget_id = ?", new String[]{String.valueOf(budgetId)});
                Log.d(TAG, "Đã cập nhật ngân sách danh mục " + categoryId + " tháng " + monthYear);
            } else {
                // Chưa tồn tại -> INSERT
                if (cursor != null) cursor.close();
                result = db.insert("budgets", null, values);
                Log.d(TAG, "Đã thêm ngân sách danh mục " + categoryId + " tháng " + monthYear);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi lưu/cập nhật ngân sách danh mục: " + e.getMessage(), e);
        } finally {
            db.close();
        }
        return result;
    }

    // =========================================================================
    // 2. CÁC PHƯƠNG THỨC CRUD CHUẨN (CREATE, READ, UPDATE, DELETE)
    // =========================================================================

    /**
     * Thêm mới một bản ghi Ngân sách (Budget)
     */
    public long insertBudget(Budget budget) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = -1;
        try {
            ContentValues values = new ContentValues();
            values.put("user_id", budget.getUserId());
            if (budget.getCategoryId() != null) {
                values.put("category_id", budget.getCategoryId());
            } else {
                values.putNull("category_id");
            }
            values.put("month_year", budget.getMonthYear());
            values.put("limit_amount", budget.getLimitAmount());
            values.put("alert_percent", budget.getAlertPercent());

            id = db.insert("budgets", null, values);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi insertBudget: " + e.getMessage(), e);
        } finally {
            db.close();
        }
        return id;
    }

    /**
     * Cập nhật thông tin bản ghi Ngân sách
     */
    public int updateBudget(Budget budget) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = 0;
        try {
            ContentValues values = new ContentValues();
            values.put("user_id", budget.getUserId());
            if (budget.getCategoryId() != null) {
                values.put("category_id", budget.getCategoryId());
            } else {
                values.putNull("category_id");
            }
            values.put("month_year", budget.getMonthYear());
            values.put("limit_amount", budget.getLimitAmount());
            values.put("alert_percent", budget.getAlertPercent());

            rows = db.update("budgets", values, "budget_id = ?", new String[]{String.valueOf(budget.getBudgetId())});
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi updateBudget: " + e.getMessage(), e);
        } finally {
            db.close();
        }
        return rows;
    }

    /**
     * Xóa ngân sách theo ID bản ghi (budget_id)
     */
    public int deleteBudget(int budgetId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = 0;
        try {
            rows = db.delete("budgets", "budget_id = ?", new String[]{String.valueOf(budgetId)});
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi deleteBudget theo budgetId: " + e.getMessage(), e);
        } finally {
            db.close();
        }
        return rows;
    }

    /**
     * Lấy bản ghi ngân sách theo budget_id
     */
    public Budget getBudgetById(int budgetId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Budget budget = null;
        Cursor cursor = null;
        try {
            cursor = db.query("budgets", null, "budget_id = ?",
                    new String[]{String.valueOf(budgetId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                budget = parseBudgetFromCursor(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi getBudgetById: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return budget;
    }

    /**
     * Lấy Ngân sách tổng tháng của người dùng (category_id IS NULL).
     * Nếu trong bảng 'budgets' chưa có, sẽ lấy từ cột 'monthly_budget_limit' trong bảng 'users'.
     */
    public Budget getTotalMonthlyBudget(int userId, String monthYear) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Budget budget = null;
        Cursor cursor = null;
        try {
            // 1. Tìm trong bảng budgets với category_id IS NULL
            cursor = db.rawQuery(
                    "SELECT * FROM budgets WHERE user_id = ? AND month_year = ? AND category_id IS NULL",
                    new String[]{String.valueOf(userId), monthYear});

            if (cursor != null && cursor.moveToFirst()) {
                budget = parseBudgetFromCursor(cursor);
            } else {
                // 2. Nếu chưa có bản ghi theo tháng, truy vấn mặc định từ bảng users
                if (cursor != null) cursor.close();
                cursor = db.rawQuery("SELECT monthly_budget_limit FROM users WHERE user_id = ?",
                        new String[]{String.valueOf(userId)});
                if (cursor != null && cursor.moveToFirst()) {
                    double defaultLimit = cursor.getDouble(cursor.getColumnIndexOrThrow("monthly_budget_limit"));
                    budget = new Budget(userId, null, monthYear, defaultLimit, 80);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi getTotalMonthlyBudget: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return budget;
    }

    /**
     * Lấy hạn mức ngân sách của một danh mục cụ thể trong tháng
     */
    public Budget getCategoryBudget(int userId, int categoryId, String monthYear) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Budget budget = null;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT b.*, c.name AS category_name, c.icon_name AS category_icon_name, c.color_code AS category_color_code " +
                            "FROM budgets b " +
                            "LEFT JOIN categories c ON b.category_id = c.category_id " +
                            "WHERE b.user_id = ? AND b.category_id = ? AND b.month_year = ?",
                    new String[]{String.valueOf(userId), String.valueOf(categoryId), monthYear});

            if (cursor != null && cursor.moveToFirst()) {
                budget = parseBudgetFromCursor(cursor);
                int catNameIdx = cursor.getColumnIndex("category_name");
                if (catNameIdx != -1) budget.setCategoryName(cursor.getString(catNameIdx));
                int catIconIdx = cursor.getColumnIndex("category_icon_name");
                if (catIconIdx != -1) budget.setCategoryIconName(cursor.getString(catIconIdx));
                int catColorIdx = cursor.getColumnIndex("category_color_code");
                if (catColorIdx != -1) budget.setCategoryColorCode(cursor.getString(catColorIdx));
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi getCategoryBudget: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return budget;
    }

    /**
     * Lấy tất cả bản ghi ngân sách trong tháng của người dùng
     */
    public List<Budget> getAllBudgetsByMonth(int userId, String monthYear) {
        List<Budget> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT b.*, c.name AS category_name, c.icon_name AS category_icon_name, c.color_code AS category_color_code " +
                            "FROM budgets b " +
                            "LEFT JOIN categories c ON b.category_id = c.category_id " +
                            "WHERE b.user_id = ? AND b.month_year = ?",
                    new String[]{String.valueOf(userId), monthYear});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Budget b = parseBudgetFromCursor(cursor);
                    int catNameIdx = cursor.getColumnIndex("category_name");
                    if (catNameIdx != -1 && !cursor.isNull(catNameIdx)) {
                        b.setCategoryName(cursor.getString(catNameIdx));
                    }
                    int catIconIdx = cursor.getColumnIndex("category_icon_name");
                    if (catIconIdx != -1 && !cursor.isNull(catIconIdx)) {
                        b.setCategoryIconName(cursor.getString(catIconIdx));
                    }
                    int catColorIdx = cursor.getColumnIndex("category_color_code");
                    if (catColorIdx != -1 && !cursor.isNull(catColorIdx)) {
                        b.setCategoryColorCode(cursor.getString(catColorIdx));
                    }
                    list.add(b);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi getAllBudgetsByMonth: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return list;
    }

    // =========================================================================
    // 3. CÁC PHƯƠNG THỨC TRUY VẤN THỐNG KÊ THU / CHI (SQL TỐI ƯU)
    // =========================================================================

    /**
     * Tính Tổng Thu Nhập (total income) theo tháng chỉ định (yyyy-MM).
     *
     * @param userId ID người dùng
     * @param monthYear Tháng cần tính (ví dụ: "2026-11")
     * @return Tổng số tiền thu nhập (double)
     */
    public double getTotalIncomeByMonth(int userId, String monthYear) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        double total = 0.0;
        Cursor cursor = null;
        try {
            String sql = "SELECT COALESCE(SUM(t.amount), 0) AS total_income " +
                    "FROM transactions t " +
                    "INNER JOIN categories c ON t.category_id = c.category_id " +
                    "WHERE t.user_id = ? AND c.type = 'INCOME' AND t.transaction_date LIKE ? || '%'";
            cursor = db.rawQuery(sql, new String[]{String.valueOf(userId), monthYear});
            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi getTotalIncomeByMonth: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return total;
    }

    /**
     * Tính Tổng Thu Nhập (total income) trong khoảng thời gian chỉ định.
     *
     * @param userId ID người dùng
     * @param startDate Ngày bắt đầu (yyyy-MM-dd)
     * @param endDate Ngày kết thúc (yyyy-MM-dd)
     * @return Tổng số tiền thu nhập (double)
     */
    public double getTotalIncomeByPeriod(int userId, String startDate, String endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        double total = 0.0;
        Cursor cursor = null;
        try {
            String sql = "SELECT COALESCE(SUM(t.amount), 0) AS total_income " +
                    "FROM transactions t " +
                    "INNER JOIN categories c ON t.category_id = c.category_id " +
                    "WHERE t.user_id = ? AND c.type = 'INCOME' AND t.transaction_date BETWEEN ? AND ?";
            cursor = db.rawQuery(sql, new String[]{String.valueOf(userId), startDate, endDate});
            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi getTotalIncomeByPeriod: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return total;
    }

    /**
     * Tính Tổng Chi Tiêu (total expense) theo tháng chỉ định (yyyy-MM).
     *
     * @param userId ID người dùng
     * @param monthYear Tháng cần tính (ví dụ: "2026-11")
     * @return Tổng số tiền chi tiêu (double)
     */
    public double getTotalExpenseByMonth(int userId, String monthYear) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        double total = 0.0;
        Cursor cursor = null;
        try {
            String sql = "SELECT COALESCE(SUM(t.amount), 0) AS total_expense " +
                    "FROM transactions t " +
                    "INNER JOIN categories c ON t.category_id = c.category_id " +
                    "WHERE t.user_id = ? AND c.type = 'EXPENSE' AND t.transaction_date LIKE ? || '%'";
            cursor = db.rawQuery(sql, new String[]{String.valueOf(userId), monthYear});
            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi getTotalExpenseByMonth: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return total;
    }

    /**
     * Tính Tổng Chi Tiêu (total expense) trong khoảng thời gian chỉ định.
     *
     * @param userId ID người dùng
     * @param startDate Ngày bắt đầu (yyyy-MM-dd)
     * @param endDate Ngày kết thúc (yyyy-MM-dd)
     * @return Tổng số tiền chi tiêu (double)
     */
    public double getTotalExpenseByPeriod(int userId, String startDate, String endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        double total = 0.0;
        Cursor cursor = null;
        try {
            String sql = "SELECT COALESCE(SUM(t.amount), 0) AS total_expense " +
                    "FROM transactions t " +
                    "INNER JOIN categories c ON t.category_id = c.category_id " +
                    "WHERE t.user_id = ? AND c.type = 'EXPENSE' AND t.transaction_date BETWEEN ? AND ?";
            cursor = db.rawQuery(sql, new String[]{String.valueOf(userId), startDate, endDate});
            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi getTotalExpenseByPeriod: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return total;
    }

    /**
     * Lấy tổng chi tiêu cho 1 danh mục cụ thể trong tháng.
     */
    public double getExpenseByCategoryId(int userId, int categoryId, String monthYear) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        double total = 0.0;
        Cursor cursor = null;
        try {
            String sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
                    "WHERE user_id = ? AND category_id = ? AND transaction_date LIKE ? || '%'";
            cursor = db.rawQuery(sql, new String[]{String.valueOf(userId), String.valueOf(categoryId), monthYear});
            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi getExpenseByCategoryId: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return total;
    }

    // =========================================================================
    // 4. TRUY VẤN TIẾN ĐỘ & SO SÁNH CHI TIÊU VỚI NGÂN SÁCH (CHO ADAPTER/UI)
    // =========================================================================

    /**
     * Lấy danh sách ngân sách từng danh mục ĐÃ THIẾT LẬP kèm tổng số tiền đã chi tiêu trong tháng.
     * Dùng để so sánh hạn mức ngân sách và hiển thị trên BudgetProgressAdapter.
     *
     * @param userId ID người dùng
     * @param monthYear Tháng cần truy vấn (dạng yyyy-MM)
     * @return Danh sách đối tượng Budget chứa thêm thông tin categoryName, icon, colorCode và spentAmount.
     */
    public List<Budget> getCategoryBudgetsWithSpending(int userId, String monthYear) {
        List<Budget> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            String sql = "SELECT b.budget_id, b.user_id, b.category_id, b.month_year, b.limit_amount, b.alert_percent, " +
                    "c.name AS category_name, c.icon_name AS category_icon_name, c.color_code AS category_color_code, " +
                    "COALESCE(SUM(t.amount), 0) AS spent_amount " +
                    "FROM budgets b " +
                    "INNER JOIN categories c ON b.category_id = c.category_id " +
                    "LEFT JOIN transactions t ON b.category_id = t.category_id " +
                    "                        AND b.user_id = t.user_id " +
                    "                        AND t.transaction_date LIKE ? || '%' " +
                    "WHERE b.user_id = ? AND b.month_year = ? AND b.category_id IS NOT NULL " +
                    "GROUP BY b.budget_id, b.user_id, b.category_id, b.month_year, b.limit_amount, b.alert_percent, c.name, c.icon_name, c.color_code";

            cursor = db.rawQuery(sql, new String[]{monthYear, String.valueOf(userId), monthYear});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Budget b = parseBudgetFromCursor(cursor);

                    int catNameIdx = cursor.getColumnIndex("category_name");
                    if (catNameIdx != -1) b.setCategoryName(cursor.getString(catNameIdx));

                    int catIconIdx = cursor.getColumnIndex("category_icon_name");
                    if (catIconIdx != -1) b.setCategoryIconName(cursor.getString(catIconIdx));

                    int catColorIdx = cursor.getColumnIndex("category_color_code");
                    if (catColorIdx != -1) b.setCategoryColorCode(cursor.getString(catColorIdx));

                    int spentIdx = cursor.getColumnIndex("spent_amount");
                    if (spentIdx != -1) b.setSpentAmount(cursor.getDouble(spentIdx));

                    list.add(b);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi getCategoryBudgetsWithSpending: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return list;
    }

    /**
     * Lấy TẤT CẢ các danh mục chi tiêu (EXPENSE) kèm hạn mức ngân sách (nếu có) và thực tế chi tiêu trong tháng.
     * Giúp hiển thị toàn bộ danh mục ngay cả khi chưa thiết lập hạn mức riêng (hạn mức = 0).
     */
    public List<Budget> getAllCategorySpendingProgress(int userId, String monthYear) {
        List<Budget> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            String sql = "SELECT c.category_id, c.name AS category_name, c.icon_name AS category_icon_name, c.color_code AS category_color_code, " +
                    "b.budget_id, b.month_year, COALESCE(b.limit_amount, 0) AS limit_amount, COALESCE(b.alert_percent, 80) AS alert_percent, " +
                    "COALESCE(SUM(t.amount), 0) AS spent_amount " +
                    "FROM categories c " +
                    "LEFT JOIN budgets b ON c.category_id = b.category_id AND b.user_id = ? AND b.month_year = ? " +
                    "LEFT JOIN transactions t ON c.category_id = t.category_id AND t.user_id = ? AND t.transaction_date LIKE ? || '%' " +
                    "WHERE c.type = 'EXPENSE' " +
                    "GROUP BY c.category_id, c.name, c.icon_name, c.color_code, b.budget_id, b.month_year, b.limit_amount, b.alert_percent";

            cursor = db.rawQuery(sql, new String[]{String.valueOf(userId), monthYear, String.valueOf(userId), monthYear});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Budget b = new Budget();
                    b.setUserId(userId);

                    int catIdIdx = cursor.getColumnIndex("category_id");
                    if (catIdIdx != -1) b.setCategoryId(cursor.getInt(catIdIdx));

                    int budgetIdIdx = cursor.getColumnIndex("budget_id");
                    if (budgetIdIdx != -1 && !cursor.isNull(budgetIdIdx)) {
                        b.setBudgetId(cursor.getInt(budgetIdIdx));
                    }

                    b.setMonthYear(monthYear);

                    int limitIdx = cursor.getColumnIndex("limit_amount");
                    if (limitIdx != -1) b.setLimitAmount(cursor.getDouble(limitIdx));

                    int alertIdx = cursor.getColumnIndex("alert_percent");
                    if (alertIdx != -1) b.setAlertPercent(cursor.getInt(alertIdx));

                    int catNameIdx = cursor.getColumnIndex("category_name");
                    if (catNameIdx != -1) b.setCategoryName(cursor.getString(catNameIdx));

                    int catIconIdx = cursor.getColumnIndex("category_icon_name");
                    if (catIconIdx != -1) b.setCategoryIconName(cursor.getString(catIconIdx));

                    int catColorIdx = cursor.getColumnIndex("category_color_code");
                    if (catColorIdx != -1) b.setCategoryColorCode(cursor.getString(catColorIdx));

                    int spentIdx = cursor.getColumnIndex("spent_amount");
                    if (spentIdx != -1) b.setSpentAmount(cursor.getDouble(spentIdx));

                    list.add(b);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi getAllCategorySpendingProgress: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return list;
    }

    /**
     * Tính tổng thu hoặc chi theo tiền tố ngày (yyyy-MM cho tháng hoặc yyyy cho năm).
     */
    public double getTotalAmountByDatePrefix(int userId, String type, String datePrefix) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        double total = 0.0;
        Cursor cursor = null;
        try {
            String sql = "SELECT COALESCE(SUM(t.amount), 0) " +
                    "FROM transactions t " +
                    "INNER JOIN categories c ON t.category_id = c.category_id " +
                    "WHERE t.user_id = ? AND c.type = ? AND t.transaction_date LIKE ? || '%'";
            cursor = db.rawQuery(sql, new String[]{String.valueOf(userId), type, datePrefix});
            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi getTotalAmountByDatePrefix: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return total;
    }

    /**
     * Thống kê tổng tiền và số lượng giao dịch theo từng danh mục (EXPENSE hoặc INCOME)
     * trong khoảng thời gian theo tiền tố ngày (yyyy-MM hoặc yyyy).
     */
    public List<CategorySummary> getCategorySummaryByDatePrefix(int userId, String type, String datePrefix) {
        List<CategorySummary> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            String sql = "SELECT c.category_id, c.name, c.icon_name, c.color_code, " +
                    "COUNT(t.transaction_id) AS tx_count, COALESCE(SUM(t.amount), 0) AS total_amount " +
                    "FROM transactions t " +
                    "INNER JOIN categories c ON t.category_id = c.category_id " +
                    "WHERE t.user_id = ? AND c.type = ? AND t.transaction_date LIKE ? || '%' " +
                    "GROUP BY c.category_id, c.name, c.icon_name, c.color_code " +
                    "ORDER BY total_amount DESC";
            cursor = db.rawQuery(sql, new String[]{String.valueOf(userId), type, datePrefix});
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    CategorySummary item = new CategorySummary();
                    item.categoryId = cursor.getInt(cursor.getColumnIndexOrThrow("category_id"));
                    item.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    item.iconName = cursor.getString(cursor.getColumnIndexOrThrow("icon_name"));
                    item.colorCode = cursor.getString(cursor.getColumnIndexOrThrow("color_code"));
                    item.transactionCount = cursor.getInt(cursor.getColumnIndexOrThrow("tx_count"));
                    item.totalAmount = cursor.getDouble(cursor.getColumnIndexOrThrow("total_amount"));
                    list.add(item);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi getCategorySummaryByDatePrefix: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
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

    // =========================================================================
    // HELPER PRIVATE METHOD
    // =========================================================================

    /**
     * Đọc các trường cơ bản của bảng budgets từ Cursor
     */
    private Budget parseBudgetFromCursor(Cursor cursor) {
        Budget b = new Budget();

        int budgetIdIdx = cursor.getColumnIndex("budget_id");
        if (budgetIdIdx != -1) b.setBudgetId(cursor.getInt(budgetIdIdx));

        int userIdIdx = cursor.getColumnIndex("user_id");
        if (userIdIdx != -1) b.setUserId(cursor.getInt(userIdIdx));

        int catIdIdx = cursor.getColumnIndex("category_id");
        if (catIdIdx != -1 && !cursor.isNull(catIdIdx)) {
            b.setCategoryId(cursor.getInt(catIdIdx));
        } else {
            b.setCategoryId(null);
        }

        int monthYearIdx = cursor.getColumnIndex("month_year");
        if (monthYearIdx != -1) b.setMonthYear(cursor.getString(monthYearIdx));

        int limitIdx = cursor.getColumnIndex("limit_amount");
        if (limitIdx != -1) b.setLimitAmount(cursor.getDouble(limitIdx));

        int alertIdx = cursor.getColumnIndex("alert_percent");
        if (alertIdx != -1) b.setAlertPercent(cursor.getInt(alertIdx));

        return b;
    }
}
