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
    private Button testBtn;
    private int pl, pt, pr, pb;

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
        int y = Math.max(10, (this.height - 250) / 2);
        pl = x - 12; pr = x + W + 12; pt = y - 8;

        addRenderableWidget(EliScreen.centered(this.font,
                Component.translatable("elihelper.stats").withStyle(Style.EMPTY.withBold(true).withColor(EliScreen.TEXT_PURPLE)),
                x, y + 2, W));
        y += 22;

        lSession = line(x, y); y += 13;
        lAll = line(x, y); y += 13;
        l24h = line(x, y); y += 13;
        lHour = line(x, y); y += 20;

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
        lProj = line(x, y); y += 20;

        addRenderableWidget(Button.builder(Component.translatable("elihelper.reset"), b -> SellStats.resetSession())
                .bounds(x, y, W / 2 - 2, H).build());
        addRenderableWidget(Button.builder(Component.translatable("elihelper.back"), b -> onClose())
                .bounds(x + W / 2 + 2, y, W / 2 - 2, H).build());
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

        super.extractRenderState(graphics, mouseX, mouseY, delta);
        EliScreen.frame(graphics, pl, pt, pr, pb);
    }

    @Override
    public void onClose() { this.minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
