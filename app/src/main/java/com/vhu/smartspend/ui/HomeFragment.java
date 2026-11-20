package com.vhu.smartspend.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
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
import com.vhu.smartspend.utils.CurrencyUtils;
import com.vhu.smartspend.utils.DateUtils;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private NestedScrollView nestedScrollViewHome;
    private TextView tvUserName, tvTotalBalanceMain, tvHomeIncome, tvHomeExpense;
    private TextView tvBudgetTitle, tvBudgetDaysLeft, tvBudgetUsed, tvBudgetLimit, tvBudgetRemaining;
    private ProgressBar pbHomeBudget;
    private TextView tvRecentCount, tvEmptyRecentTransactions;
    private LinearLayout layoutRecentTransactions;
    private View fabAddTransaction;
    private BudgetDAO budgetDAO;

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

        nestedScrollViewHome = view.findViewById(R.id.nestedScrollViewHome);
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
        fabAddTransaction = view.findViewById(R.id.fabAddTransaction);

        if (fabAddTransaction != null) {
            fabAddTransaction.setOnClickListener(v -> {
                if (getActivity() != null) {
                    startActivity(new Intent(getActivity(), AddEditTransactionActivity.class));
                }
            });
        }

        if (nestedScrollViewHome != null) {
            nestedScrollViewHome.post(() -> nestedScrollViewHome.scrollTo(0, 0));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHomeData();
    }

    private void loadHomeData() {
        if (budgetDAO == null || getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences("SmartSpendPrefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("user_id", 1);
        if (userId <= 0) userId = 1;
        String fullName = prefs.getString("full_name", "Khách hàng");
        if (TextUtils.isEmpty(fullName)) fullName = "Khách hàng";

        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH) + 1;
        String monthYear = String.format(Locale.getDefault(), "%04d-%02d", cal.get(Calendar.YEAR), month);

        double income = budgetDAO.getTotalIncomeByMonth(userId, monthYear);
        double expense = budgetDAO.getTotalExpenseByMonth(userId, monthYear);
        double totalBalance = budgetDAO.getTotalBalance(userId);

        tvUserName.setText(fullName);
        tvTotalBalanceMain.setText(CurrencyUtils.formatVND(totalBalance));
        tvHomeIncome.setText("+" + CurrencyUtils.formatVND(income));
        tvHomeExpense.setText("-" + CurrencyUtils.formatVND(expense));

        updateBudgetCard(userId, month, monthYear, expense, cal);
        loadRecentTransactions(userId);

        BudgetAlertHelper.BudgetAlertStatus alertStatus =
                BudgetAlertHelper.checkTotalMonthlyBudgetAlert(getContext(), userId, monthYear);
        if (alertStatus.isExceeded()) {
            BudgetAlertHelper.processBudgetAlertActions(getContext(), alertStatus);
        }
    }

    private void updateBudgetCard(int userId, int month, String monthYear, double expense, Calendar cal) {
        int daysLeft = cal.getActualMaximum(Calendar.DAY_OF_MONTH) - cal.get(Calendar.DAY_OF_MONTH);
        Budget monthlyBudget = budgetDAO.getTotalMonthlyBudget(userId, monthYear);
        double limit = (monthlyBudget != null && monthlyBudget.getLimitAmount() > 0)
                ? monthlyBudget.getLimitAmount() : 5000000;
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

        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < list.size(); i++) {
            Transaction tx = list.get(i);
            boolean isIncome = Category.TYPE_INCOME.equalsIgnoreCase(tx.getCategoryType());

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, Math.round(8 * d), 0, Math.round(8 * d));
            row.setClickable(true);
            row.setFocusable(true);

            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(Color.parseColor(isIncome ? "#D1FAE5" : "#FFE4DE"));

            TextView tvIcon = new TextView(getContext());
            tvIcon.setLayoutParams(new LinearLayout.LayoutParams(Math.round(40 * d), Math.round(40 * d)));
            tvIcon.setGravity(Gravity.CENTER);
            tvIcon.setText(mapEmoji(tx.getCategoryIconName()));
            tvIcon.setTextSize(17f);
            tvIcon.setBackground(circle);

            LinearLayout info = new LinearLayout(getContext());
            info.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            infoLp.setMarginStart(Math.round(12 * d));
            info.setLayoutParams(infoLp);

            TextView tvTitle = new TextView(getContext());
            tvTitle.setText(!TextUtils.isEmpty(tx.getTitle()) ? tx.getTitle() : tx.getCategoryName());
            tvTitle.setTextColor(Color.parseColor("#1A1A1A"));
            tvTitle.setTextSize(14f);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setMaxLines(1);
            tvTitle.setEllipsize(TextUtils.TruncateAt.END);

            String cat = !TextUtils.isEmpty(tx.getCategoryName()) ? tx.getCategoryName() : "Khác";
            TextView tvSub = new TextView(getContext());
            tvSub.setText(cat + " • " + DateUtils.formatToDisplay(tx.getTransactionDate()));
            tvSub.setTextColor(Color.parseColor("#757575"));
            tvSub.setTextSize(11f);

            info.addView(tvTitle);
            info.addView(tvSub);

            TextView tvAmount = new TextView(getContext());
            tvAmount.setText((isIncome ? "+" : "-") + CurrencyUtils.formatVND(tx.getAmount()));
            tvAmount.setTextColor(Color.parseColor(isIncome ? "#2E7D32" : "#C62828"));
            tvAmount.setTextSize(14f);
            tvAmount.setTypeface(null, Typeface.BOLD);

            row.addView(tvIcon);
            row.addView(info);
            row.addView(tvAmount);

            row.setOnClickListener(v -> {
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), TransactionDetailActivity.class);
                    intent.putExtra("transaction_id", tx.getTransactionId());
                    startActivity(intent);
                }
            });

            layoutRecentTransactions.addView(row);

            if (i < list.size() - 1) {
                View divider = new View(getContext());
                LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Math.round(1 * d));
                divLp.setMargins(0, Math.round(4 * d), 0, Math.round(4 * d));
                divider.setLayoutParams(divLp);
                divider.setBackgroundColor(Color.parseColor("#F0F0F0"));
                layoutRecentTransactions.addView(divider);
            }
        }
    }

    private String mapEmoji(String iconName) {
        if (iconName == null) return "📌";
        switch (iconName.toLowerCase(Locale.getDefault())) {
            case "ic_food": return "🍴";
            case "ic_school": return "📚";
            case "ic_shopping": return "🛍️";
            case "ic_transport": return "🚗";
            case "ic_award": return "🎓";
            case "ic_work": return "💼";
            default: return "📌";
        }
    }
}
