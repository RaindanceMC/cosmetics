package de.raindancer.modules.cosmetics.skins;

import de.raindancer.core.RainsCore;
import de.raindancer.core.content.pack.JarPack;
import de.raindancer.core.platform.log.Log;
import de.raindancer.core.ui.menu.Backdrop;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The weapon skins' models (art/skins/convert.py) and their menus' painting (art/skins/menus.py), offered to Core's
 * resource pack. The credits for every pack the skins came from ship inside it, as CC-BY asks.
 */
public final class SkinPack {

    private static final String ROOT = "/de/raindancer/modules/cosmetics/pack/";
    private static final String OWNER = "Cosmetics";
    private static final String NAME = "skins";
    private static final Key MENU_FONT = Key.key(SkinRules.OUR_NAMESPACE, "menu");
    /** Dark roast on the cream card. */
    private static final TextColor INK = TextColor.color(0x5C3A22);

    private SkinPack() {
    }

    public static void contribute(Plugin plugin, RainsCore core) {
        try {
            JarPack.offer(plugin, core.resourcePacks(), SkinPack.class, ROOT, files(), OWNER, NAME,
                    "Weapon skins for supporters, and their coffee-house menus");
        } catch (IOException failure) {
            Log.of("cosmetics").warn("The weapon skins could not be written ({}); skinned weapons look like plain ones.",
                    failure.getMessage());
        }
    }

    public static void withdraw(RainsCore core) {
        core.resourcePacks().withdraw(OWNER.toLowerCase(java.util.Locale.ROOT) + ":" + NAME);
    }

    /** The menu card, for a window of {@code rows}. */
    public static Backdrop backdrop(int rows) {
        return new Backdrop(MENU_FONT, "" + (char) (0xE000 + Math.max(1, Math.min(6, rows))) + "", INK);
    }

    static List<String> files() {
        try (InputStream in = SkinPack.class.getResourceAsStream(ROOT + "files.txt")) {
            if (in == null) {
                return List.of();
            }
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                    .map(String::trim).filter(line -> !line.isEmpty()).toList();
        } catch (IOException unreadable) {
            return List.of();
        }
    }
}
