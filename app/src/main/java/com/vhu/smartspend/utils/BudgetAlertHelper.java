package com.vhu.smartspend.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.vhu.smartspend.R;
import com.vhu.smartspend.database.BudgetDAO;
import com.vhu.smartspend.model.Budget;

import java.util.Locale;

/**
 * Lớp Xử lý Hệ thống Cảnh báo Ngân sách (BudgetAlertHelper)
 * Điểm cốt lõi của Đề tài 2 - Phân chia 3 cấp độ cảnh báo:
 * - Cấp 1 (Dưới 80%): Bình thường / An toàn (Màu xanh lá)
 * - Cấp 2 (Chạm 80% - 100%): Cảnh báo (Chuyển sang màu cam)
 * - Cấp 3 (Vượt 100%): Báo động (Màu đỏ, bật AlertDialog cảnh báo & phát Notification)
 *
 * Tác giả: Lê Phúc Thịnh (MSSV: 231A290032)
 */
public class BudgetAlertHelper {

    private static final String TAG = "BudgetAlertHelper";
    public static final String CHANNEL_ID = "smart_spend_budget_alert_channel";
    public static final String CHANNEL_NAME = "Cảnh báo ngân sách SmartSpend";
    public static final int NOTIFICATION_ID = 2026;

    // Các hằng số Cấp độ Cảnh báo
    public static final int LEVEL_SAFE = 1;      // Dưới 80% (An toàn - Xanh)
    public static final int LEVEL_WARNING = 2;   // Chạm ngưỡng 80% - 100% (Cảnh báo - Màu cam)
    public static final int LEVEL_EXCEEDED = 3;  // Vượt 100% (Báo động - Màu đỏ & Dialog + Notification)

    /**
     * Lớp đối tượng chứa dữ liệu Trạng thái Cảnh báo Ngân sách
     */
    public static class BudgetAlertStatus {
        private final int level;
        private final double spentAmount;
        private final double limitAmount;
        private final int percentage;
        private final int alertColor;
        private final String categoryName;
        private final String statusMessage;

        public BudgetAlertStatus(int level, double spentAmount, double limitAmount, int percentage,
                                 int alertColor, String categoryName, String statusMessage) {
            this.level = level;
            this.spentAmount = spentAmount;
            this.limitAmount = limitAmount;
            this.percentage = percentage;
            this.alertColor = alertColor;
            this.categoryName = categoryName;
            this.statusMessage = statusMessage;
        }

        public int getLevel() {
            return level;
        }

        public double getSpentAmount() {
            return spentAmount;
        }

        public double getLimitAmount() {
            return limitAmount;
        }

        public int getPercentage() {
            return percentage;
        }

        public int getAlertColor() {
            return alertColor;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public boolean isExceeded() {
            return level == LEVEL_EXCEEDED;
        }

        public boolean isWarning() {
            return level == LEVEL_WARNING;
        }
    }

    /**
     * Kiểm tra trạng thái ngân sách danh mục sau khi phát sinh giao dịch mới.
     *
     * @param context Context ứng dụng
     * @param userId ID người dùng
     * @param categoryId ID danh mục
     * @param monthYear Tháng cần kiểm tra (yyyy-MM)
     * @return Đối tượng BudgetAlertStatus chứa chi tiết cấp độ và màu sắc
     */
    public static BudgetAlertStatus checkCategoryBudgetAlert(Context context, int userId, int categoryId, String monthYear) {
        BudgetDAO budgetDAO = new BudgetDAO(context);
        Budget budget = budgetDAO.getCategoryBudget(userId, categoryId, monthYear);

        double spent = budgetDAO.getExpenseByCategoryId(userId, categoryId, monthYear);
        double limit = (budget != null) ? budget.getLimitAmount() : 0;
        int alertThreshold = (budget != null && budget.getAlertPercent() > 0) ? budget.getAlertPercent() : 80;
        String catName = (budget != null && budget.getCategoryName() != null) ? budget.getCategoryName() : "Danh mục";

        return calculateStatus(catName, spent, limit, alertThreshold);
    }

    /**
     * Kiểm tra trạng thái ngân sách tổng hàng tháng của người dùng.
     */
    public static BudgetAlertStatus checkTotalMonthlyBudgetAlert(Context context, int userId, String monthYear) {
        BudgetDAO budgetDAO = new BudgetDAO(context);
        Budget totalBudget = budgetDAO.getTotalMonthlyBudget(userId, monthYear);

        double totalSpent = budgetDAO.getTotalExpenseByMonth(userId, monthYear);
        double totalLimit = (totalBudget != null) ? totalBudget.getLimitAmount() : 0;
        int alertThreshold = (totalBudget != null && totalBudget.getAlertPercent() > 0) ? totalBudget.getAlertPercent() : 80;

        return calculateStatus("Ngân sách tổng tháng", totalSpent, totalLimit, alertThreshold);
    }

