package de.raindancer.modules.cosmetics.store;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;
import java.util.TreeSet;

/**
 * The name tags a player took off, in their own persistent data under {@code rainscosmetics:} — so Core carries
 * the choice to every server with the rest of their cosmetics. Only touch a player on their own thread.
 */
public final class TagChoices {

    private static final NamespacedKey HIDDEN = NamespacedKey.fromString("rainscosmetics:hidden-tags");

    public Set<String> read(Player player) {
        return parse(player.getPersistentDataContainer().get(HIDDEN, PersistentDataType.STRING));
    }

    public void write(Player player, Set<String> hidden) {
        String stored = format(hidden);
        if (stored == null) {
            player.getPersistentDataContainer().remove(HIDDEN);
        } else {
            player.getPersistentDataContainer().set(HIDDEN, PersistentDataType.STRING, stored);
        }
    }

    public static Set<String> parse(String stored) {
        java.util.SortedSet<String> hidden = new TreeSet<>();
        if (stored != null) {
            for (String owner : stored.split(",")) {
                if (!owner.isBlank()) {
                    hidden.add(owner.strip());
                }
            }
        }
        return java.util.Collections.unmodifiableSortedSet(hidden);
    }

    /** Null for none: nothing is kept for a player wearing everything. */
    public static String format(Set<String> hidden) {
        return hidden == null || hidden.isEmpty() ? null : String.join(",", new TreeSet<>(hidden));
    }
}
