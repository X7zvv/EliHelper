package net.elihelper;

import com.google.gson.Gson;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Zeigt die Neuerungen aus assets/elihelper/whatsnew.json. */
public class WhatsNewScreen extends Screen {
    private static final int W = 300;
    private final Screen parent;
    private Data data;
    private int pl, pt, pr, pb, titleBottom, footerTop;

    private static final class Line { String type; String text; }
    private static final class Data { String version; String title; Line[] lines; }

    public WhatsNewScreen(Screen parent) {
        super(Component.literal("What's New"));
        this.parent = parent;
    }

    private Data load() {
        try (InputStream in = WhatsNewScreen.class.getResourceAsStream("/assets/elihelper/whatsnew.json")) {
            if (in == null) return null;
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return new Gson().fromJson(json, Data.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static int colorFor(String type) {
        if (type == null) return 0xFFFFFF;
        return switch (type.toUpperCase()) {
            case "NEW" -> 0x66FF99;
            case "FIX" -> 0xFFD166;
            case "GONE" -> 0xFF6B6B;
            default -> 0xFFFFFF;
        };
    }

    @Override
    protected void init() {
        if (data == null) data = load();
        int lines = data == null || data.lines == null ? 1 : data.lines.length;
        int contentH = 26 + 10 + lines * 13 + 14 + 24;
        int x = (this.width - W) / 2;
        int top = Math.max(8, (this.height - contentH) / 2);
        pl = x - 14; pr = x + W + 14; pt = top; titleBottom = pt + 26;

        String title = data == null || data.title == null ? "EliHelper" : data.title;
        addRenderableWidget(EliScreen.centered(this.font,
                Component.literal(title).withStyle(Style.EMPTY.withBold(true).withColor(EliScreen.TEXT_PURPLE)),
                x, pt + 8, W));

        int y = titleBottom + 8;
        if (data != null && data.lines != null) {
            for (Line ln : data.lines) {
                String tag = ln.type == null ? "" : ln.type.toUpperCase();
                Component c = Component.literal("● " + tag + "  ")
                        .withStyle(Style.EMPTY.withColor(colorFor(ln.type)))
                        .append(Component.literal(ln.text == null ? "" : ln.text)
                                .withStyle(Style.EMPTY.withColor(0xE8E0FF)));
                addRenderableWidget(new StringWidget(x + 4, y, W - 8, 11, c, this.font));
                y += 13;
            }
        } else {
            addRenderableWidget(EliScreen.centered(this.font,
                    Component.literal("Willkommen!").withStyle(Style.EMPTY.withColor(0xE8E0FF)), x, y, W));
            y += 13;
        }
        y += 6;

        footerTop = y - 4;
        y += 2;
        addRenderableWidget(Button.builder(Component.translatable("elihelper.whatsnew.ok"), b -> onClose())
                .bounds(x, y, W, 20).build());
        pb = y + 28;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Chrome.back(graphics, pl, pt, pr, pb, titleBottom, footerTop);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        Chrome.front(graphics, pl, pt, pr, pb, titleBottom, footerTop);
    }

    @Override
    public void onClose() { this.minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
