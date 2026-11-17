package com.vhu.smartspend.database;

import android.content.Context;

public class TransactionDAO {
    private final DatabaseHelper helper;

    public TransactionDAO(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    // TODO: Triển khai các phương thức CRUD cho bảng Transaction
}
