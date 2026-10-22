package net.elihelper;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public final class UiThemeScreen extends Screen {
    private final Screen parent;
    private int left, top, right, bottom;

    public UiThemeScreen(Screen parent) {
        super(Component.literal("EliHelper • UI-Farben"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = 320, h = 166;
        left = (width - w) / 2;
        top = (height - h) / 2;
        right = left + w;
        bottom = top + h;
        addRenderableWidget(new StringWidget(left + 12, top + 12, w - 24, 16,
                Component.literal("Wähle deine Akzentfarbe").withStyle(Style.EMPTY.withBold(true)), font));
        addRenderableWidget(new StringWidget(left + 12, top + 30, w - 24, 12,
                Component.literal("Die Farbe wird gespeichert und für EliHelper-Menüs verwendet."), font));
        int buttonW = 136, buttonH = 24, gap = 8;
        for (int i = 0; i < UiTheme.NAMES.length; i++) {
            final int preset = i;
            int col = i % 2, row = i / 2;
            String mark = UiTheme.selected() == i ? "✓ " : "";
            addRenderableWidget(Button.builder(Component.literal(mark + UiTheme.NAMES[i]), b -> {
                UiTheme.choose(preset);
                if (minecraft != null) minecraft.setScreen(new UiThemeScreen(parent));
            }).bounds(left + 16 + col * (buttonW + gap), top + 52 + row * (buttonH + 6), buttonW, buttonH).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Zurück"), b -> {
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(left + 16, bottom - 30, w - 32, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        UiTheme.load();
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        Chrome.back(graphics, left, top, right, bottom, top + 44, bottom - 36);
        Chrome.front(graphics, left, top, right, bottom, top + 44, bottom - 36);
    }

    @Override public boolean isPauseScreen() { return false; }
}
