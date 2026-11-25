package com.vhu.smartspend.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vhu.smartspend.R;
import com.vhu.smartspend.database.BudgetDAO.CategorySummary;
import com.vhu.smartspend.utils.CategoryUiHelper;
import com.vhu.smartspend.utils.CurrencyUtils;

import java.util.List;

/**
 * Adapter hiển thị danh sách thống kê từng danh mục trong StatisticFragment.
 */
public class StatisticCategoryAdapter extends RecyclerView.Adapter<StatisticCategoryAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onClick(CategorySummary item);
    }

    private final Context context;
    private final List<CategorySummary> list;
    private final double totalAmount;
    private final boolean isExpenseMode;
    private final OnCategoryClickListener listener;

    public StatisticCategoryAdapter(Context context, List<CategorySummary> list,
                                    double totalAmount, boolean isExpenseMode,
                                    OnCategoryClickListener listener) {
        this.context = context;
        this.list = list;
        this.totalAmount = totalAmount;
        this.isExpenseMode = isExpenseMode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_statistic_category, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        CategorySummary item = list.get(position);
        int percent = totalAmount > 0 ? (int) Math.round((item.totalAmount * 100.0) / totalAmount) : 0;
        int color = CategoryUiHelper.parseColor(item.colorCode, isExpenseMode ? "#D32F2F" : "#0B8043");

        h.tvIcon.setText(CategoryUiHelper.getEmoji(item.iconName));
        h.tvIcon.setBackground(CategoryUiHelper.createCircleBg(context, color, 2));
        h.tvTitle.setText(item.name);
        h.tvSub.setText(item.transactionCount + " giao dịch");

        h.tvAmount.setText((isExpenseMode ? "-" : "+") + CurrencyUtils.formatVND(item.totalAmount));
        h.tvAmount.setTextColor(Color.parseColor(isExpenseMode ? "#B91C1C" : "#047857"));

        h.tvPercent.setText(percent + "%");
        h.tvPercent.setTextColor(color);
        h.tvPercent.setBackground(CategoryUiHelper.createRoundedBg(
                context, CategoryUiHelper.withAlpha(color, 40), 8));

        h.pb.setProgress(percent);
        h.pb.setProgressTintList(ColorStateList.valueOf(color));
        h.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvIcon, tvTitle, tvSub, tvAmount, tvPercent;
        final ProgressBar pb;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvStatCatIcon);
            tvTitle = itemView.findViewById(R.id.tvStatCatTitle);
            tvSub = itemView.findViewById(R.id.tvStatCatSub);
            tvAmount = itemView.findViewById(R.id.tvStatCatAmount);
            tvPercent = itemView.findViewById(R.id.tvStatCatPercent);
            pb = itemView.findViewById(R.id.pbStatCategory);
        }
    }
}
