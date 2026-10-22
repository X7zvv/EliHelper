package net.elihelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Hauptmenue: Reiter, Kachel-Grid, Suche, Themes, Animation. */
public class EliScreen extends Screen {
    static final int PURPLE_MAIN = 0xFF7C3AED;
    static final int PURPLE_LIGHT = 0xFFB388FF;
    static final int TEXT_PURPLE = 0xB388FF;

    static final int TITLE_COL = 0xE8E0FF;
    static final int ON_COL    = 0x66FF99;
    static final int OFF_COL   = 0xFF6B6B;
    static final int MUTED_COL = 0x9A93B5;

    private static final int W = 300, TILE_H = 30, GAP = 4, H = 20;

    private int panelLeft, panelTop, panelRight, panelBottom, titleBottom, footerTop;
    private final List<Bind> binds = new ArrayList<>();
    private final List<int[]> tileRects = new ArrayList<>();
    private EditBox searchBox;
    private String lastQuery = "";
    private int currentTab = 0;
    private final long openedAt = System.currentTimeMillis();

    public EliScreen() {
        super(Component.translatable("elihelper.title"));
    }

    private static final class Tile {
        final String label; final Supplier<Component> status; final Runnable action;
        Tile(String label, Supplier<Component> status, Runnable action) {
            this.label = label; this.status = status; this.action = action;
        }
    }
    private static final class Cat {
        final String name; final List<Tile> tiles = new ArrayList<>();
        Cat(String name) { this.name = name; }
    }
    private static final class Bind {
        final StringWidget w; final Supplier<Component> s;
        Bind(StringWidget w, Supplier<Component> s) { this.w = w; this.s = s; }
    }

