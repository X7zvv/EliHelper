package net.elihelper;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** Verkaufsstatistik, Minuten-Test und Multiplikator-Rechner. */
public class StatsScreen extends Screen {
    private static final int W = 260, H = 20;

    private final Screen parent;
    private StringWidget lSession, lAll, l24h, lHour, lTest, lResult, lProj;
    private EditBox multiBox, targetBox;
    private Button testBtn, pauseBtn;
    private int pl, pt, pr, pb, titleBottom, footerTop;
    private int sep1;

    public StatsScreen(Screen parent) {
        super(Component.translatable("elihelper.stats"));
        this.parent = parent;
    }

    private StringWidget line(int x, int y) {
        StringWidget w = new StringWidget(x, y, W, 11, Component.empty(), this.font);
        addRenderableWidget(w);
        return w;
    }

    @Override
    protected void init() {
        int x = (this.width - W) / 2;
        int top = Math.max(8, (this.height - 290) / 2);
        pl = x - 14; pr = x + W + 14; pt = top; titleBottom = pt + 26;

        addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose())
                .bounds(pr - 24, pt + 4, 18, 18).build());
        addRenderableWidget(EliScreen.centered(this.font,
                Component.translatable("elihelper.stats").withStyle(Style.EMPTY.withBold(true).withColor(EliScreen.TEXT_PURPLE)),
                x, pt + 8, W));

        int y = titleBottom + 8;

        lSession = line(x, y); y += 13;
        lAll = line(x, y); y += 13;
        l24h = line(x, y); y += 13;
        lHour = line(x, y); y += 10;

        sep1 = y; y += 8;

        addRenderableWidget(EliScreen.centered(this.font, EliScreen.purple("elihelper.minutetest"), x, y, W));
        y += 14;

        multiBox = new EditBox(this.font, x, y, 120, H, Component.literal("Multi"));
        multiBox.setValue("1.0");
        multiBox.setMaxLength(8);
        multiBox.setHint(Component.translatable("elihelper.multi.current"));
        addRenderableWidget(multiBox);
        targetBox = new EditBox(this.font, x + 124, y, W - 124, H, Component.literal("Ziel"));
        targetBox.setValue("2.0");
        targetBox.setMaxLength(8);
        targetBox.setHint(Component.translatable("elihelper.multi.target"));
        addRenderableWidget(targetBox);
        y += H + 4;

        testBtn = Button.builder(Component.translatable("elihelper.test.start"), b -> {
            if (SellStats.testRunning) SellStats.cancelTest();
            else SellStats.startTest(num(multiBox, 1.0));
        }).bounds(x, y, W, H).build();
        addRenderableWidget(testBtn);
        y += H + 6;

        lTest = line(x, y); y += 13;
        lResult = line(x, y); y += 13;
        lProj = line(x, y); y += 18;

        // Tracking an/aus
        pauseBtn = Button.builder(Component.empty(), b -> {
            EliConfig.get().statsEnabled = !EliConfig.get().statsEnabled;
            EliConfig.save();
        }).bounds(x, y, W, H).build();
        addRenderableWidget(pauseBtn);
        y += H + 6;

        // Footer
        footerTop = y - 6;
        int half = (W - 4) / 2;
        addRenderableWidget(Button.builder(Component.translatable("elihelper.reset"), b -> SellStats.resetSession())
                .bounds(x, y, half, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.back"), b -> onClose())
                .bounds(x + half + 4, y, W - half - 4, H).build());
        pb = y + H + 8;
    }

    private static double num(EditBox box, double fallback) {
        try {
            double v = Double.parseDouble(box.getValue().trim().replace(',', '.'));
            return v > 0 ? v : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Component t(String key, String value) {
        return Component.translatable(key).withStyle(Style.EMPTY.withColor(0xFFFFFF))
                .append(Component.literal(" " + value).withStyle(Style.EMPTY.withColor(EliScreen.TEXT_PURPLE)));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        lSession.setMessage(t("elihelper.stat.session", SellStats.fmt(SellStats.session)));
        lAll.setMessage(t("elihelper.stat.all", SellStats.fmt(SellStats.allTime())));
        l24h.setMessage(t("elihelper.stat.24h", SellStats.fmt(SellStats.last24h())));
        lHour.setMessage(t("elihelper.stat.hour", SellStats.fmt(SellStats.perHour())));

        if (SellStats.testRunning) {
            testBtn.setMessage(Component.translatable("elihelper.test.cancel"));
            lTest.setMessage(t("elihelper.test.running",
                    (SellStats.testTicksLeft / 20) + "s | " + SellStats.fmt(SellStats.testSum)));
        } else {
            testBtn.setMessage(Component.translatable("elihelper.test.start"));
            lTest.setMessage(Component.empty());
        }

        boolean on = EliConfig.get().statsEnabled;
        pauseBtn.setMessage(Component.translatable(on ? "elihelper.stats.pause" : "elihelper.stats.resume")
                .withStyle(Style.EMPTY.withColor(on ? 0xFFFFFF : 0xFF8855)));

        SellStats.TestResult r = SellStats.lastTest();
        if (r == null) {
            lResult.setMessage(Component.translatable("elihelper.test.none")
                    .withStyle(Style.EMPTY.withColor(0xAAAAAA)));
            lProj.setMessage(Component.empty());
        } else {
            lResult.setMessage(t("elihelper.test.last", SellStats.fmt(r.amount) + " @ " + r.multi + "x"));
            double target = num(targetBox, 2.0);
            double perMin = SellStats.projectPerMinute(r, target);
            lProj.setMessage(t("elihelper.test.proj",
                    target + "x: " + SellStats.fmt(perMin) + "/min | " + SellStats.fmt(perMin * 60) + "/h"));
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
