package com.vhu.smartspend.model;

public class Budget {
    private int budgetId;
    private int userId;
    private Integer categoryId;
    private String monthYear;
    private double limitAmount;
    private int alertPercent;

    public Budget(int userId, Integer categoryId, String monthYear, double limitAmount, int alertPercent) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.monthYear = monthYear;
        this.limitAmount = limitAmount;
        this.alertPercent = alertPercent;
    }

    public Budget(int budgetId, int userId, Integer categoryId, String monthYear, double limitAmount, int alertPercent) {
        this(userId, categoryId, monthYear, limitAmount, alertPercent);
        this.budgetId = budgetId;
    }

    public int getBudgetId() {
        return budgetId;
    }

    public int getUserId() {
        return userId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public double getLimitAmount() {
        return limitAmount;
    }

    public int getAlertPercent() {
        return alertPercent;
    }
}
