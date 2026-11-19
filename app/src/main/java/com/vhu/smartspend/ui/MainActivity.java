package com.vhu.smartspend.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vhu.smartspend.R;
import com.vhu.smartspend.utils.CsvExporter;
import com.vhu.smartspend.utils.SessionManager;

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

        // Đọc extras từ Intent nếu có khi từ LoginActivity chuyển sang
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int userId = extras.getInt("user_id", -1);
            String username = extras.getString("username");
            if (userId != -1 && username != null && !username.isEmpty()) {
                // Session already active via LoginActivity
            }
        }

        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_statistic) {
                selectedFragment = new StatisticFragment();
            } else if (itemId == R.id.nav_account) {
                selectedFragment = new SettingFragment();
            } else {
                selectedFragment = new HomeFragment();
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            return true;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
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
            CsvExporter.exportTransactionsToCsv(this, sessionManager.getUserId());
            return true;
        } else if (id == MENU_LOGOUT) {
            sessionManager.logout();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
