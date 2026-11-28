package com.vhu.smartspend.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.vhu.smartspend.R;
import com.vhu.smartspend.model.Category;
import com.vhu.smartspend.utils.CategoryUiHelper;

import java.util.List;

public class CategoryGridAdapter extends BaseAdapter {
    private final Context context;
    private List<Category> categoryList;
    private int selectedCategoryId = -1;

    public CategoryGridAdapter(Context context, List<Category> categoryList) {
        this.context = context;
        this.categoryList = categoryList;
    }

    public void updateData(List<Category> newList) {
        this.categoryList = newList;
        notifyDataSetChanged();
    }

    public void setSelectedCategoryId(int categoryId) {
        this.selectedCategoryId = categoryId;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return categoryList != null ? categoryList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return categoryList != null ? categoryList.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return categoryList != null ? categoryList.get(position).getCategoryId() : 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_category_grid, parent, false);
        }

        Category category = categoryList.get(position);
        if (category == null) return convertView;

        MaterialCardView card = convertView.findViewById(R.id.cardCategoryItem);
        TextView tvIcon = convertView.findViewById(R.id.tvCategoryIconCircle);
        TextView tvName = convertView.findViewById(R.id.tvCategoryName);
        TextView tvType = convertView.findViewById(R.id.tvCategoryType);

        int borderColor = CategoryUiHelper.parseColor(category.getColorCode(), "#0A5C36");
        tvName.setText(category.getName());
        tvIcon.setBackground(CategoryUiHelper.createCircleBg(context, borderColor, 2));
        tvIcon.setText(CategoryUiHelper.getEmoji(category.getIconName()));

        boolean isSelected = category.getCategoryId() == selectedCategoryId;
        card.setCardBackgroundColor(isSelected ? CategoryUiHelper.withAlpha(borderColor, 28) : Color.WHITE);
        card.setStrokeWidth(CategoryUiHelper.dp(context, isSelected ? 2 : 1));
        card.setStrokeColor(isSelected ? borderColor : CategoryUiHelper.withAlpha(borderColor, 90));

        boolean isIncome = Category.TYPE_INCOME.equalsIgnoreCase(category.getType());
        tvType.setBackgroundResource(isIncome ? R.drawable.bg_cat_type_income : R.drawable.bg_cat_type_expense);
        tvType.setText(isIncome ? "Thu nhập" : "Chi tiêu");
        tvType.setTextColor(Color.parseColor(isIncome ? "#15803D" : "#B91C1C"));

        return convertView;
    }
}
