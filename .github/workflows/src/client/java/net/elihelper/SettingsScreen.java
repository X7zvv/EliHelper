package net.elihelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** Einstellungen: Farbcodes, Statistik-Regex, Makros und Auto-TPA. */
public class SettingsScreen extends Screen {
    private static final int W = 300, H = 18;

    private final Screen parent;
    private EditBox prefix, hexFormat, renameLimit, signLimit, payoutRegex, sellCommand, tpaRegex, tpaAccept;
    private Button tpaBtn, macroBtn, statsBtn;
    private int pl, pt, pr, pb, titleBottom, footerTop;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("elihelper.settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        EliConfig c = EliConfig.get();
        int x = (this.width - W) / 2;
        int top = Math.max(8, (this.height - 300) / 2);
        pl = x - 14; pr = x + W + 14; pt = top; titleBottom = pt + 26;

        addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose())
                .bounds(pr - 24, pt + 4, 18, 18).build());
        addRenderableWidget(EliScreen.centered(this.font,
                Component.translatable("elihelper.settings").withStyle(Style.EMPTY.withBold(true).withColor(EliScreen.textPurple())),
                x, pt + 8, W));

        int y = titleBottom + 6;
        int half = (W - 6) / 2;

        caption(x, y, "elihelper.set.colors"); y += 10;
        prefix = box(x, y, half, c.colorPrefix, "elihelper.set.prefix");
        hexFormat = box(x + half + 6, y, W - half - 6, c.hexFormat, "elihelper.set.hex"); y += H + 4;

        caption(x, y, "elihelper.set.limits"); y += 10;
        renameLimit = box(x, y, half, String.valueOf(c.renameLimit), "elihelper.set.rename");
        signLimit = box(x + half + 6, y, W - half - 6, String.valueOf(c.signLimit), "elihelper.set.sign"); y += H + 4;

        caption(x, y, "elihelper.set.payout"); y += 10;
        payoutRegex = box(x, y, W, c.payoutRegex, "elihelper.set.payout"); y += H + 4;

        caption(x, y, "elihelper.set.sell"); y += 10;
        sellCommand = box(x, y, W, c.sellCommand, "elihelper.sell.cmd"); y += H + 4;

        caption(x, y, "elihelper.set.tpa"); y += 10;
        tpaRegex = box(x, y, half, c.tpaRegex, "elihelper.set.tparegex");
        tpaAccept = box(x + half + 6, y, W - half - 6, c.tpaAccept, "elihelper.set.tpaaccept"); y += H + 6;

        // Schalter
        int third = (W - 2 * 4) / 3;
        tpaBtn = toggle(x, y, third, () -> { c.autoTpaEnabled = !c.autoTpaEnabled; EliConfig.save(); });
        macroBtn = toggle(x + third + 4, y, third, () -> { c.macroEnabled = !c.macroEnabled; EliConfig.save(); });
        statsBtn = toggle(x + 2 * (third + 4), y, W - 2 * (third + 4), () -> { c.statsEnabled = !c.statsEnabled; EliConfig.save(); });
        y += H + 8;

        footerTop = y - 6;
        addRenderableWidget(Button.builder(Component.translatable("elihelper.save"), b -> {
            save();
            Minecraft.getInstance().gui.setOverlayMessage(Component.literal("EliHelper: gespeichert"), false);
        }).bounds(x, y, half, H + 2).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.back"), b -> onClose())
                .bounds(x + half + 6, y, W - half - 6, H + 2).build());
        pb = y + H + 10;
    }

    private void caption(int x, int y, String key) {
        addRenderableWidget(new StringWidget(x + 1, y, W, 9,
                Component.translatable(key).withStyle(Style.EMPTY.withColor(EliScreen.textPurple())), this.font));
    }

    private EditBox box(int x, int y, int w, String value, String hintKey) {
        EditBox e = new EditBox(this.font, x, y, w, H, Component.translatable(hintKey));
        e.setMaxLength(120);
        e.setValue(value == null ? "" : value);
        e.setHint(Component.translatable(hintKey));
        addRenderableWidget(e);
        return e;
    }

    private Button toggle(int x, int y, int w, Runnable onClick) {
        Button b = Button.builder(Component.empty(), btn -> onClick.run()).bounds(x, y, w, H).build();
        addRenderableWidget(b);
        return b;
    }

    private void save() {
        EliConfig c = EliConfig.get();
        c.colorPrefix = prefix.getValue();
        c.hexFormat = hexFormat.getValue().isBlank() ? "&#%s" : hexFormat.getValue();
        c.renameLimit = parseInt(renameLimit.getValue(), c.renameLimit);
        c.signLimit = parseInt(signLimit.getValue(), c.signLimit);
        c.payoutRegex = payoutRegex.getValue();
        c.sellCommand = sellCommand.getValue().trim();
        c.tpaRegex = tpaRegex.getValue();
        c.tpaAccept = tpaAccept.getValue().trim();
        EliConfig.save();
    }

    private static int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return fallback; }
    }

    private static Component toggleLabel(String key, boolean on) {
        return Component.translatable(key).withStyle(Style.EMPTY.withColor(0xFFFFFF))
                .append(Component.literal(on ? " ✓" : " ✗").withStyle(Style.EMPTY.withColor(on ? 0x66FF99 : 0xFF6B6B)));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        EliConfig c = EliConfig.get();
        tpaBtn.setMessage(toggleLabel("elihelper.autotpa", c.autoTpaEnabled));
        macroBtn.setMessage(toggleLabel("elihelper.set.macro", c.macroEnabled));
        statsBtn.setMessage(toggleLabel("elihelper.set.stats", c.statsEnabled));

        Chrome.back(graphics, pl, pt, pr, pb, titleBottom, footerTop);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        Chrome.front(graphics, pl, pt, pr, pb, titleBottom, footerTop);
    }

    @Override
    public void onClose() { this.minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
