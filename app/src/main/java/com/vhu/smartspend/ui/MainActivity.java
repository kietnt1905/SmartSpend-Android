package com.vhu.smartspend.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.vhu.smartspend.R;
import com.vhu.smartspend.database.TransactionDAO;
import com.vhu.smartspend.model.Transaction;
import com.vhu.smartspend.utils.SessionManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int MENU_DARK_MODE = 101;
    private static final int MENU_CATEGORY_MANAGE = 102;
    private static final int MENU_EXPORT_CSV = 103;
    private static final int MENU_LOGOUT = 104;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Áp dụng trạng thái Dark Mode từ SharedPreferences
        if (sessionManager.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        // Kiểm tra phiên đăng nhập
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Nạp SettingFragment làm màn hình mặc định
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SettingFragment())
                    .commit();
        }
    }

    /**
     * Tạo OptionsMenu trên thanh công cụ
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_DARK_MODE, 1, sessionManager.isDarkMode() ? "Tắt Dark Mode" : "Bật Dark Mode");
        menu.add(Menu.NONE, MENU_CATEGORY_MANAGE, 2, "Quản lý danh mục");
        menu.add(Menu.NONE, MENU_EXPORT_CSV, 3, "Xuất báo cáo CSV");
        menu.add(Menu.NONE, MENU_LOGOUT, 4, "Đăng xuất");
        return true;
    }

    /**
     * Xử lý sự kiện click OptionsMenu
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == MENU_DARK_MODE) {
            boolean isDark = !sessionManager.isDarkMode();
            sessionManager.setDarkMode(isDark);
            AppCompatDelegate.setDefaultNightMode(
                    isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            recreate();
            return true;
        } else if (id == MENU_CATEGORY_MANAGE) {
            Intent intent = new Intent(MainActivity.this, CategoryManageActivity.class);
            startActivity(intent);
            return true;
        } else if (id == MENU_EXPORT_CSV) {
            exportCsvReport();
            return true;
        } else if (id == MENU_LOGOUT) {
            sessionManager.logout();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportCsvReport() {
        TransactionDAO dao = new TransactionDAO(this);
        List<Transaction> list = dao.getAllTransactionsByUserId(sessionManager.getUserId());
        if (list.isEmpty()) {
            list = dao.getAllTransactions();
        }

        if (list.isEmpty()) {
            Toast.makeText(this, "Không có giao dịch để xuất báo cáo CSV!", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Tiêu đề,Số tiền (VND),Loại,Danh mục,Ngày giao dịch,Ghi chú\n");
        for (Transaction t : list) {
            String type = "INCOME".equalsIgnoreCase(t.getCategoryType()) ? "Thu nhập" : "Chi tiêu";
            String cat = t.getCategoryName() != null ? t.getCategoryName() : "";
            String note = t.getNote() != null ? t.getNote().replace(",", " ") : "";
            String title = t.getTitle() != null ? t.getTitle().replace(",", " ") : "";

            csv.append(t.getTransactionId()).append(",")
                    .append(title).append(",")
                    .append(t.getAmount()).append(",")
                    .append(type).append(",")
                    .append(cat).append(",")
                    .append(t.getTransactionDate()).append(",")
                    .append(note).append("\n");
        }

        try {
            File exportDir = getExternalFilesDir(null);
            if (exportDir == null) {
                exportDir = getFilesDir();
            }
            File file = new File(exportDir, "SmartSpend_Report.csv");
            FileWriter writer = new FileWriter(file);
            writer.write(csv.toString());
            writer.flush();
            writer.close();

            Toast.makeText(this, "Đã xuất báo cáo CSV:\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi xuất file CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}