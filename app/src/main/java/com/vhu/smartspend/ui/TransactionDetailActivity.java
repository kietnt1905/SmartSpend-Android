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

    private ImageView imgIcon, imgReceipt;
    private TextView tvCategoryName, tvType, tvAmount, tvDate, tvNote;
    private View cardReceipt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_transaction_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        dao = new TransactionDAO(this);
        transactionId = getIntent().getIntExtra(EXTRA_TRANSACTION_ID, -1);

        imgIcon = findViewById(R.id.imgDetailCategoryIcon);
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
        if (tx == null && transactionId == -1) {
            int userId = getSharedPreferences("SmartSpendPrefs", MODE_PRIVATE).getInt("user_id", 1);
            java.util.List<Transaction> list = dao.getTransactionsByUser(userId > 0 ? userId : 1);
            if (!list.isEmpty()) {
                tx = list.get(0);
                transactionId = tx.getTransactionId();
            }
        }
        if (tx == null) {
            Toast.makeText(this, "Không tìm thấy giao dịch", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        boolean isIncome = Category.TYPE_INCOME.equalsIgnoreCase(tx.getCategoryType());

        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.parseColor(isIncome ? "#D1FAE5" : "#FFE4DE"));
        imgIcon.setBackground(circle);
        imgIcon.setImageResource(getCategoryIcon(tx.getCategoryIconName()));
        imgIcon.setColorFilter(Color.parseColor(isIncome ? "#0A5C36" : "#B91C1C"));

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

    private int getCategoryIcon(String iconName) {
        if (iconName == null) return R.drawable.ic_tx_category_default;
        switch (iconName.toLowerCase(Locale.getDefault())) {
            case "ic_food": return R.drawable.ic_tx_food;
            case "ic_school": return R.drawable.ic_tx_school;
            case "ic_shopping": return R.drawable.ic_tx_shopping;
            case "ic_transport": return R.drawable.ic_tx_transport;
            case "ic_award": return R.drawable.ic_tx_award;
            case "ic_work": return R.drawable.ic_tx_work;
            default: return R.drawable.ic_tx_category_default;
        }
    }
}
