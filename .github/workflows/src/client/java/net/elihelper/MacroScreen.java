package net.elihelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** Sell- und Order-Makro einstellen und starten. Nur nutzen, wenn der Server es erlaubt. */
public class MacroScreen extends Screen {
    private static final int W = 260, H = 20;

    private final Screen parent;
    private EditBox sellCmd, sellRepeat, sellDelay;
    private EditBox orderItem, orderAmount, orderPrice, orderRepeat;
    private StringWidget preview, status;
    private int pl, pt, pr, pb, titleBottom, footerTop;
    private int sep1;

    public MacroScreen(Screen parent) {
        super(Component.translatable("elihelper.macros"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        EliConfig c = EliConfig.get();
        int x = (this.width - W) / 2;
        int top = Math.max(8, (this.height - 290) / 2);
        pl = x - 14; pr = x + W + 14; pt = top; titleBottom = pt + 26;

        addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose())
                .bounds(pr - 24, pt + 4, 18, 18).build());
        addRenderableWidget(EliScreen.centered(this.font,
                Component.translatable("elihelper.macros").withStyle(Style.EMPTY.withBold(true).withColor(EliScreen.TEXT_PURPLE)),
                x, pt + 8, W));

        int y = titleBottom + 8;

        // ---- Sell ----
        addRenderableWidget(EliScreen.centered(this.font, EliScreen.purple("elihelper.sell"), x, y, W));
        y += 14;
        sellCmd = box(x, y, W, c.sellCommand, "elihelper.sell.cmd"); y += H + 4;

        int third = (W - 8) / 3;
        sellRepeat = box(x, y, third, String.valueOf(c.sellRepeat), "elihelper.sell.repeat");
        sellDelay = box(x + third + 4, y, W - third - 4, String.valueOf(c.sellDelayTicks), "elihelper.sell.delay");
        y += H + 6;

        int half = (W - 4) / 2;
        addRenderableWidget(Button.builder(Component.translatable("elihelper.sell.start"), b -> {
            sync();
            MacroRunner.startSell(Minecraft.getInstance());
        }).bounds(x, y, half, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.macro.stop"), b -> MacroRunner.stop())
                .bounds(x + half + 4, y, W - half - 4, H).build());
        y += H + 8;

        sep1 = y - 4;

        // ---- Order ----
        addRenderableWidget(EliScreen.centered(this.font, EliScreen.purple("elihelper.order.title"), x, y, W));
        y += 14;
        orderItem = box(x, y, W, c.orderItem, "elihelper.order.item"); y += H + 4;
        orderAmount = box(x, y, third, c.orderAmount, "elihelper.order.amount");
        orderPrice = box(x + third + 4, y, third, c.orderPrice, "elihelper.order.price");
        orderRepeat = box(x + 2 * (third + 4), y, W - 2 * (third + 4), String.valueOf(c.orderRepeat), "elihelper.sell.repeat");
        y += H + 4;

        preview = new StringWidget(x, y, W, 11, Component.empty(), this.font);
        addRenderableWidget(preview); y += 14;

        addRenderableWidget(Button.builder(Component.translatable("elihelper.order.send"), b -> {
            sync();
            MacroRunner.startOrder(Minecraft.getInstance());
        }).bounds(x, y, W, H).build());
        y += H + 6;

        status = new StringWidget(x, y, W, 11, Component.empty(), this.font);
        addRenderableWidget(status); y += 13;

        addRenderableWidget(EliScreen.centered(this.font,
                Component.translatable("elihelper.macro.note").withStyle(Style.EMPTY.withColor(0xAAAA55)), x, y, W));
        y += 16;

        // Footer
        footerTop = y - 6;
        addRenderableWidget(Button.builder(Component.translatable("elihelper.save"), b -> {
            sync();
            Minecraft.getInstance().gui.setOverlayMessage(Component.literal("EliHelper: gespeichert"), false);
        }).bounds(x, y, half, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.back"), b -> onClose())
                .bounds(x + half + 4, y, W - half - 4, H).build());
        pb = y + H + 8;
    }

    private EditBox box(int x, int y, int w, String value, String hintKey) {
        EditBox e = new EditBox(this.font, x, y, w, H, Component.translatable(hintKey));
        e.setMaxLength(100);
        e.setValue(value == null ? "" : value);
        e.setHint(Component.translatable(hintKey));
        addRenderableWidget(e);
        return e;
    }

    /** Schreibt die Eingaben in die Config und speichert. */
    private void sync() {
        EliConfig c = EliConfig.get();
        c.sellCommand = sellCmd.getValue().trim();
        c.sellRepeat = clamp(parseInt(sellRepeat.getValue(), 1), 1, Math.max(1, c.macroMaxRepeat));
        c.sellDelayTicks = Math.max(2, parseInt(sellDelay.getValue(), 10));
        c.orderCommand = c.orderCommand == null || c.orderCommand.isBlank()
                ? "order {item} {amount} {price}" : c.orderCommand;
        c.orderItem = orderItem.getValue().trim();
        c.orderAmount = orderAmount.getValue().trim();
        c.orderPrice = orderPrice.getValue().trim();
        c.orderRepeat = clamp(parseInt(orderRepeat.getValue(), 1), 1, Math.max(1, c.macroMaxRepeat));
        EliConfig.save();
    }

    private static int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return fallback; }
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Live-Vorschau des Order-Befehls
        EliConfig c = EliConfig.get();
        String tmpl = c.orderCommand == null || c.orderCommand.isBlank() ? "order {item} {amount} {price}" : c.orderCommand;
        String built = tmpl
                .replace("{item}", orderItem == null ? "" : orderItem.getValue().trim())
                .replace("{amount}", orderAmount == null ? "" : orderAmount.getValue().trim())
                .replace("{price}", orderPrice == null ? "" : orderPrice.getValue().trim())
                .replaceAll("\\s+", " ").trim();
        preview.setMessage(Component.translatable("elihelper.macro.preview")
                .withStyle(Style.EMPTY.withColor(0xFFFFFF))
                .append(Component.literal(" /" + built).withStyle(Style.EMPTY.withColor(EliScreen.TEXT_PURPLE))));

        if (MacroRunner.running()) {
            status.setMessage(Component.translatable("elihelper.macro.running")
                    .append(Component.literal(" " + MacroRunner.label() + " – " + MacroRunner.remaining()))
                    .withStyle(Style.EMPTY.withColor(0x55FF88)));
        } else {
            status.setMessage(Component.translatable("elihelper.macro.idle")
                    .withStyle(Style.EMPTY.withColor(0x888888)));
        }

        Chrome.back(graphics, pl, pt, pr, pb, titleBottom, footerTop);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        Chrome.front(graphics, pl, pt, pr, pb, titleBottom, footerTop);
        Chrome.separator(graphics, pl + 10, sep1, pr - pl - 20);
    }

    @Override
    public void onClose() { this.minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
