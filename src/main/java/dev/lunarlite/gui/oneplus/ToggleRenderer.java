package dev.lunarlite.gui.oneplus;

import dev.lunarlite.gui.Draw;

/** Công tắc viên thuốc: nền chuyển màu mượt, núm trắng có bóng, trượt theo animation delta-time. */
public final class ToggleRenderer {
    private ToggleRenderer() {}

    /**
     * @param key khoá animation (mỗi công tắc một khoá)
     * @param animate false = nhảy thẳng tới trạng thái (tắt animation menu)
     */
    public static void draw(String key, int x, int y, int w, int h, boolean on, float hover, int accent, int accentHi,
                            boolean animate) {
        float a = AnimationManager.approach("sw:" + key, on ? 1f : 0f, 14f, animate);
        int off = Draw.lerp(0x66808EA2, 0x80808EA2, hover);
        int onC = Draw.lerp(accent, accentHi, hover);
        int track = Draw.lerp(off, onC, a);
        GUIRenderer.fillBorder(x, y, w, h, h / 2f, track, track, 0x66FFFFFF, 1f);

        float kd = h - 4f;
        float kx = x + 2f + (w - kd - 4f) * a;
        float ky = y + 2f;
        GUIRenderer.shadow(kx, ky, kd, kd, kd / 2f, 3f, 1f, 0x55000000);
        GUIRenderer.fill(kx, ky, kd, kd, kd / 2f, 0xFFFFFFFF, 0xFFF1F4F8);
    }
}
