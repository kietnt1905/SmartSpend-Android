package com.vhu.smartspend.model;

import java.io.Serializable;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private int transactionId;
    private int userId;
    private int categoryId;
    private String title;
    private double amount;
    private String transactionDate;
    private String note;
    private String receiptUri;

    // Trường mở rộng phục vụ hiển thị khi JOIN với bảng categories
    private String categoryName;
    private String categoryType;
    private String categoryIconName;
    private String categoryColorCode;

    public Transaction() {
    }

    public Transaction(int userId, int categoryId, String title, double amount,
                       String transactionDate, String note, String receiptUri) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.title = title;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.note = note;
        this.receiptUri = receiptUri;
    }

    public Transaction(int transactionId, int userId, int categoryId, String title, double amount,
                       String transactionDate, String note, String receiptUri) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.title = title;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.note = note;
        this.receiptUri = receiptUri;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getReceiptUri() {
        return receiptUri;
    }

    public void setReceiptUri(String receiptUri) {
        this.receiptUri = receiptUri;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryType() {
        return categoryType;
    }

    public void setCategoryType(String categoryType) {
        this.categoryType = categoryType;
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
}
