package dev.lunarlite.gui.oneplus;

import java.util.HashMap;
import java.util.Map;

/**
 * Giá trị animation theo thời gian thực (delta time), không phụ thuộc FPS.
 * Gọi {@link #tick()} đúng một lần mỗi khung hình trước khi hỏi giá trị.
 */
public final class AnimationManager {
    private AnimationManager() {}

    private static final Map<String, Float> VALUES = new HashMap<String, Float>();
    private static long lastNs;
    private static float dt = 0.016f;

    public static void tick() {
        long now = System.nanoTime();
        dt = lastNs == 0L ? 0.016f : Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
    }

    /** Thời gian của khung hình trước (giây, tối đa 0.1). */
    public static float dt() {
        return dt;
    }

    /**
     * Tiến giá trị của {@code key} về {@code target} theo hàm mũ (tốc độ {@code speed} ~ 1/giây).
     * Lần đầu gặp khoá, giá trị nhảy thẳng tới đích (không animation lúc mở).
     * {@code animate = false} thì nhảy thẳng tới đích.
     */
    public static float approach(String key, float target, float speed, boolean animate) {
        Float f = VALUES.get(key);
        float v = f == null || !animate ? target : f.floatValue();
        v += (target - v) * (1f - (float) Math.exp(-dt * speed));
        if (Math.abs(target - v) < 0.002f) v = target;
        VALUES.put(key, Float.valueOf(v));
        return v;
    }

    public static float approach(String key, float target, float speed) {
        return approach(key, target, speed, true);
    }

    public static void clear() {
        VALUES.clear();
        lastNs = 0L;
    }
}
