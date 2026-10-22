package net.elihelper;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Schickt eine Reihe von Befehlen mit Abstand ab (Sell- und Order-Makro).
 * Laeuft im Client-Tick, genau wie die Hotkey-Logik. Keine Klicks, kein
 * Auto-Loop: feste Anzahl, Abstand und eine harte Obergrenze; jederzeit per
 * Stop-Taste oder Stop-Knopf abbrechbar.
 */
public final class MacroRunner {
    private static final Deque<String> queue = new ArrayDeque<>();
    private static int cooldown = 0;
    private static int delay = 10;
    private static String label = "";

    private MacroRunner() {}

    public static boolean running() { return !queue.isEmpty(); }
    public static int remaining() { return queue.size(); }
    public static String label() { return label; }

    public static void stop() {
        boolean was = !queue.isEmpty();
        queue.clear();
        cooldown = 0;
        if (was) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) overlay(mc, "Makro gestoppt");
        }
    }

    /** Jeden Client-Tick aufrufen. Schickt hoechstens einen Befehl pro Intervall. */
    public static void tick(Minecraft mc) {
        if (queue.isEmpty()) return;
        if (mc.player == null) { queue.clear(); return; }
        if (cooldown > 0) { cooldown--; return; }
        String cmd = queue.poll();
        send(mc, cmd);
        cooldown = delay;
        overlay(mc, label + " – " + queue.size() + " uebrig");
    }

    private static void start(String label, List<String> cmds, int delayTicks, Minecraft mc) {
        if (cmds.isEmpty()) return;
        queue.clear();
        MacroRunner.label = label;
        MacroRunner.delay = Math.max(2, delayTicks);
        MacroRunner.cooldown = 0;
        queue.addAll(cmds);
        overlay(mc, label + " gestartet (" + cmds.size() + "x)");
    }

    // ---- Sell ----
    public static void startSellCurrent() { startSell(Minecraft.getInstance()); }

    public static void startSell(Minecraft mc) {
        EliConfig c = EliConfig.get();
        if (!c.macroEnabled) { overlay(mc, "Makros sind aus (Config)"); return; }
        String cmd = c.sellCommand == null ? "" : c.sellCommand.trim();
        if (cmd.isEmpty()) { overlay(mc, "Kein Sell-Befehl gesetzt"); return; }
        int n = clamp(c.sellRepeat, 1, Math.max(1, c.macroMaxRepeat));
        List<String> cmds = new ArrayList<>(n);
        for (int i = 0; i < n; i++) cmds.add(cmd);
        start("Sell", cmds, c.sellDelayTicks, mc);
    }

    // ---- Order ----
    public static void startOrder(Minecraft mc) {
        EliConfig c = EliConfig.get();
        if (!c.macroEnabled) { overlay(mc, "Makros sind aus (Config)"); return; }
        String built = buildOrder(c);
        if (built.isEmpty()) { overlay(mc, "Order-Befehl ist leer"); return; }
        int n = clamp(c.orderRepeat, 1, Math.max(1, c.macroMaxRepeat));
        List<String> cmds = new ArrayList<>(n);
        for (int i = 0; i < n; i++) cmds.add(built);
        start("Order", cmds, c.sellDelayTicks, mc);
    }

    /** Baut den Order-Befehl aus dem Template und den gespeicherten Feldern. */
    public static String buildOrder(EliConfig c) {
        String t = c.orderCommand == null ? "" : c.orderCommand;
        t = t.replace("{item}", nz(c.orderItem))
             .replace("{amount}", nz(c.orderAmount))
             .replace("{price}", nz(c.orderPrice));
        return t.replaceAll("\\s+", " ").trim();
    }

    private static void send(Minecraft mc, String cmd) {
        String c = cmd.startsWith("/") ? cmd.substring(1) : cmd;
        mc.player.connection.sendCommand(c);
    }

    private static void overlay(Minecraft mc, String msg) {
        if (mc.gui != null) mc.gui.setOverlayMessage(Component.literal("EliHelper: " + msg), false);
    }

    private static String nz(String s) { return s == null ? "" : s.trim(); }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
