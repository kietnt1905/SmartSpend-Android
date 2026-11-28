package com.vhu.smartspend.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.vhu.smartspend.R;

/**
 * Cảnh báo bằng AlertDialog và Notification khi chi tiêu tháng vượt 100% hạn mức.
 */
public class BudgetAlertHelper {

    private static final String CHANNEL_ID = "smart_spend_budget_alert_channel";
    private static final int NOTIFICATION_ID = 2026;

    public static void checkAndAlertMonthlyBudget(Context context, double spent, double limit) {
        if (context == null || limit <= 0 || spent < limit) return;

        String message = "🚨 BÁO ĐỘNG: Ngân sách tổng tháng đã vượt 100% hạn mức! (Đã chi "
                + CurrencyUtils.formatVND(spent) + " / Hạn mức " + CurrencyUtils.formatVND(limit) + ")";

        sendNotification(context, "🚨 CẢNH BÁO VƯỢT NGÂN SÁCH!", message);

        new AlertDialog.Builder(context)
                .setTitle("🚨 Cảnh Báo Vượt Ngân Sách!")
                .setMessage(message + "\n\nHãy cân đối lại các khoản chi tiêu để đảm bảo kế hoạch tài chính!")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    private static void sendNotification(Context context, String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Cảnh báo ngân sách SmartSpend",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }
}
