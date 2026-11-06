package dev.lunarlite.gui;

import dev.lunarlite.Lang;
import dev.lunarlite.LunarLite;
import dev.lunarlite.module.AnimationsModule;
import dev.lunarlite.module.Category;
import dev.lunarlite.module.HudModule;
import dev.lunarlite.module.Module;
import dev.lunarlite.module.ThemeModule;
import dev.lunarlite.gui.oneplus.AnimationManager;
import dev.lunarlite.gui.oneplus.CategoryRenderer;
import dev.lunarlite.gui.oneplus.GUIRenderer;
import dev.lunarlite.gui.oneplus.OnePlusTheme;
import dev.lunarlite.gui.oneplus.ToggleRenderer;
import dev.lunarlite.setting.BoolSetting;
import dev.lunarlite.setting.ColorSetting;
import dev.lunarlite.setting.ModeSetting;
import dev.lunarlite.setting.Setting;
import dev.lunarlite.setting.SliderSetting;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Menu "OnePlus Crystal": kính mờ trắng bo góc lớn trên nền thế giới đã làm mờ, bố cục 3 cột:
 *   TRÁI    : logo, nhóm tính năng (Tất cả / HUD / PvP ...), Sửa HUD, Hồ sơ, trạng thái FPS / Ping.
 *   GIỮA    : header ZHWII CLIENT, lưới thẻ tính năng (icon đơn sắc + công tắc xanh), thanh chân trang.
 *             Bấm thẻ = mở tuỳ chỉnh của tính năng (nhãn bên trái, điều khiển bên phải), bấm công tắc = bật / tắt.
 *   PHẢI    : thẻ người chơi, Client Info, Quick Settings (Blur Background, Animations, Theme), Resume Game.
 * Toàn bộ giao diện vẽ trong khung ảo VW x VH rồi tự thu nhỏ cho vừa màn hình.
 */
public class MenuScreen extends GuiScreen {
    private static final String[] COLOR_NAMES = {
        "White", "Red", "Orange", "Yellow", "Green", "Cyan", "Blue", "Purple", "Pink", "Black"
    };

    // ---- khung ảo
    private static final int VW = 640, VH = 340;
    private static final float MENU_SCALE = 1.0f;

    // ---- cột trái (sidebar)
    private static final int SB_X = 10, SB_Y = 10, SB_W = 104, SB_H = VH - 20;
    private static final int NAV_Y = SB_Y + 76, NAV_H = 22, NAV_P = 24;

    // ---- cột giữa
    private static final int MAIN_X = 122, MAIN_W = 348;
    private static final int HEAD_Y = 10, HEAD_H = 56;
    private static final int MOD_Y = 74, MOD_H = 208;
    private static final int FOOT_Y = 290, FOOT_H = 40;

    // ---- cột phải
    private static final int RP_X = 478, RP_W = 152;
    private static final int PROF_Y = 10, PROF_H = 56;
    private static final int INFO_Y = 74, INFO_H = 84;
    private static final int QS_Y = 166, QS_H = 116;

    // ---- lưới thẻ
    private static final int GRID_X = MAIN_X + 10, GRID_W = MAIN_W - 20;
    private static final int GRID_Y = MOD_Y + 44, GRID_B = MOD_Y + MOD_H - 10;
    private static final int COLS = 2, GAP = 8, CARD_H = 46;

    // ---- chi tiết
    private static final int COL_X = MAIN_X + 10, COL_W = MAIN_W - 20;
    private static final int DET_Y = MOD_Y + 38, DET_B = MOD_Y + MOD_H - 8;
    private static final int ROW_H = 26;
    private static final int TRACK_W = 120;

    // ---- màu (kính trắng, chữ navy, nhấn theo màu giao diện - mặc định xanh OnePlus)
    private static final int INK = 0xFF13233A;
    private static final int INK2 = 0xFF44546A;
    private static final int INK3 = 0xFF7A8798;
    private static final int RED = 0xFFD9404B;
    private static final int GREEN = 0xFF2EC27E;
    private static final int OP_RED = 0xFFEB0028;

    private static Module lastModule;
    private static Category lastTab; // null = nhóm "Tất cả"
    static long animStart;

    private static final Comparator<Module> BY_NAME = new Comparator<Module>() {
        @Override
        public int compare(Module a, Module b) {
            return Lang.vi(a.name).compareToIgnoreCase(Lang.vi(b.name));
        }
    };

    private static int accent() {
        return Draw.ACCENT;
    }

    private static int accentHi() {
        return Draw.lerp(Draw.ACCENT, 0xFFFFFFFF, 0.28f);
    }

    private static int accentDk() {
        return Draw.lerp(Draw.ACCENT, 0xFF000000, 0.28f);
    }

    // ------------------------------------------------------------------ hàng tuỳ chỉnh

    private static final int R_HEAD = 0, R_BOOL = 1, R_MODE = 2, R_COLOR = 3, R_SLIDER = 4, R_ACTION = 5, R_RESET = 6;

    private static class SRow {
        int kind;
        String text;
        Setting s;
        Module.Action a;
        int y, h = ROW_H;
    }

    private static int rank(Setting st) {
        if (st instanceof BoolSetting) return 0;
        if (st instanceof ModeSetting) return 1;
        if (st instanceof ColorSetting) return 2;
        return 3;
    }

    private static int presetIndex(int rgb) {
        for (int i = 0; i < ColorSetting.PRESETS.length; i++) {
            if (ColorSetting.PRESETS[i] == (rgb & 0xFFFFFF)) return i;
        }
        return -1;
    }

    private static String colorName(int rgb) {
        int i = presetIndex(rgb);
        return Lang.vi(i < 0 || i >= COLOR_NAMES.length ? "Custom" : COLOR_NAMES[i]);
    }

    private static long visSig(Module m) {
        long sig = 0;
        int i = 0;
        for (Setting st : m.settings) {
            if (st.isVisible() && i < 63) sig |= 1L << i;
            i++;
        }
        return sig;
    }

    // -------------------------------------------------------------------- trạng thái

    private Module module;
    private boolean detail;
    private Category tabCat;
    private final List<Category> tabCats = new ArrayList<Category>();
    private final List<Module> shown = new ArrayList<Module>();
    private final List<SRow> rows = new ArrayList<SRow>();
    private String[] descLines = new String[0];
    private int contentH;
    private float gridScroll, gridTarget;
    private float setScroll, setTarget;
    private long lastFrameNs;
    private float scrollK = 1f;
    private int curMx, curMy;

    // nhấn / kéo
    private String pressKey;
    private int pressX, pressY;
    private float pressScroll;
    private boolean dragged;
    private SRow activeSlider;

    // ---- animation
    private final long openStart;
    private long closeStart;
    private boolean closing;
    private boolean soundOpened;

    public MenuScreen() {
        openStart = animStart = System.currentTimeMillis();
        module = lastModule;
        if (module == null || !LunarLite.MODULES.contains(module)) {
            module = LunarLite.MODULES.isEmpty() ? null : LunarLite.MODULES.get(0);
        }
        tabCat = lastTab;
        buildShown();
    }

    static float stagger(int id) {
        if (!AnimationsModule.menu()) return 1f;
        float t = (System.currentTimeMillis() - animStart) * AnimationsModule.speed() / 240f - Math.min(id, 12) * 0.04f;
        return AnimationsModule.ease(t);
    }

    /** Tiến độ trượt vào của từng cột (0 = trái, 1 = giữa, 2 = phải). */
    private float panelIn(int k) {
        if (!AnimationsModule.menu() || closing) return 1f;
        float t = (System.currentTimeMillis() - openStart) * AnimationsModule.speed() / 260f - k * 0.08f;
        return AnimationsModule.ease(t);
    }

    private float openProgress() {
        if (!AnimationsModule.menu()) return closing ? 0f : 1f;
        float sp = AnimationsModule.speed();
        long n = System.currentTimeMillis();
        if (closing) return 1f - AnimationsModule.ease((n - closeStart) * sp / 150f);
        return AnimationsModule.ease((n - openStart) * sp / 220f);
    }

    private void requestClose() {
        if (closing) return;
        play("menu.close");
        if (!AnimationsModule.menu()) {
            mc.displayGuiScreen(null);
            return;
        }
        closing = true;
        closeStart = System.currentTimeMillis();
    }

