package com.vhu.smartspend.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
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
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.vhu.smartspend.R;
import com.vhu.smartspend.database.BudgetDAO;
import com.vhu.smartspend.model.Category;
import com.vhu.smartspend.model.Transaction;
import com.vhu.smartspend.utils.CurrencyUtils;
import com.vhu.smartspend.utils.DateUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Màn hình Thống kê Thu / Chi (StatisticFragment)
 * - Lọc theo Ngày / Tháng / Năm
 * - Tách riêng chế độ xem Chi tiêu và Thu nhập
 * - Thẻ tổng quan gọn, đồng bộ theo loại đang xem
 *
 * Tác giả: Lê Phúc Thịnh (MSSV: 231A290032)
 */
public class StatisticFragment extends Fragment {

    private static final int TAB_DAY = 0;
    private static final int TAB_MONTH = 1;
    private static final int TAB_YEAR = 2;

    private static final int TYPE_EXPENSE = 0;
    private static final int TYPE_INCOME = 1;

    private static final int SORT_PERCENT_DESC = 0;
    private static final int SORT_PERCENT_ASC = 1;
    private static final int SORT_TX_COUNT_DESC = 2;
    private static final int SORT_NAME_ASC = 3;

    // View Components
    private NestedScrollView nestedScrollView;
    private TextView tabDay, tabMonth, tabYear;
    private TextView tabExpense, tabIncome;
    private TextView btnPrevDate, btnNextDate, tvDateRangeDisplay;
    private CardView cardDateRangePicker;

    private TextView tvOverviewTitle, tvComparisonBadge, tvTotalExpenseMain;
    private MaterialCardView cardOverviewSub, cardNetBalance;
    private TextView tvOverviewSubLabel, tvTotalIncome, tvNetBalance;
    private TextView tvCategorySectionTitle, tvSortOrder, tvEmptyState;
    private SearchView svTransactionSearch;
    private RecyclerView rvCategorySpending;

    // Data
    private List<CategoryDetailItem> allCategoryItems = new ArrayList<>();
    private CategoryAdapter categoryAdapter;
    private BottomSheetDialog activeCategorySheet;
    private View activeSheetView;
    private CategoryDetailItem activeSheetItem;

    // Database & Calendar
    private BudgetDAO budgetDAO;
    private int selectedYear;
    private int selectedMonth; // 0-based (0..11)
    private int selectedDay;
    private int currentTab = TAB_MONTH;
    private int currentType = TYPE_EXPENSE;
    private int currentSortMode = SORT_PERCENT_DESC;

    private int getCurrentUserId() {
        if (getContext() == null) return 1;
        int id = getContext().getSharedPreferences("SmartSpendPrefs", Context.MODE_PRIVATE).getInt("user_id", 1);
        return id > 0 ? id : 1;
    }

