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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;

import com.vhu.smartspend.R;
import com.vhu.smartspend.database.TransactionDAO;
import com.vhu.smartspend.model.Category;
import com.vhu.smartspend.model.Transaction;
import com.vhu.smartspend.utils.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddEditTransactionActivity extends AppCompatActivity {

    public static final String EXTRA_TRANSACTION_ID = "transaction_id";

    private TextView tvScreenTitle, tabExpense, tabIncome, tvAmountLabel, tvAmountSign, tvTransactionDate, tvSaveButtonText;
    private EditText edtAmount, edtNote;
    private LinearLayout layoutCategoryGrid;
    private NestedScrollView scrollCategoryGrid;
    private View layoutReceiptPlaceholder, layoutReceiptPreview;
    private ImageView imgReceiptPreview;

    private TransactionDAO dao;
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
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_add_edit_transaction);

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
        tvScreenTitle = findViewById(R.id.tvScreenTitle);
        tabExpense = findViewById(R.id.tabExpense);
        tabIncome = findViewById(R.id.tabIncome);
        tvAmountLabel = findViewById(R.id.tvAmountLabel);
        tvAmountSign = findViewById(R.id.tvAmountSign);
        edtAmount = findViewById(R.id.edtAmount);
        layoutCategoryGrid = findViewById(R.id.layoutCategoryGrid);
        scrollCategoryGrid = findViewById(R.id.scrollCategoryGrid);
        scrollCategoryGrid.setOnTouchListener((v, event) -> {
            if (categories.size() > 8) {
                int action = event.getActionMasked();
                boolean disallow = action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL;
                scrollCategoryGrid.getParent().requestDisallowInterceptTouchEvent(disallow);
            }
            return false;
        });
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
        tabExpense.setBackground(isExpense ? roundedBg("#FFE4DE", 22) : null);
        tabIncome.setTextColor(Color.parseColor(isExpense ? "#475569" : "#0A5C36"));
        tabIncome.setBackground(isExpense ? null : roundedBg("#D1FAE5", 22));
        tvAmountLabel.setText(isExpense ? "Số tiền chi tiêu" : "Số tiền thu nhập");
        tvAmountSign.setText(isExpense ? "-" : "+");
        tvAmountSign.setTextColor(Color.parseColor(isExpense ? "#B91C1C" : "#0A5C36"));
    }

    private int getCurrentUserId() {
        int userId = getSharedPreferences("SmartSpendPrefs", MODE_PRIVATE).getInt("user_id", 1);
        return userId > 0 ? userId : 1;
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

        if (categories.size() > 8 && layoutCategoryGrid.getChildCount() >= 2) {
            scrollCategoryGrid.setScrollbarFadingEnabled(false);
            layoutCategoryGrid.post(() -> {
                if (layoutCategoryGrid.getChildCount() >= 2) {
                    int twoRowsHeight = layoutCategoryGrid.getChildAt(0).getHeight()
                            + layoutCategoryGrid.getChildAt(1).getHeight();
                    if (twoRowsHeight > 0) {
                        ViewGroup.LayoutParams lp = scrollCategoryGrid.getLayoutParams();
                        if (lp.height != twoRowsHeight) {
                            lp.height = twoRowsHeight;
                            scrollCategoryGrid.setLayoutParams(lp);
                        }
                    }
                }
            });
        } else {
            scrollCategoryGrid.setScrollbarFadingEnabled(true);
            ViewGroup.LayoutParams lp = scrollCategoryGrid.getLayoutParams();
            if (lp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                scrollCategoryGrid.setLayoutParams(lp);
            }
        }
    }

    private View buildCategoryItem(Category cat) {
        boolean selected = cat.getCategoryId() == selectedCategoryId;

        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        item.setPadding(dp(4), dp(8), dp(4), dp(8));
        item.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        item.setTag(cat);

        TextView icon = new TextView(this);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
        icon.setGravity(Gravity.CENTER);
        icon.setText(getCategoryEmoji(cat.getIconName()));
        icon.setTextSize(22f);

        TextView label = new TextView(this);
        label.setPadding(0, dp(6), 0, 0);
        label.setGravity(Gravity.CENTER);
        label.setText(cat.getName());
        label.setTextSize(12f);
        label.setMaxLines(1);
        label.setEllipsize(TextUtils.TruncateAt.END);

        item.addView(icon);
        item.addView(label);
        applyCategoryItemStyle(item, cat, selected);

        item.setOnTouchListener((v, event) -> {
            if (categories.size() > 8) {
                int action = event.getActionMasked();
                boolean disallow = action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL;
                scrollCategoryGrid.getParent().requestDisallowInterceptTouchEvent(disallow);
            }
            return false;
        });
        item.setOnClickListener(v -> {
            selectedCategoryId = cat.getCategoryId();
            updateCategorySelection();
        });
        return item;
    }

    private void applyCategoryItemStyle(LinearLayout item, Category cat, boolean selected) {
        int borderColor = parseCategoryColor(cat.getColorCode());

        if (selected) {
            GradientDrawable selectedBox = new GradientDrawable();
            selectedBox.setCornerRadius(dp(14));
            selectedBox.setColor(Color.argb(26, Color.red(borderColor), Color.green(borderColor), Color.blue(borderColor)));
            selectedBox.setStroke(dp(2), borderColor);
            item.setBackground(selectedBox);
        } else {
            item.setBackground(null);
        }

        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(selected
                ? Color.argb(36, Color.red(borderColor), Color.green(borderColor), Color.blue(borderColor))
                : Color.parseColor("#F8FAFC"));
        circle.setStroke(dp(selected ? 3 : 2), borderColor);

        TextView icon = (TextView) item.getChildAt(0);
        icon.setBackground(circle);

        TextView label = (TextView) item.getChildAt(1);
        label.setTextColor(Color.parseColor(selected ? "#0F172A" : "#334155"));
        label.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void updateCategorySelection() {
        for (int r = 0; r < layoutCategoryGrid.getChildCount(); r++) {
            View rowView = layoutCategoryGrid.getChildAt(r);
            if (!(rowView instanceof LinearLayout)) continue;
            LinearLayout row = (LinearLayout) rowView;
            for (int c = 0; c < row.getChildCount(); c++) {
                View child = row.getChildAt(c);
                if (child instanceof LinearLayout && child.getTag() instanceof Category) {
                    Category cat = (Category) child.getTag();
                    applyCategoryItemStyle((LinearLayout) child, cat, cat.getCategoryId() == selectedCategoryId);
                }
            }
        }
    }

    private int parseCategoryColor(String colorCode) {
        if (colorCode != null && !colorCode.trim().isEmpty()) {
            try {
                return Color.parseColor(colorCode.trim());
            } catch (Exception ignored) {
            }
        }
        return Color.parseColor("#0A5C36");
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
        String raw = edtAmount.getText().toString().trim();
        double amount;
        try {
            amount = Double.parseDouble(raw);
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

        boolean ok;
        if (editingId != -1) {
            ok = dao.updateTransaction(new Transaction(editingId, userId, selectedCategoryId, title, amount, dbDate, note, receiptUri)) > 0;
        } else {
            ok = dao.insertTransaction(new Transaction(userId, selectedCategoryId, title, amount, dbDate, note, receiptUri)) > 0;
        }

        Toast.makeText(this, ok ? "Lưu giao dịch thành công!" : "Lưu giao dịch thất bại!", Toast.LENGTH_SHORT).show();
        if (ok) {
            setResult(RESULT_OK);
            finish();
        }
    }

    private GradientDrawable roundedBg(String hex, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(hex));
        gd.setCornerRadius(dp(radiusDp));
        return gd;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