    @Override
    public void updateScreen() {
        if (closing && openProgress() <= 0.001f) mc.displayGuiScreen(null);
    }

    // ------------------------------------------------------------------ âm thanh

    private void play(String ev) {
        ResourceLocation loc = new ResourceLocation("lunarlite", ev);
        if (mc.thePlayer != null) {
            mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.create(loc,
                    (float) mc.thePlayer.posX, (float) mc.thePlayer.posY, (float) mc.thePlayer.posZ));
        } else {
            mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.create(loc, 1.0F));
        }
    }

    // ------------------------------------------------------------------ khung ảo <-> màn hình

    private float ui() {
        return Math.min(MENU_SCALE, Math.min((width - 8f) / VW, (height - 8f) / VH));
    }

    private int vx(int mx) {
        return Math.round((mx - width / 2f) / ui() + VW / 2f);
    }

    private int vy(int my) {
        return Math.round((my - height / 2f) / ui() + VH / 2f);
    }

    private void clip(int x, int y, int w, int h) {
        float u = ui();
        int rx = Math.round((x - VW / 2f) * u + width / 2f);
        int ry = Math.round((y - VH / 2f) * u + height / 2f);
        Draw.scissor(mc, rx, ry, Math.round(w * u), Math.round(h * u));
    }

    // ------------------------------------------------------------------ nhóm + danh sách

    private void buildShown() {
        shown.clear();
        tabCats.clear();
        for (Category c : Category.values()) {
            List<Module> inCat = new ArrayList<Module>();
            for (Module m : LunarLite.MODULES) {
                if (m.category == c) inCat.add(m);
            }
            if (inCat.isEmpty()) continue;
            tabCats.add(c);
            if (tabCat != null && c != tabCat) continue;
            Collections.sort(inCat, BY_NAME);
            shown.addAll(inCat);
        }
        if (shown.isEmpty() && tabCat != null) {
            tabCat = lastTab = null;
            buildShown();
        }
    }

    private int tabCount() {
        return 1 + tabCats.size();
    }

    private int tabIndex() {
        return tabCat == null ? 0 : tabCats.indexOf(tabCat) + 1;
    }

    private String tabLabel(int i) {
        return i == 0 ? "Tất cả" : tabCats.get(i - 1).label;
    }

    private String tabIcon(int i) {
        return Icons.cat(i == 0 ? null : tabCats.get(i - 1));
    }

    private void setTab(int i) {
        Category c = i <= 0 || i > tabCats.size() ? null : tabCats.get(i - 1);
        tabCat = c;
        lastTab = c;
        gridScroll = gridTarget = 0f;
        animStart = System.currentTimeMillis();
        buildShown();
    }

    private void cycleTab(int d) {
        int n = tabCount();
        setTab((tabIndex() + d + n) % n);
    }

    // ---- vị trí sidebar
    private int navY(int i) {
        return NAV_Y + i * NAV_P;
    }

    private int extraY(int k) {
        return NAV_Y + tabCount() * NAV_P + 10 + k * NAV_P;
    }

    private static final int NAV_X = SB_X + 8, NAV_W = SB_W - 16;

    // ---- nút đóng trong header
    private static final int CLOSE_X = MAIN_X + MAIN_W - 28, CLOSE_Y = HEAD_Y + 10, CLOSE_S = 16;

    // ---- Quick Settings
    private static final int QS_ROW_Y = QS_Y + 30, QS_ROW_H = 24;
    private static final int RESUME_Y = FOOT_Y, RESUME_H = FOOT_H;

    // ---- lưới thẻ
    private int cardW() {
        return (GRID_W - (COLS - 1) * GAP) / COLS;
    }

    private int cardX(int i) {
        return GRID_X + (i % COLS) * (cardW() + GAP);
    }

    private int cardY(int i) {
        return GRID_Y + (i / COLS) * (CARD_H + GAP) - Math.round(gridScroll);
    }

    private int gridContentH() {
        int r = (shown.size() + COLS - 1) / COLS;
        return r == 0 ? 0 : r * (CARD_H + GAP) - GAP;
    }

    private float gridMax() {
        return Math.max(0, gridContentH() - (GRID_B - GRID_Y));
    }

    private void clampGrid() {
        float max = gridMax();
        gridTarget = Math.max(0f, Math.min(max, gridTarget));
        gridScroll = Math.max(0f, Math.min(max, gridScroll));
    }

    // ---- chi tiết
    private int descH() {
        return descLines.length == 0 ? 0 : descLines.length * 10 + 4;
    }

    private int listTop() {
        return DET_Y + descH() + 6;
    }

    private int listViewH() {
        return DET_B - listTop();
    }

    private float setMax() {
        return Math.max(0, contentH - listViewH());
    }

    private void clampSet() {
        float max = setMax();
        setTarget = Math.max(0f, Math.min(max, setTarget));
        setScroll = Math.max(0f, Math.min(max, setScroll));
    }

    // ------------------------------------------------------------------ hành động

    private void openModule(Module m) {
        if (m == null) return;
        play("menu.select");
        module = m;
        lastModule = m;
        detail = true;
        setScroll = setTarget = 0f;
        animStart = System.currentTimeMillis();
        buildRows();
    }

    private void goBack() {
        play("menu.back");
        detail = false;
        activeSlider = null;
        animStart = System.currentTimeMillis();
    }

    private void moveModule(int d) {
        int i = shown.indexOf(module);
        if (i < 0) return;
        int j = i + d;
        if (j < 0 || j >= shown.size()) return;
        openModule(shown.get(j));
    }

    private void toggleModule(Module m) {
        m.toggle();
        play(m.enabled ? "menu.on" : "menu.off");
    }

    // ------------------------------------------------------------------ dựng hàng tuỳ chỉnh

    private void addRow(int kind, String text, Setting s, Module.Action a, int h) {
        SRow r = new SRow();
        r.kind = kind;
        r.text = text;
        r.s = s;
        r.a = a;
        r.h = h;
        rows.add(r);
    }

    private void buildRows() {
        rows.clear();
        descLines = new String[0];
        contentH = 0;
        Module m = module;
        if (m == null) return;

        int maxW = COL_W - 4;
        if (m.description != null && m.description.length() > 0) {
            List<?> ls = fontRendererObj.listFormattedStringToWidth(Lang.vi(m.description), maxW);
            int n = Math.min(2, ls.size());
            descLines = new String[n];
            for (int i = 0; i < n; i++) descLines[i] = (String) ls.get(i);
            if (ls.size() > 2) descLines[1] = fit(descLines[1] + "...", maxW);
        }

        List<Setting> vis = new ArrayList<Setting>();
        for (Setting st : m.settings) if (st.isVisible()) vis.add(st);

        List<String> found = new ArrayList<String>();
        for (Setting st : vis) if (!found.contains(st.section)) found.add(st.section);
        List<String> sections = new ArrayList<String>();
        for (String sec : found) if (!"Appearance".equals(sec) && !"Rainbow".equals(sec)) sections.add(sec);
        if (found.contains("Appearance")) sections.add("Appearance");
        if (found.contains("Rainbow")) sections.add("Rainbow");

        List<Module.Action> acts = m.actions();
        boolean heads = sections.size() > 1 || (sections.size() == 1 && !acts.isEmpty());
        for (String sec : sections) {
            if (heads) addRow(R_HEAD, Lang.vi(sec), null, null, 22);
            List<Setting> inSec = new ArrayList<Setting>();
            for (Setting st : vis) {
                if (sec.equals(st.section)) inSec.add(st);
            }
            Collections.sort(inSec, new Comparator<Setting>() {
                @Override
                public int compare(Setting x, Setting y) {
                    return rank(x) - rank(y);
                }
            });
            for (Setting st : inSec) {
                if (st instanceof SliderSetting) addRow(R_SLIDER, null, st, null, ROW_H);
                else if (st instanceof BoolSetting) addRow(R_BOOL, null, st, null, ROW_H);
                else if (st instanceof ModeSetting) addRow(R_MODE, null, st, null, ROW_H);
                else if (st instanceof ColorSetting) addRow(R_COLOR, null, st, null, ROW_H);
            }
        }
        if (!acts.isEmpty()) {
            if (heads) addRow(R_HEAD, "Thao tác", null, null, 22);
            for (Module.Action a : acts) addRow(R_ACTION, a.label, null, a, 28);
        }
        addRow(R_RESET, "Đặt lại tuỳ chỉnh", null, null, 34);

        int y = 2;
        for (SRow r : rows) {
            r.y = y;
            y += r.h;
        }
        contentH = y + 4;
        clampSet();
    }

    // ------------------------------------------------------------------ nền blur (post-shader của game)

    private static final ResourceLocation BLUR_SHADER = new ResourceLocation("lunarlite", "shaders/post/menu_blur.json");
    private boolean blurOn;

    private void startBlur() {
        if (blurOn || mc == null || mc.theWorld == null || !OpenGlHelper.shadersSupported) return;
        if (mc.entityRenderer.isShaderActive()) return;
        try {
            mc.entityRenderer.loadShader(BLUR_SHADER);
            blurOn = mc.entityRenderer.isShaderActive();
        } catch (Throwable t) {
            blurOn = false;
        }
    }

    private void stopBlur() {
        if (!blurOn) return;
        blurOn = false;
        try {
            if (mc != null && mc.entityRenderer != null) mc.entityRenderer.stopUseShader();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void initGui() {
        if (!soundOpened) {
            soundOpened = true;
            play("menu.open");
        }
        if (LunarLite.menuBlur && !GUIRenderer.blurSupported()) startBlur(); // dự phòng: post-shader của game
        if (detail) buildRows();
    }

    // ------------------------------------------------------------------ vẽ: tiện ích

    private String fit(String s, int maxW) {
        FontRenderer fr = fontRendererObj;
        if (fr.getStringWidth(s) <= maxW) return s;
        int dots = fr.getStringWidth("..");
        return fr.trimStringToWidth(s, Math.max(0, maxW - dots)) + "..";
    }

    /** Độ mờ chung của menu khi mở / đóng (0..1), nhân vào chữ và icon. */
    private float fade = 1f;

    private int fa(int color) {
        return fade >= 0.999f ? color : Draw.mulAlpha(color, fade);
    }

    private void txt(String s, float x, float y, int color) {
        fontRendererObj.drawString(s, x, y, fa(color), false);
    }

    private void txtC(String s, float cx, float y, int color) {
        fontRendererObj.drawString(s, cx - fontRendererObj.getStringWidth(s) / 2f, y, fa(color), false);
    }

    private void txtR(String s, float rx, float y, int color) {
        fontRendererObj.drawString(s, rx - fontRendererObj.getStringWidth(s), y, fa(color), false);
    }

    /** Chữ phóng to {@code sc} lần (dùng cho tiêu đề). */
    private void txtS(String s, float x, float y, int color, float sc) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0f);
        GlStateManager.scale(sc, sc, 1f);
        fontRendererObj.drawString(s, 0f, 0f, fa(color), false);
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** Hover mượt theo delta time (không phụ thuộc FPS). */
    private float hv(String key, boolean over) {
        return AnimationManager.approach("hv:" + key, over ? 1f : 0f, 16f, AnimationsModule.menu());
    }

    /** Đang giữ chuột trên vùng {@code key}: dùng cho hiệu ứng nhấn (scale nhẹ). */
    private float pressAnim(String key) {
        boolean down = pressKey != null && pressKey.equals(key) && !dragged;
        return AnimationManager.approach("press:" + key, down ? 1f : 0f, 28f, AnimationsModule.menu());
    }

    /** Bắt đầu scale nhẹ quanh tâm khi nhấn; trả true nếu đã push ma trận (phải gọi {@link #endPress}). */
    private boolean beginPress(String key, float x, float y, float w, float h) {
        float k = 1f - 0.04f * pressAnim(key);
        if (k > 0.9995f) return false;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + w / 2f, y + h / 2f, 0f);
        GlStateManager.scale(k, k, 1f);
        GlStateManager.translate(-(x + w / 2f), -(y + h / 2f), 0f);
        return true;
    }

    private void endPress(boolean pushed) {
        if (pushed) GlStateManager.popMatrix();
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return Draw.in(mx, my, x, y, w, h);
    }

    // ---- hình khối kính

    /** Độ lệch ngang của hàng thứ i (tính từ mép) để tạo góc bo bán kính r. */
    private static int inset(int i, int r) {
        double dy = r - i - 0.5;
        return r - (int) Math.round(Math.sqrt(Math.max(0.0, r * r - dy * dy)));
    }

    /** Viền 1px bo góc bán kính r tuỳ ý. */
    private static void rring(int x, int y, int w, int h, int r, int c) {
        if (GUIRenderer.ready()) {
            GUIRenderer.border(x, y, w, h, r, c, 1f);
            return;
        }
        legacyRring(x, y, w, h, r, c);
    }

    private static void legacyRring(int x, int y, int w, int h, int r, int c) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 1) {
            Draw.outline(x, y, x + w, y + h, c);
            return;
        }
        int o0 = inset(0, r);
        Gui.drawRect(x + o0, y, x + w - o0, y + 1, c);
        Gui.drawRect(x + o0, y + h - 1, x + w - o0, y + h, c);
        for (int i = 1; i < r; i++) {
            int a = inset(i, r), b = Math.max(a + 1, inset(i - 1, r));
            Gui.drawRect(x + a, y + i, x + b, y + i + 1, c);
            Gui.drawRect(x + w - b, y + i, x + w - a, y + i + 1, c);
            Gui.drawRect(x + a, y + h - 1 - i, x + b, y + h - i, c);
            Gui.drawRect(x + w - b, y + h - 1 - i, x + w - a, y + h - i, c);
        }
        Gui.drawRect(x, y + r, x + 1, y + h - r, c);
        Gui.drawRect(x + w - 1, y + r, x + w, y + h - r, c);
    }

    /** Hình chữ nhật bo góc, alpha chuyển dần từ trên xuống dưới (vẽ bằng các dải ngang). */
    private static void gradRRect(int x, int y, int w, int h, int r, int rgb, int aTop, int aBot) {
        if (w <= 0 || h <= 0) return;
        if (GUIRenderer.ready()) {
            GUIRenderer.fill(x, y, w, h, r, ((aTop & 255) << 24) | (rgb & 0xFFFFFF), ((aBot & 255) << 24) | (rgb & 0xFFFFFF));
            return;
        }
        legacyGradRRect(x, y, w, h, r, rgb, aTop, aBot);
    }

    private static void legacyGradRRect(int x, int y, int w, int h, int r, int rgb, int aTop, int aBot) {
        r = Math.min(r, Math.min(w, h) / 2);
        int yy = 0;
        while (yy < h) {
            int step;
            int ins;
            if (yy < r) {
                step = 1;
                ins = inset(yy, r);
            } else if (yy >= h - r) {
                step = 1;
                ins = inset(h - 1 - yy, r);
            } else {
                step = Math.max(1, Math.min(4, (h - r) - yy));
                ins = 0;
            }
            float t = (yy + step / 2f) / h;
            int a = Math.round(aTop + (aBot - aTop) * t);
            if (a > 0) Gui.drawRect(x + ins, y + yy, x + w - ins, y + yy + step, (a << 24) | (rgb & 0xFFFFFF));
            yy += step;
        }
    }

    /** Tấm kính lớn: bóng mềm, nền trắng sữa trong suốt, viền kép sáng, vệt sáng mép trên và vệt chéo ở góc. */
    private static void glassPanel(int x, int y, int w, int h, int r, int alpha, boolean streak) {
        if (GUIRenderer.ready()) {
            // kính mờ thật: lấy mẫu texture đã blur, bo góc + bóng + viền bằng shader SDF
            GUIRenderer.panel(x, y, w, h, Math.min(OnePlusTheme.cornerRadius(), Math.min(w, h) / 2f));
            return;
        }
        legacyGlassPanel(x, y, w, h, r, alpha, streak);
    }

    private static void legacyGlassPanel(int x, int y, int w, int h, int r, int alpha, boolean streak) {
        int[] sh = {0x1C, 0x14, 0x0E, 0x09, 0x05};
        for (int k = 1; k <= sh.length; k++) {
            legacyRring(x - k, y - k + 1, w + 2 * k, h + 2 * k, r + k, sh[k - 1] << 24);
        }
        legacyGradRRect(x, y, w, h, r, 0xFFFFFF, Math.min(255, alpha + 0x1C), Math.max(0, alpha - 0x14));
        legacyRring(x, y, w, h, r, 0xA6FFFFFF);
        legacyRring(x + 1, y + 1, w - 2, h - 2, r - 1, 0x2EFFFFFF);
        Gui.drawRect(x + r, y + 1, x + w - r, y + 2, 0xC0FFFFFF);
        if (streak && w > 60 && h > 40) {
            Draw.line(x + 12, y + 3, x + 3, y + 12, 3f, 0x3CFFFFFF);
            Draw.line(x + w - 4, y + h - 14, x + w - 14, y + h - 4, 3f, 0x30FFFFFF);
        }
    }

    /** Thẻ kính nhỏ (module, hàng cài đặt). */
    private static void glassCard(int x, int y, int w, int h, int r, int alpha, int edge) {
        if (GUIRenderer.ready()) {
            int top = (Math.min(255, alpha + 0x20) << 24) | 0xFFFFFF;
            int bot = (Math.max(0, alpha - 0x10) << 24) | 0xFFFFFF;
            GUIRenderer.fillBorder(x, y, w, h, r, top, bot, edge, 1f);
            return;
        }
        gradRRect(x, y, w, h, r, 0xFFFFFF, Math.min(255, alpha + 0x20), Math.max(0, alpha - 0x10));
        rring(x, y, w, h, r, edge);
        Gui.drawRect(x + r, y + 1, x + w - r, y + 2, 0x70FFFFFF);
    }

    /** Công tắc dạng viên thuốc: núm trắng trượt, nền màu nhấn khi bật. */
    private void drawSwitch(String key, int x, int y, int w, int h, boolean on, float hov) {
        if (GUIRenderer.ready()) {
            ToggleRenderer.draw(key, x, y, w, h, on, hov, accent(), accentHi(), AnimationsModule.menu());
            return;
        }
        float a = on ? 1f : 0f;
        int off = Draw.lerp(0x55556678, 0x77556678, hov);
        int on = Draw.lerp(accent(), accentHi(), hov);
        Draw.rrect(x, y, x + w, y + h, h / 2, Draw.lerp(off, on, a));
        rring(x, y, w, h, h / 2, 0x55FFFFFF);
        int kd = h - 4;
        int kx = x + 2 + Math.round((w - kd - 4) * a);
        Draw.disc(kx + kd / 2f, y + h / 2f + 0.5f, kd / 2f + 1f, 0x30000000);
        Draw.disc(kx + kd / 2f, y + h / 2f, kd / 2f, 0xFFFFFFFF);
    }

    /** Biểu tượng pixel 8x8 phóng {@code sc} lần. */
    private void iconPx(String id, float x, float y, float sc, int argb) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0f);
        GlStateManager.scale(sc, sc, 1f);
        Icons.draw(id, 0, 0, 1, fa(argb));
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** Logo ZHWII: khung vuông bo góc đỏ + chữ Z (logo riêng của client). */
    private static void drawLogo(int x, int y, int s) {
        rring(x, y, s, s, s / 4, OP_RED);
        rring(x + 1, y + 1, s - 2, s - 2, s / 4 - 1, OP_RED);
        rring(x + 2, y + 2, s - 4, s - 4, s / 4 - 2, OP_RED);
        float m = s * 0.28f, t = 3f;
        Draw.line(x + m, y + m + 1f, x + s - m, y + m + 1f, t, OP_RED);
        Draw.line(x + s - m, y + m + 1f, x + m, y + s - m - 1f, t, OP_RED);
        Draw.line(x + m, y + s - m - 1f, x + s - m, y + s - m - 1f, t, OP_RED);
    }

    /** Vẽ một vật phẩm / block Minecraft ở toạ độ (x, y), cạnh 16 * s điểm ảnh. */
    private void drawItem(ItemStack st, float x, float y, float s) {
        if (st == null) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0f);
        GlStateManager.scale(s, s, 1f);
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(st, 0, 0);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.popMatrix();
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** Nút kính / nút màu nhấn bo tròn. */
    private void button(int x, int y, int w, int h, String label, float hov, boolean primary, int textColor) {
        int r = Math.min(h / 2, 8);
        if (primary) {
            gradRRect(x, y, w, h, r, Draw.lerp(accent(), accentHi(), hov) & 0xFFFFFF, 0xFF, 0xD8);
            rring(x, y, w, h, r, 0xAAFFFFFF);
            Gui.drawRect(x + r, y + 1, x + w - r, y + 2, 0x66FFFFFF);
        } else {
            glassCard(x, y, w, h, r, Math.round(0x38 + 0x30 * hov), 0xB0FFFFFF);
        }
        if (label != null) {
            String t = fit(label, w - 6);
            txtC(t, x + w / 2f, y + (h - 8) / 2f, textColor);
        }
    }

    // ------------------------------------------------------------------ dữ liệu thật: FPS / ping / trạng thái

    private int pingMs() {
        if (mc.thePlayer != null && mc.getNetHandler() != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            if (info != null) return info.getResponseTime();
        }
        return 0;
    }

    private String statusText() {
        if (mc.getCurrentServerData() != null) return "Online";
        if (mc.isSingleplayer()) return "Singleplayer";
        return "Offline";
    }

    private int statusColor() {
        return mc.getCurrentServerData() != null || mc.isSingleplayer() ? GREEN : 0xFF9AA5B4;
    }

    private String playerName() {
        return mc.getSession() == null ? "Player" : mc.getSession().getUsername();
    }

    // ------------------------------------------------------------------ vẽ: màn hình

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        ThemeModule.apply();
        if (detail && module != null && module.consumeUiDirty()) buildRows();
        curMx = vx(mx);
        curMy = vy(my);
        float op = openProgress();

        long nowNs = System.nanoTime();
        float dt = lastFrameNs == 0L ? 0.016f : Math.min(0.1f, (nowNs - lastFrameNs) / 1e9f);
        lastFrameNs = nowNs;
        scrollK = AnimationsModule.menu() ? 1f - (float) Math.exp(-dt * 14f) : 1f;
        gridScroll += (gridTarget - gridScroll) * scrollK;
        if (Math.abs(gridTarget - gridScroll) < 0.4f) gridScroll = gridTarget;
        setScroll += (setTarget - setScroll) * scrollK;
        if (Math.abs(setTarget - setScroll) < 0.4f) setScroll = setTarget;

        // blur thế giới (framebuffer + Gaussian 2 lượt) rồi vẽ nền blur; GUI vẽ SAU nên chữ / icon không bị blur
        float oc = Math.max(0f, Math.min(1f, op));
        fade = oc;
        GUIRenderer.beginFrame(mc, width, height, oc);
        boolean blurred = blurOn || GUIRenderer.blurActive();
        if (blurred) Gui.drawRect(0, 0, width, height, (Math.round(0x16 * oc) << 24) | 0xFFFFFF);
        else Gui.drawRect(0, 0, width, height, (Math.round(0x40 * oc) << 24) | 0x0A1424);

        GlStateManager.pushMatrix();
        float pop = 0.90f + 0.10f * op;
        if (!closing && AnimationsModule.menu()) {
            pop = 0.84f + 0.16f * AnimationsModule.easeOutBack((System.currentTimeMillis() - openStart) * AnimationsModule.speed() / 380f);
        }
        float kk = pop * ui();
        GlStateManager.translate(width / 2f, height / 2f + (1f - op) * 10f, 0f);
        GlStateManager.scale(kk, kk, 1f);
        GlStateManager.translate(-VW / 2f, -VH / 2f, 0f);

        // cột trái trượt vào từ trái, cột phải từ phải, cột giữa từ dưới lên
        GlStateManager.pushMatrix();
        GlStateManager.translate(-(1f - panelIn(0)) * 22f, 0f, 0f);
        drawSidebar(curMx, curMy);
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        GlStateManager.translate(0f, (1f - panelIn(1)) * 14f, 0f);
        drawHeader(curMx, curMy);
        if (detail && module != null) drawDetail(curMx, curMy);
        else drawModules(curMx, curMy);
        drawFooter(curMx, curMy);
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        GlStateManager.translate((1f - panelIn(2)) * 22f, 0f, 0f);
        drawRight(curMx, curMy);
        GlStateManager.popMatrix();

        GlStateManager.popMatrix();
        GUIRenderer.endFrame();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    // ----------------------------------------------------------- cột trái

    private void drawNavItem(String key, String label, String icon, boolean sel, int x, int y, int w, int mx, int my) {
        float h = hv(key, in(mx, my, x, y, w, NAV_H));
        boolean shader = GUIRenderer.ready();
        float s = shader ? CategoryRenderer.selected(key, sel, AnimationsModule.menu()) : (sel ? 1f : 0f);
        if (!shader && sel) {
            gradRRect(x, y, w, NAV_H, 10, accentHi() & 0xFFFFFF, 0xF2, 0xC0);
            rring(x, y, w, NAV_H, 10, 0xB0FFFFFF);
            Gui.drawRect(x + 10, y + 1, x + w - 10, y + 2, 0x80FFFFFF);
        } else if (!sel && h > 0.02f) {
            gradRRect(x, y, w, NAV_H, 10, 0xFFFFFF, Math.round(0x50 * h * (1f - s)), Math.round(0x30 * h * (1f - s)));
        }
        int col = Draw.lerp(Draw.lerp(INK2, INK, h), Skin.onAccent(), s);
        iconPx(icon, x + 8, y + (NAV_H - 16) / 2f, 2f, col);
        txt(fit(label, w - 34), x + 30, y + (NAV_H - 8) / 2f, col);
    }

    private void drawSidebar(int mx, int my) {
        glassPanel(SB_X, SB_Y, SB_W, SB_H, 18, 0x5A, true);

        drawLogo(SB_X + (SB_W - 26) / 2, SB_Y + 10, 26);
        if (GUIRenderer.ready()) { // viên chọn trượt giữa các nhóm
            CategoryRenderer.drawSelection(NAV_X, navY(tabIndex()), NAV_W, NAV_H, 10, accentHi(), AnimationsModule.menu());
        }
        txtC("§lZHWII CLIENT", SB_X + SB_W / 2f, SB_Y + 43, INK);
        txtC("Minecraft 1.8.9", SB_X + SB_W / 2f, SB_Y + 55, INK3);

        for (int i = 0; i < tabCount(); i++) {
            drawNavItem("nav" + i, tabLabel(i), tabIcon(i), i == tabIndex(), NAV_X, navY(i), NAV_W, mx, my);
        }
        int sepY = extraY(0) - 6;
        Gui.drawRect(NAV_X + 6, sepY, NAV_X + NAV_W - 6, sepY + 1, 0x33000000);
        drawNavItem("hud", "Sửa HUD", "scoreboard", false, NAV_X, extraY(0), NAV_W, mx, my);
        drawNavItem("prof", "Hồ sơ", "gear", false, NAV_X, extraY(1), NAV_W, mx, my);

        int by = SB_Y + SB_H - 40;
        Gui.drawRect(NAV_X + 6, by, NAV_X + NAV_W - 6, by + 1, 0x33000000);
        Draw.disc(SB_X + 17f, by + 13f, 3.2f, statusColor());
        txt(statusText(), SB_X + 26, by + 9, INK);
        txt("FPS: " + Minecraft.getDebugFPS() + "  |  Ping: " + pingMs() + "ms", SB_X + 12, by + 22, INK3);
    }

    // ----------------------------------------------------------- cột giữa

    private void drawHeader(int mx, int my) {
        glassPanel(MAIN_X, HEAD_Y, MAIN_W, HEAD_H, 18, 0x5A, true);
        drawLogo(MAIN_X + 14, HEAD_Y + 14, 28);
        Gui.drawRect(MAIN_X + 54, HEAD_Y + 14, MAIN_X + 55, HEAD_Y + 42, 0x44000000);
        txtS("§lZHWII CLIENT", MAIN_X + 64, HEAD_Y + 12, INK, 1.7f);
        txt("Better. Smoother. Faster.", MAIN_X + 64, HEAD_Y + 33, INK2);

        String ver = "v" + LunarLite.VERSION;
        int pw = fontRendererObj.getStringWidth(ver) + 16;
        int px = MAIN_X + MAIN_W - 14 - pw, py = HEAD_Y + HEAD_H - 24;
        glassCard(px, py, pw, 14, 7, 0x50, 0xB0FFFFFF);
        txtC(ver, px + pw / 2f, py + 3, INK2);

        float h = hv("x", in(mx, my, CLOSE_X, CLOSE_Y, CLOSE_S, CLOSE_S));
        Draw.disc(CLOSE_X + CLOSE_S / 2f, CLOSE_Y + CLOSE_S / 2f, CLOSE_S / 2f, Draw.lerp(0x40FFFFFF, 0x99FFFFFF, h));
        txtC("x", CLOSE_X + CLOSE_S / 2f, CLOSE_Y + 4, Draw.lerp(INK2, RED, h));
    }

    private void drawModules(int mx, int my) {
        glassPanel(MAIN_X, MOD_Y, MAIN_W, MOD_H, 18, 0x5A, false);

        iconPx(tabIcon(tabIndex()), MAIN_X + 16, MOD_Y + 11, 2f, INK);
        txtS("§l" + tabLabel(tabIndex()), MAIN_X + 40, MOD_Y + 9, INK, 1.2f);
        txt(shown.size() + " tính năng", MAIN_X + 40, MOD_Y + 22, INK3);
        Gui.drawRect(MAIN_X + 14, MOD_Y + 37, MAIN_X + MAIN_W - 14, MOD_Y + 38, 0x30000000);

        clip(GRID_X - 3, GRID_Y - 2, GRID_W + 6, GRID_B - GRID_Y + 4);
        int cw = cardW();
        for (int i = 0; i < shown.size(); i++) {
            int x = cardX(i), y = cardY(i);
            if (y + CARD_H < GRID_Y - 4 || y > GRID_B + 4) continue;
            float st = stagger(i);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0f, (1f - st) * 12f, 0f);
            drawCard(shown.get(i), i, x, y, cw, mx, my);
            GlStateManager.popMatrix();
        }
        Draw.endScissor();

        if (shown.isEmpty()) txtC("Chưa có tính năng nào", MAIN_X + MAIN_W / 2f, MOD_Y + MOD_H / 2f, INK3);

        float max = gridMax();
        if (max > 0) {
            int trackH = GRID_B - GRID_Y;
            int th = Math.max(20, Math.round(trackH * trackH / (float) gridContentH()));
            int ty = GRID_Y + Math.round((trackH - th) * (gridScroll / max));
            int sx = MAIN_X + MAIN_W - 7;
            Draw.rrect(sx, GRID_Y, sx + 3, GRID_B, 1, 0x28000000);
            Draw.rrect(sx, ty, sx + 3, ty + th, 1, accent());
        }
    }

    private void drawCard(Module m, int i, int x, int y, int w, int mx, int my) {
        boolean pressed = beginPress("card" + i, x, y, w, CARD_H);
        drawCardBody(m, i, x, y, w, mx, my);
        endPress(pressed);
    }

    private void drawCardBody(Module m, int i, int x, int y, int w, int mx, int my) {
        boolean inGrid = my >= GRID_Y && my < GRID_B;
        boolean over = inGrid && in(mx, my, x, y, w, CARD_H);
        float h = hv("card" + i, over);
        boolean on = m.enabled;

        glassCard(x, y, w, CARD_H, 12, Math.round(0x58 + 0x30 * h), over ? 0xFFFFFFFF : 0xB4FFFFFF);
        if (over) rring(x - 1, y - 1, w + 2, CARD_H + 2, 13, (0x70 << 24) | (accentHi() & 0xFFFFFF));

        // icon đơn sắc trong ô kính tròn
        int ix = x + 8, iy = y + (CARD_H - 28) / 2;
        gradRRect(ix, iy, 28, 28, 9, 0xFFFFFF, 0x70, 0x38);
        rring(ix, iy, 28, 28, 9, 0x80FFFFFF);
        iconPx(m.id, ix + 6, iy + 6, 2f, on ? accentDk() : INK2);

        // tên + mô tả
        int tx = x + 44, tw = w - 44 - 46;
        txt(fit(Lang.vi(m.name), tw), tx, y + 12, on ? INK : INK2);
        String d = m.description == null ? "" : Lang.vi(m.description);
        txt(fit(d, tw), tx, y + 25, INK3);

        // công tắc
        boolean overTog = inGrid && in(mx, my, x + w - 46, y + 6, 40, CARD_H - 12);
        drawSwitch("tog:" + m.id, x + w - 40, y + (CARD_H - 16) / 2, 30, 16, on, hv("tog" + i, overTog));
    }

    // ----------------------------------------------------------- tuỳ chỉnh một tính năng

    private void drawDetail(int mx, int my) {
        Module m = module;
        glassPanel(MAIN_X, MOD_Y, MAIN_W, MOD_H, 18, 0x5A, false);

        // thanh trên: quay lại, icon + tên, công tắc bật / tắt
        int by = MOD_Y + 9;
        button(COL_X, by, 68, 20, "< Quay lại", hv("back", in(mx, my, COL_X, by, 68, 20)), false, INK);
        iconPx(m.id, COL_X + 80, by + 2, 2f, m.enabled ? accentDk() : INK2);
        txt("§l" + fit(Lang.vi(m.name), COL_W - 80 - 22 - 70), COL_X + 102, by + 6, INK);
        int sx = COL_X + COL_W - 34;
        txtR(m.enabled ? "ON" : "OFF", sx - 6, by + 6, m.enabled ? accentDk() : INK3);
        drawSwitch("en:" + m.id, sx, by + 2, 34, 18, m.enabled, hv("en", in(mx, my, sx - 30, by, 64, 22)));

        for (int i = 0; i < descLines.length; i++) txt(descLines[i], COL_X + 2, DET_Y + i * 10, INK2);

        int top = listTop(), vh = listViewH();
        glassCard(COL_X - 2, top - 4, COL_W + 4, vh + 8, 12, 0x30, 0x90FFFFFF);

        clip(COL_X, top - 2, COL_W, vh + 4);
        int rx = COL_X + 10, rw = COL_W - 20;
        for (int i = 0; i < rows.size(); i++) {
            SRow r = rows.get(i);
            int y = top + r.y - Math.round(setScroll);
            if (y + r.h < top - 2 || y > top + vh + 2) continue;
            drawRow(r, i, rx, y, rw, mx, my, top, vh);
        }
        Draw.endScissor();

        float max = setMax();
        if (max > 0) {
            int th = Math.max(20, Math.round(vh * vh / (float) contentH));
            int ty = top + Math.round((vh - th) * (setScroll / max));
            int sbx = MAIN_X + MAIN_W - 7;
            Draw.rrect(sbx, top, sbx + 3, top + vh, 1, 0x28000000);
            Draw.rrect(sbx, ty, sbx + 3, ty + th, 1, accent());
        }
    }

    private int trackX(int rx, int rw) {
        return rx + rw - 46 - 10 - TRACK_W;
    }

    private void drawRow(SRow r, int i, int rx, int y, int rw, int mx, int my, int top, int vh) {
        boolean inView = my >= top && my < top + vh;
        boolean over = inView && in(mx, my, rx - 4, y, rw + 8, r.h);
        float h = hv("row" + i, over);
        int ty = y + (r.h - 8) / 2;

        switch (r.kind) {
            case R_HEAD: {
                txt(r.text, rx, y + 8, accentDk());
                Gui.drawRect(rx, y + r.h - 3, rx + rw, y + r.h - 2, 0x30000000);
                break;
            }
            case R_BOOL: {
                BoolSetting b = (BoolSetting) r.s;
                if (over) gradRRect(rx - 4, y + 1, rw + 8, r.h - 2, 8, 0xFFFFFF, 0x40, 0x30);
                txt(fit(Lang.vi(r.s.label), rw - 44), rx, ty, INK);
                drawSwitch("set:" + module.id + ":" + r.s.key, rx + rw - 30, y + (r.h - 16) / 2, 30, 16, b.value, h);
                break;
            }
            case R_MODE: {
                ModeSetting ms = (ModeSetting) r.s;
                txt(fit(Lang.vi(r.s.label), rw - 130), rx, ty, INK);
                int bx = rx + rw - 120, by = y + (r.h - 18) / 2;
                button(bx, by, 120, 18, Lang.vi(ms.get()), h, true, Skin.onAccent());
                break;
            }
            case R_COLOR: {
                ColorSetting cs = (ColorSetting) r.s;
                txt(fit(Lang.vi(r.s.label), rw - 130), rx, ty, INK);
                int sx = rx + rw - 120;
                txt(colorName(cs.value), sx, ty, INK2);
                int bx = rx + rw - 46, by = y + (r.h - 16) / 2;
                Draw.rrect(bx - 1, by - 1, bx + 47, by + 17, 8, Draw.lerp(0xAAFFFFFF, 0xFFFFFFFF, h));
                Draw.rrect(bx, by, bx + 46, by + 16, 7, 0xFF000000 | cs.value);
                break;
            }
            case R_SLIDER: {
                SliderSetting ss = (SliderSetting) r.s;
                int tx = trackX(rx, rw);
                txt(fit(Lang.vi(r.s.label), tx - rx - 10), rx, ty, INK);
                int cy = y + r.h / 2;
                int hx = tx + Math.round((float) ss.ratio() * (TRACK_W - 8));
                Draw.rrect(tx, cy - 1, tx + TRACK_W, cy + 2, 1, 0x40000000);
                Draw.rrect(tx, cy - 1, hx + 4, cy + 2, 1, accent());
                Draw.disc(hx + 4f, cy + 0.5f, 6.6f, 0x30000000);
                Draw.disc(hx + 4f, cy + 0.5f, 5.8f, Draw.lerp(0xFFFFFFFF, accentHi(), h));
                Draw.disc(hx + 4f, cy + 0.5f, 3.2f, accent());
                int vx0 = rx + rw - 46;
                glassCard(vx0, y + (r.h - 16) / 2, 46, 16, 8, 0x60, 0xB0FFFFFF);
                txtC(fit(ss.display(), 42), vx0 + 23, ty, INK);
                break;
            }
            case R_ACTION: {
                int bw = Math.min(240, rw);
                int bx = rx + (rw - bw) / 2, by = y + 3;
                button(bx, by, bw, r.h - 6, null, h, false, INK);
                int tw = fontRendererObj.getStringWidth(r.text);
                int ix = bx + (bw - tw) / 2;
                if (r.a != null && r.a.icon != null) {
                    drawItem(r.a.icon, ix - 12, by + (r.h - 6 - 12) / 2f, 0.75f);
                    ix += 8;
                }
                txt(r.text, ix, by + (r.h - 6 - 8) / 2f, INK);
                break;
            }
            case R_RESET: {
                int bw = Math.min(180, rw);
                int bx = rx + (rw - bw) / 2, by = y + 6;
                button(bx, by, bw, 22, r.text, h, false, RED);
                break;
            }
            default:
                break;
        }
    }

    // ----------------------------------------------------------- chân trang

    private void drawFooter(int mx, int my) {
        glassPanel(MAIN_X, FOOT_Y, MAIN_W, FOOT_H, 16, 0x5A, true);
        // ba cột "sóng" - cao dần theo FPS hiện tại
        int fps = Minecraft.getDebugFPS();
        int lvl = fps >= 100 ? 3 : fps >= 50 ? 2 : 1;
        int bx = MAIN_X + 18, base = FOOT_Y + FOOT_H - 12;
        for (int k = 0; k < 3; k++) {
            int bh = 8 + k * 7;
            Draw.rrect(bx + k * 8, base - bh, bx + k * 8 + 5, base, 2, k < lvl ? accent() : 0x40FFFFFF);
        }
        txt("§lZHWII CLIENT", MAIN_X + 56, FOOT_Y + 11, INK);
        txt("Enjoy your game!", MAIN_X + 56, FOOT_Y + 23, INK3);
        txtR("Esc: đóng  |  < >: đổi nhóm", MAIN_X + MAIN_W - 16, FOOT_Y + (FOOT_H - 8) / 2f, INK3);
    }

    // ----------------------------------------------------------- cột phải

    private void drawAvatar(int x, int y, int s) {
        ResourceLocation skin = DefaultPlayerSkin.getDefaultSkinLegacy();
        if (mc.thePlayer != null && mc.getNetHandler() != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            if (info != null) skin = info.getLocationSkin();
        }
        Draw.rrect(x - 1, y - 1, x + s + 1, y + s + 1, 6, 0xB0FFFFFF);
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        mc.getTextureManager().bindTexture(skin);
        Gui.drawScaledCustomSizeModalRect(x, y, 8f, 8f, 8, 8, s, s, 64f, 64f);
        Gui.drawScaledCustomSizeModalRect(x, y, 40f, 8f, 8, 8, s, s, 64f, 64f);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawRight(int mx, int my) {
        // ---- thẻ người chơi (bấm = mở Hồ sơ)
        glassPanel(RP_X, PROF_Y, RP_W, PROF_H, 18, 0x5A, true);
        float hp = hv("pcard", in(mx, my, RP_X, PROF_Y, RP_W, PROF_H));
        drawAvatar(RP_X + 12, PROF_Y + 12, 32);
        txt("§l" + fit(playerName(), RP_W - 48 - 28), RP_X + 52, PROF_Y + 14, INK);
        String pm = "Premium";
        int pw = fontRendererObj.getStringWidth(pm) + 16;
        gradRRect(RP_X + 52, PROF_Y + 28, pw, 14, 7, 0x7A4BE0, 0xF0, 0xB8);
        rring(RP_X + 52, PROF_Y + 28, pw, 14, 7, 0x80FFFFFF);
        txtC(pm, RP_X + 52 + pw / 2f, PROF_Y + 31, 0xFFFFFFFF);
        Draw.arrow(RP_X + RP_W - 18, PROF_Y + PROF_H / 2f, 4f + hp * 1.5f, 4f, false, Draw.lerp(INK3, INK, hp));

        // ---- Client Info
        glassPanel(RP_X, INFO_Y, RP_W, INFO_H, 18, 0x5A, false);
        Draw.disc(RP_X + 21f, INFO_Y + 17f, 7f, INK);
        txtC("i", RP_X + 21f, INFO_Y + 13, 0xFFFFFFFF);
        txt("§lClient Info", RP_X + 34, INFO_Y + 13, INK);
        Gui.drawRect(RP_X + 12, INFO_Y + 28, RP_X + RP_W - 12, INFO_Y + 29, 0x30000000);
        String[] lab = {"Version", "Minecraft", "Status"};
        String[] val = {LunarLite.VERSION, "1.8.9", statusText()};
        for (int k = 0; k < 3; k++) {
            int ry = INFO_Y + 36 + k * 15;
            txt(lab[k], RP_X + 14, ry, INK2);
            if (k == 2) {
                int vw = fontRendererObj.getStringWidth(val[k]);
                Draw.disc(RP_X + RP_W - 14 - vw - 8f, ry + 4f, 3.2f, statusColor());
            }
            txtR(val[k], RP_X + RP_W - 14, ry, INK);
        }

        // ---- Quick Settings
        glassPanel(RP_X, QS_Y, RP_W, QS_H, 18, 0x5A, false);
        iconPx("gear", RP_X + 14, QS_Y + 7, 1.6f, INK);
        txt("§lQuick Settings", RP_X + 34, QS_Y + 13, INK);
        Gui.drawRect(RP_X + 12, QS_Y + 28, RP_X + RP_W - 12, QS_Y + 29, 0x30000000);
        int rowX = RP_X + 14, rowW = RP_W - 28;
        qsRow(0, "Blur Background", mx, my, rowX, rowW, LunarLite.menuBlur);
        qsRow(1, "Animations", mx, my, rowX, rowW, AnimationsModule.menu());
        // Theme: hiện tên bảng màu + chấm màu nhấn, bấm để đổi
        int ty = QS_ROW_Y + 2 * QS_ROW_H + 6;
        float ht = hv("qtheme", in(mx, my, RP_X + 6, ty - 4, RP_W - 12, QS_ROW_H));
        if (ht > 0.02f) gradRRect(RP_X + 6, ty - 5, RP_W - 12, QS_ROW_H - 2, 8, 0xFFFFFF, Math.round(0x40 * ht), Math.round(0x28 * ht));
        txt("Theme", rowX, ty, INK2);
        Draw.arrow(RP_X + RP_W - 18, ty + 4f, 3f, 3.5f, false, INK3);
        String tn = fit(ThemeModule.presetName(), 60);
        txtR(tn, RP_X + RP_W - 26, ty, INK);
        Draw.disc(RP_X + RP_W - 26 - fontRendererObj.getStringWidth(tn) - 8f, ty + 4f, 4f, Draw.ACCENT);

        // ---- Resume Game
        float hr = hv("resume", in(mx, my, RP_X, RESUME_Y, RP_W, RESUME_H));
        boolean resPressed = beginPress("resume", RP_X, RESUME_Y, RP_W, RESUME_H);
        int sc = (int) (hr * 0x10);
        gradRRect(RP_X, RESUME_Y, RP_W, RESUME_H, 16, Draw.lerp(accent(), accentHi(), hr) & 0xFFFFFF, 0xFF, 0xC8 + sc);
        rring(RP_X, RESUME_Y, RP_W, RESUME_H, 16, 0xC0FFFFFF);
        Gui.drawRect(RP_X + 16, RESUME_Y + 1, RP_X + RP_W - 16, RESUME_Y + 2, 0x90FFFFFF);
        int oc = Skin.onAccent();
        int tw = fontRendererObj.getStringWidth("Resume Game");
        int cx0 = RP_X + (RP_W - tw - 18) / 2;
        Draw.arrow(cx0, RESUME_Y + RESUME_H / 2f, 8f, 6f, false, oc);
        txt("§lResume Game", cx0 + 18, RESUME_Y + (RESUME_H - 8) / 2f, oc);
        endPress(resPressed);
    }

    private void qsRow(int k, String label, int mx, int my, int rowX, int rowW, boolean on) {
        int ry = QS_ROW_Y + k * QS_ROW_H;
        float h = hv("q" + k, in(mx, my, RP_X + 6, ry - 3, RP_W - 12, QS_ROW_H));
        if (h > 0.02f) gradRRect(RP_X + 6, ry - 3, RP_W - 12, QS_ROW_H - 2, 8, 0xFFFFFF, Math.round(0x40 * h), Math.round(0x28 * h));
        txt(label, rowX, ry + 4, INK2);
        drawSwitch("q" + k, rowX + rowW - 30, ry, 30, 16, on, h);
    }

    // ----------------------------------------------------------- vị trí các vùng bấm

    /** Trả về khoá của vùng bấm tại (mx, my) trong khung ảo, hoặc null. */
    private String hit(int mx, int my) {
        // cột phải + nút đóng + sidebar luôn bấm được
        for (int i = 0; i < tabCount(); i++) {
            if (in(mx, my, NAV_X, navY(i), NAV_W, NAV_H)) return "nav" + i;
        }
        if (in(mx, my, NAV_X, extraY(0), NAV_W, NAV_H)) return "hud";
        if (in(mx, my, NAV_X, extraY(1), NAV_W, NAV_H)) return "prof";
        if (in(mx, my, CLOSE_X, CLOSE_Y, CLOSE_S, CLOSE_S)) return "x";
        if (in(mx, my, RP_X, PROF_Y, RP_W, PROF_H)) return "pcard";
        if (in(mx, my, RP_X + 6, QS_ROW_Y - 3, RP_W - 12, QS_ROW_H)) return "qblur";
        if (in(mx, my, RP_X + 6, QS_ROW_Y + QS_ROW_H - 3, RP_W - 12, QS_ROW_H)) return "qanim";
        if (in(mx, my, RP_X + 6, QS_ROW_Y + 2 * QS_ROW_H + 2, RP_W - 12, QS_ROW_H)) return "qtheme";
        if (in(mx, my, RP_X, RESUME_Y, RP_W, RESUME_H)) return "resume";

        if (detail && module != null) {
            int by = MOD_Y + 9;
            if (in(mx, my, COL_X, by, 68, 20)) return "back";
            if (in(mx, my, COL_X + COL_W - 64, by, 64, 22)) return "en";
            int top = listTop(), vh = listViewH();
            if (my < top || my >= top + vh || mx < COL_X || mx >= COL_X + COL_W) return null;
            int rx = COL_X + 10, rw = COL_W - 20;
            for (int i = 0; i < rows.size(); i++) {
                SRow r = rows.get(i);
                int y = top + r.y - Math.round(setScroll);
                if (my < y || my >= y + r.h) continue;
                switch (r.kind) {
                    case R_BOOL:
                        return "row" + i;
                    case R_MODE:
                        return in(mx, my, rx + rw - 120, y + (r.h - 18) / 2, 120, 18) ? "row" + i : null;
                    case R_COLOR:
                        return in(mx, my, rx + rw - 46, y + (r.h - 16) / 2, 46, 16) ? "row" + i : null;
                    case R_SLIDER:
                        return mx >= trackX(rx, rw) - 6 && mx < trackX(rx, rw) + TRACK_W + 6 ? "row" + i : null;
                    case R_ACTION: {
                        int bw = Math.min(240, rw);
                        return in(mx, my, rx + (rw - bw) / 2, y + 3, bw, r.h - 6) ? "row" + i : null;
                    }
                    case R_RESET: {
                        int bw = Math.min(180, rw);
                        return in(mx, my, rx + (rw - bw) / 2, y + 6, bw, 22) ? "row" + i : null;
                    }
                    default:
                        return null;
                }
            }
            return null;
        }
        if (my >= GRID_Y && my < GRID_B) {
            int cw = cardW();
            for (int i = 0; i < shown.size(); i++) {
                int x = cardX(i), y = cardY(i);
                if (!in(mx, my, x, y, cw, CARD_H)) continue;
                if (in(mx, my, x + cw - 46, y + 6, 40, CARD_H - 12)) return "tog" + i;
                return "card" + i; // chạm vào đâu khác trong thẻ = mở tuỳ chỉnh
            }
        }
        return null;
    }

    private static int idx(String key, int prefixLen) {
        try {
            return Integer.parseInt(key.substring(prefixLen));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void perform(String key, boolean right) {
        if (key == null) return;
        if (key.startsWith("nav")) {
            play("menu.click");
            if (detail) detail = false;
            setTab(idx(key, 3));
        } else if (key.equals("prof") || key.equals("pcard")) {
            play("menu.click");
            mc.displayGuiScreen(new ProfileScreen());
        } else if (key.equals("hud")) {
            play("menu.click");
            mc.displayGuiScreen(new HudEditorScreen());
        } else if (key.equals("x") || key.equals("resume")) {
            requestClose();
        } else if (key.equals("qblur")) {
            LunarLite.menuBlur = !LunarLite.menuBlur;
            if (!GUIRenderer.blurSupported()) {
                if (LunarLite.menuBlur) startBlur();
                else stopBlur();
            }
            play(LunarLite.menuBlur ? "menu.on" : "menu.off");
        } else if (key.equals("qanim")) {
            AnimationsModule.toggleMenu();
            play(AnimationsModule.menu() ? "menu.on" : "menu.off");
        } else if (key.equals("qtheme")) {
            play("menu.click");
            ThemeModule.cyclePreset();
        } else if (key.startsWith("tog")) {
            int i = idx(key, 3);
            if (i >= 0 && i < shown.size()) toggleModule(shown.get(i));
        } else if (key.startsWith("card")) {
            int i = idx(key, 4);
            if (i >= 0 && i < shown.size()) {
                if (right) toggleModule(shown.get(i));
                else openModule(shown.get(i));
            }
        } else if (key.equals("back")) {
            goBack();
        } else if (key.equals("en")) {
            if (module != null) toggleModule(module);
        } else if (key.startsWith("row")) {
            int i = idx(key, 3);
            if (i < 0 || i >= rows.size()) return;
            performRow(rows.get(i), right);
        }
    }

    private void performRow(SRow r, boolean right) {
        Module m = module;
        switch (r.kind) {
            case R_BOOL: {
                long before = visSig(m);
                BoolSetting b = (BoolSetting) r.s;
                b.toggle();
                play(b.value ? "menu.on" : "menu.off");
                if (visSig(m) != before) buildRows();
                break;
            }
            case R_MODE: {
                play("menu.click");
                if (right) ((ModeSetting) r.s).prev();
                else ((ModeSetting) r.s).next();
                break;
            }
            case R_COLOR: {
                play("menu.click");
                ColorSetting col = (ColorSetting) r.s;
                int n = ColorSetting.PRESETS.length, i = presetIndex(col.value);
                int next = !right ? (i + 1) % n : (i < 0 ? n - 1 : (i + n - 1) % n);
                col.value = ColorSetting.PRESETS[next];
                break;
            }
            case R_ACTION:
                play("menu.click");
                r.a.run.run();
                buildRows();
                break;
            case R_RESET:
                play("menu.click");
                m.resetSettings();
                if (m instanceof HudModule) ((HudModule) m).resetPosition();
                buildRows();
                break;
            default:
                break;
        }
    }

    private void setSliderFromMouse(SRow r, int mx) {
        int rx = COL_X + 10, rw = COL_W - 20;
        float v = (mx - trackX(rx, rw) - 4) / (float) (TRACK_W - 8);
        ((SliderSetting) r.s).setRatio(Math.max(0f, Math.min(1f, v)));
    }

    // ----------------------------------------------------------- chuột

    @Override
    protected void mouseClicked(int rawX, int rawY, int btn) throws IOException {
        if (closing) return;
        int mx = vx(rawX), my = vy(rawY);
        String key = hit(mx, my);
        if (btn == 1) { // chuột phải: bật / tắt thẻ, hoặc đổi chế độ lùi lại
            if (key != null) perform(key, true);
            return;
        }
        if (btn == 2 && module != null) { // chuột giữa: mở màn cài đặt module kiểu tối
            mc.displayGuiScreen(new ModuleSettingsScreen(module, this));
            return;
        }
        if (btn != 0) return;
        pressKey = key;
        pressX = mx;
        pressY = my;
        pressScroll = detail ? setTarget : gridTarget;
        dragged = false;
        activeSlider = null;
        if (detail && key != null && key.startsWith("row")) {
            int i = idx(key, 3);
            if (i >= 0 && i < rows.size() && rows.get(i).kind == R_SLIDER) {
                activeSlider = rows.get(i);
                setSliderFromMouse(activeSlider, mx);
            }
        }
    }

    @Override
    protected void mouseClickMove(int rawX, int rawY, int btn, long time) {
        if (btn != 0) return;
        int mx = vx(rawX), my = vy(rawY);
        if (activeSlider != null) {
            setSliderFromMouse(activeSlider, mx);
            return;
        }
        if (!dragged && Math.abs(my - pressY) > 5) dragged = true;
        if (dragged) { // kéo để cuộn (dùng được bằng cảm ứng trên điện thoại)
            if (detail) {
                setTarget = setScroll = pressScroll - (my - pressY);
                clampSet();
            } else {
                gridTarget = gridScroll = pressScroll - (my - pressY);
                clampGrid();
            }
        }
    }

    @Override
    protected void mouseReleased(int rawX, int rawY, int state) {
        if (state != 0) return;
        int mx = vx(rawX), my = vy(rawY);
        if (activeSlider != null) {
            activeSlider = null;
            pressKey = null;
            return;
        }
        String key = pressKey;
        pressKey = null;
        if (key == null || dragged) return;
        if (key.equals(hit(mx, my))) perform(key, false);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d == 0) return;
        if (detail) {
            setTarget += (d > 0 ? -1 : 1) * 2f * ROW_H;
            clampSet();
        } else {
            gridTarget += (d > 0 ? -1 : 1) * (CARD_H + GAP) * 0.75f;
            clampGrid();
        }
    }

    // ----------------------------------------------------------- bàn phím

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) {
            if (detail) goBack();
            else requestClose();
            return;
        }
        if (LunarLite.keyMenu != null && key == LunarLite.keyMenu.getKeyCode()) {
            requestClose();
            return;
        }
        if (detail) {
            if (key == Keyboard.KEY_LEFT) moveModule(-1);
            else if (key == Keyboard.KEY_RIGHT) moveModule(1);
            else if (key == Keyboard.KEY_UP) {
                setTarget -= ROW_H;
                clampSet();
            } else if (key == Keyboard.KEY_DOWN) {
                setTarget += ROW_H;
                clampSet();
            }
            return;
        }
        if (key == Keyboard.KEY_LEFT) cycleTab(-1);
        else if (key == Keyboard.KEY_RIGHT || key == Keyboard.KEY_TAB) cycleTab(1);
        else if (key == Keyboard.KEY_UP) {
            gridTarget -= (CARD_H + GAP);
            clampGrid();
        } else if (key == Keyboard.KEY_DOWN) {
            gridTarget += (CARD_H + GAP);
            clampGrid();
        }
    }

    @Override
    public void onGuiClosed() {
        stopBlur();
        GUIRenderer.release(); // trả framebuffer blur khi menu đóng
        lastModule = module;
        LunarLite.saveConfig();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
