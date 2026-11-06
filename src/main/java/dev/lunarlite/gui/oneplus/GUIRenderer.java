package dev.lunarlite.gui.oneplus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

/**
 * Cửa chính của bộ renderer OnePlus Crystal. Biên dịch shader một lần (lười, ở khung hình đầu tiên có GL context),
 * giữ trạng thái GL nhất quán quanh mỗi lần vẽ, và cho {@code MenuScreen} các hàm vẽ tĩnh gọn.
 * Nếu GPU/shader không dùng được, {@link #ready()} trả false và MenuScreen quay về đường vẽ CPU cũ.
 */
public final class GUIRenderer {
    private GUIRenderer() {}

    private static final Logger LOG = LogManager.getLogger("ZhwiiGUI");

    /** Độ mờ chung (fade in / fade out) nhân vào mọi thứ vẽ bằng shader. */
    public static float opacity = 1f;

    private static boolean tried, ok;
    private static boolean alphaWas = true;
    private static RoundedRectRenderer rect;
    private static ShadowRenderer shadow;
    private static GlassPanelRenderer glass;
    private static BlurRenderer blur;

    public static boolean ready() {
        if (!tried) init();
        return ok;
    }

    private static void init() {
        tried = true;
        if (!OpenGlHelper.shadersSupported || !ShaderProgram.supported()) {
            LOG.warn("GPU khong ho tro GLSL: dung giao dien CPU du phong.");
            return;
        }
        ShaderProgram r = ShaderProgram.load("quad.vert", "rounded_rect.frag");
        ShaderProgram s = ShaderProgram.load("quad.vert", "shadow.frag");
        ShaderProgram g = ShaderProgram.load("quad.vert", "glass_panel.frag");
        if (r == null || s == null || g == null) {
            if (r != null) r.delete();
            if (s != null) s.delete();
            if (g != null) g.delete();
            LOG.warn("Shader giao dien loi: dung giao dien CPU du phong.");
            return;
        }
        rect = new RoundedRectRenderer(r);
        shadow = new ShadowRenderer(s);
        glass = new GlassPanelRenderer(g, shadow);
        blur = BlurRenderer.create();
        ok = true;
    }

    /** Có thể blur bằng framebuffer + shader của mod (nếu không, MenuScreen dùng post-shader có sẵn của game). */
    public static boolean blurSupported() {
        return ready() && blur != null && OpenGlHelper.isFramebufferEnabled();
    }

    public static boolean blurActive() {
        return blur != null && blur.frameValid();
    }

    // ------------------------------------------------------------------ vòng đời khung hình

    /**
     * Gọi đầu {@code drawScreen}, khi thế giới đã nằm trong framebuffer chính và GUI chưa được vẽ:
     * blur thế giới rồi vẽ nền blur phủ màn hình.
     */
    public static void beginFrame(Minecraft mc, int guiW, int guiH, float op) {
        AnimationManager.tick();
        opacity = Math.max(0f, Math.min(1f, op));
        alphaWas = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        if (!ready() || blur == null) return;
        if (OnePlusTheme.blurEnabled()) {
            if (blur.render(mc, OnePlusTheme.blurRadius(), OnePlusTheme.blurQuality())) {
                blur.drawBackground(guiW, guiH, opacity);
            }
        } else {
            blur.invalidate();
        }
    }

    public static void endFrame() {
        ShaderProgram.stop();
        if (alphaWas) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        opacity = 1f;
    }

    /** Đóng GUI: trả framebuffer phụ về cho driver (shader giữ lại). */
    public static void release() {
        if (blur != null) blur.release();
        AnimationManager.clear();
    }

    // ------------------------------------------------------------------ trạng thái quanh mỗi lần vẽ

    static void beginDraw() {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
    }

    static void endDraw() {
        ShaderProgram.stop();
        if (alphaWas) GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    static void quad(float x0, float y0, float x1, float y1) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x0, y0);
        GL11.glVertex2f(x0, y1);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x1, y0);
        GL11.glEnd();
    }

    // ------------------------------------------------------------------ hàm vẽ cho giao diện

    /** Tấm kính lớn (sidebar, header, panel chính...). */
    public static void panel(float x, float y, float w, float h, float r) {
        glass.draw(x, y, w, h, r, blur);
    }

    /** Hình bo góc đặc / chuyển màu dọc, không viền. */
    public static void fill(float x, float y, float w, float h, float r, int top, int bot) {
        rect.draw(x, y, w, h, r, top, bot, 0, 0f);
    }

    /** Hình bo góc có viền {@code bw}. */
    public static void fillBorder(float x, float y, float w, float h, float r, int top, int bot, int border, float bw) {
        rect.draw(x, y, w, h, r, top, bot, border, bw);
    }

    /** Chỉ viền bo góc (ruột trong suốt). */
    public static void border(float x, float y, float w, float h, float r, int argb, float bw) {
        rect.draw(x, y, w, h, r, 0, 0, argb, bw);
    }

    public static void shadow(float x, float y, float w, float h, float r, float blurPx, float offY, int argb) {
        shadow.draw(x, y, w, h, r, blurPx, offY, argb);
    }
}
