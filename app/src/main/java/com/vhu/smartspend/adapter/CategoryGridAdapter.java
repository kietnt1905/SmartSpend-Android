package com.vhu.smartspend.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.vhu.smartspend.R;
import com.vhu.smartspend.model.Category;

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
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_category_grid, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Category category = categoryList.get(position);
        if (category != null) {
            holder.tvName.setText(category.getName());

            int borderColor = parseColor(category.getColorCode());
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(Color.argb(32, Color.red(borderColor), Color.green(borderColor), Color.blue(borderColor)));
            circle.setStroke(dpToPx(2), borderColor);
            holder.tvIconCircle.setBackground(circle);
            holder.tvIconCircle.setText(getIconSymbol(category.getIconName(), category.getName()));

            boolean isSelected = category.getCategoryId() == selectedCategoryId;
            holder.card.setCardBackgroundColor(isSelected
                    ? Color.argb(28, Color.red(borderColor), Color.green(borderColor), Color.blue(borderColor))
                    : Color.WHITE);
            holder.card.setStrokeWidth(dpToPx(isSelected ? 2 : 1));
            holder.card.setStrokeColor(isSelected
                    ? borderColor
                    : Color.argb(90, Color.red(borderColor), Color.green(borderColor), Color.blue(borderColor)));

            boolean isIncome = Category.TYPE_INCOME.equalsIgnoreCase(category.getType());
            holder.tvType.setBackgroundResource(isIncome ? R.drawable.bg_cat_type_income : R.drawable.bg_cat_type_expense);
            holder.tvType.setText(isIncome ? "Thu nhập" : "Chi tiêu");
            holder.tvType.setTextColor(Color.parseColor(isIncome ? "#15803D" : "#B91C1C"));
        }

        return convertView;
    }

    private int parseColor(String colorCode) {
        if (colorCode != null && !colorCode.trim().isEmpty()) {
            try {
                return Color.parseColor(colorCode);
            } catch (Exception ignored) {
            }
        }
        return Color.parseColor("#0A5C36");
    }

    private String getIconSymbol(String iconName, String categoryName) {
        if (iconName != null) {
            switch (iconName.toLowerCase()) {
                case "ic_food":
                    return "🍔";
                case "ic_coffee":
                    return "☕";
                case "ic_school":
                    return "📚";
                case "ic_shopping":
                    return "🛒";
                case "ic_transport":
                    return "🚗";
                case "ic_home":
                    return "🏠";
                case "ic_bill":
                    return "⚡";
                case "ic_entertainment":
                    return "🎮";
                case "ic_health":
                    return "💊";
                case "ic_beauty":
                    return "💄";
                case "ic_travel":
                    return "✈️";
                case "ic_pet":
                    return "🐾";
                case "ic_sport":
                    return "🏋️";
                case "ic_phone":
                    return "📱";
                case "ic_gift":
                    return "🎁";
                case "ic_award":
                    return "🎓";
                case "ic_work":
                    return "💼";
                case "ic_savings":
                    return "💰";
                case "ic_invest":
                    return "📈";
                case "ic_other":
                    return "⭐";
            }
        }
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            return categoryName.trim().substring(0, 1).toUpperCase();
        }
        return "⭐";
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static class ViewHolder {
        final MaterialCardView card;
        final TextView tvIconCircle;
        final TextView tvName;
        final TextView tvType;

        ViewHolder(View itemView) {
            card = itemView.findViewById(R.id.cardCategoryItem);
            tvIconCircle = itemView.findViewById(R.id.tvCategoryIconCircle);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvType = itemView.findViewById(R.id.tvCategoryType);
        }
    }
}
