package com.vhu.smartspend.database;

import android.content.Context;

public class BudgetDAO {
    private final DatabaseHelper helper;

    public BudgetDAO(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    // TODO: Triển khai các phương thức CRUD cho bảng Budget
}
