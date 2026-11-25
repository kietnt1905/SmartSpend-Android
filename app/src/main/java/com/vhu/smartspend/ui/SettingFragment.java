package com.vhu.smartspend.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.vhu.smartspend.R;
import com.vhu.smartspend.database.DatabaseHelper;
import com.vhu.smartspend.utils.CsvExporter;
import com.vhu.smartspend.utils.SessionManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class SettingFragment extends Fragment {

    private TextView tvUserFullName, tvUsername, tvMonthlyLimit;
    private View layoutProfileAvatar, rowMonthlyLimit, btnManageCategory, btnExportCsv, btnLogout;

    private SessionManager sessionManager;
    private DatabaseHelper dbHelper;
    private double currentMonthlyLimit = 4500000;

    public SettingFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        sessionManager = new SessionManager(requireContext());
        dbHelper = new DatabaseHelper(requireContext());

        tvUserFullName = view.findViewById(R.id.tvUserFullName);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvMonthlyLimit = view.findViewById(R.id.tvMonthlyLimit);

        layoutProfileAvatar = view.findViewById(R.id.layoutProfileAvatar);
        rowMonthlyLimit = view.findViewById(R.id.rowMonthlyLimit);
        btnManageCategory = view.findViewById(R.id.btnManageCategory);
        btnExportCsv = view.findViewById(R.id.btnExportCsv);
        btnLogout = view.findViewById(R.id.btnLogout);

        bindUserInfo();
        loadMonthlyLimit();

        layoutProfileAvatar.setOnClickListener(v -> showEditFullNameDialog());
        rowMonthlyLimit.setOnClickListener(v -> showEditMonthlyLimitDialog());

        btnManageCategory.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), CategoryManageActivity.class);
            startActivity(intent);
        });

        btnExportCsv.setOnClickListener(v ->
                CsvExporter.exportTransactionsToCsv(requireContext(), sessionManager.getUserId())
        );

        btnLogout.setOnClickListener(v -> showLogoutConfirmDialog());

        return view;
    }

    private void bindUserInfo() {
        String fullName = sessionManager.getFullName();
        String username = sessionManager.getUsername();

        tvUserFullName.setText(!fullName.isEmpty() ? fullName : "Người dùng");
        tvUsername.setText(!username.isEmpty() ? "@" + username : "@smartspend");
    }

    private void showEditFullNameDialog() {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(sessionManager.getFullName());
        input.setSelection(input.getText().length());

        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(requireContext());
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle("Đổi tên hiển thị")
                .setView(container)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(requireContext(), "Tên không được để trống", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int userId = sessionManager.getUserId();
                    if (userId > 0) {
                        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
                            ContentValues cv = new ContentValues();
                            cv.put("full_name", newName);
                            db.update("users", cv, "user_id = ?", new String[]{String.valueOf(userId)});
                        }
                    }
                    sessionManager.updateFullName(newName);
                    tvUserFullName.setText(newName);
                    Toast.makeText(requireContext(), "Đã cập nhật tên hiển thị!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showLogoutConfirmDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất tài khoản không?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    sessionManager.logout();
                    Intent intent = new Intent(requireActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void loadMonthlyLimit() {
        int userId = sessionManager.getUserId();
        if (userId <= 0) {
            tvMonthlyLimit.setText(formatVnd(currentMonthlyLimit));
            return;
        }
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT monthly_budget_limit FROM users WHERE user_id = ?",
                     new String[]{String.valueOf(userId)})) {
            if (cursor.moveToFirst()) {
                currentMonthlyLimit = cursor.getDouble(0);
            }
        } catch (Exception ignored) {
        }
        tvMonthlyLimit.setText(formatVnd(currentMonthlyLimit));
    }

    private void showEditMonthlyLimitDialog() {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf((long) currentMonthlyLimit));
        input.setSelection(input.getText().length());

        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(requireContext());
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle("Hạn mức chi tiêu tháng")
                .setMessage("Nhập số tiền hạn mức (VNĐ):")
                .setView(container)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String raw = input.getText().toString().trim();
                    if (raw.isEmpty()) return;
                    try {
                        double newLimit = Double.parseDouble(raw);
                        if (newLimit <= 0) {
                            Toast.makeText(requireContext(), "Hạn mức phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int userId = sessionManager.getUserId();
                        if (userId > 0) {
                            try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
                                ContentValues cv = new ContentValues();
                                cv.put("monthly_budget_limit", newLimit);
                                db.update("users", cv, "user_id = ?", new String[]{String.valueOf(userId)});
                            }
                        }
                        currentMonthlyLimit = newLimit;
                        tvMonthlyLimit.setText(formatVnd(currentMonthlyLimit));
                        Toast.makeText(requireContext(), "Đã cập nhật hạn mức tháng!", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private String formatVnd(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(amount) + " đ";
    }
}
