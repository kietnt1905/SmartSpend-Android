package com.vhu.smartspend.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.vhu.smartspend.R;
import com.vhu.smartspend.model.Category;

import java.util.List;

/** Hiển thị danh mục dạng lưới: icon tròn đổi màu theo danh mục + tên + loại thu/chi. */
public class CategoryGridAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater inflater;
    private final List<Category> list;

    public CategoryGridAdapter(Context context, List<Category> list) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.list = list;
    }

    @Override
    public int getCount() { return list.size(); }

    @Override
    public Category getItem(int position) { return list.get(position); }

    @Override
    public long getItemId(int position) { return list.get(position).getId(); }

    private static class ViewHolder {
        TextView tvIcon;
        TextView tvName;
        TextView tvType;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder h;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_category, parent, false);
            h = new ViewHolder();
            h.tvIcon = convertView.findViewById(R.id.tvIcon);
            h.tvName = convertView.findViewById(R.id.tvName);
            h.tvType = convertView.findViewById(R.id.tvType);
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }

        Category c = list.get(position);
        h.tvIcon.setText(c.getIcon());
        h.tvName.setText(c.getName());

        boolean income = Category.TYPE_INCOME.equals(c.getType());
        h.tvType.setText(income ? "Thu" : "Chi");
        int colorRes = income ? R.color.incomeColor : R.color.expenseColor;
        h.tvType.setTextColor(ContextCompat.getColor(context, colorRes));

        // Nền tròn đổi màu theo trường color
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        try {
            bg.setColor(Color.parseColor(c.getColor()));
        } catch (IllegalArgumentException | NullPointerException e) {
            bg.setColor(Color.GRAY);
        }
        h.tvIcon.setBackground(bg);

        return convertView;
    }
}
