package dev.zxht.client.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/** Small 2D drawing helpers for the glass look (rounded translucent panels). */
public final class Draw {
    private Draw() {}

    public static int argb(int a, int r, int g, int b) {
        return (MathHelper.clamp(a, 0, 255) << 24) | (r << 16) | (g << 8) | b;
    }

    /** Gray level 0-255 with alpha 0-255. */
    public static int gray(int level, int alpha) {
        return argb(alpha, level, level, level);
    }

    public static float ease(float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Horizontal inset of each corner row for radius r (row 0 = outermost row). */
    private static int[] insets(int r) {
        int[] out = new int[r];
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            out[i] = (int) Math.round(r - Math.sqrt(Math.max(0, (double) r * r - dy * dy)));
        }
        return out;
    }

    /** Filled rounded rectangle. */
    public static void roundRect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) {
            c.fill(x, y, x + w, y + h, color);
            return;
        }
        int[] in = insets(r);
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            c.fill(x + in[i], y + i, x + w - in[i], y + i + 1, color);
            c.fill(x + in[i], y + h - i - 1, x + w - in[i], y + h - i, color);
        }
    }

    /** 1px rounded outline. */
    public static void roundOutline(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) {
            c.fill(x, y, x + w, y + 1, color);
            c.fill(x, y + h - 1, x + w, y + h, color);
            c.fill(x, y + 1, x + 1, y + h - 1, color);
            c.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
            return;
        }
        int[] in = insets(r);
        // straight edges
        c.fill(x + r, y, x + w - r, y + 1, color);
        c.fill(x + r, y + h - 1, x + w - r, y + h, color);
        c.fill(x, y + r, x + 1, y + h - r, color);
        c.fill(x + w - 1, y + r, x + w, y + h - r, color);
        // corners
        for (int i = 0; i < r; i++) {
            int from = in[i];
            int to = (i == 0) ? r : Math.max(in[i] + 1, in[i - 1]);
            // top-left / top-right
            c.fill(x + from, y + i, x + to, y + i + 1, color);
            c.fill(x + w - to, y + i, x + w - from, y + i + 1, color);
            // bottom-left / bottom-right
            c.fill(x + from, y + h - i - 1, x + to, y + h - i, color);
            c.fill(x + w - to, y + h - i - 1, x + w - from, y + h - i, color);
        }
    }

    /** A glass panel: dark translucent fill + faint white border. */
    public static void glass(DrawContext c, int x, int y, int w, int h, int r, float alpha) {
        roundRect(c, x, y, w, h, r, argb((int) (115 * alpha), 8, 8, 10));
        roundOutline(c, x, y, w, h, r, argb((int) (70 * alpha), 255, 255, 255));
    }
}
