package dev.lunarlite.gui;

import dev.lunarlite.LunarLite;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

/** Menu chính (mẫu A): thẻ tối, nút Singleplayer nổi bật, Modules mở menu client. */
public class MainMenuScreen extends GuiScreen {
    private static final int VW = 240, VH = 214;
    private final Dark.View v = new Dark.View(VW, VH);
    private final long t0 = System.currentTimeMillis();

    // nút: {x, y, w, h}
    private static final int[][] B = {
        {22, 66, 196, 28}, {22, 98, 196, 28}, {22, 130, 196, 28}, {22, 162, 96, 28}, {122, 162, 96, 28}
    };
    private static final String[] L = {"SINGLEPLAYER", "MULTIPLAYER", "MODULES", "OPTIONS", "QUIT"};
    private static final String[] G = {"play", "people", "sliders", "gear", "power"};

    @Override
    public void drawScreen(int mouseX, int mouseY, float pt) {
        v.layout(width, height, 2.2f);
        Dark.backdrop(width, height, mc.theWorld != null);
        float a = Math.min(1f, (System.currentTimeMillis() - t0) / 260f);
        a = 1f - (1f - a) * (1f - a);
        int mx = v.mx(mouseX), my = v.my(mouseY);
        FontRenderer f = mc.fontRendererObj;

        GlStateManager.pushMatrix();
        GlStateManager.translate(v.ox, v.oy + (1f - a) * 10f * v.s, 0);
        GlStateManager.scale(v.s, v.s, 1f);
        GlStateManager.enableBlend();

        Dark.panel(0, 0, VW, VH, 10);

        // header
        Draw.rrect(14, 14, 46, 46, 8, Dark.PRIMARY);
        Draw.poly(new float[] {32, 19, 22, 32, 30, 32, 27, 42, 38, 28, 30, 28}, 0xFFFFFFFF);
        GlStateManager.pushMatrix();
        GlStateManager.translate(54, 15, 0);
        GlStateManager.scale(1.5f, 1.5f, 1f);
        f.drawString("LunarLite", 0, 0, Dark.TEXT);
        GlStateManager.popMatrix();
        Dark.text("Client 1.8.9", 54, 33, Dark.SUB);
        String user = mc.getSession().getUsername();
        int bw = f.getStringWidth(user) + 26;
        Draw.rrect(VW - 14 - bw, 20, VW - 14, 38, 5, Dark.BTN);
        Draw.disc(VW - 14 - bw + 9, 29, 3f, Dark.SUB);
        f.drawString(user, VW - 14 - bw + 18, 25, Dark.TEXT);

        // nút
        for (int i = 0; i < B.length; i++) {
            int[] b = B[i];
            boolean hov = Draw.in(mx, my, b[0], b[1], b[2], b[3]);
            float st = Math.max(0f, Math.min(1f, a * 1.4f - i * 0.08f));
            if (st <= 0f) continue;
            Dark.button(b[0], b[1], b[2], b[3], false, hov);
            int cy = b[1] + (b[3] - 8) / 2;
            int tw = f.getStringWidth(L[i]);
            int total = 14 + tw;
            int x0 = b[0] + (b[2] - total) / 2;
            glyph(G[i], x0 + 5, b[1] + b[3] / 2, 0xFFE6E6E6);
            f.drawString(L[i], x0 + 14, cy, Dark.TEXT);
        }

        // footer
        Gui_line(14, VH - 28, VW - 14);
        Dark.text("Build " + LunarLite.VERSION + " \u00b7 up to date", 14, VH - 20, Dark.SUB);
        SaoIcons.draw("bubble", VW - 44, VH - 24, 0xFF8A8A98);
        SaoIcons.draw("cube", VW - 28, VH - 24, 0xFF8A8A98);

        GlStateManager.popMatrix();
    }

    private static void Gui_line(int x1, int y, int x2) {
        net.minecraft.client.gui.Gui.drawRect(x1, y, x2, y + 1, Dark.LINE);
    }

    /** Biểu tượng nhỏ vẽ quanh tâm (cx, cy), ~10px. */
    private static void glyph(String g, int cx, int cy, int c) {
        if ("play".equals(g)) {
            Draw.poly(new float[] {cx - 3, cy - 5, cx - 3, cy + 5, cx + 5, cy}, c);
        } else if ("people".equals(g)) {
            Draw.disc(cx - 2, cy - 2, 2.6f, c);
            Draw.rrect(cx - 6, cy + 2, cx + 2, cy + 6, 2, c);
            Draw.disc(cx + 4, cy - 1, 2f, Draw.mulAlpha(c, 0.7f));
            Draw.rrect(cx + 1, cy + 3, cx + 7, cy + 6, 1, Draw.mulAlpha(c, 0.7f));
        } else if ("power".equals(g)) {
            Draw.arc(cx, cy + 1, 4.5f, -Math.PI * 0.3, Math.PI * 1.3, 1.5f, c);
            net.minecraft.client.gui.Gui.drawRect(cx, cy - 5, cx + 1, cy, c);
        } else {
            SaoIcons.draw(g, cx - 8, cy - 8, c);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int btn) throws IOException {
        if (btn != 0) return;
        int mx = v.mx(mouseX), my = v.my(mouseY);
        for (int i = 0; i < B.length; i++) {
            int[] b = B[i];
            if (!Draw.in(mx, my, b[0], b[1], b[2], b[3])) continue;
            mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord
                .create(new net.minecraft.util.ResourceLocation("gui.button.press"), 1.0F));
            switch (i) {
                case 0: mc.displayGuiScreen(new GuiSelectWorld(this)); break;
                case 1: mc.displayGuiScreen(new GuiMultiplayer(this)); break;
                case 2: mc.displayGuiScreen(new MenuScreen()); break;
                case 3: mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings)); break;
                default: mc.shutdown();
            }
            return;
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        // không đóng bằng ESC khi đang ở menu chính
        if (key == Keyboard.KEY_ESCAPE && mc.theWorld == null) return;
        super.keyTyped(c, key);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
