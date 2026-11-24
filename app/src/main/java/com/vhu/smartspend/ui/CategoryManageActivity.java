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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.vhu.smartspend.R;
import com.vhu.smartspend.adapter.CategoryGridAdapter;
import com.vhu.smartspend.database.CategoryDAO;
import com.vhu.smartspend.model.Category;
import com.vhu.smartspend.utils.CategoryUiHelper;
import com.vhu.smartspend.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class CategoryManageActivity extends AppCompatActivity {

    private EditText edtCategoryName;
    private TextView tvCategoryPreview;
    private LinearLayout layoutIconPicker, layoutColorPicker;
    private RadioButton rbExpense, rbIncome;
    private Button btnSaveCategory, btnDeleteCategory;
    private GridView gvCategories;

    private final List<TextView> iconViews = new ArrayList<>();
    private final List<TextView> colorViews = new ArrayList<>();

    private String selectedIconName = CategoryUiHelper.ICON_NAMES[0];
    private String selectedColorHex = CategoryUiHelper.COLORS[0];

    private CategoryDAO categoryDAO;
    private SessionManager sessionManager;
    private CategoryGridAdapter gridAdapter;
    private List<Category> categoryList = new ArrayList<>();
    private Category selectedCategory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manage);

        sessionManager = new SessionManager(this);
        categoryDAO = new CategoryDAO(this);

        tvCategoryPreview = findViewById(R.id.tvCategoryPreview);
        edtCategoryName = findViewById(R.id.edtCategoryName);
        layoutIconPicker = findViewById(R.id.layoutIconPicker);
        layoutColorPicker = findViewById(R.id.layoutColorPicker);
        rbExpense = findViewById(R.id.rbExpense);
        rbIncome = findViewById(R.id.rbIncome);
        btnSaveCategory = findViewById(R.id.btnSaveCategory);
        btnDeleteCategory = findViewById(R.id.btnDeleteCategory);
        gvCategories = findViewById(R.id.gvCategories);

        gridAdapter = new CategoryGridAdapter(this, categoryList);
        gvCategories.setAdapter(gridAdapter);

        setupIconPicker();
        setupColorPicker();
        refreshPickerSelection();
        updateActionButtons();
        loadCategories();

        int preselectId = getIntent().getIntExtra("category_id", -1);
        if (preselectId != -1) {
            for (Category c : categoryList) {
                if (c.getCategoryId() == preselectId) {
                    selectedCategory = c;
                    populateInputs(c);
                    break;
                }
            }
        }

        findViewById(R.id.btnBackCategory).setOnClickListener(v -> finish());
        btnSaveCategory.setOnClickListener(v -> saveCategory());
        btnDeleteCategory.setOnClickListener(v -> {
            if (selectedCategory != null) showDeleteConfirmDialog(selectedCategory);
        });

        gvCategories.setOnItemClickListener((parent, view, position, id) -> {
            Category clicked = categoryList.get(position);
            if (selectedCategory != null && selectedCategory.getCategoryId() == clicked.getCategoryId()) {
                resetInputs();
            } else {
                selectedCategory = clicked;
                populateInputs(clicked);
            }
        });

        gvCategories.setOnItemLongClickListener((parent, view, position, id) -> {
            selectedCategory = categoryList.get(position);
            populateInputs(selectedCategory);
            showDeleteConfirmDialog(selectedCategory);
            return true;
        });
    }

    private void setupIconPicker() {
        layoutIconPicker.removeAllViews();
        iconViews.clear();
        int size = CategoryUiHelper.dp(this, 42);
        int gap = CategoryUiHelper.dp(this, 6);

        for (int rowStart = 0; rowStart < CategoryUiHelper.ICON_NAMES.length; rowStart += 10) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (rowStart > 0) rowParams.topMargin = gap;
            row.setLayoutParams(rowParams);

            int rowEnd = Math.min(rowStart + 10, CategoryUiHelper.ICON_NAMES.length);
            for (int i = rowStart; i < rowEnd; i++) {
                String iconName = CategoryUiHelper.ICON_NAMES[i];
                TextView tv = new TextView(this);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(size, size);
                if (i < rowEnd - 1) p.setMarginEnd(gap);
                tv.setLayoutParams(p);
                tv.setGravity(Gravity.CENTER);
                tv.setText(CategoryUiHelper.ICON_EMOJIS[i]);
                tv.setTextSize(19f);
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
        int size = CategoryUiHelper.dp(this, 34);
        int gap = CategoryUiHelper.dp(this, 8);

        for (int i = 0; i < CategoryUiHelper.COLORS.length; i++) {
            String colorHex = CategoryUiHelper.COLORS[i];
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(size, size);
            if (i < CategoryUiHelper.COLORS.length - 1) p.setMarginEnd(gap);
            tv.setLayoutParams(p);
            tv.setGravity(Gravity.CENTER);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(14f);
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
        int dividerColor = ContextCompat.getColor(this, R.color.setting_divider);
        int checkStroke = ContextCompat.getColor(this, R.color.setting_text_primary);
        int borderColor = CategoryUiHelper.parseColor(selectedColorHex, "#0A5C36");

        for (int i = 0; i < CategoryUiHelper.ICON_NAMES.length; i++) {
            boolean isSelected = CategoryUiHelper.ICON_NAMES[i].equalsIgnoreCase(selectedIconName);
            GradientDrawable bg = CategoryUiHelper.createRoundedBg(this, isSelected ? selectedBg : unselectedBg, 10);
            bg.setStroke(CategoryUiHelper.dp(this, isSelected ? 2 : 1), isSelected ? borderColor : dividerColor);
            iconViews.get(i).setBackground(bg);
        }

        for (int i = 0; i < CategoryUiHelper.COLORS.length; i++) {
            boolean isSelected = CategoryUiHelper.COLORS[i].equalsIgnoreCase(selectedColorHex);
            TextView tv = colorViews.get(i);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(Color.parseColor(CategoryUiHelper.COLORS[i]));
            circle.setStroke(CategoryUiHelper.dp(this, isSelected ? 3 : 1), isSelected ? checkStroke : dividerColor);
            tv.setText(isSelected ? "✓" : "");
            tv.setBackground(circle);
        }

        tvCategoryPreview.setText(CategoryUiHelper.getEmoji(selectedIconName));
        GradientDrawable previewBg = new GradientDrawable();
        previewBg.setShape(GradientDrawable.OVAL);
        previewBg.setColor(unselectedBg);
        previewBg.setStroke(CategoryUiHelper.dp(this, 3), borderColor);
        tvCategoryPreview.setBackground(previewBg);
    }

    private int getCurrentUserId() {
        int id = sessionManager.getUserId();
        return id > 0 ? id : 1;
    }

    private void loadCategories() {
        categoryList = categoryDAO.getAllCategories(getCurrentUserId());
        gridAdapter.updateData(categoryList);
    }

    private void saveCategory() {
        String name = edtCategoryName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            edtCategoryName.setError("Vui lòng nhập tên danh mục");
            return;
        }

        int userId = getCurrentUserId();
        String type = rbIncome.isChecked() ? Category.TYPE_INCOME : Category.TYPE_EXPENSE;
        int excludeId = selectedCategory != null ? selectedCategory.getCategoryId() : -1;
        if (categoryDAO.existsCategoryName(userId, name, type, excludeId)) {
            edtCategoryName.setError("Danh mục này đã tồn tại");
            return;
        }

        boolean ok;
        if (selectedCategory == null) {
            Category cat = new Category(name, type, selectedIconName, selectedColorHex);
            cat.setUserId(userId);
            ok = categoryDAO.insertCategory(cat) > 0;
            Toast.makeText(this, ok ? "Thêm danh mục thành công!" : "Thêm thất bại!", Toast.LENGTH_SHORT).show();
        } else {
            selectedCategory.setUserId(userId);
            selectedCategory.setName(name);
            selectedCategory.setType(type);
            selectedCategory.setIconName(selectedIconName);
            selectedCategory.setColorCode(selectedColorHex);
            ok = categoryDAO.updateCategory(selectedCategory) > 0;
            Toast.makeText(this, ok ? "Cập nhật danh mục thành công!" : "Cập nhật thất bại!", Toast.LENGTH_SHORT).show();
        }

        if (ok) {
            loadCategories();
            resetInputs();
        }
    }

    private void showDeleteConfirmDialog(Category cat) {
        if (categoryDAO.isCategoryInUse(cat.getCategoryId())) {
            Toast.makeText(this, "Không thể xóa danh mục đang có giao dịch!", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa danh mục '" + cat.getName() + "'?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    int rows = categoryDAO.deleteCategory(cat.getCategoryId(), getCurrentUserId());
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
        selectedIconName = !TextUtils.isEmpty(cat.getIconName()) ? cat.getIconName() : CategoryUiHelper.ICON_NAMES[0];
        selectedColorHex = !TextUtils.isEmpty(cat.getColorCode()) ? cat.getColorCode() : CategoryUiHelper.COLORS[0];
        gridAdapter.setSelectedCategoryId(cat.getCategoryId());
        refreshPickerSelection();
        updateActionButtons();
    }

    private void resetInputs() {
        selectedCategory = null;
        edtCategoryName.setError(null);
        edtCategoryName.setText("");
        rbExpense.setChecked(true);
        selectedIconName = CategoryUiHelper.ICON_NAMES[0];
        selectedColorHex = CategoryUiHelper.COLORS[0];
        gridAdapter.setSelectedCategoryId(-1);
        refreshPickerSelection();
        updateActionButtons();
    }

    private void updateActionButtons() {
        btnSaveCategory.setText(selectedCategory == null ? "Tạo" : "Lưu");
        btnDeleteCategory.setVisibility(selectedCategory == null ? View.GONE : View.VISIBLE);
    }
}
