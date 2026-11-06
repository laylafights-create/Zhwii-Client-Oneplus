package dev.lunarlite.gui.oneplus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * Gaussian blur hai lượt (ngang + dọc) bằng framebuffer ping-pong.
 *
 * Mỗi khung hình (khi menu mở), ngay trước khi vẽ GUI, thế giới vừa vẽ nằm trong framebuffer chính:
 *   chính -> B (ngang) -> A (dọc)   => texture nền (blur vừa)
 *   A -> B (ngang) -> C (dọc)       => texture kính (blur mạnh hơn, dùng trong panel)
 * Chữ và icon của GUI được vẽ SAU bước này nên không bao giờ bị blur.
 */
public final class BlurRenderer {
    private final ShaderProgram prog;
    private final FramebufferManager fbs = new FramebufferManager();
    private boolean valid;
    private float screenW = 1f, screenH = 1f;

    private BlurRenderer(ShaderProgram prog) {
        this.prog = prog;
    }

    static BlurRenderer create() {
        ShaderProgram p = ShaderProgram.load("gaussian_blur.vert", "gaussian_blur.frag");
        return p == null ? null : new BlurRenderer(p);
    }

    /** True nếu khung hình hiện tại đã có texture blur hợp lệ. */
    public boolean frameValid() {
        return valid && fbs.ready();
    }

    public int backgroundTexture() {
        return fbs.a.framebufferTexture;
    }

    public int glassTexture() {
        return fbs.c.framebufferTexture;
    }

    public float screenW() {
        return screenW;
    }

    public float screenH() {
        return screenH;
    }

    public void invalidate() {
        valid = false;
    }

    /** Giải phóng framebuffer (khi đóng GUI); shader được giữ lại để lần mở sau không phải biên dịch lại. */
    public void release() {
        valid = false;
        fbs.release();
    }

    public void destroy() {
        release();
        prog.delete();
    }

    /**
     * Blur framebuffer chính vào các framebuffer phụ. Luôn bind lại framebuffer chính + khôi phục trạng thái GL
     * trước khi trả về. {@code radius}: bán kính blur ước lượng theo pixel của framebuffer chính.
     */
    public boolean render(Minecraft mc, float radius, int quality) {
        valid = false;
        Framebuffer main = mc.getFramebuffer();
        if (main == null || !OpenGlHelper.isFramebufferEnabled()) return false;

        // ---- lưu trạng thái GL mà các lượt blur sẽ đụng tới
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean tex2d = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);

        boolean ok = false;
        try {
            float scale = quality <= 0 ? 0.25f : 0.5f;
            if (fbs.ensure(main.framebufferWidth, main.framebufferHeight, scale)) {
                screenW = main.framebufferWidth;
                screenH = main.framebufferHeight;

                GL11.glDisable(GL11.GL_SCISSOR_TEST);
                GlStateManager.disableBlend();
                GlStateManager.disableDepth();
                GlStateManager.disableAlpha();
                GlStateManager.disableCull();
                GlStateManager.disableLighting();
                GlStateManager.enableTexture2D();
                GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
                GlStateManager.color(1f, 1f, 1f, 1f);

                GlStateManager.matrixMode(5889);
                GlStateManager.pushMatrix();
                GlStateManager.loadIdentity();
                GlStateManager.ortho(0.0, 1.0, 0.0, 1.0, -1.0, 1.0);
                GlStateManager.matrixMode(5888);
                GlStateManager.pushMatrix();
                GlStateManager.loadIdentity();

                // tương đương bán kính ~2 sigma; 17 tap có sigma = 4 tap nên bước tap = radius * scale / 8
                float step1 = Math.max(0.05f, radius * scale / 8f);
                float step2 = step1 * 1.4f;

                prog.use();
                prog.set1i("uTex", 0);
                main.setFramebufferFilter(GL11.GL_LINEAR);
                pass(main.framebufferTexture, fbs.b, 1f, 0f, step1);
                pass(fbs.b.framebufferTexture, fbs.a, 0f, 1f, step1);
                pass(fbs.a.framebufferTexture, fbs.b, 1f, 0f, step2);
                pass(fbs.b.framebufferTexture, fbs.c, 0f, 1f, step2);
                main.setFramebufferFilter(GL11.GL_NEAREST);
                ok = true;

                GlStateManager.matrixMode(5889);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(5888);
                GlStateManager.popMatrix();
            }
        } finally {
            // ---- khôi phục
            GL20.glUseProgram(prevProgram);
            main.bindFramebuffer(true); // bind lại framebuffer chính + viewport
            GlStateManager.bindTexture(prevTex);
            if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
            if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
            if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
            if (alpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
            if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
            if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
            if (tex2d) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
        valid = ok;
        return ok;
    }

    private void pass(int srcTex, Framebuffer dst, float dx, float dy, float step) {
        dst.bindFramebuffer(true); // đặt viewport = kích thước framebuffer đích
        GlStateManager.bindTexture(srcTex);
        prog.set2f("uStep", dx * step / dst.framebufferWidth, dy * step / dst.framebufferHeight);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(0f, 0f);
        GL11.glTexCoord2f(1f, 0f);
        GL11.glVertex2f(1f, 0f);
        GL11.glTexCoord2f(1f, 1f);
        GL11.glVertex2f(1f, 1f);
        GL11.glTexCoord2f(0f, 1f);
        GL11.glVertex2f(0f, 1f);
        GL11.glEnd();
    }

    /** Vẽ texture nền đã blur phủ kín màn hình GUI (toạ độ GUI, gốc trên-trái) với độ mờ {@code alpha}. */
    public void drawBackground(int guiW, int guiH, float alpha) {
        if (!frameValid() || alpha <= 0.002f) return;
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.color(1f, 1f, 1f, Math.min(1f, alpha));
        GlStateManager.bindTexture(backgroundTexture());
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(0f, guiH);
        GL11.glTexCoord2f(1f, 0f);
        GL11.glVertex2f(guiW, guiH);
        GL11.glTexCoord2f(1f, 1f);
        GL11.glVertex2f(guiW, 0f);
        GL11.glTexCoord2f(0f, 1f);
        GL11.glVertex2f(0f, 0f);
        GL11.glEnd();
        GlStateManager.enableCull();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
