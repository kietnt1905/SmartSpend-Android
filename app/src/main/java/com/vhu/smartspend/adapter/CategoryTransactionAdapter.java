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
import com.vhu.smartspend.model.Transaction;
import com.vhu.smartspend.utils.CategoryUiHelper;
import com.vhu.smartspend.utils.CurrencyUtils;
import com.vhu.smartspend.utils.DateUtils;

import java.util.List;

/**
 * Adapter hiển thị danh sách giao dịch gom theo ngày trong ListView.
 */
public class CategoryTransactionAdapter extends BaseAdapter {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public static class RowItem {
        public final boolean isHeader;
        public final String dateLabel;
        public final double dayTotal;
        public final Transaction transaction;

        public RowItem(String dateLabel, double dayTotal) {
            this.isHeader = true;
            this.dateLabel = dateLabel;
            this.dayTotal = dayTotal;
            this.transaction = null;
        }

        public RowItem(Transaction transaction) {
            this.isHeader = false;
            this.dateLabel = null;
            this.dayTotal = 0;
            this.transaction = transaction;
        }
    }

    private final Context context;
    private final List<RowItem> rows;
    private final String defaultCategoryName;
    private final boolean isExpenseMode;

    public CategoryTransactionAdapter(Context context, List<RowItem> rows,
                                      String defaultCategoryName, boolean isExpenseMode) {
        this.context = context;
        this.rows = rows;
        this.defaultCategoryName = defaultCategoryName;
        this.isExpenseMode = isExpenseMode;
    }

    @Override
    public int getCount() {
        return rows != null ? rows.size() : 0;
    }

    @Override
    public RowItem getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).isHeader ? TYPE_HEADER : TYPE_ITEM;
    }

    @Override
    public boolean isEnabled(int position) {
        return !rows.get(position).isHeader;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RowItem item = rows.get(position);
        LayoutInflater inflater = LayoutInflater.from(context);

        if (item.isHeader) {
            View view = convertView != null ? convertView : inflater.inflate(R.layout.item_tx_date_header, parent, false);
            TextView tvDateLabel = view.findViewById(R.id.tvDateGroupLabel);
            TextView tvDateTotal = view.findViewById(R.id.tvDateGroupTotal);

            tvDateLabel.setText("Ngày " + DateUtils.formatToDisplay(item.dateLabel));
            tvDateTotal.setText((isExpenseMode ? "-" : "+") + CurrencyUtils.formatVND(item.dayTotal));
            tvDateTotal.setTextColor(Color.parseColor(isExpenseMode ? "#B91C1C" : "#047857"));
            return view;
        }

        View view = convertView != null ? convertView : inflater.inflate(R.layout.item_recent_transaction, parent, false);
        view.setClickable(false);
        view.setFocusable(false);
        if (view instanceof MaterialCardView) {
            ((MaterialCardView) view).setCardBackgroundColor(Color.WHITE);
        }

        Transaction tx = item.transaction;
        if (tx == null) return view;
        boolean txIncome = Category.TYPE_INCOME.equalsIgnoreCase(tx.getCategoryType());

        TextView tvIcon = view.findViewById(R.id.tvTxIcon);
        TextView tvTitle = view.findViewById(R.id.tvTxTitle);
        TextView tvSub = view.findViewById(R.id.tvTxSub);
        TextView tvAmount = view.findViewById(R.id.tvTxAmount);

        int borderColor = CategoryUiHelper.parseColor(tx.getCategoryColorCode(), txIncome ? "#0A5C36" : "#C62828");
        tvIcon.setBackground(CategoryUiHelper.createCircleBg(context, borderColor, 2));
        tvIcon.setText(CategoryUiHelper.getEmoji(tx.getCategoryIconName()));

        String title = defaultCategoryName;
        if (tx.getNote() != null && !tx.getNote().trim().isEmpty()) {
            title = tx.getNote();
        } else if (tx.getTitle() != null && !tx.getTitle().trim().isEmpty()) {
            title = tx.getTitle();
        }
        tvTitle.setText(title);
        tvSub.setText("Chạm để chỉnh sửa • " + DateUtils.formatToDisplay(tx.getTransactionDate()));
        tvSub.setTextColor(Color.parseColor("#0A6847"));

        tvAmount.setText((txIncome ? "+" : "-") + CurrencyUtils.formatVND(tx.getAmount()));
        tvAmount.setTextColor(Color.parseColor(txIncome ? "#047857" : "#B91C1C"));
        tvAmount.setBackgroundResource(txIncome ? R.drawable.bg_tx_amount_income : R.drawable.bg_tx_amount_expense);

        return view;
    }
}
