package net.elihelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** Editor fuer farbige Namen (/rename, /sign): Vorschau, Zeichenzaehler, Gradient, Kopieren, Senden. */
public class ColorScreen extends Screen {
    private static final int W = 260, H = 20;
    private static final String CODES = "0123456789abcdef";

    private final Screen parent;
    private EditBox input, hexInput, startHex, endHex;
    private StringWidget preview, counter;
    private boolean signMode = false;
    private int pl, pt, pr, pb, titleBottom, footerTop;

    public ColorScreen(Screen parent) {
        super(Component.translatable("elihelper.colors"));
        this.parent = parent;
    }

    private String prefix() { return EliConfig.get().colorPrefix; }

    @Override
    protected void init() {
        int x = (this.width - W) / 2;
        int top = Math.max(8, (this.height - 300) / 2);
        pl = x - 14; pr = x + W + 14; pt = top; titleBottom = pt + 26;

        addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose())
                .bounds(pr - 24, pt + 4, 18, 18).build());
        addRenderableWidget(EliScreen.centered(this.font,
                Component.translatable("elihelper.colors").withStyle(Style.EMPTY.withBold(true).withColor(EliScreen.TEXT_PURPLE)),
                x, pt + 8, W));

        int y = titleBottom + 8;

        String old = input == null ? "" : input.getValue();
        input = new EditBox(this.font, x, y, W, H, Component.literal("Text"));
        input.setMaxLength(200);
        input.setValue(old);
        addRenderableWidget(input);
        y += H + 6;

        preview = new StringWidget(x, y, W, 12, Component.empty(), this.font);
        addRenderableWidget(preview);
        y += 14;
        counter = new StringWidget(x, y, W, 12, Component.empty(), this.font);
        addRenderableWidget(counter);
        y += 18;

        int cw = W / 8;
        for (int i = 0; i < 16; i++) {
            char c = CODES.charAt(i);
            int rgb = ColorCodes.COLORS.get(c);
            Component label = Component.literal("■").withStyle(Style.EMPTY.withColor(rgb == 0 ? 0x333333 : rgb));
            addRenderableWidget(Button.builder(label, b -> insert(prefix() + c))
                    .bounds(x + (i % 8) * cw, y + (i / 8) * (H + 2), cw - 2, H).build());
        }
        y += 2 * (H + 2) + 4;

        String[][] fmts = {{"l", "B"}, {"o", "I"}, {"n", "U"}, {"m", "S"}, {"k", "?"}, {"r", "R"}};
        int fw = W / fmts.length;
        for (int i = 0; i < fmts.length; i++) {
            String code = fmts[i][0];
            addRenderableWidget(Button.builder(Component.literal(fmts[i][1]), b -> insert(prefix() + code))
                    .bounds(x + i * fw, y, fw - 2, H).build());
        }
        y += H + 6;

        hexInput = new EditBox(this.font, x, y, 90, H, Component.literal("RRGGBB"));
        hexInput.setMaxLength(6);
        hexInput.setHint(Component.literal("RRGGBB"));
        addRenderableWidget(hexInput);
        addRenderableWidget(Button.builder(Component.translatable("elihelper.hex"), b -> {
            String hex = hexInput.getValue().trim();
            if (hex.matches("[0-9a-fA-F]{6}")) insert(String.format(EliConfig.get().hexFormat, hex));
        }).bounds(x + 94, y, W - 94, H).build());
        y += H + 6;

        // Gradient: Start-Hex + End-Hex + Knopf
        int third = (W - 8) / 3;
        startHex = new EditBox(this.font, x, y, third, H, Component.literal("Start"));
        startHex.setMaxLength(6);
        startHex.setHint(Component.translatable("elihelper.gradient.start"));
        addRenderableWidget(startHex);
        endHex = new EditBox(this.font, x + third + 4, y, third, H, Component.literal("End"));
        endHex.setMaxLength(6);
        endHex.setHint(Component.translatable("elihelper.gradient.end"));
        addRenderableWidget(endHex);
        addRenderableWidget(Button.builder(Component.translatable("elihelper.gradient"), b -> {
            String g = ColorCodes.gradient(input.getValue(), startHex.getValue(), endHex.getValue(),
                    prefix(), EliConfig.get().hexFormat);
            input.setValue(g);
            setFocused(input);
        }).bounds(x + 2 * (third + 4), y, W - 2 * (third + 4), H).build());
        y += H + 6;

        // /rename <-> /sign + Kopieren + Senden
        addRenderableWidget(Button.builder(Component.literal(signMode ? "/sign" : "/rename"), b -> {
            signMode = !signMode;
            b.setMessage(Component.literal(signMode ? "/sign" : "/rename"));
        }).bounds(x, y, 70, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.copy"), b -> {
            this.minecraft.keyboardHandler.setClipboard(fullCommand());
        }).bounds(x + 74, y, (W - 74 - 4) / 2, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.send"), b -> send())
                .bounds(x + 74 + (W - 74 - 4) / 2 + 4, y, W - 74 - 4 - (W - 74 - 4) / 2, H).build());
        y += H + 6;

        // Footer: Leeren + Zurueck
        footerTop = y - 6;
        int half = (W - 4) / 2;
        addRenderableWidget(Button.builder(Component.translatable("elihelper.clear"), b -> {
            input.setValue("");
            setFocused(input);
        }).bounds(x, y, half, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.back"), b -> onClose())
                .bounds(x + half + 4, y, W - half - 4, H).build());
        pb = y + H + 8;
    }

    private String fullCommand() {
        return (signMode ? "/sign " : "/rename ") + input.getValue();
    }

    private void send() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String cmd = fullCommand();
        this.onClose();
        mc.player.connection.sendCommand(cmd.startsWith("/") ? cmd.substring(1) : cmd);
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

        Chrome.back(graphics, pl, pt, pr, pb, titleBottom, footerTop);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        Chrome.front(graphics, pl, pt, pr, pb, titleBottom, footerTop);
    }

    @Override
    public void onClose() { this.minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
