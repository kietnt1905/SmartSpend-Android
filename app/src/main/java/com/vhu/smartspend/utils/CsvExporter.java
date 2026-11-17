package com.vhu.smartspend.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.vhu.smartspend.database.DatabaseHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Xuất báo cáo thu chi ra file CSV. Android 10+ lưu vào Downloads (không cần xin quyền). */
public class CsvExporter {

    /** @return đường dẫn/mô tả nơi lưu file */
    public static String export(Context context, int userId) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Ngày,Danh mục,Loại,Số tiền,Ghi chú\n");

        try (Cursor c = DatabaseHelper.getInstance(context).getReadableDatabase().rawQuery(
                "SELECT t.date, c.name, t.type, t.amount, t.note "
                        + "FROM transactions t LEFT JOIN categories c ON t.category_id = c.id "
                        + "WHERE t.user_id = ? ORDER BY t.date DESC",
                new String[]{String.valueOf(userId)})) {
            while (c.moveToNext()) {
                String type = "income".equals(c.getString(2)) ? "Thu" : "Chi";
                sb.append(esc(c.getString(0))).append(',')
                        .append(esc(c.getString(1))).append(',')
                        .append(type).append(',')
                        .append(String.format(Locale.US, "%.0f", c.getDouble(3))).append(',')
                        .append(esc(c.getString(4))).append('\n');
            }
        }

        // BOM để Excel đọc đúng tiếng Việt
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        String fileName = "smartspend_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date()) + ".csv";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            v.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = context.getContentResolver()
                    .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) throw new IOException("Không tạo được file");
            try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                if (os == null) throw new IOException("Không mở được file");
                os.write(bom);
                os.write(body);
            }
            return "Downloads/" + fileName;
        } else {
            File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null) throw new IOException("Không có bộ nhớ ngoài");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, fileName);
            try (FileOutputStream os = new FileOutputStream(f)) {
                os.write(bom);
                os.write(body);
            }
            return f.getAbsolutePath();
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
