package dev.lunarlite.gui;

import dev.lunarlite.LunarLite;
import dev.lunarlite.module.Category;
import dev.lunarlite.module.Module;
import dev.lunarlite.setting.BoolSetting;
import dev.lunarlite.setting.ColorSetting;
import dev.lunarlite.setting.ModeSetting;
import dev.lunarlite.setting.Setting;
import dev.lunarlite.setting.SliderSetting;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Cài đặt một module (mẫu C): thanh nhóm bên trái, tiêu đề + công tắc, danh sách tuỳ chỉnh, Reset / Done. */
public class ModuleSettingsScreen extends GuiScreen {
    private static final int VW = 300, VH = 236;
    private static final int SB_X = 8, SB_W = 26;
    private static final int CX = 44, CR = VW - 14;            // cột nội dung: trái / phải
    private static final int LIST_Y = 54, LIST_B = VH - 46, ROW = 26;

    private final Dark.View v = new Dark.View(VW, VH);
    private final GuiScreen parent;
    private Module module;
    private int scroll;
    private SliderSetting dragging;
    private int dragX, dragW;

    public ModuleSettingsScreen(Module m, GuiScreen parent) {
        this.module = m;
        this.parent = parent;
    }

    private List<Setting> rows() {
        List<Setting> out = new ArrayList<Setting>();
        for (Setting s : module.settings) if (s.isVisible()) out.add(s);
        return out;
    }

    private int contentH() {
        return rows().size() * ROW + (module.actions().isEmpty() ? 0 : ROW);
    }

    private int maxScroll() { return Math.max(0, contentH() - (LIST_B - LIST_Y)); }

    private List<Module> inCat(Category c) {
        List<Module> l = new ArrayList<Module>();
        for (Module m : LunarLite.MODULES) if (m.category == c) l.add(m);
        return l;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float pt) {
        v.layout(width, height, 2.4f);
        Dark.backdrop(width, height, mc.theWorld != null);
        int mx = v.mx(mouseX), my = v.my(mouseY);
        FontRenderer f = mc.fontRendererObj;
        scroll = Math.max(0, Math.min(maxScroll(), scroll));

        GlStateManager.pushMatrix();
        GlStateManager.translate(v.ox, v.oy, 0);
        GlStateManager.scale(v.s, v.s, 1f);
        GlStateManager.enableBlend();
        Dark.panel(0, 0, VW, VH, 10);

        // thanh nhóm
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            int y = 12 + i * 30;
            boolean sel = cats[i] == module.category;
            boolean hov = Draw.in(mx, my, SB_X, y, SB_W, 26);
            Draw.rrect(SB_X, y, SB_X + SB_W, y + 26, 7, sel ? Dark.PRIMARY : (hov ? Dark.BTN_HOV : Dark.BTN));
            SaoIcons.draw(SaoIcons.cat(cats[i]), SB_X + 5, y + 5, sel ? 0xFFFFFFFF : 0xFFD8D8D8);
        }
        boolean hg = Draw.in(mx, my, SB_X, VH - 38, SB_W, 26);
        Draw.rrect(SB_X, VH - 38, SB_X + SB_W, VH - 12, 7, hg ? Dark.BTN_HOV : Dark.BTN);
        SaoIcons.draw("gear", SB_X + 5, VH - 33, 0xFFD8D8D8);

        // tiêu đề
        GlStateManager.pushMatrix();
        GlStateManager.translate(CX, 13, 0);
        GlStateManager.scale(1.4f, 1.4f, 1f);
        f.drawString(module.name, 0, 0, Dark.TEXT);
        GlStateManager.popMatrix();
        String sub = module.category.label + " \u00b7 " + module.description;
        while (f.getStringWidth(sub) > CR - CX - 64 && sub.length() > 4) sub = sub.substring(0, sub.length() - 2);
        Dark.text(sub, CX, 31, Dark.SUB);
        Dark.status(f, CR - 56, 15, 56, module.enabled);

        // danh sách (cuộn)
        scissorVirtual(CX - 2, LIST_Y, CR - CX + 4, LIST_B - LIST_Y);
        List<Setting> rs = rows();
        int y = LIST_Y - scroll;
        for (Setting s : rs) {
            drawRow(s, y, mx, my, f);
            y += ROW;
        }
        if (!module.actions().isEmpty()) {
            int x = CX;
            for (Module.Action a : module.actions()) {
                boolean hov = Draw.in(mx, my, x, y + 5, Dark.chipW(f, a.label), 14);
                Dark.chip(f, a.label, x, y + 5, false, hov);
                x += Dark.chipW(f, a.label) + 6;
            }
        }
        Draw.endScissor();
        if (maxScroll() > 0) {
            int th = Math.max(16, (LIST_B - LIST_Y) * (LIST_B - LIST_Y) / contentH());
            int ty = LIST_Y + (int) ((LIST_B - LIST_Y - th) * (scroll / (float) maxScroll()));
            Draw.rrect(VW - 8, ty, VW - 5, ty + th, 1, 0x40FFFFFF);
        }

        // Reset / Done
        int by = VH - 38, half = (CR - CX - 8) / 2;
        boolean hr = Draw.in(mx, my, CX, by, half, 26), hd = Draw.in(mx, my, CX + half + 8, by, half, 26);
        Dark.button(CX, by, half, 26, false, hr);
        Dark.button(CX + half + 8, by, half, 26, true, hd);
        Dark.text("RESET", CX + (half - f.getStringWidth("RESET")) / 2, by + 9, Dark.TEXT);
        Dark.text("DONE", CX + half + 8 + (half - f.getStringWidth("DONE")) / 2, by + 9, Dark.TEXT);

