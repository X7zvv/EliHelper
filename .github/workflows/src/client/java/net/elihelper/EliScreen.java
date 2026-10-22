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

/** Hauptmenue als Kachel-Grid mit Suchleiste, Kategorien und Live-Status. */
public class EliScreen extends Screen {
    static final int PURPLE_MAIN = 0xFF7C3AED;
    static final int PURPLE_LIGHT = 0xFFB388FF;
    static int textPurple() { return UiTheme.text(); }

    static final int TITLE_COL  = 0xE8E0FF;
    static final int ON_COL     = 0x66FF99;
    static final int OFF_COL    = 0xFF6B6B;
    static final int MUTED_COL  = 0x9A93B5;

    private static final int W = 340, TILE_H = 34, GAP = 7, H = 22;

    private int panelLeft, panelTop, panelRight, panelBottom, titleBottom, footerTop;
    private final List<Integer> catSeps = new ArrayList<>();
    private final List<Bind> binds = new ArrayList<>();
    private EditBox searchBox;
    private String lastQuery = "";

    public EliScreen() {
        super(Component.translatable("elihelper.title"));
    }

    // ---- kleine Hilfstypen ----
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
        return Component.translatable(key).withStyle(Style.EMPTY.withColor(textPurple()));
    }
    static StringWidget centered(Font font, Component c, int x, int y, int w) {
        int tw = font.width(c);
        return new StringWidget(x + (w - tw) / 2, y, tw, 10, c, font);
    }
    private static Component st(String text, int color) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(color));
    }

    /** Baut die Kategorien aus Config + festen Eintraegen und filtert nach der Suche. */
    private List<Cat> model(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<Cat> all = new ArrayList<>();

        Cat cmds = new Cat(Component.translatable("elihelper.section.commands").getString());
        for (EliConfig.CommandEntry e : EliConfig.get().commands) {
            final String command = e.command;
            cmds.tiles.add(new Tile("» " + e.label,
                    () -> st("/" + command, MUTED_COL),
                    () -> sendCommand(command)));
        }
        all.add(cmds);

        Cat handel = new Cat(Component.translatable("elihelper.section.trade").getString());
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

        Cat tools = new Cat(Component.translatable("elihelper.section.tools").getString());
        tools.tiles.add(new Tile("✎ " + tr("elihelper.colors"),
                () -> st("/rename, /sign", MUTED_COL),
                () -> this.minecraft.setScreen(new ColorScreen(this))));
        tools.tiles.add(new Tile("⇄ " + tr("elihelper.autotpa"),
                () -> EliConfig.get().autoTpaEnabled ? st(tr("elihelper.on"), ON_COL) : st(tr("elihelper.off"), OFF_COL),
                () -> { EliConfig.get().autoTpaEnabled = !EliConfig.get().autoTpaEnabled; EliConfig.save(); }));
        tools.tiles.add(new Tile("⚙ " + tr("elihelper.settings"),
                () -> st(tr("elihelper.settings.sub"), MUTED_COL),
                () -> this.minecraft.setScreen(new SettingsScreen(this))));
        all.add(tools);

        Cat hugo = new Cat("HugoSMP");
        hugo.tiles.add(new Tile("✦ Styles & Cosmetics",
                () -> st("Crystal, Armor, Sky & mehr", MUTED_COL),
                () -> this.minecraft.setScreen(new de.elihelper.styles.EliHelperConfigScreen(this))));
        hugo.tiles.add(new Tile("◈ Mehr Texturpacks",
                () -> st("Stone, Ores & Swords", MUTED_COL),
                () -> this.minecraft.setScreen(new de.elihelper.styles.EliHelperConfigScreen(this))));
        hugo.tiles.add(new Tile("● UI-Farbe ändern",
                () -> st(UiTheme.NAMES[UiTheme.selected()], textPurple()),
                () -> this.minecraft.setScreen(new UiThemeScreen(this))));
        all.add(hugo);

        if (q.isEmpty()) return all;
        List<Cat> out = new ArrayList<>();
        for (Cat c : all) {
            Cat f = new Cat(c.name);
            for (Tile t : c.tiles) if (t.label.toLowerCase().contains(q)) f.tiles.add(t);
            if (!f.tiles.isEmpty()) out.add(f);
        }
        return out;
    }

    private static String tr(String key) { return Component.translatable(key).getString(); }

    @Override
    protected void init() {
        binds.clear();
        catSeps.clear();
        int x0 = (this.width - W) / 2;

        List<Cat> cats = model(lastQuery);
        int tileW = (W - GAP) / 2;

        // Hoehe abschaetzen
        int contentH = 30;
        for (Cat c : cats) contentH += 16 + ((c.tiles.size() + 1) / 2) * (TILE_H + GAP);
        if (cats.isEmpty()) contentH += 20;
        contentH += 10 + H + 10;

        panelTop = Math.max(8, (this.height - contentH) / 2);
        panelLeft = x0 - 14;
        panelRight = x0 + W + 14;
        titleBottom = panelTop + 30;

        // Titelzeile: Suchfeld + Close-X
        searchBox = new EditBox(this.font, x0, panelTop + 6, W - 28, 18, Component.translatable("elihelper.search"));
        searchBox.setMaxLength(40);
        searchBox.setHint(Component.translatable("elihelper.search"));
        searchBox.setValue(lastQuery);
        addRenderableWidget(searchBox);
        setFocused(searchBox);
        addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose())
                .bounds(panelRight - 24, panelTop + 6, 18, 18).build());

        int y = titleBottom + 8;

        if (cats.isEmpty()) {
            addRenderableWidget(centered(this.font,
                    Component.translatable("elihelper.search.none").withStyle(Style.EMPTY.withColor(MUTED_COL)),
                    x0, y + 4, W));
            y += 20;
        }

        for (Cat c : cats) {
            addRenderableWidget(new StringWidget(x0 + 2, y,
                    this.font.width(Component.literal(c.name)), 10,
                    Component.literal(c.name).withStyle(Style.EMPTY.withColor(textPurple()).withBold(true)), this.font));
            y += 13;
            catSeps.add(y - 2);
            y += 1;

            int col = 0;
            for (Tile t : c.tiles) {
                int tx = x0 + col * (tileW + GAP);
                addRenderableWidget(Button.builder(Component.empty(), b -> t.action.run())
                        .bounds(tx, y, tileW, TILE_H).build());
                addRenderableWidget(new StringWidget(tx + 8, y + 5, tileW - 14, 10,
                        Component.literal(t.label).withStyle(Style.EMPTY.withColor(TITLE_COL)), this.font));
                StringWidget sw = new StringWidget(tx + 8, y + 17, tileW - 14, 10, Component.empty(), this.font);
                addRenderableWidget(sw);
                binds.add(new Bind(sw, t.status));
                col++;
                if (col == 2) { col = 0; y += TILE_H + GAP; }
            }
            if (col == 1) y += TILE_H + GAP;
            y += 6;
        }

        // Footer
        footerTop = y - 4;
        y += 2;
        int fw = (W - 2 * GAP) / 3;
        addRenderableWidget(Button.builder(Component.translatable("elihelper.reload"), b -> {
            EliConfig.load();
            this.rebuildWidgets();
        }).bounds(x0, y, fw, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.settings"),
                b -> this.minecraft.setScreen(new SettingsScreen(this))).bounds(x0 + fw + GAP, y, fw, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.close"), b -> onClose())
                .bounds(x0 + 2 * (fw + GAP), y, W - 2 * (fw + GAP), H).build());
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
        // Live-Suche: bei Aenderung neu aufbauen (abgesichert)
        if (searchBox != null && !searchBox.getValue().equals(lastQuery)) {
            lastQuery = searchBox.getValue();
            try { this.rebuildWidgets(); } catch (Throwable ignored) {}
        }
        for (Bind b : binds) b.w.setMessage(b.s.get());

        Chrome.back(graphics, panelLeft, panelTop, panelRight, panelBottom, titleBottom, footerTop);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        Chrome.front(graphics, panelLeft, panelTop, panelRight, panelBottom, titleBottom, footerTop);
        for (int sy : catSeps) Chrome.separator(graphics, panelLeft + 10, sy, panelRight - panelLeft - 20);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (EliHelperClient.OPEN_KEY.matches(event)) { this.onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
