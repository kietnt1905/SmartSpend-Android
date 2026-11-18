package com.vhu.smartspend.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.vhu.smartspend.R;
import com.vhu.smartspend.database.BudgetDAO;
import com.vhu.smartspend.utils.BudgetAlertHelper;
import com.vhu.smartspend.utils.CurrencyUtils;

import java.util.Calendar;
import java.util.Locale;

/**
 * Màn hình Trang chủ (HomeFragment)
 * Hiển thị Dashboard Tài chính tổng quan chuẩn pixel-perfect:
 * - Tổng tài sản khả dụng & Nút nạp tiền/chuyển/quét QR
 * - Phím tắt nhanh (Thêm GD, Quét AI, Sổ nợ, Ngân sách, Báo cáo)
 * - Tiến độ ngân sách tháng hiện tại
 * - Thẻ tài khoản & ví liên kết (VCB, MB Bank)
 * - Danh sách 4 giao dịch gần đây nhất
 *
 * Tác giả: Lê Phúc Thịnh (MSSV: 231A290032)
 */
public class HomeFragment extends Fragment {

    private NestedScrollView nestedScrollViewHome;
    private TextView tvUserName, tvTotalBalanceMain, tvHomeIncome, tvHomeExpense;
    private LinearLayout btnActionAdd, btnActionBudget, btnActionReport;
    private BudgetDAO budgetDAO;
    private final int currentUserId = 1;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getContext() != null) {
            budgetDAO = new BudgetDAO(getContext());
        }

        initViews(view);
        setupClickListeners();
        loadHomeData();

        if (nestedScrollViewHome != null) {
            nestedScrollViewHome.post(() -> nestedScrollViewHome.scrollTo(0, 0));
        }
    }

    private void initViews(View view) {
        nestedScrollViewHome = view.findViewById(R.id.nestedScrollViewHome);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvTotalBalanceMain = view.findViewById(R.id.tvTotalBalanceMain);
        tvHomeIncome = view.findViewById(R.id.tvHomeIncome);
        tvHomeExpense = view.findViewById(R.id.tvHomeExpense);

        btnActionAdd = view.findViewById(R.id.btnActionAdd);
        btnActionBudget = view.findViewById(R.id.btnActionBudget);
        btnActionReport = view.findViewById(R.id.btnActionReport);
    }

    private void setupClickListeners() {
        if (btnActionAdd != null) {
            btnActionAdd.setOnClickListener(v -> {
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), AddEditTransactionActivity.class);
                    startActivity(intent);
                }
            });
        }

        if (btnActionBudget != null || btnActionReport != null) {
            View.OnClickListener navStatisticListener = v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new StatisticFragment())
                            .commit();
                }
            };

            if (btnActionBudget != null) btnActionBudget.setOnClickListener(navStatisticListener);
            if (btnActionReport != null) btnActionReport.setOnClickListener(navStatisticListener);
        }
    }

    private void loadHomeData() {
        if (budgetDAO == null) return;

        Calendar cal = Calendar.getInstance();
        String monthYear = String.format(Locale.getDefault(), "%04d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);

        double income = budgetDAO.getTotalIncomeByMonth(currentUserId, monthYear);
        double expense = budgetDAO.getTotalExpenseByMonth(currentUserId, monthYear);

        if (income == 0 && expense == 0) {
            income = 38500000;
            expense = 24850000;
        }

        double totalAsset = 85420000;

        if (tvUserName != null) {
            tvUserName.setText("Khách hàng");
        }
        tvTotalBalanceMain.setText(CurrencyUtils.formatVND(totalAsset));
        tvHomeIncome.setText("+" + CurrencyUtils.formatVND(income));
        tvHomeExpense.setText("-" + CurrencyUtils.formatVND(expense));

        // Tự động kiểm tra cảnh báo ngân sách
        if (getContext() != null) {
            BudgetAlertHelper.BudgetAlertStatus alertStatus =
                    BudgetAlertHelper.checkTotalMonthlyBudgetAlert(getContext(), currentUserId, monthYear);
            if (alertStatus.isExceeded()) {
                BudgetAlertHelper.processBudgetAlertActions(getContext(), alertStatus);
            }
        }
    }
}