    public StatisticFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistic, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getContext() != null) {
            budgetDAO = new BudgetDAO(getContext());
        }
        Calendar currentCalendar = Calendar.getInstance();
        selectedYear = currentCalendar.get(Calendar.YEAR);
        selectedMonth = currentCalendar.get(Calendar.MONTH);
        selectedDay = currentCalendar.get(Calendar.DAY_OF_MONTH);

        initViews(view);
        setupTimeFilterTabs();
        setupTypeFilterTabs();
        setupMonthYearPicker();
        setupInstantSearchView();
        setupSortOrder();

        rvCategorySpending.setLayoutManager(new LinearLayoutManager(getContext()));

        loadStatisticData();

        if (nestedScrollView != null) {
            nestedScrollView.post(() -> nestedScrollView.scrollTo(0, 0));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (budgetDAO != null) {
            loadStatisticData();
            if (activeCategorySheet != null && activeCategorySheet.isShowing()
                    && activeSheetView != null && activeSheetItem != null) {
                bindCategoryTransactionsSheet(activeSheetView, activeSheetItem);
            }
        }
    }

    private void initViews(View view) {
        nestedScrollView = view.findViewById(R.id.nestedScrollView);
        tabDay = view.findViewById(R.id.tabDay);
        tabMonth = view.findViewById(R.id.tabMonth);
        tabYear = view.findViewById(R.id.tabYear);
        tabExpense = view.findViewById(R.id.tabExpense);
        tabIncome = view.findViewById(R.id.tabIncome);

        btnPrevDate = view.findViewById(R.id.btnPrevDate);
        btnNextDate = view.findViewById(R.id.btnNextDate);
        tvDateRangeDisplay = view.findViewById(R.id.tvDateRangeDisplay);
        cardDateRangePicker = view.findViewById(R.id.cardDateRangePicker);

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
    }

    private void setupInstantSearchView() {
        if (svTransactionSearch == null) return;

        svTransactionSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterCategoryList(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCategoryList(newText);
                return true;
            }
        });
    }

    private void setupSortOrder() {
        if (tvSortOrder == null) return;
        updateSortLabel();
        tvSortOrder.setOnClickListener(v -> {
            if (getContext() == null) return;
            PopupMenu popup = new PopupMenu(getContext(), tvSortOrder);
            popup.getMenu().add(0, SORT_PERCENT_DESC, 0, "% cao nhất");
            popup.getMenu().add(0, SORT_PERCENT_ASC, 1, "% thấp nhất");
            popup.getMenu().add(0, SORT_TX_COUNT_DESC, 2, "Nhiều giao dịch nhất");
            popup.getMenu().add(0, SORT_NAME_ASC, 3, "Tên danh mục (A-Z)");

            popup.setOnMenuItemClickListener(item -> {
                currentSortMode = item.getItemId();
                updateSortLabel();
                String currentQuery = svTransactionSearch != null ? svTransactionSearch.getQuery().toString() : "";
                filterCategoryList(currentQuery);
                return true;
            });
            popup.show();
        });
    }

    private void updateSortLabel() {
        if (tvSortOrder == null) return;
        switch (currentSortMode) {
            case SORT_PERCENT_ASC:
                tvSortOrder.setText("Sắp xếp: % thấp nhất ▾");
                break;
            case SORT_TX_COUNT_DESC:
                tvSortOrder.setText("Sắp xếp: Nhiều giao dịch ▾");
                break;
            case SORT_NAME_ASC:
                tvSortOrder.setText("Sắp xếp: Tên A-Z ▾");
                break;
            case SORT_PERCENT_DESC:
            default:
                tvSortOrder.setText("Sắp xếp: % cao nhất ▾");
                break;
        }
    }

    private void filterCategoryList(String query) {
        if (allCategoryItems == null) return;

        List<CategoryDetailItem> filteredList = new ArrayList<>();
        String lowerQuery = query != null ? query.trim().toLowerCase(Locale.getDefault()) : "";

        for (CategoryDetailItem item : allCategoryItems) {
            if (lowerQuery.isEmpty() || item.name.toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                filteredList.add(item);
            }
        }

        sortCategoryItems(filteredList);

        if (filteredList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvCategorySpending.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvCategorySpending.setVisibility(View.VISIBLE);
            categoryAdapter = new CategoryAdapter(filteredList, currentType == TYPE_EXPENSE, this::openCategoryTransactionsSheet);
            rvCategorySpending.setAdapter(categoryAdapter);
        }
    }

    private void sortCategoryItems(List<CategoryDetailItem> items) {
        Collator collator = Collator.getInstance(Locale.forLanguageTag("vi-VN"));
        collator.setStrength(Collator.PRIMARY);

        items.sort((a, b) -> {
            switch (currentSortMode) {
                case SORT_PERCENT_ASC:
                    return Double.compare(a.amount, b.amount);
                case SORT_TX_COUNT_DESC:
                    int countCmp = Integer.compare(b.txCount, a.txCount);
                    return countCmp != 0 ? countCmp : Double.compare(b.amount, a.amount);
                case SORT_NAME_ASC:
                    String nameA = a.name != null ? a.name : "";
                    String nameB = b.name != null ? b.name : "";
                    return collator.compare(nameA, nameB);
                case SORT_PERCENT_DESC:
                default:
                    return Double.compare(b.amount, a.amount);
            }
        });
    }

    private void setupTimeFilterTabs() {
        View.OnClickListener tabListener = v -> {
            int id = v.getId();
            if (id == R.id.tabDay) {
                currentTab = TAB_DAY;
            } else if (id == R.id.tabMonth) {
                currentTab = TAB_MONTH;
            } else if (id == R.id.tabYear) {
                currentTab = TAB_YEAR;
            }
            clampSelectedDay();
            updateTimeTabStyles();
            updateDateRangeDisplay();
            loadStatisticData();
        };

        tabDay.setOnClickListener(tabListener);
        tabMonth.setOnClickListener(tabListener);
        tabYear.setOnClickListener(tabListener);

        updateTimeTabStyles();
        updateDateRangeDisplay();

        btnPrevDate.setOnClickListener(v -> {
            adjustDateRange(-1);
            loadStatisticData();
        });

        btnNextDate.setOnClickListener(v -> {
            adjustDateRange(1);
            loadStatisticData();
        });
    }

    private void setupTypeFilterTabs() {
        View.OnClickListener typeListener = v -> {
            int id = v.getId();
            if (id == R.id.tabExpense) {
                currentType = TYPE_EXPENSE;
            } else if (id == R.id.tabIncome) {
                currentType = TYPE_INCOME;
            }
            updateTypeTabStyles();
            loadStatisticData();
        };

        tabExpense.setOnClickListener(typeListener);
        tabIncome.setOnClickListener(typeListener);

        updateTypeTabStyles();
    }

    private void updateTimeTabStyles() {
        resetTab(tabDay);
        resetTab(tabMonth);
        resetTab(tabYear);

        TextView activeTab;
        if (currentTab == TAB_DAY) {
            activeTab = tabDay;
        } else if (currentTab == TAB_YEAR) {
            activeTab = tabYear;
        } else {
            activeTab = tabMonth;
        }
        activeTab.setTextColor(Color.WHITE);
        activeTab.setBackground(createTabDrawable(Color.parseColor("#0B5A4B")));
    }

    private void updateTypeTabStyles() {
        resetTab(tabExpense);
        resetTab(tabIncome);

        if (currentType == TYPE_EXPENSE) {
            tabExpense.setTextColor(Color.WHITE);
            tabExpense.setBackground(createTabDrawable(Color.parseColor("#D32F2F")));
        } else {
            tabIncome.setTextColor(Color.WHITE);
            tabIncome.setBackground(createTabDrawable(Color.parseColor("#0B8043")));
        }
    }

    private void resetTab(TextView tab) {
        tab.setTextColor(Color.parseColor("#666666"));
        tab.setBackground(null);
    }

    private GradientDrawable createTabDrawable(int color) {
        float density = getResources().getDisplayMetrics().density;
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(17f * density);
        return gd;
    }

    private void clampSelectedDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(selectedYear, selectedMonth, 1);
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (selectedDay > maxDay) {
            selectedDay = maxDay;
        } else if (selectedDay < 1) {
            selectedDay = 1;
        }
    }

    private void adjustDateRange(int delta) {
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
        updateDateRangeDisplay();
    }

    private void updateDateRangeDisplay() {
        String displayStr;
        if (currentTab == TAB_DAY) {
            displayStr = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                    selectedDay, selectedMonth + 1, selectedYear);
        } else if (currentTab == TAB_YEAR) {
            displayStr = String.format(Locale.getDefault(), "Năm %04d", selectedYear);
        } else {
            displayStr = String.format(Locale.getDefault(), "Tháng %02d/%04d", selectedMonth + 1, selectedYear);
        }
        tvDateRangeDisplay.setText(displayStr);
    }

    /**
     * Mở hộp thoại chọn Ngày (DatePickerDialog) hoặc Tháng/Năm (NumberPicker)
     */
    private void setupMonthYearPicker() {
        cardDateRangePicker.setOnClickListener(v -> {
            if (getContext() == null) return;

            if (currentTab == TAB_DAY) {
                new DatePickerDialog(
                        getContext(),
                        (view, year, month, dayOfMonth) -> {
                            selectedYear = year;
                            selectedMonth = month;
                            selectedDay = dayOfMonth;
                            updateDateRangeDisplay();
                            loadStatisticData();
                        },
                        selectedYear,
                        selectedMonth,
                        selectedDay
                ).show();
                return;
            }

            float density = getResources().getDisplayMetrics().density;
            int pad = Math.round(16 * density);

            LinearLayout container = new LinearLayout(getContext());
            container.setOrientation(LinearLayout.HORIZONTAL);
            container.setGravity(Gravity.CENTER);
            container.setPadding(pad, pad, pad, pad / 2);

            // Picker Tháng
            LinearLayout monthCol = new LinearLayout(getContext());
            monthCol.setOrientation(LinearLayout.VERTICAL);
            monthCol.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            monthCol.setLayoutParams(colParams);

            TextView tvMonthLabel = new TextView(getContext());
            tvMonthLabel.setText("Tháng");
            tvMonthLabel.setTextSize(13f);
            tvMonthLabel.setTypeface(null, Typeface.BOLD);
            tvMonthLabel.setTextColor(Color.parseColor("#0B5A4B"));
            monthCol.addView(tvMonthLabel);

            NumberPicker npMonth = new NumberPicker(getContext());
            npMonth.setMinValue(1);
            npMonth.setMaxValue(12);
            npMonth.setValue(selectedMonth + 1);
            String[] monthLabels = new String[12];
            for (int i = 0; i < 12; i++) {
                monthLabels[i] = "T" + (i + 1);
            }
            npMonth.setDisplayedValues(monthLabels);
            npMonth.setWrapSelectorWheel(true);
            monthCol.addView(npMonth);

            // Picker Năm
            LinearLayout yearCol = new LinearLayout(getContext());
            yearCol.setOrientation(LinearLayout.VERTICAL);
            yearCol.setGravity(Gravity.CENTER_HORIZONTAL);
            yearCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvYearLabel = new TextView(getContext());
            tvYearLabel.setText("Năm");
            tvYearLabel.setTextSize(13f);
            tvYearLabel.setTypeface(null, Typeface.BOLD);
            tvYearLabel.setTextColor(Color.parseColor("#0B5A4B"));
            yearCol.addView(tvYearLabel);

            NumberPicker npYear = new NumberPicker(getContext());
            int minYear = Math.min(2020, selectedYear - 5);
            int maxYear = Math.max(2035, selectedYear + 5);
            npYear.setMinValue(minYear);
            npYear.setMaxValue(maxYear);
            npYear.setValue(selectedYear);
            npYear.setWrapSelectorWheel(false);
            yearCol.addView(npYear);

            if (currentTab == TAB_MONTH) {
                container.addView(monthCol);
            }
            container.addView(yearCol);

            String title = (currentTab == TAB_YEAR) ? "Chọn năm" : "Chọn tháng và năm";

            new AlertDialog.Builder(getContext())
                    .setTitle(title)
                    .setView(container)
                    .setPositiveButton("Chọn", (dialog, which) -> {
                        if (currentTab == TAB_MONTH) {
                            selectedMonth = npMonth.getValue() - 1;
                        }
                        selectedYear = npYear.getValue();
                        clampSelectedDay();
                        updateDateRangeDisplay();
                        loadStatisticData();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    private void loadStatisticData() {
        if (budgetDAO == null) return;

        String datePrefix = getCurrentDatePrefix();
        String prevPrefix = getPreviousDatePrefix();
        int userId = getCurrentUserId();

        double totalExpense = budgetDAO.getTotalAmountByDatePrefix(userId, Category.TYPE_EXPENSE, datePrefix);
        double totalIncome = budgetDAO.getTotalAmountByDatePrefix(userId, Category.TYPE_INCOME, datePrefix);
        double netBalance = totalIncome - totalExpense;

        boolean isExpenseMode = (currentType == TYPE_EXPENSE);
        double mainAmount = isExpenseMode ? totalExpense : totalIncome;
        double prevAmount = budgetDAO.getTotalAmountByDatePrefix(
                userId,
                isExpenseMode ? Category.TYPE_EXPENSE : Category.TYPE_INCOME,
                prevPrefix
        );

        // Cập nhật cardOverview gọn, hợp lý theo chế độ Chi tiêu / Thu nhập
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

        if (netBalance > 0) {
            tvNetBalance.setText("+" + CurrencyUtils.formatVND(netBalance));
            tvNetBalance.setTextColor(Color.parseColor("#1E40AF"));
            cardNetBalance.setCardBackgroundColor(Color.parseColor("#EFF6FF"));
            cardNetBalance.setStrokeColor(Color.parseColor("#DBEAFE"));
        } else if (netBalance < 0) {
            tvNetBalance.setText("-" + CurrencyUtils.formatVND(Math.abs(netBalance)));
            tvNetBalance.setTextColor(Color.parseColor("#B91C1C"));
            cardNetBalance.setCardBackgroundColor(Color.parseColor("#FEF2F2"));
            cardNetBalance.setStrokeColor(Color.parseColor("#FECACA"));
        } else {
            tvNetBalance.setText("0 đ");
            tvNetBalance.setTextColor(Color.parseColor("#1E40AF"));
            cardNetBalance.setCardBackgroundColor(Color.parseColor("#EFF6FF"));
            cardNetBalance.setStrokeColor(Color.parseColor("#DBEAFE"));
        }

        updateComparisonBadge(mainAmount, prevAmount, isExpenseMode);

        // Truy vấn danh sách chi tiết danh mục theo loại đang chọn (EXPENSE / INCOME)
        String queryType = isExpenseMode ? Category.TYPE_EXPENSE : Category.TYPE_INCOME;
        List<BudgetDAO.CategorySummary> summaries = budgetDAO.getCategorySummaryByDatePrefix(userId, queryType, datePrefix);

        allCategoryItems = new ArrayList<>();
        for (BudgetDAO.CategorySummary summary : summaries) {
            int percentage = mainAmount > 0 ? (int) Math.round((summary.totalAmount * 100.0) / mainAmount) : 0;
            int color = parseColorSafe(summary.colorCode, isExpenseMode ? "#D32F2F" : "#0B8043");
            int iconBg = adjustAlpha(color, 0.16f);
            String emoji = mapIconEmoji(summary.iconName);

            allCategoryItems.add(new CategoryDetailItem(
                    summary.categoryId,
                    summary.name,
                    summary.totalAmount,
                    percentage,
                    summary.transactionCount,
                    summary.transactionCount + " giao dịch",
                    emoji,
                    color,
                    iconBg
            ));
        }

        String currentQuery = svTransactionSearch != null ? svTransactionSearch.getQuery().toString() : "";
        filterCategoryList(currentQuery);
    }

    private void updateComparisonBadge(double currentAmount, double prevAmount, boolean isExpenseMode) {
        if (prevAmount <= 0) {
            tvComparisonBadge.setVisibility(View.GONE);
            return;
        }

        long diffPct = Math.round(((currentAmount - prevAmount) / prevAmount) * 100.0);
        tvComparisonBadge.setVisibility(View.VISIBLE);

        String arrow = diffPct >= 0 ? "↑ +" : "↓ ";
        tvComparisonBadge.setText(arrow + diffPct + "% vs kỳ trước");

        boolean isGood = isExpenseMode ? (diffPct <= 0) : (diffPct >= 0);
        if (isGood) {
            tvComparisonBadge.setTextColor(Color.parseColor("#047857"));
            tvComparisonBadge.setBackground(createTabDrawable(Color.parseColor("#D1FAE5")));
        } else {
            tvComparisonBadge.setTextColor(Color.parseColor("#B91C1C"));
            tvComparisonBadge.setBackground(createTabDrawable(Color.parseColor("#FEE2E2")));
        }
    }

    private String getCurrentDatePrefix() {
        if (currentTab == TAB_DAY) {
            return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    selectedYear, selectedMonth + 1, selectedDay);
        }
        if (currentTab == TAB_YEAR) {
            return String.format(Locale.getDefault(), "%04d", selectedYear);
        }
        return String.format(Locale.getDefault(), "%04d-%02d", selectedYear, selectedMonth + 1);
    }

    private String getPreviousDatePrefix() {
        if (currentTab == TAB_DAY) {
            Calendar cal = Calendar.getInstance();
            cal.set(selectedYear, selectedMonth, selectedDay);
            cal.add(Calendar.DAY_OF_MONTH, -1);
            return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        }
        if (currentTab == TAB_YEAR) {
            return String.format(Locale.getDefault(), "%04d", selectedYear - 1);
        }
        int prevMonth = selectedMonth - 1;
        int prevYear = selectedYear;
        if (prevMonth < 0) {
            prevMonth = 11;
            prevYear--;
        }
        return String.format(Locale.getDefault(), "%04d-%02d", prevYear, prevMonth + 1);
    }

    private int parseColorSafe(String colorCode, String fallbackHex) {
        if (colorCode != null && !colorCode.trim().isEmpty()) {
            try {
                return Color.parseColor(colorCode.trim());
            } catch (Exception ignored) {
            }
        }
        return Color.parseColor(fallbackHex);
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(255 * factor);
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private String mapIconEmoji(String iconName) {
        if (iconName == null) return "⭐";
        switch (iconName.toLowerCase(Locale.getDefault())) {
            case "ic_food": return "🍔";
            case "ic_coffee": return "☕";
            case "ic_school": return "📚";
            case "ic_shopping": return "🛒";
            case "ic_transport": return "🚗";
            case "ic_home": return "🏠";
            case "ic_bill": return "⚡";
            case "ic_entertainment": return "🎮";
            case "ic_health": return "💊";
            case "ic_beauty": return "💄";
            case "ic_travel": return "✈️";
            case "ic_pet": return "🐾";
            case "ic_sport": return "🏋️";
            case "ic_phone": return "📱";
            case "ic_gift": return "🎁";
            case "ic_award": return "🎓";
            case "ic_work": return "💼";
            case "ic_savings": return "💰";
            case "ic_invest": return "📈";
            case "ic_other": return "⭐";
            default: return "⭐";
        }
    }

    private void openCategoryTransactionsSheet(CategoryDetailItem item) {
        if (getContext() == null || budgetDAO == null) return;
        if (activeCategorySheet != null && activeCategorySheet.isShowing()) {
            activeCategorySheet.dismiss();
        }

        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_category_transactions, null);
        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        dialog.setContentView(sheetView);
        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundColor(Color.TRANSPARENT);
            }
        });
        dialog.setOnDismissListener(d -> {
            activeCategorySheet = null;
            activeSheetView = null;
            activeSheetItem = null;
        });

        activeCategorySheet = dialog;
        activeSheetView = sheetView;
        activeSheetItem = item;
        bindCategoryTransactionsSheet(sheetView, item);
        dialog.show();
    }

    private void bindCategoryTransactionsSheet(View sheetView, CategoryDetailItem item) {
        if (getContext() == null || budgetDAO == null) return;

        TextView tvCatIcon = sheetView.findViewById(R.id.tvSheetCatIcon);
        TextView tvCatName = sheetView.findViewById(R.id.tvSheetCatName);
        TextView tvCatSub = sheetView.findViewById(R.id.tvSheetCatSub);
        TextView tvCatTotal = sheetView.findViewById(R.id.tvSheetCatTotal);
        LinearLayout layoutList = sheetView.findViewById(R.id.layoutSheetTransactions);
        TextView tvEmpty = sheetView.findViewById(R.id.tvSheetEmpty);

        float d = getResources().getDisplayMetrics().density;
        GradientDrawable catCircle = new GradientDrawable();
        catCircle.setShape(GradientDrawable.OVAL);
        catCircle.setColor(item.iconBgColor);
        catCircle.setStroke(Math.round(2 * d), item.color);
        tvCatIcon.setBackground(catCircle);
        tvCatIcon.setText(item.iconEmoji);
        tvCatName.setText(item.name);

        boolean isExpenseMode = (currentType == TYPE_EXPENSE);
        List<Transaction> transactions = budgetDAO.getTransactionsByCategoryAndDatePrefix(
                getCurrentUserId(), item.categoryId, getCurrentDatePrefix());

        double total = 0;
        for (Transaction tx : transactions) {
            total += tx.getAmount();
        }

        String periodText = tvDateRangeDisplay.getText().toString();
        tvCatSub.setText(periodText + " • " + transactions.size() + " giao dịch");
        String prefix = isExpenseMode ? "-" : "+";
        tvCatTotal.setText(transactions.isEmpty() ? "0 đ" : prefix + CurrencyUtils.formatVND(total));
        tvCatTotal.setTextColor(Color.parseColor(isExpenseMode ? "#B91C1C" : "#047857"));
        tvCatTotal.setBackgroundResource(isExpenseMode ? R.drawable.bg_tx_amount_expense : R.drawable.bg_tx_amount_income);

        layoutList.removeAllViews();
        if (transactions.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvEmpty.setVisibility(View.GONE);

        // Nhóm giao dịch theo từng ngày (đã sort mới nhất lên đầu từ SQL)
        Map<String, List<Transaction>> groupedByDate = new LinkedHashMap<>();
        for (Transaction tx : transactions) {
            String dateKey = !TextUtils.isEmpty(tx.getTransactionDate()) ? tx.getTransactionDate() : "";
            List<Transaction> dayList = groupedByDate.get(dateKey);
            if (dayList == null) {
                dayList = new ArrayList<>();
                groupedByDate.put(dateKey, dayList);
            }
            dayList.add(tx);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (Map.Entry<String, List<Transaction>> entry : groupedByDate.entrySet()) {
            List<Transaction> dayList = entry.getValue();
            double dayTotal = 0;
            for (Transaction tx : dayList) {
                dayTotal += tx.getAmount();
            }

            View headerView = inflater.inflate(R.layout.item_tx_date_header, layoutList, false);
            TextView tvDateLabel = headerView.findViewById(R.id.tvDateGroupLabel);
            TextView tvDateTotal = headerView.findViewById(R.id.tvDateGroupTotal);
            tvDateLabel.setText("Ngày " + DateUtils.formatToDisplay(entry.getKey()));
            tvDateTotal.setText(prefix + CurrencyUtils.formatVND(dayTotal));
            tvDateTotal.setTextColor(Color.parseColor(isExpenseMode ? "#B91C1C" : "#047857"));
            layoutList.addView(headerView);

            for (Transaction tx : dayList) {
                boolean txIncome = Category.TYPE_INCOME.equalsIgnoreCase(tx.getCategoryType());
                View row = inflater.inflate(R.layout.item_recent_transaction, layoutList, false);
                if (row instanceof MaterialCardView) {
                    ((MaterialCardView) row).setCardBackgroundColor(Color.WHITE);
                }

                TextView tvIcon = row.findViewById(R.id.tvTxIcon);
                TextView tvTitle = row.findViewById(R.id.tvTxTitle);
                TextView tvSub = row.findViewById(R.id.tvTxSub);
                TextView tvAmount = row.findViewById(R.id.tvTxAmount);

                int borderColor = parseColorSafe(tx.getCategoryColorCode(), txIncome ? "#0A5C36" : "#C62828");
                GradientDrawable circle = new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);
                circle.setColor(adjustAlpha(borderColor, 0.14f));
                circle.setStroke(Math.round(2 * d), borderColor);
                tvIcon.setBackground(circle);
                tvIcon.setText(mapIconEmoji(tx.getCategoryIconName()));

                String displayTitle = !TextUtils.isEmpty(tx.getNote())
                        ? tx.getNote()
                        : (!TextUtils.isEmpty(tx.getTitle()) ? tx.getTitle() : item.name);
                tvTitle.setText(displayTitle);
                tvSub.setText("Chạm để chỉnh sửa • " + DateUtils.formatToDisplay(tx.getTransactionDate()));
                tvSub.setTextColor(Color.parseColor("#0A6847"));

                tvAmount.setText((txIncome ? "+" : "-") + CurrencyUtils.formatVND(tx.getAmount()));
                tvAmount.setTextColor(Color.parseColor(txIncome ? "#047857" : "#B91C1C"));
                tvAmount.setBackgroundResource(txIncome ? R.drawable.bg_tx_amount_income : R.drawable.bg_tx_amount_expense);

                row.setOnClickListener(v -> {
                    if (getActivity() != null) {
                        Intent intent = new Intent(getActivity(), AddEditTransactionActivity.class);
                        intent.putExtra("transaction_id", tx.getTransactionId());
                        startActivity(intent);
                    }
                });

                layoutList.addView(row);
            }
        }
    }

    // =========================================================================
    // MODEL & ADAPTER HIỂN THỊ DANH MỤC CHI TIẾT
    // =========================================================================

    private interface OnCategoryClickListener {
        void onCategoryClick(CategoryDetailItem item);
    }

    private static class CategoryDetailItem {
        final int categoryId;
        final String name;
        final double amount;
        final int percentage;
        final int txCount;
        final String transactionCount;
        final String iconEmoji;
        final int color;
        final int iconBgColor;

        CategoryDetailItem(int categoryId, String name, double amount, int percentage, int txCount,
                           String transactionCount, String iconEmoji, int color, int iconBgColor) {
            this.categoryId = categoryId;
            this.name = name;
            this.amount = amount;
            this.percentage = percentage;
            this.txCount = txCount;
            this.transactionCount = transactionCount;
            this.iconEmoji = iconEmoji;
            this.color = color;
            this.iconBgColor = iconBgColor;
        }
    }

    private static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private final List<CategoryDetailItem> list;
        private final boolean isExpense;
        private final OnCategoryClickListener clickListener;

        public CategoryAdapter(List<CategoryDetailItem> list, boolean isExpense, OnCategoryClickListener clickListener) {
            this.list = list;
            this.isExpense = isExpense;
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_statistic_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CategoryDetailItem item = list.get(position);
            float density = holder.tvIcon.getResources().getDisplayMetrics().density;

            holder.tvIcon.setText(item.iconEmoji);
            GradientDrawable iconBg = new GradientDrawable();
            iconBg.setShape(GradientDrawable.OVAL);
            iconBg.setColor(item.iconBgColor);
            iconBg.setStroke(Math.round(2 * density), item.color);
            holder.tvIcon.setBackground(iconBg);

            holder.tvTitle.setText(item.name);
            holder.tvSub.setText(item.transactionCount);

            String prefix = isExpense ? "-" : "+";
            holder.tvAmount.setText(prefix + CurrencyUtils.formatVND(item.amount));
            holder.tvAmount.setTextColor(Color.parseColor(isExpense ? "#B91C1C" : "#047857"));

            holder.tvPercent.setText(item.percentage + "%");
            holder.tvPercent.setTextColor(item.color);
            GradientDrawable pctPill = new GradientDrawable();
            pctPill.setCornerRadius(8f * density);
            pctPill.setColor(item.iconBgColor);
            holder.tvPercent.setBackground(pctPill);

            holder.pb.setProgress(item.percentage);
            holder.pb.setProgressTintList(ColorStateList.valueOf(item.color));

            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onCategoryClick(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
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
