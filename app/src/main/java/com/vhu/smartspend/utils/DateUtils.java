package com.vhu.smartspend.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    private static final String DB_FORMAT = "yyyy-MM-dd";
    private static final String UI_FORMAT = "dd/MM/yyyy";

    /**
     * Chuyển chuỗi ngày từ SQLite (yyyy-MM-dd) sang hiển thị (dd/MM/yyyy).
     */
    public static String formatToDisplay(String dbDate) {
        Date date = parseDbDate(dbDate);
        return date != null ? toDisplayString(date) : (dbDate != null ? dbDate : "");
    }

    public static String toDbString(Date date) {
        if (date == null) return "";
        return new SimpleDateFormat(DB_FORMAT, Locale.getDefault()).format(date);
    }

    public static String toDisplayString(Date date) {
        if (date == null) return "";
        return new SimpleDateFormat(UI_FORMAT, Locale.getDefault()).format(date);
    }

    public static Date parseDbDate(String dbDate) {
        if (dbDate == null || dbDate.trim().isEmpty()) return null;
        try {
            return new SimpleDateFormat(DB_FORMAT, Locale.getDefault()).parse(dbDate.trim());
        } catch (ParseException e) {
            return null;
        }
    }
}
