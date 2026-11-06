package dev.lunarlite.gui.oneplus;

import dev.lunarlite.LunarLite;
import dev.lunarlite.module.ThemeModule;

/**
 * Thông số giao diện OnePlus Crystal. Giá trị người chơi chỉnh nằm trong module "Màu giao diện"
 * (tự lưu vào config); lớp này chỉ là cửa sổ đọc gọn cho các renderer.
 */
public final class OnePlusTheme {
    private OnePlusTheme() {}

    // giá trị khởi đầu theo đặc tả
    public static final float DEF_GLASS_ALPHA = 0.65f;
    public static final float DEF_BORDER_ALPHA = 0.30f;
    public static final int DEF_CORNER_RADIUS = 20;
    public static final int DEF_SHADOW_BLUR = 14;
    public static final int DEF_BLUR_RADIUS = 20;

    /** Logo OnePlus đỏ. */
    public static final int RED = 0xFFEB0028;
    /** Màu sữa của kính (RGB). */
    public static final int GLASS_TINT = 0xFFFFFF;
    /** Màu bóng đổ (RGB). */
    public static final int SHADOW_RGB = 0x0B1A33;

    public static float glassAlpha() { return ThemeModule.glassAlpha(); }
    public static float borderAlpha() { return ThemeModule.borderAlpha(); }
    public static int cornerRadius() { return ThemeModule.cornerRadius(); }
    public static int shadowBlur() { return ThemeModule.shadowBlur(); }
    public static int blurRadius() { return ThemeModule.blurRadius(); }
    /** 0 = thấp (1/4 độ phân giải), 1 = cao (1/2 độ phân giải). */
    public static int blurQuality() { return ThemeModule.blurQuality(); }
    public static boolean blurEnabled() { return LunarLite.menuBlur; }
}
