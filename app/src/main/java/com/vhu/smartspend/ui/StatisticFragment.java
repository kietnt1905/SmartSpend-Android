package com.vhu.smartspend.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.vhu.smartspend.R;
import com.vhu.smartspend.database.BudgetDAO;
import com.vhu.smartspend.model.Category;
import com.vhu.smartspend.utils.CategoryUiHelper;
import com.vhu.smartspend.utils.CurrencyUtils;
import com.vhu.smartspend.utils.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Màn hình Thống kê Thu / Chi theo Ngày / Tháng / Năm.
 */
public class StatisticFragment extends Fragment {

    private static final int TAB_DAY = 0;
    private static final int TAB_MONTH = 1;
    private static final int TAB_YEAR = 2;

    private static final int SORT_PERCENT_DESC = 0;
    private static final int SORT_PERCENT_ASC = 1;
    private static final int SORT_TX_COUNT_DESC = 2;
    private static final int SORT_NAME_ASC = 3;

    private TextView tabDay, tabMonth, tabYear, tabExpense, tabIncome;
    private TextView tvDateRangeDisplay, tvOverviewTitle, tvComparisonBadge, tvTotalExpenseMain;
    private MaterialCardView cardOverviewSub, cardNetBalance;
    private TextView tvOverviewSubLabel, tvTotalIncome, tvNetBalance;
    private TextView tvCategorySectionTitle, tvSortOrder, tvEmptyState;
    private SearchView svTransactionSearch;
    private RecyclerView rvCategorySpending;

    private BudgetDAO budgetDAO;
    private SessionManager sessionManager;
    private List<CategoryItem> allCategoryItems = new ArrayList<>();

    private int selectedYear, selectedMonth, selectedDay;
    private int currentTab = TAB_MONTH;
    private boolean isExpenseMode = true;
    private int currentSortMode = SORT_PERCENT_DESC;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistic, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        budgetDAO = new BudgetDAO(requireContext());
        sessionManager = new SessionManager(requireContext());

        Calendar cal = Calendar.getInstance();
        selectedYear = cal.get(Calendar.YEAR);
        selectedMonth = cal.get(Calendar.MONTH);
        selectedDay = cal.get(Calendar.DAY_OF_MONTH);

        initViews(view);
        setupEvents(view);
        updateTabStyles();
        updateDateDisplay();
        loadStatisticData();

        NestedScrollView scrollView = view.findViewById(R.id.nestedScrollView);
        if (scrollView != null) {
            scrollView.post(() -> scrollView.scrollTo(0, 0));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (budgetDAO != null) {
            loadStatisticData();
        }
    }

    private void initViews(View view) {
        tabDay = view.findViewById(R.id.tabDay);
        tabMonth = view.findViewById(R.id.tabMonth);
        tabYear = view.findViewById(R.id.tabYear);
        tabExpense = view.findViewById(R.id.tabExpense);
        tabIncome = view.findViewById(R.id.tabIncome);
        tvDateRangeDisplay = view.findViewById(R.id.tvDateRangeDisplay);

        tvOverviewTitle = view.findViewById(R.id.tvOverviewTitle);
        tvComparisonBadge = view.findViewById(R.id.tvComparisonBadge);
        tvTotalExpenseMain = view.findViewById(R.id.tvTotalExpenseMain);
        cardOverviewSub = view.findViewById(R.id.cardOverviewSub);
        cardNetBalance = view.findViewById(R.id.cardNetBalance);
        tvOverviewSubLabel = view.findViewById(R.id.tvOverviewSubLabel);
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome);
        tvNetBalance = view.findViewById(R.id.tvNetBalance);

