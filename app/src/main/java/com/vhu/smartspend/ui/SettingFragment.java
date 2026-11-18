package com.vhu.smartspend.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.vhu.smartspend.R;
import com.vhu.smartspend.database.TransactionDAO;
import com.vhu.smartspend.model.Transaction;
import com.vhu.smartspend.utils.SessionManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class SettingFragment extends Fragment {

    private TextView tvUserFullName, tvUsername, tvUserRole;
    private SwitchCompat swDarkMode;
    private Button btnManageCategory, btnExportCsv, btnLogout;

    private SessionManager sessionManager;
    private TransactionDAO transactionDAO;

    public SettingFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        sessionManager = new SessionManager(requireContext());
        transactionDAO = new TransactionDAO(requireContext());

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
        btnExportCsv.setOnClickListener(v -> exportReportToCSV());

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

    private void exportReportToCSV() {
        int userId = sessionManager.getUserId();
        List<Transaction> transactions = transactionDAO.getAllTransactionsByUserId(userId);
        if (transactions.isEmpty()) {
            transactions = transactionDAO.getAllTransactions();
        }

        if (transactions.isEmpty()) {
            Toast.makeText(getContext(), "Không có giao dịch nào để xuất báo cáo!", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csvData = new StringBuilder();
        csvData.append("ID,Tiêu đề,Số tiền (VND),Loại,Danh mục,Ngày giao dịch,Ghi chú\n");

        for (Transaction t : transactions) {
            String cType = "INCOME".equalsIgnoreCase(t.getCategoryType()) ? "Thu nhập" : "Chi tiêu";
            String cName = t.getCategoryName() != null ? t.getCategoryName() : "";
            String note = t.getNote() != null ? t.getNote().replace(",", " ") : "";
            String title = t.getTitle() != null ? t.getTitle().replace(",", " ") : "";

            csvData.append(t.getTransactionId()).append(",")
                    .append(title).append(",")
                    .append(t.getAmount()).append(",")
                    .append(cType).append(",")
                    .append(cName).append(",")
                    .append(t.getTransactionDate()).append(",")
                    .append(note).append("\n");
        }

        try {
            File exportDir = requireContext().getExternalFilesDir(null);
            if (exportDir == null) {
                exportDir = requireContext().getFilesDir();
            }
            File file = new File(exportDir, "SmartSpend_Report.csv");
            FileWriter writer = new FileWriter(file);
            writer.write(csvData.toString());
            writer.flush();
            writer.close();

            Toast.makeText(getContext(), "Đã xuất CSV thành công!\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi khi xuất file CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}