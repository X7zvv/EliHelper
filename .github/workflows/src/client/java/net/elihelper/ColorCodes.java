package net.elihelper;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Map;

/** Wandelt Text mit &-Farbcodes in eine Vorschau-Komponente um. */
public final class ColorCodes {
    public static final Map<Character, Integer> COLORS = Map.ofEntries(
            Map.entry('0', 0x000000), Map.entry('1', 0x0000AA), Map.entry('2', 0x00AA00), Map.entry('3', 0x00AAAA),
            Map.entry('4', 0xAA0000), Map.entry('5', 0xAA00AA), Map.entry('6', 0xFFAA00), Map.entry('7', 0xAAAAAA),
            Map.entry('8', 0x555555), Map.entry('9', 0x5555FF), Map.entry('a', 0x55FF55), Map.entry('b', 0x55FFFF),
            Map.entry('c', 0xFF5555), Map.entry('d', 0xFF55FF), Map.entry('e', 0xFFFF55), Map.entry('f', 0xFFFFFF));

    private ColorCodes() {}

    public static Component parse(String s, String prefix) {
        MutableComponent out = Component.empty();
        StringBuilder buf = new StringBuilder();
        Style style = Style.EMPTY;
        int i = 0;
        while (i < s.length()) {
            if (!prefix.isEmpty() && s.startsWith(prefix, i)) {
                int j = i + prefix.length();
                if (j + 7 <= s.length() && s.charAt(j) == '#' && isHex(s.substring(j + 1, j + 7))) {
                    flush(out, buf, style);
                    style = Style.EMPTY.withColor(Integer.parseInt(s.substring(j + 1, j + 7), 16));
                    i = j + 7;
                    continue;
                }
                if (j < s.length()) {
                    char c = Character.toLowerCase(s.charAt(j));
                    Integer rgb = COLORS.get(c);
                    if (rgb != null) {
                        flush(out, buf, style);
                        style = Style.EMPTY.withColor(rgb);
                        i = j + 1;
                        continue;
                    }
                    Style next = switch (c) {
                        case 'l' -> style.withBold(true);
                        case 'o' -> style.withItalic(true);
                        case 'n' -> style.withUnderlined(true);
                        case 'm' -> style.withStrikethrough(true);
                        case 'k' -> style.withObfuscated(true);
                        case 'r' -> Style.EMPTY;
                        default -> null;
                    };
                    if (next != null) {
                        flush(out, buf, style);
                        style = next;
                        i = j + 1;
                        continue;
                    }
                }
            }
            buf.append(s.charAt(i));
            i++;
        }
        flush(out, buf, style);
        return out;
    }

    private static void flush(MutableComponent out, StringBuilder buf, Style style) {
        if (buf.length() > 0) {
            out.append(Component.literal(buf.toString()).withStyle(style));
            buf.setLength(0);
        }
    }

    private static boolean isHex(String s) {
        if (s.length() != 6) return false;
        for (int k = 0; k < 6; k++) if (Character.digit(s.charAt(k), 16) < 0) return false;
        return true;
    }

    /** Entfernt alle Farb-/Format-Codes (prefix+Code und prefix+#RRGGBB), laesst nur den Text. */
    public static String stripCodes(String s, String prefix) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            if (!prefix.isEmpty() && s.startsWith(prefix, i)) {
                int j = i + prefix.length();
                if (j + 7 <= s.length() && s.charAt(j) == '#' && isHex(s.substring(j + 1, j + 7))) {
                    i = j + 7; continue;
                }
                if (j < s.length()) {
                    char c = Character.toLowerCase(s.charAt(j));
                    if (COLORS.containsKey(c) || "lonmkr".indexOf(c) >= 0) { i = j + 1; continue; }
                }
            }
            out.append(s.charAt(i));
            i++;
        }
        return out.toString();
    }

    /**
     * Faerbt den Text als Farbverlauf von startHex nach endHex ein.
     * Bestehende Codes werden vorher entfernt. Jeder Buchstabe bekommt sein eigenes &#RRGGBB.
     */
    public static String gradient(String text, String startHex, String endHex, String prefix, String hexFormat) {
        String plain = stripCodes(text, prefix);
        if (plain.isEmpty()) return text;
        int[] a = rgb(startHex), b = rgb(endHex);
        if (a == null || b == null) return text;
        StringBuilder out = new StringBuilder();
        int n = plain.length();
        int ci = 0; // Index nur fuer sichtbare Zeichen (Leerzeichen bekommen keinen Code)
        int visible = 0;
        for (int k = 0; k < n; k++) if (plain.charAt(k) != ' ') visible++;
        int denom = Math.max(1, visible - 1);
        for (int k = 0; k < n; k++) {
            char ch = plain.charAt(k);
            if (ch == ' ') { out.append(' '); continue; }
            double tt = visible <= 1 ? 0.0 : (double) ci / denom;
            int r = (int) Math.round(a[0] + (b[0] - a[0]) * tt);
            int g = (int) Math.round(a[1] + (b[1] - a[1]) * tt);
            int bl = (int) Math.round(a[2] + (b[2] - a[2]) * tt);
            out.append(String.format(hexFormat, String.format("%02X%02X%02X", r, g, bl)));
            out.append(ch);
            ci++;
        }
        return out.toString();
    }

    private static int[] rgb(String hex) {
        if (hex == null) return null;
        hex = hex.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (!isHex(hex)) return null;
        return new int[]{
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16)
        };
    }
}
