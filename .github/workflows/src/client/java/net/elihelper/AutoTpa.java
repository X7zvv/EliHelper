package net.elihelper;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.regex.Pattern;

/** Nimmt Teleport-Anfragen automatisch an, wenn aktiviert. Liest nur Chat und sendet tpaccept. */
public final class AutoTpa {
    private static Pattern pattern;
    private static String patternSource = "";
    private static long lastAccept = 0;

    private AutoTpa() {}

    private static Pattern pattern() {
        String src = EliConfig.get().tpaRegex;
        if (pattern == null || !src.equals(patternSource)) {
            try {
                pattern = Pattern.compile(src);
            } catch (RuntimeException e) {
                EliHelperClient.LOGGER.warn("tpaRegex ungueltig: {}", src);
                pattern = Pattern.compile("(?!x)x"); // matcht nie
            }
            patternSource = src;
        }
        return pattern;
    }

    public static void onChat(String text) {
        EliConfig c = EliConfig.get();
        if (!c.autoTpaEnabled) return;
        try {
            if (!pattern().matcher(text).find()) return;
        } catch (RuntimeException e) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastAccept < 2000) return; // Spam-Schutz
        lastAccept = now;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String cmd = c.tpaAccept == null ? "" : c.tpaAccept.trim();
        if (cmd.isEmpty()) return;
        mc.player.connection.sendCommand(cmd.startsWith("/") ? cmd.substring(1) : cmd);
        if (mc.gui != null) mc.gui.setOverlayMessage(Component.literal("EliHelper: TPA angenommen"), false);
    }
}
