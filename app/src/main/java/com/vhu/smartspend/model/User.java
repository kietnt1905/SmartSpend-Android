package com.vhu.smartspend.model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private String password;
    private String fullName;
    private String role;
    private double monthlyBudgetLimit;

    public User() {
        this.role = "USER";
        this.monthlyBudgetLimit = 5000000;
    }

    public User(String username, String password, String fullName, String role, double monthlyBudgetLimit) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.monthlyBudgetLimit = monthlyBudgetLimit;
    }

    public User(int userId, String username, String password, String fullName, String role, double monthlyBudgetLimit) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.monthlyBudgetLimit = monthlyBudgetLimit;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public double getMonthlyBudgetLimit() {
        return monthlyBudgetLimit;
    }

    public void setMonthlyBudgetLimit(double monthlyBudgetLimit) {
        this.monthlyBudgetLimit = monthlyBudgetLimit;
    }
}
