package net.elihelper;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Gemeinsames Fenster-Aussehen fuer alle EliHelper-Screens.
 * Nutzt bewusst NUR g.fill(...), weil das in dieser MC-Version sicher funktioniert.
 *
 * back(...)  wird VOR super.extractRenderState aufgerufen -> liegt hinter den Widgets
 *            (Panel, Titelleiste, Footer, Schatten). Dieses Zeichnen vor super ist der
 *            einzige unsichere Teil, deshalb in try/catch: faellt es aus, bleibt nur das
 *            Panel weg, der Screen rendert normal weiter.
 * front(...) wird NACH super aufgerufen -> scharfe Raender und Akzentlinien liegen oben
 *            (so wie der urspruengliche Rahmen, der schon funktioniert hat).
 */
public final class Chrome {
    public static final int BODY     = 0xF21B1430; // dunkles Lila, fast deckend
    public static final int TITLEBAR = 0xFF2A1E4D;
    public static final int FOOTER   = 0xFF201733;
    public static final int ACCENT   = 0xFF7C3AED; // Haupt-Lila
    public static final int EDGE     = 0xFFB388FF; // helles Lila
    public static final int SEP      = 0x55B388FF; // Trennlinie
    public static final int SHADOW   = 0x66000000;

    private Chrome() {}

    /** Grosse Fuellflaechen – vor super. Abgesichert, da das Zeichnen vor super unsicher ist. */
    public static void back(GuiGraphicsExtractor g, int l, int t, int r, int b, int titleBottom, int footerTop) {
        try {
            g.fill(l + 4, b, r + 4, b + 4, SHADOW);      // Schatten unten
            g.fill(r, t + 4, r + 4, b + 4, SHADOW);      // Schatten rechts
            g.fill(l, t, r, b, BODY);                    // Panel
            g.fill(l, t, r, titleBottom, TITLEBAR);      // Titelleiste
            g.fill(l, footerTop, r, b, FOOTER);          // Footer
        } catch (Throwable ignored) {
            // Rendern laeuft ohne Panel weiter.
        }
    }

    /** Scharfe Raender und Akzente – nach super. */
    public static void front(GuiGraphicsExtractor g, int l, int t, int r, int b, int titleBottom, int footerTop) {
        try {
            g.fill(l, t, r, t + 2, ACCENT);              // oben
            g.fill(l, b - 2, r, b, ACCENT);              // unten
            g.fill(l, t, l + 2, b, EDGE);                // links
            g.fill(r - 2, t, r, b, EDGE);                // rechts
            g.fill(l, titleBottom - 1, r, titleBottom, ACCENT); // Linie unter Titel
            g.fill(l, footerTop, r, footerTop + 1, ACCENT);     // Linie ueber Footer
        } catch (Throwable ignored) {
        }
    }

    /** Duenne Trennlinie zwischen Abschnitten. */
    public static void separator(GuiGraphicsExtractor g, int x, int y, int w) {
        try {
            g.fill(x, y, x + w, y + 1, SEP);
        } catch (Throwable ignored) {
        }
    }
}
