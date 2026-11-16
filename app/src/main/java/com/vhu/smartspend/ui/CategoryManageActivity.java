package com.vhu.smartspend.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.vhu.smartspend.R;
import com.vhu.smartspend.adapter.CategoryGridAdapter;
import com.vhu.smartspend.dao.CategoryDAO;
import com.vhu.smartspend.model.Category;
import com.vhu.smartspend.utils.SessionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Quản lý danh mục thu/chi.
 * Chạm vào ô: Sửa. Nhấn giữ: Xóa (chỉ admin). Nút "+ Thêm": Thêm mới.
 */
public class CategoryManageActivity extends AppCompatActivity {
    private static final String[] ICONS =
            {"🍜", "🎓", "🛍️", "🚌", "🏠", "💊", "🎮", "☕", "💰", "🎁", "📱", "✈️"};
    private static final String[] COLOR_NAMES =
            {"Cam", "Xanh dương", "Hồng", "Xanh ngọc", "Xanh lá", "Tím", "Đỏ", "Vàng"};
    private static final String[] COLOR_HEX =
            {"#FF9800", "#3F51B5", "#E91E63", "#009688", "#4CAF50", "#9C27B0", "#F44336", "#FBC02D"};

    private CategoryDAO dao;
    private CategoryGridAdapter adapter;
    private final List<Category> data = new ArrayList<>();
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manage);

        dao = new CategoryDAO(this);
        isAdmin = new SessionManager(this).isAdmin();

        GridView grid = findViewById(R.id.gridCategory);
        Button btnAdd = findViewById(R.id.btnAdd);

        adapter = new CategoryGridAdapter(this, data);
        grid.setAdapter(adapter);

        grid.setOnItemClickListener((parent, view, position, id) ->
                showCategoryDialog(data.get(position)));
        grid.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmDelete(data.get(position));
            return true;
        });
        btnAdd.setOnClickListener(v -> showCategoryDialog(null));

        loadData();
    }

    private void loadData() {
        data.clear();
        data.addAll(dao.getAll());
        adapter.notifyDataSetChanged();
    }

    /** existing == null thì là thêm mới, ngược lại là sửa. */
    private void showCategoryDialog(Category existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_category, null);
        EditText edtName = view.findViewById(R.id.edtCategoryName);
        RadioGroup rgType = view.findViewById(R.id.rgType);
        Spinner spIcon = view.findViewById(R.id.spIcon);
        Spinner spColor = view.findViewById(R.id.spColor);

        spIcon.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, ICONS));
        spColor.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, COLOR_NAMES));

        if (existing != null) {
            edtName.setText(existing.getName());
            rgType.check(Category.TYPE_INCOME.equals(existing.getType())
                    ? R.id.rbIncome : R.id.rbExpense);
            int iconIdx = Arrays.asList(ICONS).indexOf(existing.getIcon());
            if (iconIdx >= 0) spIcon.setSelection(iconIdx);
            for (int i = 0; i < COLOR_HEX.length; i++) {
                if (COLOR_HEX[i].equalsIgnoreCase(existing.getColor())) {
                    spColor.setSelection(i);
                    break;
                }
            }
        } else {
            rgType.check(R.id.rbExpense);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Thêm danh mục" : "Sửa danh mục")
                .setView(view)
                .setPositiveButton("Lưu", null) // gán listener bên dưới để không tự đóng khi lỗi
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = edtName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        edtName.setError("Vui lòng nhập tên danh mục");
                        return;
                    }
                    String type = rgType.getCheckedRadioButtonId() == R.id.rbIncome
                            ? Category.TYPE_INCOME : Category.TYPE_EXPENSE;
                    String icon = ICONS[spIcon.getSelectedItemPosition()];
                    String color = COLOR_HEX[spColor.getSelectedItemPosition()];

                    if (existing == null) {
                        dao.insert(new Category(0, name, type, icon, color));
                        Toast.makeText(this, "Đã thêm danh mục", Toast.LENGTH_SHORT).show();
                    } else {
                        existing.setName(name);
                        existing.setType(type);
                        existing.setIcon(icon);
                        existing.setColor(color);
                        dao.update(existing);
                        Toast.makeText(this, "Đã cập nhật danh mục", Toast.LENGTH_SHORT).show();
                    }
                    loadData();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void confirmDelete(Category c) {
        // Phân quyền: chỉ admin được xóa
        if (!isAdmin) {
            Toast.makeText(this, "Chỉ quản trị viên mới được xóa danh mục", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Xóa danh mục")
                .setMessage("Xóa danh mục \"" + c.getName() + "\"?")
                .setPositiveButton("Xóa", (d, w) -> {
                    dao.delete(c.getId());
                    loadData();
                    Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
