package com.vhu.smartspend.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vhu.smartspend.R;
import com.vhu.smartspend.model.Budget;
import com.vhu.smartspend.utils.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter Hiển thị Tiến độ Ngân sách (BudgetProgressAdapter)
 * Tự động tính tỷ lệ phần trăm % chi tiêu cho từng danh mục
 * và tự động thay đổi màu sắc thanh ProgressBar theo 3 cấp độ cảnh báo.
 *
 * Tác giả: Lê Phúc Thịnh (MSSV: 231A290032)
 */
public class BudgetProgressAdapter extends RecyclerView.Adapter<BudgetProgressAdapter.ViewHolder> {

    private final Context context;
    private List<Budget> budgetList;

    public BudgetProgressAdapter(Context context, List<Budget> budgetList) {
        this.context = context;
        this.budgetList = budgetList != null ? budgetList : new ArrayList<>();
    }

    public void setBudgetList(List<Budget> budgetList) {
        this.budgetList = budgetList != null ? budgetList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget_progress, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Budget budget = budgetList.get(position);

        // 1. Hiển thị thông tin Tên & Icon danh mục
        String categoryName = budget.getCategoryName() != null ? budget.getCategoryName() : "Danh mục khác";
        holder.tvCategoryName.setText(categoryName);

        String iconEmoji = getEmojiForCategory(categoryName, budget.getCategoryIconName());
        holder.tvCategoryIcon.setText(iconEmoji);

        int categoryColor = parseCategoryColor(budget.getCategoryColorCode());
        setCircularBackground(holder.tvCategoryIcon, categoryColor);

        // 2. Tính toán Số tiền Đã chi & Hạn mức
        double spent = budget.getSpentAmount();
        double limit = budget.getLimitAmount();

        String spentStr = CurrencyUtils.formatVND(spent);
        String limitStr = limit > 0 ? CurrencyUtils.formatVND(limit) : "Chưa cài hạn mức";
        holder.tvSpentAndLimit.setText(spentStr + " / " + limitStr);

        // 3. Tính tỷ lệ % và Xác định màu sắc ProgressBar theo 3 cấp độ
        int percentage = 0;
        int progressColor;
        String statusText;

        if (limit > 0) {
            percentage = (int) Math.round((spent / limit) * 100);
            holder.tvPercentage.setText(percentage + "%");

            int alertThreshold = budget.getAlertPercent() > 0 ? budget.getAlertPercent() : 80;

            if (percentage >= 100) {
                // Cấp 3: Đỏ - Vượt ngân sách (> 100%)
                progressColor = Color.parseColor("#D32F2F");
                statusText = "🚨 Vượt ngân sách (" + percentage + "%)";
            } else if (percentage >= alertThreshold) {
                // Cấp 2: Cam - Cảnh báo chạm ngưỡng (80% - 100%)
                progressColor = Color.parseColor("#FF6D00");
                statusText = "⚠️ Cảnh báo chi tiêu (Đạt " + percentage + "%)";
            } else {
                // Cấp 1: Xanh lá - An toàn (< 80%)
                progressColor = Color.parseColor("#0B5A4B");
                statusText = "✓ An toàn (Dưới " + alertThreshold + "%)";
            }
        } else {
            // Trường hợp chưa cài hạn mức giới hạn
            holder.tvPercentage.setText("--%");
            progressColor = Color.parseColor("#1E88E5");
            statusText = "ℹ️ Chưa thiết lập hạn mức";
        }

        holder.tvPercentage.setTextColor(progressColor);
        holder.tvStatusMessage.setText(statusText);
        holder.tvStatusMessage.setTextColor(progressColor);

        // 4. Cập nhật thanh ProgressBar tiến độ và Đổi màu tự động
        int displayProgress = Math.min(percentage, 100);
        holder.progressBarBudget.setProgress(displayProgress);
        holder.progressBarBudget.setProgressTintList(ColorStateList.valueOf(progressColor));
    }

    @Override
    public int getItemCount() {
        return budgetList != null ? budgetList.size() : 0;
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Tạo hình nền tròn có viền màu theo danh mục
     */
    private void setCircularBackground(View view, int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        // Làm nhạt màu nền icon (alpha ~ 15%) và dùng màu danh mục làm viền
        int alphaColor = Color.argb(40, Color.red(color), Color.green(color), Color.blue(color));
        drawable.setColor(alphaColor);
        float density = view.getResources().getDisplayMetrics().density;
        drawable.setStroke(Math.round(2 * density), color);
        view.setBackground(drawable);
    }

    /**
     * Chuyển đổi mã màu Hex từ DB
     */
    private int parseCategoryColor(String colorCode) {
        if (colorCode != null && !colorCode.trim().isEmpty()) {
            try {
                return Color.parseColor(colorCode.trim());
            } catch (Exception ignored) {
            }
        }
        return Color.parseColor("#0B5A4B"); // Màu mặc định
    }

    /**
     * Mapping Emoji cho icon danh mục (20 icon chuẩn)
     */
    private String getEmojiForCategory(String name, String iconName) {
        if (iconName != null) {
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
            }
        }
        if (name == null) return "⭐";
        String lowerName = name.toLowerCase(Locale.getDefault());

        if (lowerName.contains("ăn") || lowerName.contains("uống") || lowerName.contains("nhà hàng")) return "🍔";
        if (lowerName.contains("hóa đơn") || lowerName.contains("điện") || lowerName.contains("nước")) return "⚡";
        if (lowerName.contains("mua sắm") || lowerName.contains("tiêu dùng")) return "🛒";
        if (lowerName.contains("di chuyển") || lowerName.contains("đi lại") || lowerName.contains("xe")) return "🚗";
        if (lowerName.contains("học") || lowerName.contains("trường")) return "🎓";
        if (lowerName.contains("giải trí") || lowerName.contains("game")) return "🎮";
        if (lowerName.contains("lương") || lowerName.contains("thu nhập")) return "💰";

        return "⭐";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCategoryIcon;
        final TextView tvCategoryName;
        final TextView tvSpentAndLimit;
        final TextView tvPercentage;
        final TextView tvStatusMessage;
        final ProgressBar progressBarBudget;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryIcon = itemView.findViewById(R.id.tvCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvSpentAndLimit = itemView.findViewById(R.id.tvSpentAndLimit);
            tvPercentage = itemView.findViewById(R.id.tvPercentage);
            tvStatusMessage = itemView.findViewById(R.id.tvStatusMessage);
            progressBarBudget = itemView.findViewById(R.id.progressBarBudget);
        }
    }
}
