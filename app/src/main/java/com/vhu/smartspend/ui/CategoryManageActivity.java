package com.vhu.smartspend.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.vhu.smartspend.R;
import com.vhu.smartspend.adapter.CategoryGridAdapter;
import com.vhu.smartspend.database.CategoryDAO;
import com.vhu.smartspend.model.Category;
import com.vhu.smartspend.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class CategoryManageActivity extends AppCompatActivity {

    private EditText edtCategoryName, edtCategoryIcon, edtCategoryColor;
    private RadioGroup rgCategoryType;
    private RadioButton rbExpense, rbIncome;
    private Button btnAddCategory, btnUpdateCategory, btnDeleteCategory, btnClearCategory;
    private GridView gvCategories;

    private CategoryDAO categoryDAO;
    private SessionManager sessionManager;
    private CategoryGridAdapter gridAdapter;
    private List<Category> categoryList;
    private Category selectedCategory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manage);

        categoryDAO = new CategoryDAO(this);
        sessionManager = new SessionManager(this);

        initViews();
        loadCategories();

        btnAddCategory.setOnClickListener(v -> addCategory());
        btnUpdateCategory.setOnClickListener(v -> updateCategory());
        btnDeleteCategory.setOnClickListener(v -> deleteCategory());
        btnClearCategory.setOnClickListener(v -> resetInputs());

        gvCategories.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory = categoryList.get(position);
            populateInputs(selectedCategory);
        });

        gvCategories.setOnItemLongClickListener((parent, view, position, id) -> {
            selectedCategory = categoryList.get(position);
            showDeleteConfirmDialog(selectedCategory);
            return true;
        });
    }

    private void initViews() {
        edtCategoryName = findViewById(R.id.edtCategoryName);
        edtCategoryIcon = findViewById(R.id.edtCategoryIcon);
        edtCategoryColor = findViewById(R.id.edtCategoryColor);
        rgCategoryType = findViewById(R.id.rgCategoryType);
        rbExpense = findViewById(R.id.rbExpense);
        rbIncome = findViewById(R.id.rbIncome);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        btnUpdateCategory = findViewById(R.id.btnUpdateCategory);
        btnDeleteCategory = findViewById(R.id.btnDeleteCategory);
        btnClearCategory = findViewById(R.id.btnClearCategory);
        gvCategories = findViewById(R.id.gvCategories);

        categoryList = new ArrayList<>();
        gridAdapter = new CategoryGridAdapter(this, categoryList);
        gvCategories.setAdapter(gridAdapter);
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
        String icon = edtCategoryIcon.getText().toString().trim();
        if (TextUtils.isEmpty(icon)) {
            icon = type.equals(Category.TYPE_INCOME) ? "ic_work" : "ic_food";
        }
        String color = edtCategoryColor.getText().toString().trim();
        if (TextUtils.isEmpty(color)) {
            color = type.equals(Category.TYPE_INCOME) ? "#4CAF50" : "#FF5722";
        }

        Category cat = new Category(name, type, icon, color);
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
        String icon = edtCategoryIcon.getText().toString().trim();
        String color = edtCategoryColor.getText().toString().trim();

        selectedCategory.setName(name);
        selectedCategory.setType(type);
        selectedCategory.setIconName(icon);
        selectedCategory.setColorCode(color);

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
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa danh mục '" + cat.getName() + "'?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    int rows = categoryDAO.deleteCategory(cat.getCategoryId());
                    if (rows > 0) {
                        Toast.makeText(this, "Đã xóa danh mục thành công!", Toast.LENGTH_SHORT).show();
                        loadCategories();
                        resetInputs();
                    } else {
                        Toast.makeText(this, "Xóa thất bại!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void populateInputs(Category cat) {
        edtCategoryName.setText(cat.getName());
        if (Category.TYPE_INCOME.equalsIgnoreCase(cat.getType())) {
            rbIncome.setChecked(true);
        } else {
            rbExpense.setChecked(true);
        }
        edtCategoryIcon.setText(cat.getIconName());
        edtCategoryColor.setText(cat.getColorCode());
    }

    private void resetInputs() {
        selectedCategory = null;
        edtCategoryName.setText("");
        rbExpense.setChecked(true);
        edtCategoryIcon.setText("");
        edtCategoryColor.setText("");
    }
}
