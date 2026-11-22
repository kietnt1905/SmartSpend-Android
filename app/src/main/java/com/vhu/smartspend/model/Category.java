package com.vhu.smartspend.model;

import java.io.Serializable;

public class Category implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TYPE_EXPENSE = "EXPENSE";
    public static final String TYPE_INCOME = "INCOME";

    private int categoryId;
    private int userId;
    private String name;
    private String type;
    private String iconName;
    private String colorCode;

    public Category() {
    }

    public Category(String name, String type, String iconName, String colorCode) {
        this.name = name;
        this.type = type;
        this.iconName = iconName;
        this.colorCode = colorCode;
    }

    public Category(int userId, String name, String type, String iconName, String colorCode) {
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.iconName = iconName;
        this.colorCode = colorCode;
    }

    public Category(int categoryId, int userId, String name, String type, String iconName, String colorCode) {
        this.categoryId = categoryId;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.iconName = iconName;
        this.colorCode = colorCode;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }
}
