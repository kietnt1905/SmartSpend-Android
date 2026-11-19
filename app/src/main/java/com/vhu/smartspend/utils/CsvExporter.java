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
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT t.transaction_id, t.title, t.amount, c.type, c.name, t.transaction_date, t.note " +
                "FROM transactions t LEFT JOIN categories c ON t.category_id = c.category_id " +
                "WHERE t.user_id = ? ORDER BY t.transaction_date DESC";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(sql, new String[]{String.valueOf(userId)});
            if (cursor == null || cursor.getCount() == 0) {
                if (cursor != null) cursor.close();
                cursor = db.rawQuery("SELECT t.transaction_id, t.title, t.amount, c.type, c.name, t.transaction_date, t.note " +
                        "FROM transactions t LEFT JOIN categories c ON t.category_id = c.category_id " +
                        "ORDER BY t.transaction_date DESC", null);
            }

            if (cursor == null || cursor.getCount() == 0) {
                Toast.makeText(context, "Không có giao dịch để xuất báo cáo CSV!", Toast.LENGTH_SHORT).show();
                if (cursor != null) cursor.close();
                db.close();
                return;
            }

            StringBuilder csv = new StringBuilder();
            // UTF-8 BOM for Excel Vietnamese support
            csv.append("\uFEFF");
            csv.append("ID,Tiêu đề,Số tiền (VND),Loại,Danh mục,Ngày giao dịch,Ghi chú\n");

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("transaction_id"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
                String typeDb = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                String catName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("transaction_date"));
                String note = cursor.getString(cursor.getColumnIndexOrThrow("note"));

                String type = "INCOME".equalsIgnoreCase(typeDb) ? "Thu nhập" : "Chi tiêu";
                String escapedTitle = escapeCsv(title);
                String escapedCat = escapeCsv(catName);
                String escapedNote = escapeCsv(note);

                csv.append(id).append(",")
                        .append(escapedTitle).append(",")
                        .append(amount).append(",")
                        .append(type).append(",")
                        .append(escapedCat).append(",")
                        .append(date).append(",")
                        .append(escapedNote).append("\n");
            }
            cursor.close();
            db.close();

            File exportDir = context.getExternalFilesDir(null);
            if (exportDir == null) {
                exportDir = context.getFilesDir();
            }
            File file = new File(exportDir, "SmartSpend_Report.csv");

            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                writer.write(csv.toString());
                writer.flush();
                Toast.makeText(context, "Đã xuất báo cáo CSV:\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Lỗi khi xuất file CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            if (cursor != null) {
                cursor.close();
            }
            db.close();
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
