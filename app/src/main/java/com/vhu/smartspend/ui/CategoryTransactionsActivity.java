package com.vhu.smartspend.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.card.MaterialCardView;
import com.vhu.smartspend.R;
import com.vhu.smartspend.database.BudgetDAO;
import com.vhu.smartspend.model.Category;
import com.vhu.smartspend.model.Transaction;
import com.vhu.smartspend.utils.CurrencyUtils;
import com.vhu.smartspend.utils.DateUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Màn hình hiển thị danh sách giao dịch của một danh mục theo từng ngày,
 * cho phép nhấn trực tiếp vào giao dịch để chuyển sang màn hình chỉnh sửa.
 */
public class CategoryTransactionsActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "category_id";
    public static final String EXTRA_CATEGORY_NAME = "category_name";
    public static final String EXTRA_CATEGORY_ICON_EMOJI = "category_icon_emoji";
    public static final String EXTRA_CATEGORY_COLOR = "category_color";
    public static final String EXTRA_CATEGORY_BG_COLOR = "category_bg_color";
    public static final String EXTRA_DATE_PREFIX = "date_prefix";
    public static final String EXTRA_PERIOD_DISPLAY = "period_display";
    public static final String EXTRA_IS_EXPENSE = "is_expense";

    private BudgetDAO budgetDAO;

    private int categoryId = -1;
    private String categoryName = "";
    private String categoryIconEmoji = "⭐";
    private int categoryColor;
    private int categoryBgColor;
    private String datePrefix = "";
    private String periodDisplay = "";
    private boolean isExpenseMode = true;

    private TextView tvCatIcon, tvCatName, tvCatSub, tvCatTotal, tvEmpty;
    private View scrollView;
    private LinearLayout layoutTransactions;

    private int getCurrentUserId() {
        int id = getSharedPreferences("SmartSpendPrefs", MODE_PRIVATE).getInt("user_id", 1);
        return id > 0 ? id : 1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_category_transactions);

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        budgetDAO = new BudgetDAO(this);

        Intent intent = getIntent();
        categoryId = intent.getIntExtra(EXTRA_CATEGORY_ID, -1);
        categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME);
        if (TextUtils.isEmpty(categoryName)) categoryName = "Danh mục";
        String emojiExtra = intent.getStringExtra(EXTRA_CATEGORY_ICON_EMOJI);
        if (!TextUtils.isEmpty(emojiExtra)) categoryIconEmoji = emojiExtra;
        isExpenseMode = intent.getBooleanExtra(EXTRA_IS_EXPENSE, true);
        int defaultColor = Color.parseColor(isExpenseMode ? "#D32F2F" : "#0B8043");
        categoryColor = intent.getIntExtra(EXTRA_CATEGORY_COLOR, defaultColor);
        categoryBgColor = intent.getIntExtra(EXTRA_CATEGORY_BG_COLOR, adjustAlpha(categoryColor, 0.16f));
        datePrefix = intent.getStringExtra(EXTRA_DATE_PREFIX);
        if (datePrefix == null) datePrefix = "";
        periodDisplay = intent.getStringExtra(EXTRA_PERIOD_DISPLAY);
        if (periodDisplay == null) periodDisplay = "";

        findViewById(R.id.btnBackCategoryTx).setOnClickListener(v -> finish());
        tvCatIcon = findViewById(R.id.tvCatTxIcon);
        tvCatName = findViewById(R.id.tvCatTxName);
        tvCatSub = findViewById(R.id.tvCatTxSub);
        tvCatTotal = findViewById(R.id.tvCatTxTotal);
        tvEmpty = findViewById(R.id.tvCategoryTxEmpty);
        scrollView = findViewById(R.id.scrollCategoryTx);
        layoutTransactions = findViewById(R.id.layoutCategoryTransactions);

        bindHeaderStatic();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTransactions();
    }

    private void bindHeaderStatic() {
        float d = getResources().getDisplayMetrics().density;
        GradientDrawable catCircle = new GradientDrawable();
        catCircle.setShape(GradientDrawable.OVAL);
        catCircle.setColor(categoryBgColor);
        catCircle.setStroke(Math.round(2 * d), categoryColor);
        tvCatIcon.setBackground(catCircle);
        tvCatIcon.setText(categoryIconEmoji);
        tvCatName.setText(categoryName);
    }

    private void loadTransactions() {
        if (categoryId <= 0) {
            finish();
            return;
        }

        List<Transaction> transactions = budgetDAO.getTransactionsByCategoryAndDatePrefix(
                getCurrentUserId(), categoryId, datePrefix);

        double total = 0;
        for (Transaction tx : transactions) {
            total += tx.getAmount();
        }

        String subPrefix = !TextUtils.isEmpty(periodDisplay) ? periodDisplay + " • " : "";
        tvCatSub.setText(subPrefix + transactions.size() + " giao dịch");

        String signPrefix = isExpenseMode ? "-" : "+";
        tvCatTotal.setText(transactions.isEmpty() ? "0 đ" : signPrefix + CurrencyUtils.formatVND(total));
        tvCatTotal.setTextColor(Color.parseColor(isExpenseMode ? "#B91C1C" : "#047857"));
        tvCatTotal.setBackgroundResource(isExpenseMode ? R.drawable.bg_tx_amount_expense : R.drawable.bg_tx_amount_income);

        layoutTransactions.removeAllViews();
        if (transactions.isEmpty()) {
            scrollView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        scrollView.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        // Nhóm giao dịch theo từng ngày (SQL đã sắp xếp mới nhất lên đầu)
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

        float d = getResources().getDisplayMetrics().density;
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Map.Entry<String, List<Transaction>> entry : groupedByDate.entrySet()) {
            List<Transaction> dayList = entry.getValue();
            double dayTotal = 0;
            for (Transaction tx : dayList) {
                dayTotal += tx.getAmount();
            }

            View headerView = inflater.inflate(R.layout.item_tx_date_header, layoutTransactions, false);
            TextView tvDateLabel = headerView.findViewById(R.id.tvDateGroupLabel);
            TextView tvDateTotal = headerView.findViewById(R.id.tvDateGroupTotal);
            tvDateLabel.setText("Ngày " + DateUtils.formatToDisplay(entry.getKey()));
            tvDateTotal.setText(signPrefix + CurrencyUtils.formatVND(dayTotal));
            tvDateTotal.setTextColor(Color.parseColor(isExpenseMode ? "#B91C1C" : "#047857"));
            layoutTransactions.addView(headerView);

            for (Transaction tx : dayList) {
                boolean txIncome = Category.TYPE_INCOME.equalsIgnoreCase(tx.getCategoryType());
                View row = inflater.inflate(R.layout.item_recent_transaction, layoutTransactions, false);
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
                        : (!TextUtils.isEmpty(tx.getTitle()) ? tx.getTitle() : categoryName);
                tvTitle.setText(displayTitle);
                tvSub.setText("Chạm để chỉnh sửa • " + DateUtils.formatToDisplay(tx.getTransactionDate()));
                tvSub.setTextColor(Color.parseColor("#0A6847"));

                tvAmount.setText((txIncome ? "+" : "-") + CurrencyUtils.formatVND(tx.getAmount()));
                tvAmount.setTextColor(Color.parseColor(txIncome ? "#047857" : "#B91C1C"));
                tvAmount.setBackgroundResource(txIncome ? R.drawable.bg_tx_amount_income : R.drawable.bg_tx_amount_expense);

                row.setOnClickListener(v -> {
                    Intent editIntent = new Intent(CategoryTransactionsActivity.this, AddEditTransactionActivity.class);
                    editIntent.putExtra("transaction_id", tx.getTransactionId());
                    startActivity(editIntent);
                });

                layoutTransactions.addView(row);
            }
        }
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
        if (iconName == null) return categoryIconEmoji;
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
}
