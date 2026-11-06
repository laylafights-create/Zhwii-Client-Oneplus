package dev.lunarlite.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

/** Bảng màu + hàm vẽ cho giao diện tối "Violet" (menu chính, cài đặt module). */
public final class Dark {
    private Dark() {}

    // Màu kiểu Lunar Client: nền xám đen mờ, nút xám, nhấn xanh dương, ENABLED xanh lá / DISABLED đỏ.
    public static final int BG = 0xB0181818, BTN = 0x90505050, BTN_HOV = 0xA8686868;
    public static final int PRIMARY = 0xFF4A7BD0, PRIMARY_HOV = 0xFF6591E6;
    public static final int ACCENT = 0xFF4A7BD0, CHIP_ON = 0xFF3F68B0;
    public static final int GREEN = 0xFF2FA566, RED = 0xFFA82A47;
    public static final int TEXT = 0xFFF2F2F2, SUB = 0xFFA8A8A8, LINE = 0x30FFFFFF, TRACK = 0x50000000;
    public static final int[] SWATCH = {0xFF4A7BD0, 0xFF2FA566, 0xFFFFAA00, 0xFFA82A47, 0xFFAA55FF, 0xFFFFFFFF};

    /** Khung ảo + hệ số phóng để vừa màn hình. */
    public static final class View {
        public float s, ox, oy;
        public final int vw, vh;
        public View(int vw, int vh) { this.vw = vw; this.vh = vh; }

        public void layout(int w, int h, float maxScale) {
            s = Math.max(0.5f, Math.min(maxScale, Math.min(w * 0.94f / vw, h * 0.92f / vh)));
            ox = (w - vw * s) / 2f;
            oy = (h - vh * s) / 2f;
        }
        public int mx(int x) { return Math.round((x - ox) / s); }
        public int my(int y) { return Math.round((y - oy) / s); }
    }

    public static void panel(int x, int y, int w, int h, int r) {
        Draw.rrect(x, y, x + w, y + h, r, BG);
        Draw.ring(x, y, x + w, y + h, 0x30FFFFFF);
    }

    public static void button(int x, int y, int w, int h, boolean primary, boolean hov) {
        int c = primary ? (hov ? PRIMARY_HOV : PRIMARY) : (hov ? BTN_HOV : BTN);
        Draw.rrect(x, y, x + w, y + h, 5, c);
        if (!primary) Draw.ring(x, y, x + w, y + h, 0x55FFFFFF);
    }

    /** Thanh ENABLED (xanh lá) / DISABLED (đỏ) kiểu Lunar. */
    public static void status(FontRenderer f, int x, int y, int w, boolean on) {
        Draw.rrect(x, y, x + w, y + 14, 3, on ? GREEN : RED);
        Draw.ring(x, y, x + w, y + 14, 0x30FFFFFF);
        String t = on ? "ENABLED" : "DISABLED";
        f.drawString(t, x + (w - f.getStringWidth(t)) / 2, y + 3, 0xFFFFFFFF);
    }

    public static void toggle(int x, int y, boolean on) {
        Draw.rrect(x, y, x + 24, y + 12, 6, on ? GREEN : TRACK);
        int kx = on ? x + 13 : x + 1;
        Draw.rrect(kx, y + 1, kx + 10, y + 11, 5, 0xFFFFFFFF);
    }

    public static void slider(int x, int y, int w, double ratio) {
        int fx = x + Math.round((float) (w * ratio));
        Draw.rrect(x, y + 4, x + w, y + 7, 1, TRACK);
        Draw.rrect(x, y + 4, fx, y + 7, 1, ACCENT);
        Draw.disc(fx, y + 5.5f, 5f, 0xFFF0F0F5);
    }

    public static void chip(FontRenderer f, String t, int x, int y, boolean on, boolean hov) {
        int w = f.getStringWidth(t) + 14;
        Draw.rrect(x, y, x + w, y + 14, 7, on ? CHIP_ON : (hov ? BTN_HOV : BTN));
        f.drawString(t, x + 7, y + 3, on ? 0xFFFFFFFF : SUB);
    }

    public static int chipW(FontRenderer f, String t) { return f.getStringWidth(t) + 14; }

    public static void text(String t, int x, int y, int c) {
        Minecraft.getMinecraft().fontRendererObj.drawString(t, x, y, c);
    }

    public static void rightText(String t, int xr, int y, int c) {
        FontRenderer f = Minecraft.getMinecraft().fontRendererObj;
        f.drawString(t, xr - f.getStringWidth(t), y, c);
    }

    /** Nền tối có quầng sáng tím mờ. */
    public static void backdrop(int w, int h, boolean world) {
        if (world) {
            if (dev.lunarlite.gui.oneplus.GUIRenderer.blurSupported()) {
                dev.lunarlite.gui.oneplus.GUIRenderer.beginFrame(Minecraft.getMinecraft(), w, h, 1f); // blur thế giới
                dev.lunarlite.gui.oneplus.GUIRenderer.endFrame();
            }
            Gui.drawRect(0, 0, w, h, 0x66000000);
        } else {
            Gui.drawRect(0, 0, w, h, 0xFF0C0F14);
            Draw.disc(w * 0.25f, h * 0.2f, Math.max(w, h) * 0.5f, 0x2A3E6FB0, 0x00000000);
            Draw.disc(w * 0.85f, h * 0.9f, Math.max(w, h) * 0.4f, 0x1A2FA566, 0x00000000);
        }
    }

    public static ScaledResolution sr() { return new ScaledResolution(Minecraft.getMinecraft()); }
}
