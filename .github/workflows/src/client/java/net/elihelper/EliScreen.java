package net.elihelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class EliScreen extends Screen {
    static final int PURPLE_MAIN = 0xFF7C3AED;
    static final int PURPLE_LIGHT = 0xFFB388FF;
    static final int TEXT_PURPLE = 0xB388FF;

    private static final int W = 220, H = 20, GAP = 4;
    private int panelLeft, panelTop, panelRight, panelBottom;

    public EliScreen() {
        super(Component.translatable("elihelper.title"));
    }

    static Component purple(String key) {
        return Component.translatable(key).withStyle(Style.EMPTY.withColor(TEXT_PURPLE));
    }

    @Override
    protected void init() {
        int x = (this.width - W) / 2;
        int cmds = EliConfig.get().commands.size();
        int totalH = 170 + cmds * (H + GAP);
        int y = Math.max(10, (this.height - totalH) / 2);
        panelLeft = x - 12;
        panelRight = x + W + 12;
        panelTop = y - 8;

        addRenderableWidget(new StringWidget(x, y + 6, W, 12,
                Component.translatable("elihelper.title").withStyle(Style.EMPTY.withBold(true).withColor(TEXT_PURPLE)),
                this.font).alignCenter());
        y += 24;

        addRenderableWidget(new StringWidget(x, y, W, 10, purple("elihelper.section.commands"), this.font).alignCenter());
        y += 14;
        for (EliConfig.CommandEntry entry : EliConfig.get().commands) {
            addRenderableWidget(Button.builder(Component.literal(entry.label), b -> sendCommand(entry.command))
                    .bounds(x, y, W, H).build());
            y += H + GAP;
        }

        y += 4;
        addRenderableWidget(new StringWidget(x, y, W, 10, purple("elihelper.section.tools"), this.font).alignCenter());
        y += 14;
        addRenderableWidget(Button.builder(Component.translatable("elihelper.colors"),
                b -> this.minecraft.setScreen(new ColorScreen(this))).bounds(x, y, W / 2 - 2, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.stats"),
                b -> this.minecraft.setScreen(new StatsScreen(this))).bounds(x + W / 2 + 2, y, W / 2 - 2, H).build());
        y += H + GAP + 4;

        addRenderableWidget(new StringWidget(x, y, W, 10, purple("elihelper.section.macros"), this.font).alignCenter());
        y += 14;
        for (String key : new String[]{"elihelper.sell", "elihelper.order"}) {
            Button b = Button.builder(Component.translatable(key), btn -> {}).bounds(x, y, W, H).build();
            b.active = false;
            addRenderableWidget(b);
            y += H + GAP;
        }

        addRenderableWidget(Button.builder(Component.translatable("elihelper.reload"), b -> {
            EliConfig.load();
            this.rebuildWidgets();
        }).bounds(x, y, W / 2 - 2, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.close"), b -> onClose())
                .bounds(x + W / 2 + 2, y, W / 2 - 2, H).build());
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
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        frame(graphics, panelLeft, panelTop, panelRight, panelBottom);
    }

    static void frame(GuiGraphicsExtractor g, int l, int t, int r, int b) {
        g.fill(l, t, r, t + 3, PURPLE_MAIN);
        g.fill(l, b - 3, r, b, PURPLE_MAIN);
        g.fill(l, t, l + 2, b, PURPLE_LIGHT);
        g.fill(r - 2, t, r, b, PURPLE_LIGHT);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (EliHelperClient.OPEN_KEY.matches(event)) { this.onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
