package de.raindancer.modules.cosmetics.flair;

import org.bukkit.entity.Player;

/** Where a player's flair is kept. Only touch a player on their own thread. */
public interface FlairStore {

    FlairChoice read(Player player);

    void write(Player player, FlairChoice choice);

    /** In the player's own persistent data under {@code rainscosmetics:}, so Core carries it to every server. */
    static FlairStore persistent() {
        org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString("rainscosmetics:flair");
        return new FlairStore() {
            @Override
            public FlairChoice read(Player player) {
                return FlairChoice.parse(player.getPersistentDataContainer().get(key,
                        org.bukkit.persistence.PersistentDataType.STRING));
            }

            @Override
            public void write(Player player, FlairChoice choice) {
                String stored = choice.format();
                if (stored == null) {
                    player.getPersistentDataContainer().remove(key);
                } else {
                    player.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, stored);
                }
            }
        };
    }
}
