package com.vhu.smartspend.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.vhu.smartspend.R;
import com.vhu.smartspend.utils.SessionManager;

/**
 * MainActivity mẫu để chạy thử phần của Khang. Khi ghép với nhóm, chỉ cần giữ lại
 * onCreateOptionsMenu / onOptionsItemSelected và đoạn mở SettingFragment.
 */
public class MainActivity extends AppCompatActivity {
    private SessionManager session;
    private View mainContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        // Nhận dữ liệu từ LoginActivity qua Intent (nếu không có thì lấy từ session)
        String role = getIntent().getStringExtra("ROLE");
        if (role == null) role = session.getRole();

        mainContent = findViewById(R.id.mainContent);
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("Xin chào, " + session.getUsername() + " (" + role + ")");

        Button btnCategory = findViewById(R.id.btnCategory);
        btnCategory.setOnClickListener(v ->
                startActivity(new Intent(this, CategoryManageActivity.class)));

        // Ẩn nội dung chính khi SettingFragment đang hiển thị
        getSupportFragmentManager().addOnBackStackChangedListener(this::updateContentVisibility);
        updateContentVisibility();
    }

    private void updateContentVisibility() {
        boolean hasFragment = getSupportFragmentManager().getBackStackEntryCount() > 0;
        mainContent.setVisibility(hasFragment ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new SettingFragment())
                        .addToBackStack(null)
                        .commit();
            }
            return true;
        } else if (id == R.id.action_logout) {
            new AlertDialog.Builder(this)
                    .setTitle("Đăng xuất")
                    .setMessage("Bạn có chắc muốn đăng xuất?")
                    .setPositiveButton("Đăng xuất", (d, w) -> {
                        session.logout();
                        goToLogin();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void goToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
