package com.vhu.smartspend.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

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
            tvIconCircle.setTextColor(ContextCompat.getColor(context, R.color.setting_text_primary));
            tvIconCircle.setTextSize(20);

            TextView tvName = new TextView(context);
            tvName.setGravity(Gravity.CENTER);
            tvName.setTextSize(13.5f);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            tvName.setMaxLines(1);
            tvName.setEllipsize(TextUtils.TruncateAt.END);

            TextView tvType = new TextView(context);
            tvType.setGravity(Gravity.CENTER);
            tvType.setTextSize(10.5f);
            tvType.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            typeParams.topMargin = dpToPx(5);
            tvType.setLayoutParams(typeParams);
            tvType.setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2));

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
            holder.tvName.setTextColor(Color.parseColor("#0F172A"));

            int borderColor = Color.parseColor("#0A5C36");
            if (category.getColorCode() != null && !category.getColorCode().trim().isEmpty()) {
                try {
                    borderColor = Color.parseColor(category.getColorCode());
                } catch (Exception ignored) {
                }
            }

            int softTint = Color.argb(32, Color.red(borderColor), Color.green(borderColor), Color.blue(borderColor));

            GradientDrawable circleDrawable = new GradientDrawable();
            circleDrawable.setShape(GradientDrawable.OVAL);
            circleDrawable.setColor(softTint);
            circleDrawable.setStroke(dpToPx(2), borderColor);
            holder.tvIconCircle.setBackground(circleDrawable);

            holder.tvIconCircle.setText(getIconSymbol(category.getIconName(), category.getName()));

            boolean isSelected = category.getCategoryId() == selectedCategoryId;
            GradientDrawable cardDrawable = new GradientDrawable();
            cardDrawable.setCornerRadius(dpToPx(14));
            if (isSelected) {
                cardDrawable.setColor(Color.argb(28, Color.red(borderColor), Color.green(borderColor), Color.blue(borderColor)));
                cardDrawable.setStroke(dpToPx(2), borderColor);
            } else {
                cardDrawable.setColor(Color.WHITE);
                cardDrawable.setStroke(dpToPx(1), Color.argb(90, Color.red(borderColor), Color.green(borderColor), Color.blue(borderColor)));
            }
            holder.root.setBackground(cardDrawable);

            boolean isIncome = Category.TYPE_INCOME.equalsIgnoreCase(category.getType());
            GradientDrawable typePill = new GradientDrawable();
            typePill.setCornerRadius(dpToPx(8));
            typePill.setColor(Color.parseColor(isIncome ? "#DCFCE7" : "#FEE2E2"));
            holder.tvType.setBackground(typePill);
            if (isIncome) {
                holder.tvType.setText("Thu nhập");
                holder.tvType.setTextColor(Color.parseColor("#15803D"));
            } else {
                holder.tvType.setText("Chi tiêu");
                holder.tvType.setTextColor(Color.parseColor("#B91C1C"));
            }
        }

        return convertView;
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
        LinearLayout root;
        TextView tvIconCircle;
        TextView tvName;
        TextView tvType;
    }
}
