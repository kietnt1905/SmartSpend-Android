package com.vhu.smartspend.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.vhu.smartspend.R;
import com.vhu.smartspend.utils.CsvExporter;
import com.vhu.smartspend.utils.SessionManager;

public class SettingFragment extends Fragment {

    private TextView tvUserFullName, tvUsername, tvUserRole;
    private SwitchCompat swDarkMode;
    private Button btnManageCategory, btnExportCsv, btnLogout;

    private SessionManager sessionManager;

    public SettingFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        sessionManager = new SessionManager(requireContext());

        tvUserFullName = view.findViewById(R.id.tvUserFullName);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvUserRole = view.findViewById(R.id.tvUserRole);
        swDarkMode = view.findViewById(R.id.swDarkMode);
        btnManageCategory = view.findViewById(R.id.btnManageCategory);
        btnExportCsv = view.findViewById(R.id.btnExportCsv);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Hiển thị thông tin phiên đăng nhập
        tvUserFullName.setText("Họ tên: " + sessionManager.getFullName());
        tvUsername.setText("Tên đăng nhập: " + sessionManager.getUsername());
        tvUserRole.setText("Quyền hạn: " + sessionManager.getRole());

        // Dark Mode switch
        swDarkMode.setChecked(sessionManager.isDarkMode());
        swDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sessionManager.setDarkMode(isChecked);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Chuyển sang màn hình quản lý danh mục
        btnManageCategory.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), CategoryManageActivity.class);
            startActivity(intent);
        });

        // Xuất báo cáo CSV
        btnExportCsv.setOnClickListener(v -> CsvExporter.exportTransactionsToCsv(requireContext(), sessionManager.getUserId()));

        // Đăng xuất
        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }
}
