package com.vhu.smartspend.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vhu.smartspend.model.Category;

import java.util.List;

public class CategoryGridAdapter extends BaseAdapter {
    private final Context context;
    private List<Category> categoryList;

    public CategoryGridAdapter(Context context, List<Category> categoryList) {
        this.context = context;
        this.categoryList = categoryList;
    }

    public void updateData(List<Category> newList) {
        this.categoryList = newList;
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
            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setGravity(Gravity.CENTER);

            int paddingPx = dpToPx(12);
            root.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

            TextView tvIconCircle = new TextView(context);
            int circleSize = dpToPx(50);
            LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(circleSize, circleSize);
            circleParams.bottomMargin = dpToPx(8);
            tvIconCircle.setLayoutParams(circleParams);
            tvIconCircle.setGravity(Gravity.CENTER);
            tvIconCircle.setTextColor(Color.WHITE);
            tvIconCircle.setTextSize(20);

            TextView tvName = new TextView(context);
            tvName.setGravity(Gravity.CENTER);
            tvName.setTextSize(14);
            tvName.setTextColor(Color.BLACK);
            tvName.setMaxLines(1);

            TextView tvType = new TextView(context);
            tvType.setGravity(Gravity.CENTER);
            tvType.setTextSize(11);
            tvType.setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2));

            root.addView(tvIconCircle);
            root.addView(tvName);
            root.addView(tvType);

            holder = new ViewHolder();
            holder.root = root;
            holder.tvIconCircle = tvIconCircle;
            holder.tvName = tvName;
            holder.tvType = tvType;

            convertView = root;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Category category = categoryList.get(position);
        if (category != null) {
            holder.tvName.setText(category.getName());

            int bgColor = Color.parseColor("#0A5C36");
            if (category.getColorCode() != null && !category.getColorCode().trim().isEmpty()) {
                try {
                    bgColor = Color.parseColor(category.getColorCode());
                } catch (Exception ignored) {
                }
            }

            GradientDrawable circleDrawable = new GradientDrawable();
            circleDrawable.setShape(GradientDrawable.OVAL);
            circleDrawable.setColor(bgColor);
            holder.tvIconCircle.setBackground(circleDrawable);

            holder.tvIconCircle.setText(getIconSymbol(category.getIconName(), category.getName()));

            GradientDrawable cardDrawable = new GradientDrawable();
            cardDrawable.setCornerRadius(dpToPx(12));
            cardDrawable.setColor(Color.WHITE);
            cardDrawable.setStroke(dpToPx(1), Color.parseColor("#E0E0E0"));
            holder.root.setBackground(cardDrawable);

            if (Category.TYPE_INCOME.equalsIgnoreCase(category.getType())) {
                holder.tvType.setText("Thu nhập");
                holder.tvType.setTextColor(Color.parseColor("#2E7D32"));
            } else {
                holder.tvType.setText("Chi tiêu");
                holder.tvType.setTextColor(Color.parseColor("#C62828"));
            }
        }

        return convertView;
    }

    private String getIconSymbol(String iconName, String categoryName) {
        if (iconName != null) {
            switch (iconName.toLowerCase()) {
                case "ic_food":
                    return "🍔";
                case "ic_school":
                    return "📚";
                case "ic_shopping":
                    return "🛒";
                case "ic_transport":
                    return "🚗";
                case "ic_award":
                    return "🎓";
                case "ic_work":
                    return "💼";
            }
        }
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            return categoryName.trim().substring(0, 1).toUpperCase();
        }
        return "★";
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static class ViewHolder {
        LinearLayout root;
        TextView tvIconCircle;
        TextView tvName;
        TextView tvType;
    }
}