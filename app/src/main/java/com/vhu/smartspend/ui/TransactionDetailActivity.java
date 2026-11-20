package com.vhu.smartspend.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.vhu.smartspend.R;
import com.vhu.smartspend.database.TransactionDAO;
import com.vhu.smartspend.model.Category;
import com.vhu.smartspend.model.Transaction;
import com.vhu.smartspend.utils.CurrencyUtils;
import com.vhu.smartspend.utils.DateUtils;

import java.util.Locale;

public class TransactionDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TRANSACTION_ID = AddEditTransactionActivity.EXTRA_TRANSACTION_ID;

    private TransactionDAO dao;
    private int transactionId = -1;

    private ImageView imgReceipt;
    private TextView tvIcon, tvCategoryName, tvType, tvAmount, tvDate, tvNote;
    private View cardReceipt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_transaction_detail);

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        dao = new TransactionDAO(this);
        transactionId = getIntent().getIntExtra(EXTRA_TRANSACTION_ID, -1);

        tvIcon = findViewById(R.id.tvDetailCategoryIcon);
        tvCategoryName = findViewById(R.id.tvDetailCategoryName);
        tvType = findViewById(R.id.tvDetailType);
        tvAmount = findViewById(R.id.tvDetailAmount);
        tvDate = findViewById(R.id.tvDetailDate);
        tvNote = findViewById(R.id.tvDetailNote);
        cardReceipt = findViewById(R.id.cardDetailReceipt);
        imgReceipt = findViewById(R.id.imgDetailReceipt);

        findViewById(R.id.btnCloseDetail).setOnClickListener(v -> finish());
        findViewById(R.id.btnDeleteTransaction).setOnClickListener(v -> confirmDelete());
        findViewById(R.id.btnEditTransaction).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditTransactionActivity.class);
            intent.putExtra(AddEditTransactionActivity.EXTRA_TRANSACTION_ID, transactionId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTransaction();
    }

    private void loadTransaction() {
        Transaction tx = dao.getTransactionById(transactionId);
        if (tx == null) {
            Toast.makeText(this, "Không tìm thấy giao dịch", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        boolean isIncome = Category.TYPE_INCOME.equalsIgnoreCase(tx.getCategoryType());
        int borderColor = parseCategoryColor(tx.getCategoryColorCode(), isIncome ? "#0A5C36" : "#B91C1C");
        float d = getResources().getDisplayMetrics().density;

        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.argb(30, Color.red(borderColor), Color.green(borderColor), Color.blue(borderColor)));
        circle.setStroke(Math.round(3 * d), borderColor);
        tvIcon.setBackground(circle);
        tvIcon.setText(getCategoryEmoji(tx.getCategoryIconName()));

        tvCategoryName.setText(!TextUtils.isEmpty(tx.getCategoryName()) ? tx.getCategoryName() : tx.getTitle());

        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(12 * getResources().getDisplayMetrics().density);
        badge.setColor(Color.parseColor(isIncome ? "#D1FAE5" : "#FFE4DE"));
        tvType.setBackground(badge);
        tvType.setText(isIncome ? "↑ Khoản thu" : "↓ Khoản chi");
        tvType.setTextColor(Color.parseColor(isIncome ? "#0A5C36" : "#B91C1C"));

        tvAmount.setText((isIncome ? "+ " : "- ") + CurrencyUtils.formatVND(tx.getAmount()));
        tvAmount.setTextColor(Color.parseColor(isIncome ? "#0A5C36" : "#B91C1C"));

        tvDate.setText(DateUtils.formatToDisplay(tx.getTransactionDate()));
        String note = !TextUtils.isEmpty(tx.getNote()) ? tx.getNote() : tx.getTitle();
        tvNote.setText(!TextUtils.isEmpty(note) ? note : "Không có ghi chú");

        if (!TextUtils.isEmpty(tx.getReceiptUri())) {
            cardReceipt.setVisibility(View.VISIBLE);
            imgReceipt.setImageURI(Uri.parse(tx.getReceiptUri()));
        } else {
            cardReceipt.setVisibility(View.GONE);
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa giao dịch")
                .setMessage("Bạn có chắc chắn muốn xóa giao dịch này không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (d, w) -> {
                    if (dao.deleteTransaction(transactionId) > 0) {
                        Toast.makeText(this, "Đã xóa giao dịch!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, "Xóa thất bại!", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private int parseCategoryColor(String colorCode, String fallbackHex) {
        if (colorCode != null && !colorCode.trim().isEmpty()) {
            try {
                return Color.parseColor(colorCode.trim());
            } catch (Exception ignored) {
            }
        }
        return Color.parseColor(fallbackHex);
    }

    private String getCategoryEmoji(String iconName) {
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
}
