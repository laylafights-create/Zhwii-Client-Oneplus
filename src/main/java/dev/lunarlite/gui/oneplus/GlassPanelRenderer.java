package dev.lunarlite.gui.oneplus;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;

/** Tấm kính mờ trắng sữa: bóng đổ + lớp kính lấy mẫu texture đã blur, viền trắng, vệt sáng mép trên. */
public final class GlassPanelRenderer {
    private final ShaderProgram prog;
    private final ShadowRenderer shadow;

    GlassPanelRenderer(ShaderProgram prog, ShadowRenderer shadow) {
        this.prog = prog;
        this.shadow = shadow;
    }

    public void draw(float x, float y, float w, float h, float r, BlurRenderer blur) {
        if (w <= 0f || h <= 0f) return;
        shadow.draw(x, y, w, h, r, OnePlusTheme.shadowBlur(), 3f, (0x38 << 24) | OnePlusTheme.SHADOW_RGB);

        boolean useBlur = blur != null && blur.frameValid();
        int prevTex = 0;
        GUIRenderer.beginDraw();
        prog.use();
        if (useBlur) {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GlStateManager.bindTexture(blur.glassTexture());
            prog.set2f("uScreenSize", blur.screenW(), blur.screenH());
        } else {
            prog.set2f("uScreenSize", 1f, 1f);
        }
        prog.set1i("uBlurTex", 0);
        prog.set1f("uBlurMix", useBlur ? 1f : 0f);
        int tint = OnePlusTheme.GLASS_TINT;
        prog.set3f("uTint", ((tint >> 16) & 255) / 255f, ((tint >> 8) & 255) / 255f, (tint & 255) / 255f);
        prog.set1f("uGlassAlpha", OnePlusTheme.glassAlpha());
        prog.set1f("uBorderAlpha", OnePlusTheme.borderAlpha());
        prog.set1f("uBorderWidth", 1.2f);
        prog.set1f("uOpacity", GUIRenderer.opacity);
        prog.set2f("uCenter", x + w / 2f, y + h / 2f);
        prog.set2f("uHalf", w / 2f, h / 2f);
        prog.set1f("uRadius", r);
        GUIRenderer.quad(x - 2f, y - 2f, x + w + 2f, y + h + 2f);
        if (useBlur) GlStateManager.bindTexture(prevTex);
        GUIRenderer.endDraw();
    }
}
