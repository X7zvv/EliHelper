package net.elihelper;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class EliHelperClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("EliHelper");
    public static final int HOTKEY_SLOTS = 4;

    public static KeyMapping OPEN_KEY;
    public static final KeyMapping[] HOTKEYS = new KeyMapping[HOTKEY_SLOTS];

    private static final Map<Integer, Long> lastPress = new HashMap<>();

    @Override
    public void onInitializeClient() {
        EliConfig.load();
        SellStats.load();

        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("elihelper", "main"));

        OPEN_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.elihelper.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));

        // Befehls-Hotkeys: standardmaessig OHNE Taste, du legst sie in der Steuerung selbst fest
        for (int i = 0; i < HOTKEY_SLOTS; i++) {
            HOTKEYS[i] = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.elihelper.cmd" + (i + 1), InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, cat));
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_KEY.consumeClick()) {
                if (client.screen == null) client.setScreen(new EliScreen());
            }
            for (int i = 0; i < HOTKEY_SLOTS; i++) {
                while (HOTKEYS[i].consumeClick()) {
                    if (client.screen == null) triggerSlot(client, i + 1);
                }
            }
            SellStats.tick();
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) SellStats.onChat(message.getString());
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> SellStats.save());
    }

    private static void triggerSlot(Minecraft mc, int slot) {
        if (mc.player == null) return;
        for (EliConfig.CommandEntry e : EliConfig.get().commands) {
            if (e.slot != slot) continue;
            long now = System.currentTimeMillis();
            if (e.confirm) {
                Long last = lastPress.get(slot);
                if (last == null || now - last > 1500) {
                    lastPress.put(slot, now);
                    mc.gui.setOverlayMessage(Component.literal("Nochmal druecken: /" + e.command), false);
                    return;
                }
                lastPress.remove(slot);
            }
            mc.player.connection.sendCommand(e.command.startsWith("/") ? e.command.substring(1) : e.command);
            return;
        }
    }
}
