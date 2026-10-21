package net.elihelper;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** Editor fuer farbige Namen (/rename, /sign): Vorschau, Zeichenzaehler, Kopieren. */
public class ColorScreen extends Screen {
    private static final int W = 260, H = 20;
    private static final String CODES = "0123456789abcdef";

    private final Screen parent;
    private EditBox input, hexInput;
    private StringWidget preview, counter;
    private boolean signMode = false;
    private int pl, pt, pr, pb;

    public ColorScreen(Screen parent) {
        super(Component.translatable("elihelper.colors"));
        this.parent = parent;
    }

    private String prefix() { return EliConfig.get().colorPrefix; }

    @Override
    protected void init() {
        int x = (this.width - W) / 2;
        int y = Math.max(10, (this.height - 190) / 2);
        pl = x - 12; pr = x + W + 12; pt = y - 8;

        addRenderableWidget(new StringWidget(x, y + 2, W, 12,
                Component.translatable("elihelper.colors").withStyle(Style.EMPTY.withBold(true).withColor(EliScreen.TEXT_PURPLE)),
                this.font).alignCenter());
        y += 20;

        String old = input == null ? "" : input.getValue();
        input = new EditBox(this.font, x, y, W, H, Component.literal("Text"));
        input.setMaxLength(200);
        input.setValue(old);
        addRenderableWidget(input);
        y += H + 6;

        preview = new StringWidget(x, y, W, 12, Component.empty(), this.font).alignCenter();
        addRenderableWidget(preview);
        y += 14;
        counter = new StringWidget(x, y, W, 12, Component.empty(), this.font).alignCenter();
        addRenderableWidget(counter);
        y += 18;

        // 16 Farben in zwei Reihen
        int cw = W / 8;
        for (int i = 0; i < 16; i++) {
            char c = CODES.charAt(i);
            int rgb = ColorCodes.COLORS.get(c);
            Component label = Component.literal("\u25A0").withStyle(Style.EMPTY.withColor(rgb == 0 ? 0x333333 : rgb));
            addRenderableWidget(Button.builder(label, b -> insert(prefix() + c))
                    .bounds(x + (i % 8) * cw, y + (i / 8) * (H + 2), cw - 2, H).build());
        }
        y += 2 * (H + 2) + 4;

        // Formate
        String[][] fmts = {{"l", "B"}, {"o", "I"}, {"n", "U"}, {"m", "S"}, {"k", "?"}, {"r", "R"}};
        int fw = W / fmts.length;
        for (int i = 0; i < fmts.length; i++) {
            String code = fmts[i][0];
            addRenderableWidget(Button.builder(Component.literal(fmts[i][1]), b -> insert(prefix() + code))
                    .bounds(x + i * fw, y, fw - 2, H).build());
        }
        y += H + 6;

        // Hex
        hexInput = new EditBox(this.font, x, y, 90, H, Component.literal("RRGGBB"));
        hexInput.setMaxLength(6);
        hexInput.setHint(Component.literal("RRGGBB"));
        addRenderableWidget(hexInput);
        addRenderableWidget(Button.builder(Component.translatable("elihelper.hex"), b -> {
            String hex = hexInput.getValue().trim();
            if (hex.matches("[0-9a-fA-F]{6}")) insert(String.format(EliConfig.get().hexFormat, hex));
        }).bounds(x + 94, y, W - 94, H).build());
        y += H + 6;

        // Modus + Kopieren
        addRenderableWidget(Button.builder(Component.literal(signMode ? "/sign" : "/rename"), b -> {
            signMode = !signMode;
            b.setMessage(Component.literal(signMode ? "/sign" : "/rename"));
        }).bounds(x, y, 80, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.copy"), b -> {
            String cmd = (signMode ? "/sign " : "/rename ") + input.getValue();
            this.minecraft.keyboardHandler.setClipboard(cmd);
        }).bounds(x + 84, y, W - 84, H).build());
        y += H + 6;

        addRenderableWidget(Button.builder(Component.translatable("elihelper.back"), b -> onClose())
                .bounds(x, y, W, H).build());
        pb = y + H + 8;
    }

    private void insert(String s) {
        input.insertText(s);
        setFocused(input);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        String text = input == null ? "" : input.getValue();
        int limit = signMode ? EliConfig.get().signLimit : EliConfig.get().renameLimit;
        boolean over = text.length() > limit;
        preview.setMessage(text.isEmpty() ? Component.empty() : ColorCodes.parse(text, prefix()));
        counter.setMessage(Component.literal(text.length() + " / " + limit)
                .withStyle(Style.EMPTY.withColor(over ? 0xFF5555 : EliScreen.TEXT_PURPLE)));
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        EliScreen.frame(graphics, pl, pt, pr, pb);
    }

    @Override
    public void onClose() { this.minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
