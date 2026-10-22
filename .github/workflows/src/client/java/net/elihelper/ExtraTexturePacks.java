package net.elihelper;

import net.fabricmc.loader.api.FabricLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Installs additional small pixel-art resource packs once, without overwriting user files. */
public final class ExtraTexturePacks {
    private record Palette(String id, String description, int base, int dark, int ore, int accent, boolean sword) {}
    private static final Palette[] PALETTES = {
        new Palette("stone_cyan", "Cyan Stone & Ores • EliHelper", 0xFF303C46, 0xFF1C252D, 0xFF00D9FF, 0xFF8CF4FF, false),
        new Palette("stone_gold", "Amber Stone & Ores • EliHelper", 0xFF40372B, 0xFF272116, 0xFFFFB52E, 0xFFFFE39A, false),
        new Palette("stone_mint", "Mint Stone & Ores • EliHelper", 0xFF2B4038, 0xFF182B24, 0xFF55F0A0, 0xFFB2FFD7, false),
        new Palette("sword_cyan", "Cyan Sword Pack • EliHelper", 0xFF173B48, 0xFF0A1E28, 0xFF00D9FF, 0xFFB6F7FF, true),
        new Palette("sword_rose", "Rose Sword Pack • EliHelper", 0xFF48283A, 0xFF241321, 0xFFFF4F9A, 0xFFFFC1DF, true),
        new Palette("sword_amber", "Amber Sword Pack • EliHelper", 0xFF46351C, 0xFF251B0B, 0xFFFFB52E, 0xFFFFE8A8, true)
    };

    private ExtraTexturePacks() {}

    public static void install() {
        Path root = FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
        for (Palette palette : PALETTES) {
            Path pack = root.resolve(palette.id);
            try {
                if (Files.exists(pack.resolve("pack.mcmeta"))) continue;
                Files.createDirectories(pack);
                String meta = "{\n  \"pack\": { \"description\": \"" + palette.description +
                    "\", \"pack_format\": 75, \"min_format\": 65, \"max_format\": 9999 }\n}";
                Files.writeString(pack.resolve("pack.mcmeta"), meta, StandardCharsets.UTF_8);
                Path textureRoot = pack.resolve("assets/minecraft/textures");
                if (palette.sword) {
                    Path items = textureRoot.resolve("item");
                    Files.createDirectories(items);
                    writeSword(items.resolve("diamond_sword.png"), palette);
                    writeSword(items.resolve("netherite_sword.png"), palette);
                } else {
                    Path blocks = textureRoot.resolve("block");
                    Files.createDirectories(blocks);
                    writeStone(blocks.resolve("stone.png"), palette, 0);
                    writeStone(blocks.resolve("coal_ore.png"), palette, 1);
                    writeStone(blocks.resolve("iron_ore.png"), palette, 2);
                    writeStone(blocks.resolve("gold_ore.png"), palette, 3);
                    writeStone(blocks.resolve("diamond_ore.png"), palette, 4);
                    writeStone(blocks.resolve("emerald_ore.png"), palette, 5);
                    writeStone(blocks.resolve("redstone_ore.png"), palette, 6);
                }
            } catch (IOException e) {
                EliHelperClient.LOGGER.warn("Could not install resource pack {}", palette.id, e);
            }
        }
    }

    private static void writeStone(Path path, Palette p, int type) throws IOException {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            int seed = (x * 17 + y * 31 + x * y * 7) % 23;
            int color = seed < 3 ? p.dark : (seed > 18 ? brighten(p.base, 12) : p.base);
            if (type > 0 && ((x * 5 + y * 7 + type * 3) % (type == 1 ? 7 : 5) == 0 ||
                    (x + y * 2 + type) % 11 == 0)) color = type == 1 ? p.dark :
                    (type == 2 ? 0xFFBFC6CC : type == 3 ? p.accent : p.ore);
            if (type == 4 && (x + y) % 5 == 0) color = p.accent;
            if (type == 5 && (x * 2 + y) % 7 == 0) color = 0xFF6DFF8C;
            if (type == 6 && (x + y * 3) % 6 == 0) color = 0xFFFF4B61;
            image.setRGB(x, y, color);
        }
        ImageIO.write(image, "png", path.toFile());
    }

    private static void writeSword(Path path, Palette p) throws IOException {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < 11; i++) {
            int x = 13 - i, y = 1 + i;
            if (x >= 0 && x < 16 && y >= 0 && y < 16) {
                image.setRGB(x, y, p.accent);
                if (x > 0) image.setRGB(x - 1, y, p.ore);
                if (y > 1 && x < 15) image.setRGB(x + 1, y - 1, p.ore);
            }
        }
        for (int i = 0; i < 5; i++) {
            int x = 5 - i, y = 9 + i;
            if (x >= 0 && x < 16 && y < 16) image.setRGB(x, y, p.dark);
        }
        for (int x = 4; x < 9; x++) image.setRGB(x, 8, p.accent);
        image.setRGB(8, 7, p.accent);
        ImageIO.write(image, "png", path.toFile());
    }

    private static int brighten(int argb, int amount) {
        int r = Math.min(255, ((argb >>> 16) & 255) + amount);
        int g = Math.min(255, ((argb >>> 8) & 255) + amount);
        int b = Math.min(255, (argb & 255) + amount);
        return (argb & 0xFF000000) | r << 16 | g << 8 | b;
    }
}
