package com.vhu.smartspend.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.vhu.smartspend.R;
import com.vhu.smartspend.adapter.CategoryGridAdapter;
import com.vhu.smartspend.database.CategoryDAO;
import com.vhu.smartspend.model.Category;
import com.vhu.smartspend.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class CategoryManageActivity extends AppCompatActivity {

    private static final String[] SAMPLE_ICON_NAMES = {
            "ic_food", "ic_coffee", "ic_school", "ic_shopping", "ic_transport",
            "ic_home", "ic_bill", "ic_entertainment", "ic_health", "ic_beauty",
            "ic_travel", "ic_pet", "ic_sport", "ic_phone", "ic_gift",
            "ic_award", "ic_work", "ic_savings", "ic_invest", "ic_other"
    };
    private static final String[] SAMPLE_ICON_EMOJIS = {
            "🍔", "☕", "📚", "🛒", "🚗",
            "🏠", "⚡", "🎮", "💊", "💄",
            "✈️", "🐾", "🏋️", "📱", "🎁",
            "🎓", "💼", "💰", "📈", "⭐"
    };
    private static final String[] SAMPLE_COLORS = {
            "#0A5C36", "#FF5722", "#2196F3", "#9C27B0",
            "#009688", "#4CAF50", "#FFC107", "#E91E63",
            "#3F51B5", "#FF9800", "#795548", "#00BCD4"
    };

    private EditText edtCategoryName;
    private TextView tvCategoryPreview;
    private LinearLayout layoutIconPicker, layoutColorPicker;
    private RadioGroup rgCategoryType;
    private RadioButton rbExpense, rbIncome;
    private Button btnSaveCategory, btnDeleteCategory;
    private GridView gvCategories;

    private final List<TextView> iconViews = new ArrayList<>();
    private final List<TextView> colorViews = new ArrayList<>();

    private String selectedIconName = SAMPLE_ICON_NAMES[0];
    private String selectedColorHex = SAMPLE_COLORS[0];

    private CategoryDAO categoryDAO;
    private SessionManager sessionManager;
    private CategoryGridAdapter gridAdapter;
    private List<Category> categoryList;
    private Category selectedCategory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);

        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_category_manage);

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(!sessionManager.isDarkMode());
        insetsController.setAppearanceLightNavigationBars(!sessionManager.isDarkMode());

        View rootView = findViewById(R.id.main);
        int basePad = dpToPx(16);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    systemBars.left + basePad,
                    systemBars.top + basePad,
                    systemBars.right + basePad,
                    systemBars.bottom + basePad
            );
            return insets;
        });

        categoryDAO = new CategoryDAO(this);

        initViews();
        setupIconPicker();
        setupColorPicker();
        refreshPickerSelection();
        updateActionButtons();
        loadCategories();

        findViewById(R.id.btnBackCategory).setOnClickListener(v -> finish());
        btnSaveCategory.setOnClickListener(v -> {
            if (selectedCategory == null) {
                addCategory();
            } else {
                updateCategory();
            }
        });
        btnDeleteCategory.setOnClickListener(v -> deleteCategory());

        gvCategories.setOnItemClickListener((parent, view, position, id) -> {
            Category clicked = categoryList.get(position);
            if (selectedCategory != null && selectedCategory.getCategoryId() == clicked.getCategoryId()) {
                resetInputs();
            } else {
                selectedCategory = clicked;
                populateInputs(selectedCategory);
            }
        });

        gvCategories.setOnItemLongClickListener((parent, view, position, id) -> {
            selectedCategory = categoryList.get(position);
            populateInputs(selectedCategory);
            showDeleteConfirmDialog(selectedCategory);
            return true;
        });
    }

    private void initViews() {
        tvCategoryPreview = findViewById(R.id.tvCategoryPreview);
        edtCategoryName = findViewById(R.id.edtCategoryName);
        layoutIconPicker = findViewById(R.id.layoutIconPicker);
        layoutColorPicker = findViewById(R.id.layoutColorPicker);
        rgCategoryType = findViewById(R.id.rgCategoryType);
        rbExpense = findViewById(R.id.rbExpense);
        rbIncome = findViewById(R.id.rbIncome);
        btnSaveCategory = findViewById(R.id.btnSaveCategory);
        btnDeleteCategory = findViewById(R.id.btnDeleteCategory);
        gvCategories = findViewById(R.id.gvCategories);

        categoryList = new ArrayList<>();
        gridAdapter = new CategoryGridAdapter(this, categoryList);
        gvCategories.setAdapter(gridAdapter);
    }

    private void setupIconPicker() {
        layoutIconPicker.removeAllViews();
        iconViews.clear();

        int sizePx = dpToPx(42);
        int gapPx = dpToPx(6);
        int perRow = 10;

        for (int rowStart = 0; rowStart < SAMPLE_ICON_NAMES.length; rowStart += perRow) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            if (rowStart > 0) {
                rowParams.topMargin = gapPx;
            }
            row.setLayoutParams(rowParams);

            int rowEnd = Math.min(rowStart + perRow, SAMPLE_ICON_NAMES.length);
            for (int i = rowStart; i < rowEnd; i++) {
                final String iconName = SAMPLE_ICON_NAMES[i];
                TextView tv = new TextView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
                if (i < rowEnd - 1) {
                    params.setMarginEnd(gapPx);
                }
                tv.setLayoutParams(params);
                tv.setGravity(Gravity.CENTER);
                tv.setText(SAMPLE_ICON_EMOJIS[i]);
                tv.setTextSize(19f);
                tv.setClickable(true);
                tv.setFocusable(true);
                tv.setOnClickListener(v -> {
                    selectedIconName = iconName;
                    refreshPickerSelection();
                });

                iconViews.add(tv);
                row.addView(tv);
            }
            layoutIconPicker.addView(row);
        }
    }

    private void setupColorPicker() {
        layoutColorPicker.removeAllViews();
        colorViews.clear();

        int sizePx = dpToPx(34);
        int marginPx = dpToPx(8);

        for (int i = 0; i < SAMPLE_COLORS.length; i++) {
            final String colorHex = SAMPLE_COLORS[i];
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
            if (i < SAMPLE_COLORS.length - 1) {
                params.setMarginEnd(marginPx);
            }
            tv.setLayoutParams(params);
            tv.setGravity(Gravity.CENTER);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(14f);
            tv.setClickable(true);
            tv.setFocusable(true);
            tv.setOnClickListener(v -> {
                selectedColorHex = colorHex;
                refreshPickerSelection();
            });

            colorViews.add(tv);
            layoutColorPicker.addView(tv);
        }
    }

    private void refreshPickerSelection() {
        int selectedBg = ContextCompat.getColor(this, R.color.nav_indicator);
        int unselectedBg = ContextCompat.getColor(this, R.color.setting_pill_bg);
        int unselectedStroke = ContextCompat.getColor(this, R.color.setting_divider);
        int colorCheckStroke = ContextCompat.getColor(this, R.color.setting_text_primary);

        int borderColor;
        try {
            borderColor = Color.parseColor(selectedColorHex);
        } catch (Exception e) {
            borderColor = Color.parseColor("#0A5C36");
        }

        // Highlight icon đang chọn
        for (int i = 0; i < SAMPLE_ICON_NAMES.length; i++) {
            boolean isSelected = SAMPLE_ICON_NAMES[i].equalsIgnoreCase(selectedIconName);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dpToPx(10));
            bg.setColor(isSelected ? selectedBg : unselectedBg);
            bg.setStroke(dpToPx(isSelected ? 2 : 1), isSelected ? borderColor : unselectedStroke);
            iconViews.get(i).setBackground(bg);
        }

        // Highlight màu đang chọn
        for (int i = 0; i < SAMPLE_COLORS.length; i++) {
            boolean isSelected = SAMPLE_COLORS[i].equalsIgnoreCase(selectedColorHex);
            TextView tv = colorViews.get(i);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(Color.parseColor(SAMPLE_COLORS[i]));
            if (isSelected) {
                circle.setStroke(dpToPx(3), colorCheckStroke);
                tv.setText("✓");
            } else {
                circle.setStroke(dpToPx(1), unselectedStroke);
                tv.setText("");
            }
            tv.setBackground(circle);
        }

        // Cập nhật vòng tròn xem trước (dùng màu đã chọn làm viền)
        tvCategoryPreview.setText(getEmojiByIconName(selectedIconName));
        GradientDrawable previewBg = new GradientDrawable();
        previewBg.setShape(GradientDrawable.OVAL);
        previewBg.setColor(unselectedBg);
        previewBg.setStroke(dpToPx(3), borderColor);
        tvCategoryPreview.setBackground(previewBg);
    }

    private String getEmojiByIconName(String iconName) {
        if (iconName != null) {
            for (int i = 0; i < SAMPLE_ICON_NAMES.length; i++) {
                if (SAMPLE_ICON_NAMES[i].equalsIgnoreCase(iconName)) {
                    return SAMPLE_ICON_EMOJIS[i];
                }
            }
        }
        return SAMPLE_ICON_EMOJIS[0];
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void loadCategories() {
        categoryList = categoryDAO.getAllCategories();
        gridAdapter.updateData(categoryList);
    }

    private void addCategory() {
        String name = edtCategoryName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            edtCategoryName.setError("Vui lòng nhập tên danh mục");
            return;
        }

        String type = rbIncome.isChecked() ? Category.TYPE_INCOME : Category.TYPE_EXPENSE;
        if (categoryDAO.existsCategoryName(name, type, -1)) {
            edtCategoryName.setError("Danh mục này đã tồn tại");
            return;
        }

        Category cat = new Category(name, type, selectedIconName, selectedColorHex);
        long result = categoryDAO.insertCategory(cat);
        if (result > 0) {
            Toast.makeText(this, "Thêm danh mục thành công!", Toast.LENGTH_SHORT).show();
            loadCategories();
            resetInputs();
        } else {
            Toast.makeText(this, "Thêm thất bại!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCategory() {
        if (selectedCategory == null) {
            Toast.makeText(this, "Vui lòng chọn danh mục từ danh sách để sửa", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = edtCategoryName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            edtCategoryName.setError("Vui lòng nhập tên danh mục");
            return;
        }

        String type = rbIncome.isChecked() ? Category.TYPE_INCOME : Category.TYPE_EXPENSE;
        if (categoryDAO.existsCategoryName(name, type, selectedCategory.getCategoryId())) {
            edtCategoryName.setError("Danh mục này đã tồn tại");
            return;
        }

        selectedCategory.setName(name);
        selectedCategory.setType(type);
        selectedCategory.setIconName(selectedIconName);
        selectedCategory.setColorCode(selectedColorHex);

        int rows = categoryDAO.updateCategory(selectedCategory);
        if (rows > 0) {
            Toast.makeText(this, "Cập nhật danh mục thành công!", Toast.LENGTH_SHORT).show();
            loadCategories();
            resetInputs();
        } else {
            Toast.makeText(this, "Cập nhật thất bại!", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteCategory() {
        if (!"ADMIN".equalsIgnoreCase(sessionManager.getRole())) {
            Toast.makeText(this, "Chỉ tài khoản ADMIN mới có quyền xóa danh mục!", Toast.LENGTH_LONG).show();
            return;
        }
        if (selectedCategory == null) {
            Toast.makeText(this, "Vui lòng chọn danh mục cần xóa!", Toast.LENGTH_SHORT).show();
            return;
        }
        showDeleteConfirmDialog(selectedCategory);
    }

    private void showDeleteConfirmDialog(Category cat) {
        if (!"ADMIN".equalsIgnoreCase(sessionManager.getRole())) {
            Toast.makeText(this, "Chỉ tài khoản ADMIN mới có quyền xóa danh mục!", Toast.LENGTH_LONG).show();
            return;
        }
        if (categoryDAO.isCategoryInUse(cat.getCategoryId())) {
            Toast.makeText(this, "Không thể xóa danh mục đang có giao dịch!", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa danh mục '" + cat.getName() + "'?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    int rows = categoryDAO.deleteCategory(cat.getCategoryId());
                    if (rows > 0) {
                        Toast.makeText(this, "Đã xóa danh mục thành công!", Toast.LENGTH_SHORT).show();
                        loadCategories();
                        resetInputs();
                    } else if (rows == -1) {
                        Toast.makeText(this, "Không thể xóa danh mục đang có giao dịch!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Xóa thất bại!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void populateInputs(Category cat) {
        edtCategoryName.setError(null);
        edtCategoryName.setText(cat.getName());
        if (Category.TYPE_INCOME.equalsIgnoreCase(cat.getType())) {
            rbIncome.setChecked(true);
        } else {
            rbExpense.setChecked(true);
        }
        selectedIconName = !TextUtils.isEmpty(cat.getIconName()) ? cat.getIconName() : SAMPLE_ICON_NAMES[0];
        selectedColorHex = !TextUtils.isEmpty(cat.getColorCode()) ? cat.getColorCode() : SAMPLE_COLORS[0];
        gridAdapter.setSelectedCategoryId(cat.getCategoryId());
        refreshPickerSelection();
        updateActionButtons();
    }

    private void resetInputs() {
        selectedCategory = null;
        edtCategoryName.setError(null);
        edtCategoryName.setText("");
        rbExpense.setChecked(true);
        selectedIconName = SAMPLE_ICON_NAMES[0];
        selectedColorHex = SAMPLE_COLORS[0];
        gridAdapter.setSelectedCategoryId(-1);
        refreshPickerSelection();
        updateActionButtons();
    }

    private void updateActionButtons() {
        if (selectedCategory == null) {
            btnSaveCategory.setText("Tạo");
            btnDeleteCategory.setVisibility(View.GONE);
        } else {
            btnSaveCategory.setText("Lưu");
            btnDeleteCategory.setVisibility(View.VISIBLE);
        }
    }
}
