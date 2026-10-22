package net.elihelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

public class EliScreen extends Screen {
    static final int PURPLE_MAIN = 0xFF7C3AED;
    static final int PURPLE_LIGHT = 0xFFB388FF;
    static final int TEXT_PURPLE = 0xB388FF;

    private static final int W = 220, H = 20, GAP = 4;
    private int panelLeft, panelTop, panelRight, panelBottom, titleBottom, footerTop;
    private final List<Integer> seps = new ArrayList<>();
    private StringWidget macroStatus;

    public EliScreen() {
        super(Component.translatable("elihelper.title"));
    }

    static Component purple(String key) {
        return Component.translatable(key).withStyle(Style.EMPTY.withColor(TEXT_PURPLE));
    }

    static StringWidget centered(Font font, Component c, int x, int y, int w) {
        int tw = font.width(c);
        return new StringWidget(x + (w - tw) / 2, y, tw, 10, c, font);
    }

    @Override
    protected void init() {
        seps.clear();
        int x = (this.width - W) / 2;
        int cmds = EliConfig.get().commands.size();
        int body = 26 + 16 + cmds * (H + GAP) + 8 + 16 + H + 8 + 16 + H + 16 + 12 + H + 12;
        int top = Math.max(8, (this.height - body) / 2);

        panelLeft = x - 14;
        panelRight = x + W + 14;
        panelTop = top;
        titleBottom = panelTop + 26;

        // Close-X oben rechts in der Titelleiste
        addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose())
                .bounds(panelRight - 24, panelTop + 4, 18, 18).build());
        // Titel
        addRenderableWidget(centered(this.font,
                Component.translatable("elihelper.title").withStyle(Style.EMPTY.withBold(true).withColor(TEXT_PURPLE)),
                x, panelTop + 8, W));

        int y = titleBottom + 8;

        // Abschnitt: Befehle
        addRenderableWidget(centered(this.font, purple("elihelper.section.commands"), x, y, W));
        y += 15;
        for (EliConfig.CommandEntry entry : EliConfig.get().commands) {
            addRenderableWidget(Button.builder(Component.literal(entry.label), b -> sendCommand(entry.command))
                    .bounds(x, y, W, H).build());
            y += H + GAP;
        }

        // Abschnitt: Werkzeuge
        y += 4;
        seps.add(y);
        y += 4;
        addRenderableWidget(centered(this.font, purple("elihelper.section.tools"), x, y, W));
        y += 14;
        int bw = (W - 2 * GAP) / 3;
        addRenderableWidget(Button.builder(Component.translatable("elihelper.colors"),
                b -> this.minecraft.setScreen(new ColorScreen(this))).bounds(x, y, bw, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.stats"),
                b -> this.minecraft.setScreen(new StatsScreen(this))).bounds(x + bw + GAP, y, bw, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.macros"),
                b -> this.minecraft.setScreen(new MacroScreen(this))).bounds(x + 2 * (bw + GAP), y, W - 2 * (bw + GAP), H).build());
        y += H + GAP;

        // Abschnitt: Makros
        y += 4;
        seps.add(y);
        y += 4;
        addRenderableWidget(centered(this.font, purple("elihelper.section.macros"), x, y, W));
        y += 14;
        int half = (W - GAP) / 2;
        addRenderableWidget(Button.builder(Component.translatable("elihelper.sell"),
                b -> MacroRunner.startSell(Minecraft.getInstance())).bounds(x, y, half, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.macro.stop"),
                b -> MacroRunner.stop()).bounds(x + half + GAP, y, W - half - GAP, H).build());
        y += H + 4;

        macroStatus = new StringWidget(x, y, W, 11, Component.empty(), this.font);
        addRenderableWidget(macroStatus);
        y += 16;

        // Footer
        footerTop = y - 6;
        addRenderableWidget(Button.builder(Component.translatable("elihelper.reload"), b -> {
            EliConfig.load();
            this.rebuildWidgets();
        }).bounds(x, y, half, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.close"), b -> onClose())
                .bounds(x + half + GAP, y, W - half - GAP, H).build());
        panelBottom = y + H + 8;
    }

    private void sendCommand(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        this.onClose();
        mc.player.connection.sendCommand(command.startsWith("/") ? command.substring(1) : command);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (macroStatus != null) {
            if (MacroRunner.running()) {
                macroStatus.setMessage(Component.translatable("elihelper.macro.running")
                        .append(Component.literal(" " + MacroRunner.label() + " – " + MacroRunner.remaining()))
                        .withStyle(Style.EMPTY.withColor(0x55FF88)));
            } else {
                macroStatus.setMessage(Component.translatable("elihelper.macro.idle")
                        .withStyle(Style.EMPTY.withColor(0x888888)));
            }
        }
        Chrome.back(graphics, panelLeft, panelTop, panelRight, panelBottom, titleBottom, footerTop);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        Chrome.front(graphics, panelLeft, panelTop, panelRight, panelBottom, titleBottom, footerTop);
        for (int sy : seps) Chrome.separator(graphics, panelLeft + 10, sy, panelRight - panelLeft - 20);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (EliHelperClient.OPEN_KEY.matches(event)) { this.onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
