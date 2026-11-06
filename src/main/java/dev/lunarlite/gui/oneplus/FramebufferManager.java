package dev.lunarlite.gui.oneplus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Ba framebuffer phụ (A, B, C) dùng cho blur ping-pong, kích thước = framebuffer chính * scale.
 * Chỉ tạo lại khi kích thước đổi (resize / fullscreen), không bao giờ tạo mỗi khung hình.
 */
public final class FramebufferManager {
    public Framebuffer a, b, c;
    private int srcW = -1, srcH = -1;
    private float scale = -1f;

    public boolean ready() {
        return a != null && b != null && c != null;
    }

    public int width() {
        return a == null ? 0 : a.framebufferWidth;
    }

    public int height() {
        return a == null ? 0 : a.framebufferHeight;
    }

    /**
     * Bảo đảm cả ba framebuffer khớp kích thước nguồn. Trả về true nếu dùng được.
     * Lưu ý: tạo framebuffer làm GL bind về 0, nên người gọi phải bind lại framebuffer chính sau cùng.
     */
    public boolean ensure(int mainW, int mainH, float scale) {
        if (!OpenGlHelper.isFramebufferEnabled() || mainW <= 0 || mainH <= 0) return false;
        if (ready() && mainW == srcW && mainH == srcH && Math.abs(scale - this.scale) < 0.001f) return true;
        release();
        int w = Math.max(16, Math.round(mainW * scale));
        int h = Math.max(16, Math.round(mainH * scale));
        a = make(w, h);
        b = make(w, h);
        c = make(w, h);
        srcW = mainW;
        srcH = mainH;
        this.scale = scale;
        return ready();
    }

    private static Framebuffer make(int w, int h) {
        Framebuffer f = new Framebuffer(w, h, false);
        f.setFramebufferColor(0f, 0f, 0f, 1f);
        f.setFramebufferFilter(GL11.GL_LINEAR);
        f.bindFramebufferTexture();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        f.unbindFramebufferTexture();
        return f;
    }

    public void release() {
        if (a != null) a.deleteFramebuffer();
        if (b != null) b.deleteFramebuffer();
        if (c != null) c.deleteFramebuffer();
        a = b = c = null;
        srcW = srcH = -1;
        scale = -1f;
    }

    /** Framebuffer chính của game (nơi thế giới vừa được vẽ vào). */
    public static Framebuffer main() {
        return Minecraft.getMinecraft().getFramebuffer();
    }
}
