package com.vhu.smartspend.ui;

import android.content.Intent;
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
import com.vhu.smartspend.database.UserDAO;
import com.vhu.smartspend.model.User;
import com.vhu.smartspend.utils.CategoryUiHelper;
import com.vhu.smartspend.utils.CsvExporter;
import com.vhu.smartspend.utils.CurrencyUtils;
import com.vhu.smartspend.utils.SessionManager;

public class SettingFragment extends Fragment {

    private TextView tvUserFullName, tvUsername, tvMonthlyLimit;
    private SessionManager sessionManager;
    private UserDAO userDAO;
    private double currentMonthlyLimit = 4500000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        sessionManager = new SessionManager(requireContext());
        userDAO = new UserDAO(requireContext());

        tvUserFullName = view.findViewById(R.id.tvUserFullName);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvMonthlyLimit = view.findViewById(R.id.tvMonthlyLimit);

        bindUserInfo();
        loadMonthlyLimit();

        view.findViewById(R.id.layoutProfileAvatar).setOnClickListener(v -> showEditFullNameDialog());
        view.findViewById(R.id.rowMonthlyLimit).setOnClickListener(v -> showEditMonthlyLimitDialog());
        view.findViewById(R.id.btnManageCategory).setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), CategoryManageActivity.class)));
        view.findViewById(R.id.btnExportCsv).setOnClickListener(v ->
                CsvExporter.exportTransactionsToCsv(requireContext(), sessionManager.getUserId()));
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutConfirmDialog());

        return view;
    }

    private void bindUserInfo() {
        String fullName = sessionManager.getFullName();
        String username = sessionManager.getUsername();
        tvUserFullName.setText(!fullName.isEmpty() ? fullName : "Người dùng");
        tvUsername.setText(!username.isEmpty() ? "@" + username : "@smartspend");
    }

    private void loadMonthlyLimit() {
        User user = userDAO.getUserById(sessionManager.getUserId());
        if (user != null) {
            currentMonthlyLimit = user.getMonthlyBudgetLimit();
        }
        tvMonthlyLimit.setText(CurrencyUtils.formatVND(currentMonthlyLimit));
    }

    private void showEditFullNameDialog() {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(sessionManager.getFullName());
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(requireContext())
                .setTitle("Đổi tên hiển thị")
                .setView(wrapDialogInput(input))
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(requireContext(), "Tên không được để trống", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (sessionManager.getUserId() > 0) {
                        userDAO.updateFullName(sessionManager.getUserId(), newName);
                    }
                    sessionManager.updateFullName(newName);
                    tvUserFullName.setText(newName);
                    Toast.makeText(requireContext(), "Đã cập nhật tên hiển thị!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditMonthlyLimitDialog() {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf((long) currentMonthlyLimit));
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(requireContext())
                .setTitle("Hạn mức chi tiêu tháng")
                .setMessage("Nhập số tiền hạn mức (VNĐ):")
                .setView(wrapDialogInput(input))
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String raw = input.getText().toString().trim();
                    if (raw.isEmpty()) return;
                    try {
                        double newLimit = Double.parseDouble(raw);
                        if (newLimit <= 0) {
                            Toast.makeText(requireContext(), "Hạn mức phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (sessionManager.getUserId() > 0) {
                            userDAO.updateMonthlyLimit(sessionManager.getUserId(), newLimit);
                        }
                        currentMonthlyLimit = newLimit;
                        tvMonthlyLimit.setText(CurrencyUtils.formatVND(currentMonthlyLimit));
                        Toast.makeText(requireContext(), "Đã cập nhật hạn mức tháng!", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
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

    private FrameLayout wrapDialogInput(EditText input) {
        int pad = CategoryUiHelper.dp(requireContext(), 20);
        FrameLayout container = new FrameLayout(requireContext());
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);
        return container;
    }
}
