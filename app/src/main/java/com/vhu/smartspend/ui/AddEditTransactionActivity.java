package com.vhu.smartspend.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.vhu.smartspend.R;
import com.vhu.smartspend.database.TransactionDAO;
import com.vhu.smartspend.model.Category;
import com.vhu.smartspend.model.Transaction;
import com.vhu.smartspend.utils.CategoryUiHelper;
import com.vhu.smartspend.utils.DateUtils;
import com.vhu.smartspend.utils.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AddEditTransactionActivity extends AppCompatActivity {

    public static final String EXTRA_TRANSACTION_ID = "transaction_id";

    private TextView tvScreenTitle, tabExpense, tabIncome, tvAmountLabel, tvAmountSign, tvTransactionDate, tvSaveButtonText;
    private EditText edtAmount, edtNote;
    private LinearLayout layoutCategoryGrid;
    private View layoutReceiptPlaceholder, layoutReceiptPreview;
    private ImageView imgReceiptPreview;

    private TransactionDAO dao;
    private SessionManager sessionManager;
    private List<Category> categories = new ArrayList<>();
    private String currentType = Category.TYPE_EXPENSE;
    private int selectedCategoryId = -1;
    private int editingId = -1;
    private String receiptUri;
    private final Calendar calendar = Calendar.getInstance();

    private final ActivityResultLauncher<String[]> pickReceipt =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                }
                receiptUri = uri.toString();
                showReceipt(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_transaction);

        dao = new TransactionDAO(this);
        sessionManager = new SessionManager(this);

        tvScreenTitle = findViewById(R.id.tvScreenTitle);
        tabExpense = findViewById(R.id.tabExpense);
        tabIncome = findViewById(R.id.tabIncome);
        tvAmountLabel = findViewById(R.id.tvAmountLabel);
        tvAmountSign = findViewById(R.id.tvAmountSign);
        edtAmount = findViewById(R.id.edtAmount);
        layoutCategoryGrid = findViewById(R.id.layoutCategoryGrid);
        tvTransactionDate = findViewById(R.id.tvTransactionDate);
        edtNote = findViewById(R.id.edtNote);
        layoutReceiptPlaceholder = findViewById(R.id.layoutReceiptPlaceholder);
        layoutReceiptPreview = findViewById(R.id.layoutReceiptPreview);
        imgReceiptPreview = findViewById(R.id.imgReceiptPreview);
        tvSaveButtonText = findViewById(R.id.tvSaveButtonText);

        findViewById(R.id.btnCloseTransaction).setOnClickListener(v -> finish());
        tabExpense.setOnClickListener(v -> switchType(Category.TYPE_EXPENSE));
        tabIncome.setOnClickListener(v -> switchType(Category.TYPE_INCOME));
        findViewById(R.id.btnCustomizeCategory).setOnClickListener(v ->
                startActivity(new Intent(this, CategoryManageActivity.class)));
        findViewById(R.id.cardDatePicker).setOnClickListener(v -> showDatePicker());
        findViewById(R.id.boxPickReceipt).setOnClickListener(v -> pickReceipt.launch(new String[]{"image/*"}));
        findViewById(R.id.btnRemoveReceipt).setOnClickListener(v -> {
            receiptUri = null;
            imgReceiptPreview.setImageDrawable(null);
            layoutReceiptPreview.setVisibility(View.GONE);
            layoutReceiptPlaceholder.setVisibility(View.VISIBLE);
        });
        findViewById(R.id.btnSaveTransaction).setOnClickListener(v -> saveTransaction());

        editingId = getIntent().getIntExtra(EXTRA_TRANSACTION_ID, -1);
        if (editingId != -1) {
            loadExisting(editingId);
        } else {
            updateTypeUi();
            tvTransactionDate.setText(DateUtils.toDisplayString(calendar.getTime()));
            loadCategories();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dao != null) loadCategories();
    }

    private int getCurrentUserId() {
        int id = sessionManager.getUserId();
        return id > 0 ? id : 1;
    }

    private void switchType(String type) {
        if (currentType.equals(type)) return;
        currentType = type;
        selectedCategoryId = -1;
        updateTypeUi();
        loadCategories();
    }

    private void updateTypeUi() {
        boolean isExpense = Category.TYPE_EXPENSE.equals(currentType);
        tabExpense.setTextColor(Color.parseColor(isExpense ? "#B91C1C" : "#475569"));
        tabExpense.setBackground(isExpense ? CategoryUiHelper.createRoundedBg(this, Color.parseColor("#FFE4DE"), 22) : null);
        tabIncome.setTextColor(Color.parseColor(isExpense ? "#475569" : "#0A5C36"));
        tabIncome.setBackground(isExpense ? null : CategoryUiHelper.createRoundedBg(this, Color.parseColor("#D1FAE5"), 22));
        tvAmountLabel.setText(isExpense ? "Số tiền chi tiêu" : "Số tiền thu nhập");
        tvAmountSign.setText(isExpense ? "-" : "+");
        tvAmountSign.setTextColor(Color.parseColor(isExpense ? "#B91C1C" : "#0A5C36"));
    }

    private void loadCategories() {
        int userId = getCurrentUserId();
        categories = dao.getCategoriesByType(userId, currentType);
        if (categories.isEmpty()) categories = dao.getAllCategories(userId);

        boolean exists = false;
        for (Category c : categories) {
            if (c.getCategoryId() == selectedCategoryId) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            selectedCategoryId = !categories.isEmpty() ? categories.get(0).getCategoryId() : -1;
        }
        renderCategories();
    }

    /**
     * Hiển thị danh sách danh mục dạng lưới 4 cột bằng layout item_category_pick.xml.
     */
    private void renderCategories() {
        layoutCategoryGrid.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int cols = 4;

        for (int i = 0; i < categories.size(); i += cols) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < cols; c++) {
                int idx = i + c;
                if (idx < categories.size()) {
                    row.addView(createCategoryView(inflater, row, categories.get(idx)));
                } else {
                    View spacer = new View(this);
                    spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
                    row.addView(spacer);
                }
            }
            layoutCategoryGrid.addView(row);
        }
    }

    private View createCategoryView(LayoutInflater inflater, LinearLayout parent, Category cat) {
        View item = inflater.inflate(R.layout.item_category_pick, parent, false);
        TextView tvIcon = item.findViewById(R.id.tvPickCatIcon);
        TextView tvName = item.findViewById(R.id.tvPickCatName);

        boolean selected = cat.getCategoryId() == selectedCategoryId;
        int borderColor = CategoryUiHelper.parseColor(cat.getColorCode(), "#0A5C36");

        tvIcon.setText(CategoryUiHelper.getEmoji(cat.getIconName()));
        tvIcon.setBackground(CategoryUiHelper.createCircleBg(this, borderColor, selected ? 3 : 2));
        tvName.setText(cat.getName());
        tvName.setTextColor(Color.parseColor(selected ? "#0F172A" : "#334155"));
        tvName.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);

        if (selected) {
            GradientDrawable box = CategoryUiHelper.createRoundedBg(this, CategoryUiHelper.withAlpha(borderColor, 26), 14);
            box.setStroke(CategoryUiHelper.dp(this, 2), borderColor);
            item.setBackground(box);
        }

        item.setOnClickListener(v -> {
            selectedCategoryId = cat.getCategoryId();
            renderCategories();
        });
        return item;
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, y, m, d) -> {
            calendar.set(y, m, d);
            tvTransactionDate.setText(DateUtils.toDisplayString(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadExisting(int id) {
        Transaction tx = dao.getTransactionById(id);
        if (tx == null) {
            Toast.makeText(this, "Không tìm thấy giao dịch", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        tvScreenTitle.setText("Sửa Giao Dịch");
        tvSaveButtonText.setText("Cập nhật giao dịch");
        currentType = Category.TYPE_INCOME.equalsIgnoreCase(tx.getCategoryType())
                ? Category.TYPE_INCOME : Category.TYPE_EXPENSE;
        selectedCategoryId = tx.getCategoryId();
        updateTypeUi();
        loadCategories();

        edtAmount.setText(String.valueOf(Math.round(tx.getAmount())));
        edtNote.setText(!TextUtils.isEmpty(tx.getNote()) ? tx.getNote() : tx.getTitle());

        Date parsed = DateUtils.parseDbDate(tx.getTransactionDate());
        if (parsed != null) calendar.setTime(parsed);
        tvTransactionDate.setText(DateUtils.toDisplayString(calendar.getTime()));

        if (!TextUtils.isEmpty(tx.getReceiptUri())) {
            receiptUri = tx.getReceiptUri();
            showReceipt(Uri.parse(receiptUri));
        }
    }

    private void showReceipt(Uri uri) {
        imgReceiptPreview.setImageURI(uri);
        layoutReceiptPlaceholder.setVisibility(View.GONE);
        layoutReceiptPreview.setVisibility(View.VISIBLE);
    }

    private void saveTransaction() {
        double amount;
        try {
            amount = Double.parseDouble(edtAmount.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Vui lòng nhập số tiền hợp lệ!", Toast.LENGTH_SHORT).show();
            edtAmount.requestFocus();
            return;
        }
        if (amount <= 0 || selectedCategoryId == -1) {
            Toast.makeText(this, "Số tiền phải > 0 và cần chọn danh mục!", Toast.LENGTH_SHORT).show();
            return;
        }

        String note = edtNote.getText().toString().trim();
        String title = note;
        if (title.isEmpty()) {
            title = "Giao dịch";
            for (Category c : categories) {
                if (c.getCategoryId() == selectedCategoryId) {
                    title = c.getName();
                    break;
                }
            }
        }

        int userId = getCurrentUserId();
        String dbDate = DateUtils.toDbString(calendar.getTime());

        boolean ok = editingId != -1
                ? dao.updateTransaction(new Transaction(editingId, userId, selectedCategoryId, title, amount, dbDate, note, receiptUri)) > 0
                : dao.insertTransaction(new Transaction(userId, selectedCategoryId, title, amount, dbDate, note, receiptUri)) > 0;

        Toast.makeText(this, ok ? "Lưu giao dịch thành công!" : "Lưu giao dịch thất bại!", Toast.LENGTH_SHORT).show();
        if (ok) {
            setResult(RESULT_OK);
            finish();
        }
    }
}
