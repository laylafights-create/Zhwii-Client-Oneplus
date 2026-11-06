package dev.lunarlite.module;

import dev.lunarlite.gui.Draw;
import dev.lunarlite.setting.ModeSetting;
import dev.lunarlite.setting.SliderSetting;

/**
 * Không có HUD: cho người chơi tự đổi màu nhấn của menu và trình chỉnh HUD.
 * Chọn một bảng màu có sẵn, hoặc chọn "Tùy chỉnh" rồi tự trộn màu bằng 3 thanh Đỏ / Xanh lá / Xanh dương.
 * Màu mới áp dụng ngay khi đang mở menu và được lưu vào config như mọi tùy chỉnh khác.
 */
public class ThemeModule extends Module {
    private static final String CUSTOM = "Tùy chỉnh";
    private static final String[] NAMES = { "Xanh dương", "Tím indigo", "Xanh ngọc", "Hồng neon", "Trắng", "Cam", CUSTOM };
    private static final int[] RGB = { 0x5B9BFF, 0x8B7CFF, 0x2EC4B6, 0xFF6FAE, 0xE6E6E6, 0xFFA94D };

    private static ThemeModule instance;

    private final ModeSetting preset = add(new ModeSetting("preset", "Bảng màu", 0, NAMES), "Màu giao diện");
    private final SliderSetting red = add(new SliderSetting("red", "Đỏ", 0x5B, 0, 255, 1, "") {
        @Override public boolean isVisible() { return preset.is(CUSTOM); }
    }, "Màu giao diện");
    private final SliderSetting green = add(new SliderSetting("green", "Xanh lá", 0x9B, 0, 255, 1, "") {
        @Override public boolean isVisible() { return preset.is(CUSTOM); }
    }, "Màu giao diện");
    private final SliderSetting blue = add(new SliderSetting("blue", "Xanh dương", 0xFF, 0, 255, 1, "") {
        @Override public boolean isVisible() { return preset.is(CUSTOM); }
    }, "Màu giao diện");

    // ---- OnePlus Crystal: kính mờ + blur (giá trị khởi đầu theo đặc tả)
    private static final String GLASS = "Hiệu ứng kính";
    private final SliderSetting glassAlpha = add(new SliderSetting("glassAlpha", "Glass Alpha", 65, 20, 95, 1, "%"), GLASS);
    private final SliderSetting borderAlpha = add(new SliderSetting("borderAlpha", "Border Alpha", 30, 0, 100, 1, "%"), GLASS);
    private final SliderSetting cornerRadius = add(new SliderSetting("cornerRadius", "Corner Radius", 20, 4, 28, 1, "px"), GLASS);
    private final SliderSetting shadowBlur = add(new SliderSetting("shadowBlur", "Shadow Blur", 14, 0, 30, 1, "px"), GLASS);
    private final SliderSetting blurRadius = add(new SliderSetting("blurRadius", "Blur Radius", 20, 4, 40, 1, "px"), GLASS);
    private final ModeSetting blurQuality = add(new ModeSetting("blurQuality", "Blur Quality", 1, "Low", "High"), GLASS);

    public ThemeModule() {
        super("guitheme", "Màu giao diện",
                "Đổi màu nhấn của menu và trình chỉnh HUD: chọn mẫu có sẵn hoặc tự trộn màu.", Category.UTILITY, true);
        instance = this;
    }

    private int rgb() {
        if (preset.is(CUSTOM)) return (red.getInt() << 16) | (green.getInt() << 8) | blue.getInt();
        for (int i = 0; i < RGB.length; i++) {
            if (preset.is(NAMES[i])) return RGB[i];
        }
        return RGB[0];
    }

    /** Tên bảng màu đang dùng (hiện ở Quick Settings). */
    public static String presetName() {
        return instance == null ? NAMES[0] : instance.preset.get();
    }

    /** Chuyển sang bảng màu kế tiếp. */
    public static void cyclePreset() {
        if (instance == null) return;
        instance.enabled = true;
        instance.preset.next();
    }

    public static float glassAlpha() {
        return instance == null ? 0.65f : instance.glassAlpha.getFloat() / 100f;
    }

    public static float borderAlpha() {
        return instance == null ? 0.30f : instance.borderAlpha.getFloat() / 100f;
    }

    public static int cornerRadius() {
        return instance == null ? 20 : instance.cornerRadius.getInt();
    }

    public static int shadowBlur() {
        return instance == null ? 14 : instance.shadowBlur.getInt();
    }

    public static int blurRadius() {
        return instance == null ? 20 : instance.blurRadius.getInt();
    }

    /** 0 = thấp, 1 = cao. */
    public static int blurQuality() {
        return instance == null ? 1 : instance.blurQuality.index;
    }

    /** Gọi đầu mỗi lần vẽ màn hình GUI: đẩy màu đang chọn vào bảng màu chung của Draw. */
    public static void apply() {
        Draw.setAccent(instance != null && instance.enabled ? instance.rgb() : RGB[0]);
    }
}