        GlStateManager.popMatrix();
    }

    private void scissorVirtual(int x, int y, int w, int h) {
        // đổi từ khung ảo sang toạ độ GUI của game rồi cắt
        Draw.scissor(mc, Math.round(v.ox + x * v.s), Math.round(v.oy + y * v.s), Math.round(w * v.s), Math.round(h * v.s));
    }

    private void drawRow(Setting s, int y, int mx, int my, FontRenderer f) {
        Gui.drawRect(CX, y + ROW - 1, CR, y + ROW, Dark.LINE);
        int ty = y + 9;
        Dark.text(s.label, CX, ty, Dark.TEXT);
        if (s instanceof SliderSetting) {
            SliderSetting sl = (SliderSetting) s;
            int vw = f.getStringWidth(sl.display());
            Dark.rightText(sl.display(), CR, ty, Dark.SUB);
            int tx = CX + 88, tw = CR - 8 - vw - tx;
            Dark.slider(tx, y + 7, tw, sl.ratio());
        } else if (s instanceof ModeSetting) {
            ModeSetting m = (ModeSetting) s;
            String t = m.get() + "  \u25be";
            int w = f.getStringWidth(t) + 14;
            boolean hov = Draw.in(mx, my, CR - w, y + 4, w, 17);
            Draw.rrect(CR - w, y + 4, CR, y + 21, 5, hov ? Dark.BTN_HOV : Dark.BTN);
            Draw.ring(CR - w, y + 4, CR, y + 21, 0x14FFFFFF);
            f.drawString(t, CR - w + 7, y + 9, Dark.TEXT);
        } else if (s instanceof BoolSetting) {
            Dark.toggle(CR - 24, y + 7, ((BoolSetting) s).value);
        } else if (s instanceof ColorSetting) {
            ColorSetting c = (ColorSetting) s;
            for (int i = 0; i < Dark.SWATCH.length; i++) {
                int cx = CR - 8 - (Dark.SWATCH.length - 1 - i) * 18;
                boolean on = (Dark.SWATCH[i] & 0xFFFFFF) == c.rgb();
                if (on) Draw.disc(cx, y + 13, 8f, 0xFFFFFFFF);
                Draw.disc(cx, y + 13, on ? 6f : 7f, Dark.SWATCH[i]);
            }
        }
    }

    private int rowAt(int mx, int my) {
        if (mx < CX || mx > CR || my < LIST_Y || my > LIST_B) return -1;
        return (my - LIST_Y + scroll) / ROW;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int btn) throws IOException {
        int mx = v.mx(mouseX), my = v.my(mouseY);
        FontRenderer f = mc.fontRendererObj;
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            if (!Draw.in(mx, my, SB_X, 12 + i * 30, SB_W, 26)) continue;
            List<Module> l = inCat(cats[i]);
            if (l.isEmpty()) return;
            int idx = l.indexOf(module);
            module = l.get(idx < 0 ? 0 : (idx + 1) % l.size()); // bấm lại cùng nhóm = module kế tiếp
            scroll = 0;
            return;
        }
        if (Draw.in(mx, my, SB_X, VH - 38, SB_W, 26)) { close(); return; }
        if (Draw.in(mx, my, CR - 56, 15, 56, 14)) { module.toggle(); return; }

        int by = VH - 38, half = (CR - CX - 8) / 2;
        if (Draw.in(mx, my, CX, by, half, 26)) { module.resetSettings(); return; }
        if (Draw.in(mx, my, CX + half + 8, by, half, 26)) { close(); return; }

        int r = rowAt(mx, my);
        List<Setting> rs = rows();
        if (r < 0) return;
        if (r >= rs.size()) {
            int x = CX, y = LIST_Y - scroll + rs.size() * ROW + 5;
            for (Module.Action a : module.actions()) {
                int w = Dark.chipW(f, a.label);
                if (Draw.in(mx, my, x, y, w, 14)) { a.run.run(); return; }
                x += w + 6;
            }
            return;
        }
        Setting s = rs.get(r);
        int y = LIST_Y - scroll + r * ROW;
        if (s instanceof SliderSetting) {
            SliderSetting sl = (SliderSetting) s;
            int tx = CX + 88, tw = CR - 8 - f.getStringWidth(sl.display()) - tx;
            if (mx >= tx - 6 && mx <= tx + tw + 6) {
                dragging = sl; dragX = tx; dragW = tw;
                sl.setRatio((mx - tx) / (double) tw);
            }
        } else if (s instanceof ModeSetting) {
            if (btn == 1) ((ModeSetting) s).prev(); else ((ModeSetting) s).next();
        } else if (s instanceof BoolSetting) {
            ((BoolSetting) s).toggle();
        } else if (s instanceof ColorSetting) {
            for (int i = 0; i < Dark.SWATCH.length; i++) {
                int cx = CR - 8 - (Dark.SWATCH.length - 1 - i) * 18;
                if (Math.abs(mx - cx) <= 8) { ((ColorSetting) s).value = Dark.SWATCH[i] & 0xFFFFFF; break; }
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int btn, long t) {
        if (dragging != null) dragging.setRatio((v.mx(mouseX) - dragX) / (double) dragW);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (dragging != null) { dragging = null; LunarLite.saveConfig(); }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int w = Mouse.getEventDWheel();
        if (w != 0) scroll = Math.max(0, Math.min(maxScroll(), scroll - (w > 0 ? 18 : -18)));
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) { close(); return; }
        super.keyTyped(c, key);
    }

    private void close() {
        LunarLite.saveConfig();
        mc.displayGuiScreen(parent);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
