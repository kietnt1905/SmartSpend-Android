package com.vhu.smartspend.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    public static final String DB_DATE_FORMAT = "yyyy-MM-dd";
    public static final String UI_DATE_FORMAT = "dd/MM/yyyy";

    /**
     * Lấy ngày hiện tại theo chuẩn lưu SQLite (yyyy-MM-dd)
     */
    public static String getCurrentDateDb() {
        SimpleDateFormat sdf = new SimpleDateFormat(DB_DATE_FORMAT, Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Lấy ngày hiện tại theo chuẩn hiển thị giao diện (dd/MM/yyyy)
     */
    public static String getCurrentDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat(UI_DATE_FORMAT, Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Chuyển từ định dạng SQLite (yyyy-MM-dd) sang định dạng hiển thị UI (dd/MM/yyyy)
     * Ví dụ: "2026-11-14" -> "14/11/2026"
     */
    public static String formatToDisplay(String dbDate) {
        if (dbDate == null || dbDate.trim().isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat dbSdf = new SimpleDateFormat(DB_DATE_FORMAT, Locale.getDefault());
            SimpleDateFormat uiSdf = new SimpleDateFormat(UI_DATE_FORMAT, Locale.getDefault());
            Date date = dbSdf.parse(dbDate.trim());
            return (date != null) ? uiSdf.format(date) : dbDate;
        } catch (ParseException e) {
            return dbDate;
        }
    }

    /**
     * Chuyển từ định dạng UI (dd/MM/yyyy) sang định dạng SQLite (yyyy-MM-dd)
     * Ví dụ: "14/11/2026" -> "2026-11-14"
     */
    public static String formatToDb(String displayDate) {
        if (displayDate == null || displayDate.trim().isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat uiSdf = new SimpleDateFormat(UI_DATE_FORMAT, Locale.getDefault());
            SimpleDateFormat dbSdf = new SimpleDateFormat(DB_DATE_FORMAT, Locale.getDefault());
            Date date = uiSdf.parse(displayDate.trim());
            return (date != null) ? dbSdf.format(date) : displayDate;
        } catch (ParseException e) {
            return displayDate;
        }
    }

    /**
     * Format đối tượng Date sang chuỗi yyyy-MM-dd để lưu SQLite
     */
    public static String toDbString(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DB_DATE_FORMAT, Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * Format đối tượng Date sang chuỗi dd/MM/yyyy để hiển thị giao diện
     */
    public static String toDisplayString(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(UI_DATE_FORMAT, Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * Parse chuỗi yyyy-MM-dd từ SQLite thành đối tượng Date
     */
    public static Date parseDbDate(String dbDate) {
        if (dbDate == null || dbDate.trim().isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DB_DATE_FORMAT, Locale.getDefault());
            return sdf.parse(dbDate.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Parse chuỗi dd/MM/yyyy từ UI thành đối tượng Date
     */
    public static Date parseDisplayDate(String displayDate) {
        if (displayDate == null || displayDate.trim().isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(UI_DATE_FORMAT, Locale.getDefault());
            return sdf.parse(displayDate.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Tạo chuỗi tiền tố tháng năm yyyy-MM (ví dụ: "2026-11") để Thịnh truy vấn LIKE '2026-11-%'
     *
     * @param year Năm (ví dụ: 2026)
     * @param month Tháng (1 - 12)
     */
    public static String getMonthPrefix(int year, int month) {
        return String.format(Locale.getDefault(), "%04d-%02d", year, month);
    }
}
