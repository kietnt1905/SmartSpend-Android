package com.vhu.smartspend.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.vhu.smartspend.database.DatabaseHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class CsvExporter {

    public static void exportTransactionsToCsv(Context context, int userId) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        String sql = "SELECT t.transaction_id, t.title, t.amount, c.type, c.name, t.transaction_date, t.note " +
                "FROM transactions t LEFT JOIN categories c ON t.category_id = c.category_id " +
                "WHERE t.user_id = ? ORDER BY t.transaction_date DESC";

        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId)})) {

            if (cursor.getCount() == 0) {
                Toast.makeText(context, "Không có giao dịch để xuất báo cáo CSV!", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder csv = new StringBuilder("\uFEFF");
            csv.append("ID,Tiêu đề,Số tiền (VND),Loại,Danh mục,Ngày giao dịch,Ghi chú\n");

            while (cursor.moveToNext()) {
                String type = "INCOME".equalsIgnoreCase(cursor.getString(3)) ? "Thu nhập" : "Chi tiêu";
                csv.append(cursor.getInt(0)).append(",")
                        .append(escapeCsv(cursor.getString(1))).append(",")
                        .append(cursor.getDouble(2)).append(",")
                        .append(type).append(",")
                        .append(escapeCsv(cursor.getString(4))).append(",")
                        .append(cursor.getString(5)).append(",")
                        .append(escapeCsv(cursor.getString(6))).append("\n");
            }

            File exportDir = context.getExternalFilesDir(null);
            if (exportDir == null) exportDir = context.getFilesDir();
            File file = new File(exportDir, "SmartSpend_Report.csv");

            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                writer.write(csv.toString());
            }

            Toast.makeText(context, "Đã xuất báo cáo CSV:\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context, "Lỗi khi xuất file CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
