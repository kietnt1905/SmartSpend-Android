package com.vhu.smartspend.model;

import java.io.Serializable;

public class Budget implements Serializable {
    private static final long serialVersionUID = 1L;

    private int budgetId;
    private int userId;
    private Integer categoryId; // Có thể null nếu là ngân sách tổng tháng
    private String monthYear;   // Định dạng yyyy-MM (ví dụ: 2026-11)
    private double limitAmount;
    private int alertPercent;

    // Trường mở rộng phục vụ hiển thị tiến độ ngân sách trên UI
    private String categoryName;
    private String categoryIconName;
    private String categoryColorCode;
    private double spentAmount;

    public Budget() {
        this.alertPercent = 80;
    }

    public Budget(int userId, Integer categoryId, String monthYear, double limitAmount, int alertPercent) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.monthYear = monthYear;
        this.limitAmount = limitAmount;
        this.alertPercent = alertPercent;
    }

    public Budget(int budgetId, int userId, Integer categoryId, String monthYear, double limitAmount, int alertPercent) {
        this.budgetId = budgetId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.monthYear = monthYear;
        this.limitAmount = limitAmount;
        this.alertPercent = alertPercent;
    }

    public int getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(int budgetId) {
        this.budgetId = budgetId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(String monthYear) {
        this.monthYear = monthYear;
    }

    public double getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(double limitAmount) {
        this.limitAmount = limitAmount;
    }

    public int getAlertPercent() {
        return alertPercent;
    }

    public void setAlertPercent(int alertPercent) {
        this.alertPercent = alertPercent;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryIconName() {
        return categoryIconName;
    }

    public void setCategoryIconName(String categoryIconName) {
        this.categoryIconName = categoryIconName;
    }

    public String getCategoryColorCode() {
        return categoryColorCode;
    }

    public void setCategoryColorCode(String categoryColorCode) {
        this.categoryColorCode = categoryColorCode;
    }

    public double getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(double spentAmount) {
        this.spentAmount = spentAmount;
    }
}
