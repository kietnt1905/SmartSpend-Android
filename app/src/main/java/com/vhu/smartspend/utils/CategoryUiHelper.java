package com.vhu.smartspend.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

/**
 * Lớp hỗ trợ dùng chung cho icon, màu sắc và nền bo tròn của danh mục.
 */
public class CategoryUiHelper {

    public static final String[] ICON_NAMES = {
            "ic_food", "ic_coffee", "ic_school", "ic_shopping", "ic_transport",
            "ic_home", "ic_bill", "ic_entertainment", "ic_health", "ic_beauty",
            "ic_travel", "ic_pet", "ic_sport", "ic_phone", "ic_gift",
            "ic_award", "ic_work", "ic_savings", "ic_invest", "ic_other"
    };

    public static final String[] ICON_EMOJIS = {
            "🍔", "☕", "📚", "🛒", "🚗",
            "🏠", "⚡", "🎮", "💊", "💄",
            "✈️", "🐾", "🏋️", "📱", "🎁",
            "🎓", "💼", "💰", "📈", "⭐"
    };

    public static final String[] COLORS = {
            "#0A5C36", "#FF5722", "#2196F3", "#9C27B0",
            "#009688", "#4CAF50", "#FFC107", "#E91E63",
            "#3F51B5", "#FF9800", "#795548", "#00BCD4"
    };

    public static String getEmoji(String iconName) {
        if (iconName != null) {
            for (int i = 0; i < ICON_NAMES.length; i++) {
                if (ICON_NAMES[i].equalsIgnoreCase(iconName)) {
                    return ICON_EMOJIS[i];
                }
            }
        }
        return "⭐";
    }

    public static int parseColor(String colorCode, String fallbackHex) {
        if (colorCode != null && !colorCode.trim().isEmpty()) {
            try {
                return Color.parseColor(colorCode.trim());
            } catch (Exception ignored) {
            }
        }
        return Color.parseColor(fallbackHex);
    }

    public static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static GradientDrawable createCircleBg(Context context, int borderColor, int strokeDp) {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(withAlpha(borderColor, 36));
        circle.setStroke(dp(context, strokeDp), borderColor);
        return circle;
    }

    public static GradientDrawable createRoundedBg(Context context, int fillColor, int radiusDp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fillColor);
        bg.setCornerRadius(dp(context, radiusDp));
        return bg;
    }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
