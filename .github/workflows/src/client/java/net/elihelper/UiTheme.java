package net.elihelper;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Persistent accent palettes for the EliHelper screens. */
public final class UiTheme {
    public static final String[] NAMES = {"Violet", "Cyan", "Emerald", "Rose", "Amber"};
    public static final int[] ACCENTS = {0xFF7C3AED, 0xFF00BFEA, 0xFF22C55E, 0xFFFF4F9A, 0xFFFFA928};
    public static final int[] LIGHTS = {0xFFB388FF, 0xFF8BE9FF, 0xFF9AF5B5, 0xFFFFB4D8, 0xFFFFD28A};
    private static int selected;
    private static boolean loaded;

    private UiTheme() {}

    public static void load() {
        if (loaded) return;
        loaded = true;
        try {
            Path path = file();
            if (Files.exists(path)) {
                Properties p = new Properties();
                try (var in = Files.newInputStream(path)) { p.load(in); }
                selected = Math.max(0, Math.min(ACCENTS.length - 1, Integer.parseInt(p.getProperty("preset", "0"))));
            }
        } catch (Exception ignored) { selected = 0; }
        apply();
    }

    public static int selected() { load(); return selected; }
    public static int accent() { load(); return ACCENTS[selected]; }
    public static int light() { load(); return LIGHTS[selected]; }
    public static int text() { return light() & 0x00FFFFFF; }

    public static void choose(int index) {
        load();
        selected = Math.max(0, Math.min(ACCENTS.length - 1, index));
        apply();
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            Properties p = new Properties();
            p.setProperty("preset", Integer.toString(selected));
            try (var out = Files.newOutputStream(path)) { p.store(out, "EliHelper UI theme"); }
        } catch (IOException ignored) { }
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("elihelper-ui.properties");
    }

    private static void apply() {
        Chrome.ACCENT = ACCENTS[selected];
        Chrome.EDGE = LIGHTS[selected];
        Chrome.SEP = (0x55000000 | (LIGHTS[selected] & 0x00FFFFFF));
        Chrome.TITLEBAR = blend(0xFF17131F, ACCENTS[selected], 0.34f);
        Chrome.FOOTER = blend(0xFF17131F, ACCENTS[selected], 0.17f);
        Chrome.BODY = blend(0xFF17131F, ACCENTS[selected], 0.12f);
    }

    private static int blend(int a, int b, float amount) {
        int r = (int)(((a >>> 16) & 255) * (1 - amount) + ((b >>> 16) & 255) * amount);
        int g = (int)(((a >>> 8) & 255) * (1 - amount) + ((b >>> 8) & 255) * amount);
        int bl = (int)((a & 255) * (1 - amount) + (b & 255) * amount);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }
}
