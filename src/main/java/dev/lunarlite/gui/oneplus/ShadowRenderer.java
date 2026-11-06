package dev.lunarlite.gui.oneplus;

/** Bóng đổ mềm quanh hình bo góc (shader SDF, bị cắt bỏ phần nằm dưới chính panel). */
public final class ShadowRenderer {
    private final ShaderProgram prog;

    ShadowRenderer(ShaderProgram prog) {
        this.prog = prog;
    }

    public void draw(float x, float y, float w, float h, float r, float blur, float offY, int argb) {
        if (w <= 0f || h <= 0f || blur <= 0f) return;
        float op = GUIRenderer.opacity;
        float m = blur * 1.6f + Math.abs(offY) + 2f;
        GUIRenderer.beginDraw();
        prog.use();
        prog.set2f("uCenter", x + w / 2f, y + h / 2f);
        prog.set2f("uHalf", w / 2f, h / 2f);
        prog.set1f("uRadius", r);
        prog.set1f("uBlur", blur);
        prog.set2f("uOffset", 0f, offY);
        prog.setColor("uColor", argb, op);
        GUIRenderer.quad(x - m, y - m, x + w + m, y + h + m);
        GUIRenderer.endDraw();
    }
}
