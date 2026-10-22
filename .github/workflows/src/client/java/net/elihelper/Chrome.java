package net.elihelper;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Gemeinsames Fenster-Aussehen fuer alle EliHelper-Screens, jetzt themenfaehig
 * und mit dezenter Animation. Nutzt bewusst NUR g.fill(...).
 *
 * back(...)  vor super (Panel/Titel/Footer/Schatten), abgesichert, da das
 *            Zeichnen vor super die einzige unsichere Stelle ist.
 * front(...) nach super (scharfe Raender + pulsierender Akzent).
 */
public final class Chrome {
    private Chrome() {}

    private static int scaleAlpha(int color, float f) {
        int a = (color >>> 24) & 0xFF;
        a = Math.max(0, Math.min(255, Math.round(a * f)));
        return (a << 24) | (color & 0xFFFFFF);
    }

    /** 0.70..1.0 pulsierend, fuer den Akzent. */
    private static float pulse() {
        return 0.80f + 0.20f * (float) Math.sin(System.currentTimeMillis() / 480.0);
    }

    public static void back(GuiGraphicsExtractor g, int l, int t, int r, int b, int titleBottom, int footerTop) {
        back(g, l, t, r, b, titleBottom, footerTop, 1.0f);
    }

    /** alpha 0..1 fuer den Einblend-Effekt. */
    public static void back(GuiGraphicsExtractor g, int l, int t, int r, int b, int titleBottom, int footerTop, float alpha) {
        try {
            Theme th = Theme.current();
            g.fill(l + 4, b, r + 4, b + 4, scaleAlpha(th.shadow(), alpha));
            g.fill(r, t + 4, r + 4, b + 4, scaleAlpha(th.shadow(), alpha));
            g.fill(l, t, r, b, scaleAlpha(th.body, alpha));
            g.fill(l, t, r, titleBottom, scaleAlpha(th.titlebar, alpha));
            g.fill(l, footerTop, r, b, scaleAlpha(th.footer, alpha));
            if (th.festive) {
                // dezenter wandernder Schimmer in der Titelleiste
                int w = r - l;
                int sx = l + (int) ((System.currentTimeMillis() / 20) % Math.max(1, w));
                g.fill(sx, t, Math.min(r, sx + 14), titleBottom, scaleAlpha(0x22FFFFFF, alpha));
            }
        } catch (Throwable ignored) {
        }
    }

    public static void front(GuiGraphicsExtractor g, int l, int t, int r, int b, int titleBottom, int footerTop) {
        try {
            Theme th = Theme.current();
            int acc = scaleAlpha(th.accent, pulse());
            g.fill(l, t, r, t + 2, acc);
            g.fill(l, b - 2, r, b, acc);
            g.fill(l, t, l + 2, b, th.edge);
            g.fill(r - 2, t, r, b, th.edge);
            g.fill(l, titleBottom - 1, r, titleBottom, acc);
            g.fill(l, footerTop, r, footerTop + 1, acc);
        } catch (Throwable ignored) {
        }
    }

    public static void separator(GuiGraphicsExtractor g, int x, int y, int w) {
        try {
            g.fill(x, y, x + w, y + 1, Theme.current().sep());
        } catch (Throwable ignored) {
        }
    }

    /** Leuchtender Rahmen um eine Kachel, wenn die Maus drueber ist. */
    public static void hoverGlow(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        try {
            int acc = scaleAlpha(Theme.current().accent, pulse());
            g.fill(x - 1, y - 1, x + w + 1, y, acc);
            g.fill(x - 1, y + h, x + w + 1, y + h + 1, acc);
            g.fill(x - 1, y, x, y + h, acc);
            g.fill(x + w, y, x + w + 1, y + h, acc);
        } catch (Throwable ignored) {
        }
    }
}
