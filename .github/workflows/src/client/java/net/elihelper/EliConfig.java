package net.elihelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** JSON-Config unter .minecraft/config/elihelper.json */
public final class EliConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("elihelper.json");

    public static final class CommandEntry {
        public String label;
        public String command;      // ohne fuehrenden Schraegstrich
        public int slot = 0;        // 1-4 = Hotkey-Slot (Taste in Steuerung festlegen), 0 = kein Hotkey
        public boolean confirm = false; // true = Hotkey muss innerhalb von 1,5 s zweimal gedrueckt werden
        public CommandEntry() {}
        public CommandEntry(String label, String command, int slot, boolean confirm) {
            this.label = label; this.command = command; this.slot = slot; this.confirm = confirm;
        }
    }

    public List<CommandEntry> commands = new ArrayList<>();

    // Farbcodes
    public String colorPrefix = "&";      // z.B. "&" oder "§"
    public String hexFormat = "&#%s";     // %s = RRGGBB
    public int renameLimit = 35;
    public int signLimit = 30;

    // Statistik: Regex auf die Verkaufs-Bestaetigung im Chat. Gruppe "amount" = Betrag.
    // ACHTUNG: Standardwert ist geraten! Mit einer echten Chatzeile anpassen.
    public boolean statsEnabled = true;
    public String payoutRegex = "(?<amount>[0-9][0-9.,]*\\s?[kKmMbB]?)\\s?\\$";

    private static EliConfig instance = new EliConfig();
    public static EliConfig get() { return instance; }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                EliConfig loaded = GSON.fromJson(Files.readString(FILE), EliConfig.class);
                if (loaded != null && loaded.commands != null) { instance = loaded; save(); return; }
            }
        } catch (IOException | RuntimeException e) {
            EliHelperClient.LOGGER.warn("Config konnte nicht gelesen werden, nutze Standard", e);
        }
        instance = defaults();
        save();
    }

    private static EliConfig defaults() {
        EliConfig c = new EliConfig();
        c.commands.add(new CommandEntry("/spawn", "spawn", 1, false));
        c.commands.add(new CommandEntry("/afk", "afk", 2, true));
        return c;
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (IOException e) {
            EliHelperClient.LOGGER.warn("Config konnte nicht gespeichert werden", e);
        }
    }
}
