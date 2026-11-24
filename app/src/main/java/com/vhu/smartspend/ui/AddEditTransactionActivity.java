package com.vhu.smartspend.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

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
    private NestedScrollView scrollCategoryGrid;
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
        scrollCategoryGrid = findViewById(R.id.scrollCategoryGrid);
        tvTransactionDate = findViewById(R.id.tvTransactionDate);
        edtNote = findViewById(R.id.edtNote);
        layoutReceiptPlaceholder = findViewById(R.id.layoutReceiptPlaceholder);
        layoutReceiptPreview = findViewById(R.id.layoutReceiptPreview);
        imgReceiptPreview = findViewById(R.id.imgReceiptPreview);
        tvSaveButtonText = findViewById(R.id.tvSaveButtonText);

        scrollCategoryGrid.setOnTouchListener(nestedScrollTouchListener);

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

    private final View.OnTouchListener nestedScrollTouchListener = (v, event) -> {
        if (categories.size() > 8) {
            int action = event.getActionMasked();
            boolean disallow = action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL;
            scrollCategoryGrid.getParent().requestDisallowInterceptTouchEvent(disallow);
        }
        return false;
    };

    private int getCurrentUserId() {
        int id = sessionManager.getUserId();
        return id > 0 ? id : 1;
    }

    private void switchType(String type) {
        if (currentType.equals(type)) return;
        currentType = type;
        selectedCategoryId = -1;
        updateTypeUi();
        scrollCategoryGrid.scrollTo(0, 0);
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

    private void renderCategories() {
        layoutCategoryGrid.removeAllViews();
        int cols = 4;
        for (int i = 0; i < categories.size(); i += cols) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < cols; c++) {
                int idx = i + c;
                if (idx < categories.size()) {
                    row.addView(buildCategoryItem(categories.get(idx)));
                } else {
                    View spacer = new View(this);
                    spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
                    row.addView(spacer);
                }
            }
            layoutCategoryGrid.addView(row);
        }

        // Nếu có hơn 8 danh mục thì giới hạn chiều cao 2 hàng để cuộn mượt bên trong
        if (categories.size() > 8 && layoutCategoryGrid.getChildCount() >= 2) {
            scrollCategoryGrid.setScrollbarFadingEnabled(false);
            layoutCategoryGrid.post(() -> {
                if (layoutCategoryGrid.getChildCount() >= 2) {
                    int h = layoutCategoryGrid.getChildAt(0).getHeight() + layoutCategoryGrid.getChildAt(1).getHeight();
                    if (h > 0 && scrollCategoryGrid.getLayoutParams().height != h) {
                        scrollCategoryGrid.getLayoutParams().height = h;
                        scrollCategoryGrid.requestLayout();
                    }
                }
            });
        } else {
            scrollCategoryGrid.setScrollbarFadingEnabled(true);
            if (scrollCategoryGrid.getLayoutParams().height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                scrollCategoryGrid.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                scrollCategoryGrid.requestLayout();
            }
        }
    }

    private View buildCategoryItem(Category cat) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        item.setPadding(CategoryUiHelper.dp(this, 4), CategoryUiHelper.dp(this, 8),
                CategoryUiHelper.dp(this, 4), CategoryUiHelper.dp(this, 8));
        item.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView icon = new TextView(this);
        int iconSize = CategoryUiHelper.dp(this, 48);
        icon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        icon.setGravity(Gravity.CENTER);
        icon.setText(CategoryUiHelper.getEmoji(cat.getIconName()));
        icon.setTextSize(22f);

        TextView label = new TextView(this);
        label.setPadding(0, CategoryUiHelper.dp(this, 6), 0, 0);
        label.setGravity(Gravity.CENTER);
        label.setText(cat.getName());
        label.setTextSize(12f);
        label.setMaxLines(1);
        label.setEllipsize(TextUtils.TruncateAt.END);

        boolean selected = cat.getCategoryId() == selectedCategoryId;
        int borderColor = CategoryUiHelper.parseColor(cat.getColorCode(), "#0A5C36");
        if (selected) {
            GradientDrawable box = CategoryUiHelper.createRoundedBg(this, CategoryUiHelper.withAlpha(borderColor, 26), 14);
            box.setStroke(CategoryUiHelper.dp(this, 2), borderColor);
            item.setBackground(box);
        }
        icon.setBackground(CategoryUiHelper.createCircleBg(this, borderColor, selected ? 3 : 2));
        label.setTextColor(Color.parseColor(selected ? "#0F172A" : "#334155"));
        label.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);

        item.addView(icon);
        item.addView(label);
        item.setOnTouchListener(nestedScrollTouchListener);
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
