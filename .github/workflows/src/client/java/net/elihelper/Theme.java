package net.elihelper;

/** Farbthemen fuer das Menue. Auswahl liegt in EliConfig.themeIndex. */
public final class Theme {
    public final String name;
    public final int body, titlebar, footer, accent, edge;
    public final boolean festive; // Halloween: dezenter animierter Akzent

    private Theme(String name, int body, int titlebar, int footer, int accent, int edge, boolean festive) {
        this.name = name; this.body = body; this.titlebar = titlebar; this.footer = footer;
        this.accent = accent; this.edge = edge; this.festive = festive;
    }

    public int sep() { return 0x55000000 | (edge & 0xFFFFFF); }
    public int shadow() { return 0x66000000; }

    public static final Theme[] ALL = {
            new Theme("Lila",        0xF21B1430, 0xFF2A1E4D, 0xFF201733, 0xFF7C3AED, 0xFFB388FF, false),
            new Theme("Mitternacht", 0xF2101522, 0xFF18203A, 0xFF141A2E, 0xFF3B82F6, 0xFF8FB7FF, false),
            new Theme("Ozean",       0xF20C1A1E, 0xFF123038, 0xFF0E242A, 0xFF14B8A6, 0xFF7FE8DC, false),
            new Theme("Wald",        0xF2101A10, 0xFF1A2E18, 0xFF142414, 0xFF4CAF50, 0xFF9FE3A0, false),
            new Theme("Feuer",       0xF2201206, 0xFF3A1E0E, 0xFF2A1608, 0xFFF97316, 0xFFFFC487, false),
            new Theme("Rose",        0xF2201020, 0xFF3A1830, 0xFF2A1222, 0xFFEC4899, 0xFFFFA8D6, false),
            new Theme("Halloween",   0xF21A1206, 0xFF2E1E08, 0xFF241706, 0xFFFF7A18, 0xFFFFC078, true),
    };

    public static int count() { return ALL.length; }

    public static Theme current() {
        int i = EliConfig.get().themeIndex;
        if (i < 0 || i >= ALL.length) i = 0;
        return ALL[i];
    }

    public static void cycle() {
        EliConfig c = EliConfig.get();
        c.themeIndex = (c.themeIndex + 1) % ALL.length;
        EliConfig.save();
    }
}
