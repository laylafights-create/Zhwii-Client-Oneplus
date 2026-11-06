package dev.lunarlite.gui;

import dev.lunarlite.LunarLite;
import dev.lunarlite.module.Category;
import dev.lunarlite.module.Module;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Menu module kiểu Lunar Client: thanh tiêu đề (logo, tab, nút đóng), danh sách nhóm bên trái,
 * hàng chip lọc + ô tìm kiếm, lưới thẻ 3 cột (icon lớn, OPTIONS + bánh răng, thanh ENABLED / DISABLED).
 */
public class ModsScreen extends GuiScreen {
    private static final int VW = 440, VH = 272;
    private static final int GX = 120, GR = VW - 10, GY = 64, GB = VH - 10;
    private static final int COLS = 3, GAP = 8, CARD_H = 98;
    private static final int CARD_W = (GR - GX - GAP * (COLS - 1)) / COLS;
    private static final int CHIP_BLUE = 0xFF3A5A8C;

    private final Dark.View v = new Dark.View(VW, VH);
    private final GuiScreen parent;
    private Category cat; // null = tất cả
    private String search = "";
    private int scroll;

    public ModsScreen(GuiScreen parent) { this.parent = parent; }

    private List<Module> shown() {
        List<Module> out = new ArrayList<Module>();
        String q = search.toLowerCase(Locale.ROOT);
        for (Module m : LunarLite.MODULES) {
            if (cat != null && m.category != cat) continue;
            if (q.length() > 0 && !m.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(m);
        }
        return out;
    }

    private int contentH(int n) {
        int rows = (n + COLS - 1) / COLS;
        return rows * (CARD_H + GAP);
    }

    private int maxScroll(int n) { return Math.max(0, contentH(n) - (GB - GY)); }

    @Override
    public void drawScreen(int mouseX, int mouseY, float pt) {
        v.layout(width, height, 2.4f);
        Dark.backdrop(width, height, mc.theWorld != null);
        int mx = v.mx(mouseX), my = v.my(mouseY);
        FontRenderer f = mc.fontRendererObj;
        List<Module> list = shown();
        scroll = Math.max(0, Math.min(maxScroll(list.size()), scroll));

        GlStateManager.pushMatrix();
        GlStateManager.translate(v.ox, v.oy, 0);
        GlStateManager.scale(v.s, v.s, 1f);
        GlStateManager.enableBlend();
        Dark.panel(0, 0, VW, VH, 6);

        // thanh tiêu đề
        Draw.poly(new float[] {22, 7, 13, 20, 19, 20, 17, 31, 28, 17, 22, 17}, 0xFFFFFFFF);
        GlStateManager.pushMatrix();
        GlStateManager.translate(36, 11, 0);
        GlStateManager.scale(1.3f, 1.3f, 1f);
        f.drawString("LUNAR", 0, 0, Dark.TEXT);
        f.drawString("LITE", f.getStringWidth("LUNAR "), 0, 0xFFB0B0B0);
        GlStateManager.popMatrix();
        tab(f, 130, 9, "MODS", true, mx, my);
        tab(f, 176, 9, "HUD EDITOR", false, mx, my);
        boolean hx = Draw.in(mx, my, VW - 30, 8, 20, 20);
        Draw.rrect(VW - 30, 8, VW - 10, 28, 3, hx ? Dark.BTN_HOV : Dark.BTN);
        Draw.line(VW - 24, 14, VW - 16, 22, 1.4f, 0xFFFFFFFF);
        Draw.line(VW - 24, 22, VW - 16, 14, 1.4f, 0xFFFFFFFF);
        Gui.drawRect(0, 38, VW, 39, Dark.LINE);

        // bên trái: danh sách nhóm
        Draw.rrect(8, 46, 108, VH - 40, 3, 0x40000000);
        Category[] cats = Category.values();
        for (int i = -1; i < cats.length; i++) {
            Category c = i < 0 ? null : cats[i];
            int y = 52 + (i + 1) * 24;
            boolean sel = c == cat;
            boolean hov = Draw.in(mx, my, 12, y, 92, 20);
            Draw.rrect(12, y, 104, y + 20, 3, sel ? 0x70FFFFFF : (hov ? 0x40FFFFFF : 0x28FFFFFF));
            Draw.ring(12, y, 104, y + 20, sel ? 0x80FFFFFF : 0x30FFFFFF);
            SaoIcons.draw(SaoIcons.cat(c), 16, y + 2, 0xFFE6E6E6);
            f.drawString((c == null ? "ALL" : c.label).toUpperCase(Locale.ROOT), 36, y + 6, Dark.TEXT);
        }
        boolean he = Draw.in(mx, my, 12, VH - 34, 92, 20);
        Dark.button(12, VH - 34, 92, 20, true, he);
        String e = "EDIT HUD LAYOUT";
        f.drawString(e, 12 + (92 - f.getStringWidth(e)) / 2, VH - 28, 0xFFFFFFFF);

        // hàng chip + tìm kiếm
        int cx = GX;
        for (int i = -1; i < cats.length; i++) {
            Category c = i < 0 ? null : cats[i];
            String t = (c == null ? "ALL" : c.label).toUpperCase(Locale.ROOT);
            int w = Dark.chipW(f, t);
            boolean sel = c == cat;
            Draw.rrect(cx, 46, cx + w, 60, 3, sel ? Dark.PRIMARY : CHIP_BLUE);
            Draw.ring(cx, 46, cx + w, 60, 0x40FFFFFF);
            f.drawString(t, cx + 7, 49, 0xFFFFFFFF);
            cx += w + 5;
        }
        int sx = GR - 90;
        Draw.rrect(sx, 46, GR, 60, 3, 0x66000000);
        Draw.ring(sx, 46, GR, 60, 0x40FFFFFF);
        String sh = search.length() == 0 ? "search..." : search;
        if (f.getStringWidth(sh) > 80) sh = sh.substring(Math.max(0, sh.length() - 14));
        f.drawString(sh, sx + 5, 49, search.length() == 0 ? Dark.SUB : Dark.TEXT);

        // lưới thẻ (cuộn)
        Draw.scissor(mc, Math.round(v.ox + (GX - 2) * v.s), Math.round(v.oy + GY * v.s),
            Math.round((GR - GX + 4) * v.s), Math.round((GB - GY) * v.s));
        for (int i = 0; i < list.size(); i++) {
            int x = GX + (i % COLS) * (CARD_W + GAP);
            int y = GY + (i / COLS) * (CARD_H + GAP) - scroll;
            if (y + CARD_H < GY || y > GB) continue;
            drawCard(f, list.get(i), x, y, mx, my);
        }
        Draw.endScissor();
        if (maxScroll(list.size()) > 0) {
            int th = Math.max(18, (GB - GY) * (GB - GY) / contentH(list.size()));
            int ty = GY + (int) ((GB - GY - th) * (scroll / (float) maxScroll(list.size())));
            Draw.rrect(VW - 8, ty, VW - 5, ty + th, 1, 0x60FFFFFF);
        }
        GlStateManager.popMatrix();
    }

    private void tab(FontRenderer f, int x, int y, String t, boolean sel, int mx, int my) {
        int w = f.getStringWidth(t) + 16;
        boolean hov = Draw.in(mx, my, x, y, w, 20);
        Draw.rrect(x, y, x + w, y + 20, 3, sel ? 0x70FFFFFF : (hov ? Dark.BTN_HOV : Dark.BTN));
        Draw.ring(x, y, x + w, y + 20, 0x40FFFFFF);
        f.drawString(t, x + 8, y + 6, Dark.TEXT);
    }

    private void drawCard(FontRenderer f, Module m, int x, int y, int mx, int my) {
        Draw.rrect(x, y, x + CARD_W, y + CARD_H, 5, 0x66202020);
        Draw.ring(x, y, x + CARD_W, y + CARD_H, 0x35FFFFFF);
        // icon lớn (x2)
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + CARD_W / 2f - 16, y + 8, 0);
        GlStateManager.scale(2f, 2f, 1f);
        SaoIcons.draw(SaoIcons.forModule(m), 0, 0, 0xFFEDEDED);
        GlStateManager.popMatrix();
        String n = m.name;
        while (f.getStringWidth(n) > CARD_W - 8 && n.length() > 3) n = n.substring(0, n.length() - 1);
        f.drawString(n, x + (CARD_W - f.getStringWidth(n)) / 2, y + 46, 0xFFC8C8C8);
        // OPTIONS + bánh răng
        int by = y + CARD_H - 34, ow = CARD_W - 22;
        boolean ho = Draw.in(mx, my, x + 3, by, ow, 15), hg = Draw.in(mx, my, x + 3 + ow + 2, by, 15, 15);
        Draw.rrect(x + 3, by, x + 3 + ow, by + 15, 2, ho ? Dark.BTN_HOV : Dark.BTN);
        String o = "OPTIONS";
        f.drawString(o, x + 3 + (ow - f.getStringWidth(o)) / 2, by + 4, Dark.TEXT);
        Draw.rrect(x + 5 + ow, by, x + 20 + ow, by + 15, 2, hg ? Dark.BTN_HOV : Dark.BTN);
        SaoIcons.draw("gear", x + 5 + ow + 0, by - 0, 0xFFE6E6E6);
        // ENABLED / DISABLED
        Dark.status(f, x + 3, y + CARD_H - 17, CARD_W - 6, m.enabled);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int btn) throws IOException {
        if (btn != 0) return;
        int mx = v.mx(mouseX), my = v.my(mouseY);
        if (Draw.in(mx, my, VW - 30, 8, 20, 20)) { close(); return; }
        if (Draw.in(mx, my, 176, 9, mc.fontRendererObj.getStringWidth("HUD EDITOR") + 16, 20)
            || Draw.in(mx, my, 12, VH - 34, 92, 20)) {
            mc.displayGuiScreen(new HudEditorScreen());
            return;
        }
        Category[] cats = Category.values();
        for (int i = -1; i < cats.length; i++) {
            int y = 52 + (i + 1) * 24;
            if (Draw.in(mx, my, 12, y, 92, 20)) { setCat(i < 0 ? null : cats[i]); return; }
        }
        FontRenderer f = mc.fontRendererObj;
        int cx = GX;
        for (int i = -1; i < cats.length; i++) {
            Category c = i < 0 ? null : cats[i];
            int w = Dark.chipW(f, (c == null ? "ALL" : c.label).toUpperCase(Locale.ROOT));
            if (Draw.in(mx, my, cx, 46, w, 14)) { setCat(c); return; }
            cx += w + 5;
        }
        if (my < GY || my > GB) return;
        List<Module> list = shown();
        for (int i = 0; i < list.size(); i++) {
            int x = GX + (i % COLS) * (CARD_W + GAP);
            int y = GY + (i / COLS) * (CARD_H + GAP) - scroll;
            if (!Draw.in(mx, my, x, y, CARD_W, CARD_H)) continue;
            Module m = list.get(i);
            int by = y + CARD_H - 34, ow = CARD_W - 22;
            if (Draw.in(mx, my, x + 3, by, ow + 17, 15)) {
                mc.displayGuiScreen(new ModuleSettingsScreen(m, this));
            } else if (Draw.in(mx, my, x + 3, y + CARD_H - 17, CARD_W - 6, 14)) {
                m.toggle();
                LunarLite.saveConfig();
            }
            return;
        }
    }

    private void setCat(Category c) { cat = c; scroll = 0; }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int w = Mouse.getEventDWheel();
        if (w != 0) scroll = Math.max(0, Math.min(maxScroll(shown().size()), scroll - (w > 0 ? 30 : -30)));
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) { close(); return; }
        if (key == Keyboard.KEY_BACK) {
            if (search.length() > 0) search = search.substring(0, search.length() - 1);
            scroll = 0;
        } else if (c >= 32 && c != 127 && search.length() < 20) {
            search += c;
            scroll = 0;
        }
    }

    private void close() {
        LunarLite.saveConfig();
        mc.displayGuiScreen(parent);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
