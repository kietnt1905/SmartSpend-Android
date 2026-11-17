package com.vhu.smartspend.utils;

import java.text.DecimalFormat;

public class CurrencyUtils {

    private static final String VND_PATTERN = "#,### đ";

    /**
     * Định dạng số tiền sang chuẩn tiền tệ Việt Nam (VND).
     * Sử dụng DecimalFormat("#,### đ") để đồng nhất định dạng trên toàn app.
     *
     * @param amount Số tiền (double)
     * @return Chuỗi định dạng ví dụ: "50,000 đ"
     */
    public static String formatVND(double amount) {
        DecimalFormat decimalFormat = new DecimalFormat(VND_PATTERN);
        return decimalFormat.format(amount);
    }

    /**
     * Overload hỗ trợ kiểu long
     */
    public static String formatVND(long amount) {
        return formatVND((double) amount);
    }
}
