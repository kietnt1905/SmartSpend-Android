package com.vhu.smartspend.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.vhu.smartspend.R;
import com.vhu.smartspend.database.TransactionDAO;
import com.vhu.smartspend.model.Category;
import com.vhu.smartspend.model.Transaction;
import com.vhu.smartspend.utils.CategoryUiHelper;
import com.vhu.smartspend.utils.CurrencyUtils;
import com.vhu.smartspend.utils.DateUtils;

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
        setContentView(R.layout.activity_transaction_detail);

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
        int borderColor = CategoryUiHelper.parseColor(tx.getCategoryColorCode(), isIncome ? "#0A5C36" : "#B91C1C");

        tvIcon.setBackground(CategoryUiHelper.createCircleBg(this, borderColor, 3));
        tvIcon.setText(CategoryUiHelper.getEmoji(tx.getCategoryIconName()));
        tvCategoryName.setText(!TextUtils.isEmpty(tx.getCategoryName()) ? tx.getCategoryName() : tx.getTitle());

        int badgeBg = Color.parseColor(isIncome ? "#D1FAE5" : "#FFE4DE");
        int textColor = Color.parseColor(isIncome ? "#0A5C36" : "#B91C1C");
        tvType.setBackground(CategoryUiHelper.createRoundedBg(this, badgeBg, 12));
        tvType.setText(isIncome ? "↑ Khoản thu" : "↓ Khoản chi");
        tvType.setTextColor(textColor);

        tvAmount.setText((isIncome ? "+ " : "- ") + CurrencyUtils.formatVND(tx.getAmount()));
        tvAmount.setTextColor(textColor);

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
}
