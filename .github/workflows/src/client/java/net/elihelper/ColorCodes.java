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
}
