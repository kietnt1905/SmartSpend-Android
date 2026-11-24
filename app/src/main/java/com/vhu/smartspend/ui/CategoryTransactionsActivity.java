package com.vhu.smartspend.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.vhu.smartspend.R;
import com.vhu.smartspend.adapter.CategoryTransactionAdapter;
import com.vhu.smartspend.adapter.CategoryTransactionAdapter.RowItem;
import com.vhu.smartspend.database.BudgetDAO;
import com.vhu.smartspend.model.Transaction;
import com.vhu.smartspend.utils.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình hiển thị danh sách giao dịch của một danh mục theo ngày (dùng ListView + BaseAdapter).
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
    private String categoryName = "Danh mục";
    private String categoryIconEmoji = "⭐";
    private int categoryColor;
    private int categoryBgColor;
    private String datePrefix = "";
    private String periodDisplay = "";
    private boolean isExpenseMode = true;

    private TextView tvCatIcon, tvCatName, tvCatSub, tvCatTotal, tvEmpty;
    private ListView lvTransactions;
    private final List<RowItem> rowList = new ArrayList<>();
    private CategoryTransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_transactions);

        budgetDAO = new BudgetDAO(this);

        // Nhận dữ liệu từ StatisticFragment truyền sang bằng Intent
        Intent intent = getIntent();
        categoryId = intent.getIntExtra(EXTRA_CATEGORY_ID, -1);
        String nameExtra = intent.getStringExtra(EXTRA_CATEGORY_NAME);
        if (nameExtra != null && !nameExtra.isEmpty()) {
            categoryName = nameExtra;
        }
        String emojiExtra = intent.getStringExtra(EXTRA_CATEGORY_ICON_EMOJI);
        if (emojiExtra != null && !emojiExtra.isEmpty()) {
            categoryIconEmoji = emojiExtra;
        }
        isExpenseMode = intent.getBooleanExtra(EXTRA_IS_EXPENSE, true);
        int defaultColor = Color.parseColor(isExpenseMode ? "#D32F2F" : "#0B8043");
        categoryColor = intent.getIntExtra(EXTRA_CATEGORY_COLOR, defaultColor);
        categoryBgColor = intent.getIntExtra(EXTRA_CATEGORY_BG_COLOR,
                Color.argb(40, Color.red(categoryColor), Color.green(categoryColor), Color.blue(categoryColor)));
        String prefixExtra = intent.getStringExtra(EXTRA_DATE_PREFIX);
        if (prefixExtra != null) datePrefix = prefixExtra;
        String periodExtra = intent.getStringExtra(EXTRA_PERIOD_DISPLAY);
        if (periodExtra != null) periodDisplay = periodExtra;

        ImageButton btnBack = findViewById(R.id.btnBackCategoryTx);
        tvCatIcon = findViewById(R.id.tvCatTxIcon);
        tvCatName = findViewById(R.id.tvCatTxName);
        tvCatSub = findViewById(R.id.tvCatTxSub);
        tvCatTotal = findViewById(R.id.tvCatTxTotal);
        tvEmpty = findViewById(R.id.tvCategoryTxEmpty);
        lvTransactions = findViewById(R.id.lvCategoryTransactions);

        btnBack.setOnClickListener(v -> finish());

        // Hiển thị thông tin danh mục trên header
        float d = getResources().getDisplayMetrics().density;
        GradientDrawable catCircle = new GradientDrawable();
        catCircle.setShape(GradientDrawable.OVAL);
        catCircle.setColor(categoryBgColor);
        catCircle.setStroke(Math.round(2 * d), categoryColor);
        tvCatIcon.setBackground(catCircle);
        tvCatIcon.setText(categoryIconEmoji);
        tvCatName.setText(categoryName);

        adapter = new CategoryTransactionAdapter(this, rowList, categoryName, categoryIconEmoji, isExpenseMode);
        lvTransactions.setAdapter(adapter);

        // Nhấn vào giao dịch để chuyển sang màn hình sửa giao dịch
        lvTransactions.setOnItemClickListener((parent, view, position, id) -> {
            RowItem clicked = rowList.get(position);
            if (!clicked.isHeader && clicked.transaction != null) {
                Intent editIntent = new Intent(CategoryTransactionsActivity.this, AddEditTransactionActivity.class);
                editIntent.putExtra("transaction_id", clicked.transaction.getTransactionId());
                startActivity(editIntent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTransactions();
    }

    private void loadTransactions() {
        if (categoryId <= 0) {
            finish();
            return;
        }

        int userId = getSharedPreferences("SmartSpendPrefs", MODE_PRIVATE).getInt("user_id", 1);
        if (userId <= 0) userId = 1;

        List<Transaction> transactions = budgetDAO.getTransactionsByCategoryAndDatePrefix(
                userId, categoryId, datePrefix);

        double total = 0;
        for (int i = 0; i < transactions.size(); i++) {
            total += transactions.get(i).getAmount();
        }

        String subPrefix = periodDisplay.isEmpty() ? "" : periodDisplay + " • ";
        tvCatSub.setText(subPrefix + transactions.size() + " giao dịch");

        String signPrefix = isExpenseMode ? "-" : "+";
        tvCatTotal.setText(transactions.isEmpty() ? "0 đ" : signPrefix + CurrencyUtils.formatVND(total));
        tvCatTotal.setTextColor(Color.parseColor(isExpenseMode ? "#B91C1C" : "#047857"));
        tvCatTotal.setBackgroundResource(isExpenseMode ? R.drawable.bg_tx_amount_expense : R.drawable.bg_tx_amount_income);

        rowList.clear();
        if (transactions.isEmpty()) {
            lvTransactions.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
            return;
        }

        lvTransactions.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        // Gom các giao dịch cùng ngày lại (SQL đã sắp xếp mới nhất lên đầu)
        int i = 0;
        while (i < transactions.size()) {
            String dateKey = transactions.get(i).getTransactionDate();
            if (dateKey == null) dateKey = "";

            double dayTotal = 0;
            int j = i;
            while (j < transactions.size()) {
                String nextDate = transactions.get(j).getTransactionDate();
                if (nextDate == null) nextDate = "";
                if (!nextDate.equals(dateKey)) break;
                dayTotal += transactions.get(j).getAmount();
                j++;
            }

            rowList.add(new RowItem(dateKey, dayTotal));
            for (int k = i; k < j; k++) {
                rowList.add(new RowItem(transactions.get(k)));
            }
            i = j;
        }

        adapter.notifyDataSetChanged();
    }
}