        tvCategorySectionTitle = view.findViewById(R.id.tvCategorySectionTitle);
        tvSortOrder = view.findViewById(R.id.tvSortOrder);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        svTransactionSearch = view.findViewById(R.id.svTransactionSearch);
        rvCategorySpending = view.findViewById(R.id.rvCategorySpending);
        rvCategorySpending.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void setupEvents(View view) {
        // Chọn tab Ngày / Tháng / Năm
        View.OnClickListener timeListener = v -> {
            if (v.getId() == R.id.tabDay) currentTab = TAB_DAY;
            else if (v.getId() == R.id.tabYear) currentTab = TAB_YEAR;
            else currentTab = TAB_MONTH;
            clampSelectedDay();
            updateTabStyles();
            updateDateDisplay();
            loadStatisticData();
        };
        tabDay.setOnClickListener(timeListener);
        tabMonth.setOnClickListener(timeListener);
        tabYear.setOnClickListener(timeListener);

        // Chuyển lùi / tới kỳ thống kê
        view.findViewById(R.id.btnPrevDate).setOnClickListener(v -> {
            adjustDate(-1);
            loadStatisticData();
        });
        view.findViewById(R.id.btnNextDate).setOnClickListener(v -> {
            adjustDate(1);
            loadStatisticData();
        });
        view.findViewById(R.id.cardDateRangePicker).setOnClickListener(v -> openDatePicker());

        // Chuyển chế độ Chi tiêu / Thu nhập
        tabExpense.setOnClickListener(v -> {
            isExpenseMode = true;
            updateTabStyles();
            loadStatisticData();
        });
        tabIncome.setOnClickListener(v -> {
            isExpenseMode = false;
            updateTabStyles();
            loadStatisticData();
        });

        // Tìm kiếm theo tên danh mục
        svTransactionSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterAndShowList(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterAndShowList(newText);
                return true;
            }
        });

        // Menu sắp xếp (PopupMenu)
        tvSortOrder.setOnClickListener(v -> showSortMenu());
    }

    private void updateTabStyles() {
        TextView[] timeTabs = {tabDay, tabMonth, tabYear};
        for (TextView tab : timeTabs) {
            tab.setTextColor(Color.parseColor("#666666"));
            tab.setBackground(null);
        }
        TextView activeTime = currentTab == TAB_DAY ? tabDay : (currentTab == TAB_YEAR ? tabYear : tabMonth);
        activeTime.setTextColor(Color.WHITE);
        activeTime.setBackground(CategoryUiHelper.createRoundedBg(requireContext(), Color.parseColor("#0B5A4B"), 17));

        tabExpense.setTextColor(isExpenseMode ? Color.WHITE : Color.parseColor("#666666"));
        tabExpense.setBackground(isExpenseMode
                ? CategoryUiHelper.createRoundedBg(requireContext(), Color.parseColor("#D32F2F"), 17) : null);

        tabIncome.setTextColor(isExpenseMode ? Color.parseColor("#666666") : Color.WHITE);
        tabIncome.setBackground(isExpenseMode
                ? null : CategoryUiHelper.createRoundedBg(requireContext(), Color.parseColor("#0B8043"), 17));
    }

    private void showSortMenu() {
        PopupMenu popup = new PopupMenu(requireContext(), tvSortOrder);
        popup.getMenu().add(0, SORT_PERCENT_DESC, 0, "% cao nhất");
        popup.getMenu().add(0, SORT_PERCENT_ASC, 1, "% thấp nhất");
        popup.getMenu().add(0, SORT_TX_COUNT_DESC, 2, "Nhiều giao dịch nhất");
        popup.getMenu().add(0, SORT_NAME_ASC, 3, "Tên danh mục (A-Z)");

        popup.setOnMenuItemClickListener(item -> {
            currentSortMode = item.getItemId();
            String[] labels = {
                    "Sắp xếp: % cao nhất ▾",
                    "Sắp xếp: % thấp nhất ▾",
                    "Sắp xếp: Nhiều giao dịch ▾",
                    "Sắp xếp: Tên A-Z ▾"
            };
            tvSortOrder.setText(labels[currentSortMode]);
            filterAndShowList(svTransactionSearch.getQuery().toString());
            return true;
        });
        popup.show();
    }

    private void openDatePicker() {
        if (currentTab == TAB_DAY) {
            new DatePickerDialog(requireContext(), (v, y, m, d) -> {
                selectedYear = y;
                selectedMonth = m;
                selectedDay = d;
                updateDateDisplay();
                loadStatisticData();
            }, selectedYear, selectedMonth, selectedDay).show();
            return;
        }

        int pad = CategoryUiHelper.dp(requireContext(), 16);
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(pad, pad, pad, pad / 2);

        NumberPicker npMonth = new NumberPicker(requireContext());
        if (currentTab == TAB_MONTH) {
            npMonth.setMinValue(1);
            npMonth.setMaxValue(12);
            npMonth.setValue(selectedMonth + 1);
            String[] labels = {"T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12"};
            npMonth.setDisplayedValues(labels);
            container.addView(createPickerColumn("Tháng", npMonth));
        }

        NumberPicker npYear = new NumberPicker(requireContext());
        npYear.setMinValue(Math.min(2020, selectedYear - 5));
        npYear.setMaxValue(Math.max(2035, selectedYear + 5));
        npYear.setValue(selectedYear);
        npYear.setWrapSelectorWheel(false);
        container.addView(createPickerColumn("Năm", npYear));

        new AlertDialog.Builder(requireContext())
                .setTitle(currentTab == TAB_YEAR ? "Chọn năm" : "Chọn tháng và năm")
                .setView(container)
                .setPositiveButton("Chọn", (dialog, which) -> {
                    if (currentTab == TAB_MONTH) selectedMonth = npMonth.getValue() - 1;
                    selectedYear = npYear.getValue();
                    clampSelectedDay();
                    updateDateDisplay();
                    loadStatisticData();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private LinearLayout createPickerColumn(String title, NumberPicker picker) {
        LinearLayout col = new LinearLayout(requireContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(title);
        tvLabel.setTextSize(13f);
        tvLabel.setTypeface(null, Typeface.BOLD);
        tvLabel.setTextColor(Color.parseColor("#0B5A4B"));

        col.addView(tvLabel);
        col.addView(picker);
        return col;
    }

    private void adjustDate(int delta) {
        if (currentTab == TAB_DAY) {
            Calendar cal = Calendar.getInstance();
            cal.set(selectedYear, selectedMonth, selectedDay);
            cal.add(Calendar.DAY_OF_MONTH, delta);
            selectedYear = cal.get(Calendar.YEAR);
            selectedMonth = cal.get(Calendar.MONTH);
            selectedDay = cal.get(Calendar.DAY_OF_MONTH);
        } else if (currentTab == TAB_MONTH) {
            selectedMonth += delta;
            if (selectedMonth > 11) {
                selectedMonth = 0;
                selectedYear++;
            } else if (selectedMonth < 0) {
                selectedMonth = 11;
                selectedYear--;
            }
            clampSelectedDay();
        } else {
            selectedYear += delta;
            clampSelectedDay();
        }
        updateDateDisplay();
    }

    private void clampSelectedDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(selectedYear, selectedMonth, 1);
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (selectedDay > maxDay) selectedDay = maxDay;
        if (selectedDay < 1) selectedDay = 1;
    }

    private void updateDateDisplay() {
        if (currentTab == TAB_DAY) {
            tvDateRangeDisplay.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d",
                    selectedDay, selectedMonth + 1, selectedYear));
        } else if (currentTab == TAB_YEAR) {
            tvDateRangeDisplay.setText(String.format(Locale.getDefault(), "Năm %04d", selectedYear));
        } else {
            tvDateRangeDisplay.setText(String.format(Locale.getDefault(), "Tháng %02d/%04d",
                    selectedMonth + 1, selectedYear));
        }
    }

    private void loadStatisticData() {
        int userId = sessionManager.getUserId() > 0 ? sessionManager.getUserId() : 1;
        String datePrefix = getDatePrefix(false);
        String prevPrefix = getDatePrefix(true);

        double totalExpense = budgetDAO.getTotalAmountByDatePrefix(userId, Category.TYPE_EXPENSE, datePrefix);
        double totalIncome = budgetDAO.getTotalAmountByDatePrefix(userId, Category.TYPE_INCOME, datePrefix);
        double netBalance = totalIncome - totalExpense;

        String activeType = isExpenseMode ? Category.TYPE_EXPENSE : Category.TYPE_INCOME;
        double mainAmount = isExpenseMode ? totalExpense : totalIncome;
        double prevAmount = budgetDAO.getTotalAmountByDatePrefix(userId, activeType, prevPrefix);

        // Cập nhật thẻ tổng quan
        if (isExpenseMode) {
            tvOverviewTitle.setText("Tổng chi tiêu");
            tvTotalExpenseMain.setText(totalExpense > 0 ? "-" + CurrencyUtils.formatVND(totalExpense) : "0 đ");
            tvTotalExpenseMain.setTextColor(Color.parseColor("#DC2626"));

            tvOverviewSubLabel.setText("Tổng thu");
            tvOverviewSubLabel.setTextColor(Color.parseColor("#047857"));
            tvTotalIncome.setText(totalIncome > 0 ? "+" + CurrencyUtils.formatVND(totalIncome) : "0 đ");
            tvTotalIncome.setTextColor(Color.parseColor("#065F46"));
            cardOverviewSub.setCardBackgroundColor(Color.parseColor("#ECFDF5"));
            cardOverviewSub.setStrokeColor(Color.parseColor("#D1FAE5"));

            tvCategorySectionTitle.setText("Chi tiết chi tiêu");
            tvEmptyState.setText("Chưa có dữ liệu chi tiêu trong khoảng thời gian này.");
        } else {
            tvOverviewTitle.setText("Tổng thu nhập");
            tvTotalExpenseMain.setText(totalIncome > 0 ? "+" + CurrencyUtils.formatVND(totalIncome) : "0 đ");
            tvTotalExpenseMain.setTextColor(Color.parseColor("#059669"));

            tvOverviewSubLabel.setText("Tổng chi");
            tvOverviewSubLabel.setTextColor(Color.parseColor("#B91C1C"));
            tvTotalIncome.setText(totalExpense > 0 ? "-" + CurrencyUtils.formatVND(totalExpense) : "0 đ");
            tvTotalIncome.setTextColor(Color.parseColor("#991B1B"));
            cardOverviewSub.setCardBackgroundColor(Color.parseColor("#FEF2F2"));
            cardOverviewSub.setStrokeColor(Color.parseColor("#FECACA"));

            tvCategorySectionTitle.setText("Chi tiết thu nhập");
            tvEmptyState.setText("Chưa có dữ liệu thu nhập trong khoảng thời gian này.");
        }

        boolean isNegativeNet = netBalance < 0;
        tvNetBalance.setText(netBalance == 0 ? "0 đ"
                : (isNegativeNet ? "-" : "+") + CurrencyUtils.formatVND(Math.abs(netBalance)));
        tvNetBalance.setTextColor(Color.parseColor(isNegativeNet ? "#B91C1C" : "#1E40AF"));
        cardNetBalance.setCardBackgroundColor(Color.parseColor(isNegativeNet ? "#FEF2F2" : "#EFF6FF"));
        cardNetBalance.setStrokeColor(Color.parseColor(isNegativeNet ? "#FECACA" : "#DBEAFE"));

        updateComparisonBadge(mainAmount, prevAmount);

        // Lấy danh sách danh mục từ SQLite
        List<BudgetDAO.CategorySummary> summaries = budgetDAO.getCategorySummaryByDatePrefix(userId, activeType, datePrefix);
        allCategoryItems = new ArrayList<>();
        for (BudgetDAO.CategorySummary s : summaries) {
            int percent = mainAmount > 0 ? (int) Math.round((s.totalAmount * 100.0) / mainAmount) : 0;
            int color = CategoryUiHelper.parseColor(s.colorCode, isExpenseMode ? "#D32F2F" : "#0B8043");
            allCategoryItems.add(new CategoryItem(
                    s.categoryId, s.name, s.totalAmount, percent, s.transactionCount,
                    CategoryUiHelper.getEmoji(s.iconName), color));
        }

        filterAndShowList(svTransactionSearch.getQuery().toString());
    }

    private void updateComparisonBadge(double currentAmount, double prevAmount) {
        if (prevAmount <= 0) {
            tvComparisonBadge.setVisibility(View.GONE);
            return;
        }
        long diffPct = Math.round(((currentAmount - prevAmount) / prevAmount) * 100.0);
        boolean isGood = isExpenseMode ? (diffPct <= 0) : (diffPct >= 0);

        tvComparisonBadge.setVisibility(View.VISIBLE);
        tvComparisonBadge.setText((diffPct >= 0 ? "↑ +" : "↓ ") + diffPct + "% vs kỳ trước");
        tvComparisonBadge.setTextColor(Color.parseColor(isGood ? "#047857" : "#B91C1C"));
        tvComparisonBadge.setBackground(CategoryUiHelper.createRoundedBg(
                requireContext(), Color.parseColor(isGood ? "#D1FAE5" : "#FEE2E2"), 17));
    }

    private void filterAndShowList(String query) {
        String keyword = query != null ? query.trim().toLowerCase(Locale.getDefault()) : "";
        List<CategoryItem> filtered = new ArrayList<>();
        for (CategoryItem item : allCategoryItems) {
            if (keyword.isEmpty() || item.name.toLowerCase(Locale.getDefault()).contains(keyword)) {
                filtered.add(item);
            }
        }

        filtered.sort((a, b) -> {
            if (currentSortMode == SORT_PERCENT_ASC) return Double.compare(a.amount, b.amount);
            if (currentSortMode == SORT_TX_COUNT_DESC) {
                int cmp = Integer.compare(b.txCount, a.txCount);
                return cmp != 0 ? cmp : Double.compare(b.amount, a.amount);
            }
            if (currentSortMode == SORT_NAME_ASC) return a.name.compareToIgnoreCase(b.name);
            return Double.compare(b.amount, a.amount);
        });

        boolean isEmpty = filtered.isEmpty();
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvCategorySpending.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (!isEmpty) {
            rvCategorySpending.setAdapter(new CategoryAdapter(filtered));
        }
    }

    private String getDatePrefix(boolean previousPeriod) {
        if (currentTab == TAB_DAY) {
            Calendar cal = Calendar.getInstance();
            cal.set(selectedYear, selectedMonth, selectedDay);
            if (previousPeriod) cal.add(Calendar.DAY_OF_MONTH, -1);
            return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        }
        if (currentTab == TAB_YEAR) {
            return String.format(Locale.getDefault(), "%04d", previousPeriod ? selectedYear - 1 : selectedYear);
        }
        int y = selectedYear;
        int m = selectedMonth;
        if (previousPeriod) {
            m--;
            if (m < 0) {
                m = 11;
                y--;
            }
        }
        return String.format(Locale.getDefault(), "%04d-%02d", y, m + 1);
    }

    private void openCategoryTransactions(CategoryItem item) {
        Intent intent = new Intent(requireActivity(), CategoryTransactionsActivity.class);
        intent.putExtra(CategoryTransactionsActivity.EXTRA_CATEGORY_ID, item.categoryId);
        intent.putExtra(CategoryTransactionsActivity.EXTRA_CATEGORY_NAME, item.name);
        intent.putExtra(CategoryTransactionsActivity.EXTRA_CATEGORY_ICON_EMOJI, item.emoji);
        intent.putExtra(CategoryTransactionsActivity.EXTRA_CATEGORY_COLOR, item.color);
        intent.putExtra(CategoryTransactionsActivity.EXTRA_DATE_PREFIX, getDatePrefix(false));
        intent.putExtra(CategoryTransactionsActivity.EXTRA_PERIOD_DISPLAY, tvDateRangeDisplay.getText().toString());
        intent.putExtra(CategoryTransactionsActivity.EXTRA_IS_EXPENSE, isExpenseMode);
        startActivity(intent);
    }

    private static class CategoryItem {
        final int categoryId, percentage, txCount, color;
        final String name, emoji;
        final double amount;

        CategoryItem(int categoryId, String name, double amount, int percentage, int txCount, String emoji, int color) {
            this.categoryId = categoryId;
            this.name = name != null ? name : "";
            this.amount = amount;
            this.percentage = percentage;
            this.txCount = txCount;
            this.emoji = emoji;
            this.color = color;
        }
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private final List<CategoryItem> list;

        CategoryAdapter(List<CategoryItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_statistic_category, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            CategoryItem item = list.get(position);
            h.tvIcon.setText(item.emoji);
            h.tvIcon.setBackground(CategoryUiHelper.createCircleBg(requireContext(), item.color, 2));
            h.tvTitle.setText(item.name);
            h.tvSub.setText(item.txCount + " giao dịch");

            h.tvAmount.setText((isExpenseMode ? "-" : "+") + CurrencyUtils.formatVND(item.amount));
            h.tvAmount.setTextColor(Color.parseColor(isExpenseMode ? "#B91C1C" : "#047857"));

            h.tvPercent.setText(item.percentage + "%");
            h.tvPercent.setTextColor(item.color);
            h.tvPercent.setBackground(CategoryUiHelper.createRoundedBg(
                    requireContext(), CategoryUiHelper.withAlpha(item.color, 40), 8));

            h.pb.setProgress(item.percentage);
            h.pb.setProgressTintList(ColorStateList.valueOf(item.color));
            h.itemView.setOnClickListener(v -> openCategoryTransactions(item));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvIcon, tvTitle, tvSub, tvAmount, tvPercent;
            final ProgressBar pb;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvIcon = itemView.findViewById(R.id.tvStatCatIcon);
                tvTitle = itemView.findViewById(R.id.tvStatCatTitle);
                tvSub = itemView.findViewById(R.id.tvStatCatSub);
                tvAmount = itemView.findViewById(R.id.tvStatCatAmount);
                tvPercent = itemView.findViewById(R.id.tvStatCatPercent);
                pb = itemView.findViewById(R.id.pbStatCategory);
            }
        }
    }
}
