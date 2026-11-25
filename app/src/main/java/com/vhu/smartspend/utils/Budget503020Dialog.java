package com.vhu.smartspend.utils;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.vhu.smartspend.R;
import com.vhu.smartspend.database.BudgetDAO;

import java.util.Calendar;
import java.util.Locale;

/**
 * Dialog Tự Động Chia Ngân Sách Theo Quy Tắc 50/30/20 (FinWise Creative Feature)
 * Tác giả: Lê Phúc Thịnh (MSSV: 231A290032)
 */
public class Budget503020Dialog {

    public interface OnBudgetAppliedListener {
        void onApplied();
    }

    public static void show(Context context, int userId, OnBudgetAppliedListener listener) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_budget_503020, null);

        EditText etTotalIncomeInput = dialogView.findViewById(R.id.etTotalIncomeInput);
        TextView tvNeedsAmount = dialogView.findViewById(R.id.tvNeedsAmount);
        TextView tvWantsAmount = dialogView.findViewById(R.id.tvWantsAmount);
        TextView tvSavingsAmount = dialogView.findViewById(R.id.tvSavingsAmount);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel503020);
        Button btnApply = dialogView.findViewById(R.id.btnApply503020);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Tự động tính toán số tiền theo quy tắc 50/30/20 khi nhập
        etTotalIncomeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateAndDisplay(s.toString(), tvNeedsAmount, tvWantsAmount, tvSavingsAmount);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Nhập mặc định 10.000.000 đ để hiển thị mẫu
        etTotalIncomeInput.setText("10000000");

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnApply.setOnClickListener(v -> {
            String inputStr = etTotalIncomeInput.getText().toString().trim();
            if (inputStr.isEmpty()) {
                Toast.makeText(context, "Vui lòng nhập số tiền thu nhập!", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double totalIncome = Double.parseDouble(inputStr);

                Calendar cal = Calendar.getInstance();
                String monthYear = String.format(Locale.getDefault(), "%04d-%02d",
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);

                BudgetDAO budgetDAO = new BudgetDAO(context);
                // Lưu ngân sách tổng tháng (50% + 30% = 80% chi tiêu)
                budgetDAO.saveOrUpdateMonthlyBudget(userId, monthYear, totalIncome * 0.80, 80);

                Toast.makeText(context, "🎯 Đã áp dụng ngân sách 50/30/20 thành công!", Toast.LENGTH_LONG).show();

                if (listener != null) {
                    listener.onApplied();
                }

                dialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(context, "Số tiền không hợp lệ!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private static void calculateAndDisplay(String input, TextView tvNeeds, TextView tvWants, TextView tvSavings) {
        if (input == null || input.trim().isEmpty()) {
            tvNeeds.setText("0 đ");
            tvWants.setText("0 đ");
            tvSavings.setText("0 đ");
            return;
        }

        try {
            double income = Double.parseDouble(input.trim());
            double needs = income * 0.50;
            double wants = income * 0.30;
            double savings = income * 0.20;

            tvNeeds.setText(CurrencyUtils.formatVND(needs));
            tvWants.setText(CurrencyUtils.formatVND(wants));
            tvSavings.setText(CurrencyUtils.formatVND(savings));
        } catch (Exception e) {
            tvNeeds.setText("0 đ");
            tvWants.setText("0 đ");
            tvSavings.setText("0 đ");
        }
    }
}
