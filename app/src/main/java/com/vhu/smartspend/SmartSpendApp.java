package com.vhu.smartspend;

import android.app.Application;

import com.vhu.smartspend.utils.ThemeHelper;

public class SmartSpendApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper.applySaved(this);
    }
}
