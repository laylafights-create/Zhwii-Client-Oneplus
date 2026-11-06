package dev.lunarlite.gui.oneplus;

/** Hình chữ nhật bo góc vẽ bằng shader SDF: nền chuyển màu dọc + viền, khử răng cưa bằng fwidth. */
public final class RoundedRectRenderer {
    private final ShaderProgram prog;

    RoundedRectRenderer(ShaderProgram prog) {
        this.prog = prog;
    }

    /**
     * @param top, bot màu ARGB mép trên / mép dưới (cùng màu = đặc)
     * @param border màu viền ARGB (alpha 0 = không viền), {@code bw} độ dày
     */
    public void draw(float x, float y, float w, float h, float r, int top, int bot, int border, float bw) {
        if (w <= 0f || h <= 0f) return;
        float op = GUIRenderer.opacity;
        GUIRenderer.beginDraw();
        prog.use();
        prog.set2f("uCenter", x + w / 2f, y + h / 2f);
        prog.set2f("uHalf", w / 2f, h / 2f);
        prog.set1f("uRadius", r);
        prog.setColor("uColorTop", top, op);
        prog.setColor("uColorBot", bot, op);
        prog.setColor("uBorder", border, op);
        prog.set1f("uBorderWidth", bw);
        GUIRenderer.quad(x - 2f, y - 2f, x + w + 2f, y + h + 2f);
        GUIRenderer.endDraw();
    }
}
