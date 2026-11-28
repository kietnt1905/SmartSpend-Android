package com.vhu.smartspend.utils;

import java.text.DecimalFormat;

public class CurrencyUtils {

    /**
     * Định dạng số tiền VNĐ (ví dụ: "50,000 đ").
     */
    public static String formatVND(double amount) {
        return new DecimalFormat("#,### đ").format(amount);
    }
}
