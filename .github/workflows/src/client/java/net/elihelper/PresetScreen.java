package net.elihelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** Presets: speichern, laden, loeschen, als Code teilen und importieren. */
public class PresetScreen extends Screen {
    private static final int W = 300, H = 18;

    private final Screen parent;
    private EditBox nameBox, importBox;
    private int pl, pt, pr, pb, titleBottom, footerTop;
    private int sep1;
    private StringWidget info;

    public PresetScreen(Screen parent) {
        super(Component.translatable("elihelper.presets"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        EliConfig c = EliConfig.get();
        int x = (this.width - W) / 2;
        int rows = c.presets.size();
        int contentH = 26 + (H + 4) + (rows == 0 ? 14 : rows * (H + 4)) + 10 + (H + 4) * 2 + 14 + 26;
        int top = Math.max(8, (this.height - contentH) / 2);
        pl = x - 14; pr = x + W + 14; pt = top; titleBottom = pt + 26;

        addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose())
                .bounds(pr - 24, pt + 4, 18, 18).build());
        addRenderableWidget(EliScreen.centered(this.font,
                Component.translatable("elihelper.presets").withStyle(Style.EMPTY.withBold(true).withColor(EliScreen.TEXT_PURPLE)),
                x, pt + 8, W));

        int y = titleBottom + 6;

        // Neu speichern
        nameBox = new EditBox(this.font, x, y, W - 94, H, Component.translatable("elihelper.preset.name"));
        nameBox.setMaxLength(24);
        nameBox.setHint(Component.translatable("elihelper.preset.name"));
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("elihelper.preset.save"), b -> {
            EliConfig.savePreset(nameBox.getValue());
            nameBox.setValue("");
            this.rebuildWidgets();
        }).bounds(x + W - 90, y, 90, H).build());
        y += H + 6;

        sep1 = y - 3;

        // Liste
        if (c.presets.isEmpty()) {
            addRenderableWidget(EliScreen.centered(this.font,
                    Component.translatable("elihelper.preset.none").withStyle(Style.EMPTY.withColor(0x9A93B5)), x, y + 2, W));
            y += 14;
        } else {
            for (EliConfig.NamedPreset p : new java.util.ArrayList<>(c.presets)) {
                addRenderableWidget(new StringWidget(x + 4, y + 4, W - 150, 10,
                        Component.literal(p.name == null ? "?" : p.name).withStyle(Style.EMPTY.withColor(0xE8E0FF)), this.font));
                addRenderableWidget(Button.builder(Component.translatable("elihelper.preset.load"), b -> {
                    if (EliConfig.applyPreset(p)) overlay("Preset geladen: " + p.name);
                    this.rebuildWidgets();
                }).bounds(x + W - 142, y, 60, H).build());
                addRenderableWidget(Button.builder(Component.translatable("elihelper.preset.copy"), b -> {
                    this.minecraft.keyboardHandler.setClipboard(p.code == null ? "" : p.code);
                    overlay("Code kopiert");
                }).bounds(x + W - 78, y, 50, H).build());
                addRenderableWidget(Button.builder(Component.literal("✕"), b -> {
                    EliConfig.deletePreset(p);
                    this.rebuildWidgets();
                }).bounds(x + W - 24, y, 24, H).build());
                y += H + 4;
            }
        }
        y += 6;

        // Export aktuell
        addRenderableWidget(Button.builder(Component.translatable("elihelper.preset.export"), b -> {
            this.minecraft.keyboardHandler.setClipboard(EliConfig.exportCode());
            overlay("Aktueller Code kopiert");
        }).bounds(x, y, W, H).build());
        y += H + 6;

        // Import
        importBox = new EditBox(this.font, x, y, W - 94, H, Component.translatable("elihelper.preset.code"));
        importBox.setMaxLength(4000);
        importBox.setHint(Component.translatable("elihelper.preset.code"));
        addRenderableWidget(importBox);
        addRenderableWidget(Button.builder(Component.translatable("elihelper.preset.import"), b -> {
            boolean ok = EliConfig.importCode(importBox.getValue());
            overlay(ok ? "Importiert" : "Code ungueltig");
            if (ok) this.rebuildWidgets();
        }).bounds(x + W - 90, y, 90, H).build());
        y += H + 6;

        info = new StringWidget(x, y, W, 10, Component.empty(), this.font);
        addRenderableWidget(info);
        y += 14;

        footerTop = y - 4;
        y += 2;
        addRenderableWidget(Button.builder(Component.translatable("elihelper.back"), b -> onClose())
                .bounds(x, y, W, H + 2).build());
        pb = y + H + 10;
    }

    private void overlay(String msg) {
        Minecraft.getInstance().gui.setOverlayMessage(Component.literal("EliHelper: " + msg), false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
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
