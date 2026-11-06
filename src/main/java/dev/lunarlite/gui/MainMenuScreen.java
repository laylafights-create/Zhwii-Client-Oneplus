package dev.lunarlite.gui;

import dev.lunarlite.LunarLite;
import java.io.IOException;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

/**
 * Menu chính kiểu Lunar Client: logo lớn giữa màn hình, 2 nút rộng (Singleplayer / Multiplayer),
 * hàng nút icon vuông bên dưới, phiên bản góc trái, bản quyền góc phải.
 * Ảnh nền: đặt assets/lunarlite/textures/menu_bg.png (không có thì dùng nền tối).
 */
public class MainMenuScreen extends GuiScreen {
    private static final int VW = 400, VH = 240;
    private static final ResourceLocation BG = new ResourceLocation("lunarlite", "textures/menu_bg.png");
    private static Boolean hasBg;

    private final Dark.View v = new Dark.View(VW, VH);
    private final long t0 = System.currentTimeMillis();

    // nút rộng: {x, y, w, h}
    private static final int[][] WIDE = {{112, 128, 176, 22}, {112, 154, 176, 22}};
    private static final String[] WIDE_L = {"SINGLEPLAYER", "MULTIPLAYER"};
    // nút icon vuông (căn giữa): Modules, Options, Quit
    private static final String[] SQ_G = {"sliders", "gear", "power"};
    private static final String[] SQ_L = {"Modules", "Options", "Quit"};
    private static final int SQ = 22, SQ_GAP = 6, SQ_Y = 188;

    private int sqX(int i) {
        int total = SQ_G.length * SQ + (SQ_G.length - 1) * SQ_GAP;
        return (VW - total) / 2 + i * (SQ + SQ_GAP);
    }

    private boolean bgExists() {
        if (hasBg == null) {
            try {
                mc.getResourceManager().getResource(BG);
                hasBg = Boolean.TRUE;
            } catch (Exception e) {
                hasBg = Boolean.FALSE;
            }
        }
        return hasBg.booleanValue();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float pt) {
        v.layout(width, height, 2.6f);
        FontRenderer f = mc.fontRendererObj;
        if (mc.theWorld != null) {
            Dark.backdrop(width, height, true);
        } else if (bgExists()) {
            GlStateManager.color(1f, 1f, 1f, 1f);
            mc.getTextureManager().bindTexture(BG);
            Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, width, height, width, height);
            Gui.drawRect(0, 0, width, height, 0x66000000);
        } else {
            Dark.backdrop(width, height, false);
        }
        float a = Math.min(1f, (System.currentTimeMillis() - t0) / 300f);
        a = 1f - (1f - a) * (1f - a);
        int mx = v.mx(mouseX), my = v.my(mouseY);

        GlStateManager.pushMatrix();
        GlStateManager.translate(v.ox, v.oy + (1f - a) * 8f * v.s, 0);
        GlStateManager.scale(v.s, v.s, 1f);
        GlStateManager.enableBlend();

        // logo: tia chớp trắng + chữ
        Draw.poly(new float[] {204, 22, 184, 54, 198, 54, 193, 82, 218, 46, 204, 46}, 0xFFFFFFFF);
        String name = "LUNARLITE";
        GlStateManager.pushMatrix();
        GlStateManager.translate(VW / 2f, 94, 0);
        GlStateManager.scale(1.6f, 1.6f, 1f);
        f.drawString(name, -f.getStringWidth(name) / 2, 0, Dark.TEXT);
        GlStateManager.popMatrix();

        // 2 nút rộng
        for (int i = 0; i < WIDE.length; i++) {
            int[] b = WIDE[i];
            boolean hov = Draw.in(mx, my, b[0], b[1], b[2], b[3]);
            Dark.button(b[0], b[1], b[2], b[3], false, hov);
            f.drawString(WIDE_L[i], b[0] + (b[2] - f.getStringWidth(WIDE_L[i])) / 2, b[1] + 7, Dark.TEXT);
        }

        // hàng icon vuông
        String tip = null;
        int tipX = 0;
        for (int i = 0; i < SQ_G.length; i++) {
            int x = sqX(i);
            boolean hov = Draw.in(mx, my, x, SQ_Y, SQ, SQ);
            Dark.button(x, SQ_Y, SQ, SQ, false, hov);
            glyph(SQ_G[i], x + SQ / 2, SQ_Y + SQ / 2, 0xFFE6E6E6);
            if (hov) { tip = SQ_L[i]; tipX = x + SQ / 2; }
        }
        if (tip != null) {
            int tw = f.getStringWidth(tip) + 8;
            Draw.rrect(tipX - tw / 2, SQ_Y + SQ + 4, tipX + tw / 2, SQ_Y + SQ + 17, 3, 0xCC000000);
            f.drawString(tip, tipX - tw / 2 + 4, SQ_Y + SQ + 7, Dark.TEXT);
        }

        // tên người chơi (góc phải trên)
        String user = mc.getSession().getUsername();
        int bw = f.getStringWidth(user) + 24;
        Dark.button(VW - 8 - bw, 8, bw, 18, false, false);
        Draw.disc(VW - 8 - bw + 9, 17, 3f, Dark.SUB);
        f.drawString(user, VW - 8 - bw + 17, 13, Dark.TEXT);

        GlStateManager.popMatrix();

        // chữ góc màn hình (không scale)
        f.drawString("LunarLite " + LunarLite.VERSION + " (1.8.9)", 6, height - 12, 0xB0FFFFFF);
        String c = "Copyright Mojang AB. Do not distribute!";
        f.drawString(c, width - f.getStringWidth(c) - 6, height - 12, 0xB0FFFFFF);
    }

    /** Biểu tượng nhỏ vẽ quanh tâm (cx, cy), ~10px. */
    private static void glyph(String g, int cx, int cy, int c) {
        if ("power".equals(g)) {
            Draw.arc(cx, cy + 1, 5f, -Math.PI * 0.3, Math.PI * 1.3, 1.6f, c);
            Gui.drawRect(cx, cy - 6, cx + 1, cy, c);
        } else {
            SaoIcons.draw(g, cx - 8, cy - 8, c);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int btn) throws IOException {
        if (btn != 0) return;
        int mx = v.mx(mouseX), my = v.my(mouseY);
        for (int i = 0; i < WIDE.length; i++) {
            int[] b = WIDE[i];
            if (!Draw.in(mx, my, b[0], b[1], b[2], b[3])) continue;
            click();
            mc.displayGuiScreen(i == 0 ? new GuiSelectWorld(this) : new GuiMultiplayer(this));
            return;
        }
        for (int i = 0; i < SQ_G.length; i++) {
            if (!Draw.in(mx, my, sqX(i), SQ_Y, SQ, SQ)) continue;
            click();
            if (i == 0) mc.displayGuiScreen(new ModsScreen(this));
            else if (i == 1) mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
            else mc.shutdown();
            return;
        }
    }

    private void click() {
        mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord
            .create(new ResourceLocation("gui.button.press"), 1.0F));
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE && mc.theWorld == null) return;
        super.keyTyped(c, key);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