    /**
     * Hàm tính toán cấp độ 1, 2, 3 và tạo thông điệp cảnh báo
     */
    private static BudgetAlertStatus calculateStatus(String categoryName, double spent, double limit, int alertThreshold) {
        if (limit <= 0) {
            return new BudgetAlertStatus(LEVEL_SAFE, spent, limit, 0,
                    Color.parseColor("#1E88E5"), categoryName, "Chưa cài đặt hạn mức");
        }

        int percentage = (int) Math.round((spent / limit) * 100);
        int level;
        int color;
        String message;

        if (percentage >= 100) {
            // Cấp 3: Vượt 100% (Màu Đỏ)
            level = LEVEL_EXCEEDED;
            color = Color.parseColor("#D32F2F");
            message = String.format(Locale.getDefault(),
                    "🚨 BÁO ĐỘNG: %s đã vượt 100%% ngân sách! (Đã chi %s / Hạn mức %s)",
                    categoryName, CurrencyUtils.formatVND(spent), CurrencyUtils.formatVND(limit));
        } else if (percentage >= alertThreshold) {
            // Cấp 2: Chạm ngưỡng 80% (Màu Cam)
            level = LEVEL_WARNING;
            color = Color.parseColor("#FF6D00");
            message = String.format(Locale.getDefault(),
                    "⚠️ CẢNH BÁO: %s đã đạt %d%% ngân sách! (Đã chi %s / Hạn mức %s)",
                    categoryName, percentage, CurrencyUtils.formatVND(spent), CurrencyUtils.formatVND(limit));
        } else {
            // Cấp 1: Dưới 80% (Màu Xanh lá - Bình thường)
            level = LEVEL_SAFE;
            color = Color.parseColor("#0B5A4B");
            message = String.format(Locale.getDefault(),
                    "✓ An toàn: %s ở mức %d%% ngân sách.", categoryName, percentage);
        }

        return new BudgetAlertStatus(level, spent, limit, percentage, color, categoryName, message);
    }

    /**
     * Thực thi hành động cảnh báo tự động sau giao dịch:
     * Nếu vượt 100% -> Bật AlertDialog & Đẩy Notification.
     *
     * @param context Context ứng dụng
     * @param status Đối tượng trạng thái cảnh báo
     */
    public static void processBudgetAlertActions(Context context, BudgetAlertStatus status) {
        if (status == null) return;

        if (status.isExceeded()) {
            // 1. Đẩy Notification Cảnh báo
            sendBudgetNotification(context,
                    "🚨 CẢNH BÁO VƯỢT NGÂN SÁCH!",
                    status.getStatusMessage());

            // 2. Hiển thị AlertDialog Cảnh báo
            showBudgetExceededDialog(context, status);
        } else if (status.isWarning()) {
            Log.w(TAG, "Cảnh báo chạm ngưỡng 80%: " + status.getStatusMessage());
        }
    }

    /**
     * Bật AlertDialog cảnh báo vượt ngân sách
     */
    public static void showBudgetExceededDialog(Context context, BudgetAlertStatus status) {
        try {
            new AlertDialog.Builder(context)
                    .setTitle("🚨 Cảnh Báo Vượt Ngân Sách!")
                    .setMessage(status.getStatusMessage() + "\n\nHãy cân đối lại các khoản chi tiêu để đảm bảo kế hoạch tài chính!")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Đã hiểu", (dialog, which) -> dialog.dismiss())
                    .setCancelable(true)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Không thể hiển thị AlertDialog: " + e.getMessage(), e);
        }
    }

    /**
     * Đẩy thông báo Notification lên thanh trạng thái hệ thống Android
     */
    public static void sendBudgetNotification(Context context, String title, String message) {
        try {
            createNotificationChannel(context);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Chưa cấp quyền POST_NOTIFICATIONS trên Android 13+");
                    return;
                }
            }

            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "Đã gửi thông báo Notification thành công: " + title);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi gửi Notification: " + e.getMessage(), e);
        }
    }

    /**
     * Khởi tạo NotificationChannel (Yêu cầu bắt buộc từ Android 8.0 Oreo - API 26+)
     */
    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Kênh gửi thông báo cảnh báo hạn mức ngân sách của SmartSpend");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
