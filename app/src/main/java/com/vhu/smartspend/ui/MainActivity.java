package com.vhu.smartspend.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vhu.smartspend.R;
import com.vhu.smartspend.database.UserDAO;
import com.vhu.smartspend.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);
        UserDAO userDAO = new UserDAO(this);

        if (!sessionManager.isLoggedIn() || userDAO.getUserById(sessionManager.getUserId()) == null) {
            sessionManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_statistic) {
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
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
}