    static Component purple(String key) {
        return Component.translatable(key).withStyle(Style.EMPTY.withColor(TEXT_PURPLE));
    }
    static StringWidget centered(Font font, Component c, int x, int y, int w) {
        int tw = font.width(c);
        return new StringWidget(x + (w - tw) / 2, y, tw, 10, c, font);
    }
    private static Component st(String text, int color) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(color));
    }
    private static String tr(String key) { return Component.translatable(key).getString(); }

    /** Alle Kategorien mit allen Kacheln (ungefiltert). */
    private List<Cat> buildAll() {
        List<Cat> all = new ArrayList<>();

        Cat cmds = new Cat(tr("elihelper.section.commands"));
        for (EliConfig.CommandEntry e : EliConfig.get().commands) {
            final String command = e.command;
            cmds.tiles.add(new Tile("» " + e.label, () -> st("/" + command, MUTED_COL), () -> sendCommand(command)));
        }
        all.add(cmds);

        Cat handel = new Cat(tr("elihelper.section.trade"));
        handel.tiles.add(new Tile("$ " + tr("elihelper.sell"),
                () -> MacroRunner.running()
                        ? st(tr("elihelper.macro.running") + " " + MacroRunner.remaining(), ON_COL)
                        : st(tr("elihelper.macro.idle"), MUTED_COL),
                MacroRunner::startSellCurrent));
        handel.tiles.add(new Tile("▸ " + tr("elihelper.order.title"),
                () -> st(tr("elihelper.macro.open"), MUTED_COL),
                () -> this.minecraft.setScreen(new MacroScreen(this))));
        handel.tiles.add(new Tile("Σ " + tr("elihelper.stats"),
                () -> st(tr("elihelper.stat.session") + " " + SellStats.fmt(SellStats.session), MUTED_COL),
                () -> this.minecraft.setScreen(new StatsScreen(this))));
        all.add(handel);

        Cat tools = new Cat(tr("elihelper.section.tools"));
        tools.tiles.add(new Tile("✎ " + tr("elihelper.colors"),
                () -> st("/rename, /sign", MUTED_COL), () -> this.minecraft.setScreen(new ColorScreen(this))));
        tools.tiles.add(new Tile("⇄ " + tr("elihelper.autotpa"),
                () -> EliConfig.get().autoTpaEnabled ? st(tr("elihelper.on"), ON_COL) : st(tr("elihelper.off"), OFF_COL),
                () -> { EliConfig.get().autoTpaEnabled = !EliConfig.get().autoTpaEnabled; EliConfig.save(); }));
        tools.tiles.add(new Tile("⭐ " + tr("elihelper.presets"),
                () -> st(EliConfig.get().presets.size() + "", MUTED_COL),
                () -> this.minecraft.setScreen(new PresetScreen(this))));
        tools.tiles.add(new Tile("⚙ " + tr("elihelper.settings"),
                () -> st(tr("elihelper.settings.sub"), MUTED_COL),
                () -> this.minecraft.setScreen(new SettingsScreen(this))));
        all.add(tools);

        return all;
    }

    @Override
    protected void init() {
        binds.clear();
        tileRects.clear();
        int x0 = (this.width - W) / 2;
        int tileW = (W - GAP) / 2;

        List<Cat> all = buildAll();
        if (currentTab >= all.size()) currentTab = 0;
        boolean searching = !lastQuery.trim().isEmpty();

        // Anzuzeigende Kacheln bestimmen
        List<Tile> tiles = new ArrayList<>();
        if (searching) {
            String q = lastQuery.trim().toLowerCase();
            for (Cat c : all) for (Tile t : c.tiles) if (t.label.toLowerCase().contains(q)) tiles.add(t);
        } else {
            tiles.addAll(all.get(currentTab).tiles);
        }

        int tileRows = (tiles.size() + 1) / 2;
        int contentH = 30 + 22 + (tiles.isEmpty() ? 20 : tileRows * (TILE_H + GAP)) + 10 + H + 10;
        panelTop = Math.max(8, (this.height - contentH) / 2);
        panelLeft = x0 - 14;
        panelRight = x0 + W + 14;
        titleBottom = panelTop + 30;

        // Suchfeld + Close-X
        searchBox = new EditBox(this.font, x0, panelTop + 6, W - 28, 18, Component.translatable("elihelper.search"));
        searchBox.setMaxLength(40);
        searchBox.setHint(Component.translatable("elihelper.search"));
        searchBox.setValue(lastQuery);
        addRenderableWidget(searchBox);
        setFocused(searchBox);
        addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose())
                .bounds(panelRight - 24, panelTop + 6, 18, 18).build());

        int y = titleBottom + 6;

        // Reiter
        int tabW = (W - 2 * GAP) / all.size();
        for (int i = 0; i < all.size(); i++) {
            final int idx = i;
            boolean active = !searching && i == currentTab;
            Component lbl = Component.literal(all.get(i).name).withStyle(active
                    ? Style.EMPTY.withColor(0xFFFFFF).withBold(true)
                    : Style.EMPTY.withColor(MUTED_COL));
            Button tab = Button.builder(lbl, b -> { currentTab = idx; lastQuery = ""; this.rebuildWidgets(); })
                    .bounds(x0 + i * (tabW + GAP), y, tabW, 16).build();
            addRenderableWidget(tab);
        }
        y += 20;

        // Kacheln
        if (tiles.isEmpty()) {
            addRenderableWidget(centered(this.font,
                    Component.translatable("elihelper.search.none").withStyle(Style.EMPTY.withColor(MUTED_COL)),
                    x0, y + 4, W));
            y += 20;
        } else {
            int col = 0;
            for (Tile t : tiles) {
                int tx = x0 + col * (tileW + GAP);
                addRenderableWidget(Button.builder(Component.empty(), b -> t.action.run())
                        .bounds(tx, y, tileW, TILE_H).build());
                addRenderableWidget(new StringWidget(tx + 8, y + 5, tileW - 14, 10,
                        Component.literal(t.label).withStyle(Style.EMPTY.withColor(TITLE_COL)), this.font));
                StringWidget sw = new StringWidget(tx + 8, y + 17, tileW - 14, 10, Component.empty(), this.font);
                addRenderableWidget(sw);
                binds.add(new Bind(sw, t.status));
                tileRects.add(new int[]{tx, y, tileW, TILE_H});
                col++;
                if (col == 2) { col = 0; y += TILE_H + GAP; }
            }
            if (col == 1) y += TILE_H + GAP;
        }
        y += 6;

        // Footer: Theme | Einstellungen | Reload | Schliessen
        footerTop = y - 4;
        y += 2;
        int fw = (W - 3 * GAP) / 4;
        addRenderableWidget(Button.builder(
                Component.literal("◆ " + Theme.current().name).withStyle(Style.EMPTY.withColor(TITLE_COL)),
                b -> { Theme.cycle(); this.rebuildWidgets(); }).bounds(x0, y, fw, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.settings"),
                b -> this.minecraft.setScreen(new SettingsScreen(this))).bounds(x0 + fw + GAP, y, fw, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.reload"),
                b -> { EliConfig.load(); this.rebuildWidgets(); }).bounds(x0 + 2 * (fw + GAP), y, fw, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.close"), b -> onClose())
                .bounds(x0 + 3 * (fw + GAP), y, W - 3 * (fw + GAP), H).build());
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
        if (searchBox != null && !searchBox.getValue().equals(lastQuery)) {
            lastQuery = searchBox.getValue();
            try { this.rebuildWidgets(); } catch (Throwable ignored) {}
        }
        for (Bind b : binds) b.w.setMessage(b.s.get());

        float alpha = Math.max(0f, Math.min(1f, (System.currentTimeMillis() - openedAt) / 160f));
        Chrome.back(graphics, panelLeft, panelTop, panelRight, panelBottom, titleBottom, footerTop, alpha);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        Chrome.front(graphics, panelLeft, panelTop, panelRight, panelBottom, titleBottom, footerTop);
        for (int[] r : tileRects) {
            if (mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3]) {
                Chrome.hoverGlow(graphics, r[0], r[1], r[2], r[3]);
                break;
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (EliHelperClient.OPEN_KEY.matches(event)) { this.onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
