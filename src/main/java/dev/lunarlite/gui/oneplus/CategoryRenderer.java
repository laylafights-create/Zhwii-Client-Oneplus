package dev.lunarlite.gui.oneplus;

/** Vệt chọn của sidebar: một viên kính màu nhấn trượt mượt giữa các nhóm (Category Transition). */
public final class CategoryRenderer {
    private CategoryRenderer() {}

    public static void drawSelection(int x, int targetY, int w, int h, int r, int accentHi, boolean animate) {
        float y = AnimationManager.approach("cat:y", targetY, 18f, animate);
        GUIRenderer.fillBorder(x, y, w, h, r, (0xF2 << 24) | (accentHi & 0xFFFFFF), (0xC0 << 24) | (accentHi & 0xFFFFFF),
                0xB0FFFFFF, 1f);
    }

    /** 0..1: mức "đang được chọn" của một mục, mượt theo thời gian. */
    public static float selected(String key, boolean sel, boolean animate) {
        return AnimationManager.approach("cat:sel:" + key, sel ? 1f : 0f, 16f, animate);
    }
}
