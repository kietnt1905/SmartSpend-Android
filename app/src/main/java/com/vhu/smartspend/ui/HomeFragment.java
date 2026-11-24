package com.vhu.smartspend.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.vhu.smartspend.R;
import com.vhu.smartspend.database.BudgetDAO;
import com.vhu.smartspend.model.Budget;
import com.vhu.smartspend.model.Category;
import com.vhu.smartspend.model.Transaction;
import com.vhu.smartspend.utils.BudgetAlertHelper;
import com.vhu.smartspend.utils.CategoryUiHelper;
import com.vhu.smartspend.utils.CurrencyUtils;
import com.vhu.smartspend.utils.DateUtils;
import com.vhu.smartspend.utils.SessionManager;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvUserName, tvTotalBalanceMain, tvHomeIncome, tvHomeExpense;
    private TextView tvBudgetTitle, tvBudgetDaysLeft, tvBudgetUsed, tvBudgetLimit, tvBudgetRemaining;
    private ProgressBar pbHomeBudget;
    private TextView tvRecentCount, tvEmptyRecentTransactions;
    private LinearLayout layoutRecentTransactions;

    private BudgetDAO budgetDAO;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        budgetDAO = new BudgetDAO(requireContext());
        sessionManager = new SessionManager(requireContext());

        NestedScrollView scrollView = view.findViewById(R.id.nestedScrollViewHome);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvTotalBalanceMain = view.findViewById(R.id.tvTotalBalanceMain);
        tvHomeIncome = view.findViewById(R.id.tvHomeIncome);
        tvHomeExpense = view.findViewById(R.id.tvHomeExpense);
        tvBudgetTitle = view.findViewById(R.id.tvBudgetTitle);
        tvBudgetDaysLeft = view.findViewById(R.id.tvBudgetDaysLeft);
        tvBudgetUsed = view.findViewById(R.id.tvBudgetUsed);
        tvBudgetLimit = view.findViewById(R.id.tvBudgetLimit);
        pbHomeBudget = view.findViewById(R.id.pbHomeBudget);
        tvBudgetRemaining = view.findViewById(R.id.tvBudgetRemaining);
        tvRecentCount = view.findViewById(R.id.tvRecentCount);
        tvEmptyRecentTransactions = view.findViewById(R.id.tvEmptyRecentTransactions);
        layoutRecentTransactions = view.findViewById(R.id.layoutRecentTransactions);

        view.findViewById(R.id.fabAddTransaction).setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), AddEditTransactionActivity.class)));

        if (scrollView != null) {
            scrollView.post(() -> scrollView.scrollTo(0, 0));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHomeData();
    }

    private void loadHomeData() {
        if (budgetDAO == null || getContext() == null) return;

        int userId = sessionManager.getUserId();
        if (userId <= 0) userId = 1;
        String fullName = sessionManager.getFullName();
        tvUserName.setText(!fullName.isEmpty() ? fullName : "Khách hàng");

        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH) + 1;
        String monthYear = String.format(Locale.getDefault(), "%04d-%02d", cal.get(Calendar.YEAR), month);

        double income = budgetDAO.getTotalIncomeByMonth(userId, monthYear);
        double expense = budgetDAO.getTotalExpenseByMonth(userId, monthYear);
        double totalBalance = budgetDAO.getTotalBalance(userId);

        tvTotalBalanceMain.setText(CurrencyUtils.formatVND(totalBalance));
        tvHomeIncome.setText("+" + CurrencyUtils.formatVND(income));
        tvHomeExpense.setText("-" + CurrencyUtils.formatVND(expense));

        Budget monthlyBudget = budgetDAO.getTotalMonthlyBudget(userId, monthYear);
        double budgetLimit = monthlyBudget != null ? monthlyBudget.getLimitAmount() : 0;

        updateBudgetCard(month, expense, budgetLimit > 0 ? budgetLimit : 5000000, cal);
        loadRecentTransactions(userId);
        BudgetAlertHelper.checkAndAlertMonthlyBudget(getContext(), expense, budgetLimit);
    }

    private void updateBudgetCard(int month, double expense, double limit, Calendar cal) {
        int daysLeft = cal.getActualMaximum(Calendar.DAY_OF_MONTH) - cal.get(Calendar.DAY_OF_MONTH);
        double remaining = limit - expense;
        int percent = limit > 0 ? (int) Math.min(100, Math.round((expense * 100.0) / limit)) : 0;

        tvBudgetTitle.setText("Ngân sách tháng " + month);
        tvBudgetDaysLeft.setText("Còn " + daysLeft + " ngày");
        tvBudgetUsed.setText("Đã dùng: " + CurrencyUtils.formatVND(expense));
        tvBudgetLimit.setText("Hạn mức: " + CurrencyUtils.formatVND(limit));
        pbHomeBudget.setProgress(percent);

        if (remaining >= 0) {
            tvBudgetRemaining.setText("Còn lại " + CurrencyUtils.formatVND(remaining));
            tvBudgetRemaining.setTextColor(Color.parseColor("#1565C0"));
        } else {
            tvBudgetRemaining.setText("Vượt ngân sách " + CurrencyUtils.formatVND(Math.abs(remaining)));
            tvBudgetRemaining.setTextColor(Color.parseColor("#C62828"));
        }
    }

    private void loadRecentTransactions(int userId) {
        List<Transaction> list = budgetDAO.getRecentTransactions(userId, 10);
        tvRecentCount.setText(String.valueOf(list.size()));
        layoutRecentTransactions.removeAllViews();

        if (list.isEmpty()) {
            tvEmptyRecentTransactions.setVisibility(View.VISIBLE);
            return;
        }
        tvEmptyRecentTransactions.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < list.size(); i++) {
            Transaction tx = list.get(i);
            boolean isIncome = Category.TYPE_INCOME.equalsIgnoreCase(tx.getCategoryType());

            View row = inflater.inflate(R.layout.item_recent_transaction, layoutRecentTransactions, false);
            if (i == list.size() - 1) {
                ((ViewGroup.MarginLayoutParams) row.getLayoutParams()).bottomMargin = 0;
            }

            TextView tvIcon = row.findViewById(R.id.tvTxIcon);
            TextView tvTitle = row.findViewById(R.id.tvTxTitle);
            TextView tvSub = row.findViewById(R.id.tvTxSub);
            TextView tvAmount = row.findViewById(R.id.tvTxAmount);

            int borderColor = CategoryUiHelper.parseColor(tx.getCategoryColorCode(), isIncome ? "#0A5C36" : "#C62828");
            tvIcon.setBackground(CategoryUiHelper.createCircleBg(requireContext(), borderColor, 2));
            tvIcon.setText(CategoryUiHelper.getEmoji(tx.getCategoryIconName()));

            tvTitle.setText(!TextUtils.isEmpty(tx.getTitle()) ? tx.getTitle() : tx.getCategoryName());
            String cat = !TextUtils.isEmpty(tx.getCategoryName()) ? tx.getCategoryName() : "Khác";
            tvSub.setText(cat + " • " + DateUtils.formatToDisplay(tx.getTransactionDate()));

            tvAmount.setText((isIncome ? "+" : "-") + CurrencyUtils.formatVND(tx.getAmount()));
            tvAmount.setTextColor(Color.parseColor(isIncome ? "#047857" : "#B91C1C"));
            tvAmount.setBackgroundResource(isIncome ? R.drawable.bg_tx_amount_income : R.drawable.bg_tx_amount_expense);

            row.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), TransactionDetailActivity.class);
                intent.putExtra("transaction_id", tx.getTransactionId());
                startActivity(intent);
            });

            layoutRecentTransactions.addView(row);
        }
    }
}
