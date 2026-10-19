package de.raindancer.modules.cosmetics.service;

import de.raindancer.core.ui.identity.NameBadges;
import de.raindancer.modules.cosmetics.CosmeticsSettings;
import de.raindancer.modules.cosmetics.store.TagChoices;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Which wearable name tags (Core's {@link NameBadges} with a label: a guild's tag, its stars) each player wears.
 * Core's wardrobe: asked from the chat thread and every name drawn, so it answers from memory, filled from the
 * player's data whenever {@link DataWatch} sees it change.
 */
public final class TagService implements ICosmeticsService {

    private final TagChoices store = new TagChoices();
    private final Map<UUID, Set<String>> hidden = new ConcurrentHashMap<>();
    private final Consumer<UUID> redraw;

    /** @param redraw draws a player's name again in chat, the tablist and above their head */
    public TagService(Consumer<UUID> redraw) {
        this.redraw = redraw;
    }

    @Override
    public void settings(CosmeticsSettings fresh) {
        // Nothing configurable: every wearable tag is the player's to take off.
    }

    public void install() {
        NameBadges.wardrobe(this::wears);
    }

    public void uninstall() {
        NameBadges.wardrobe(null);
        hidden.clear();
    }

    public boolean wears(UUID player, String owner) {
        Set<String> off = hidden.get(player);
        return off == null || !off.contains(owner);
    }

    public Set<String> hidden(UUID player) {
        return hidden.getOrDefault(player, Set.of());
    }

    /** Puts a tag on or takes it off. @return the tags now taken off */
    public Set<String> toggle(UUID player, String owner) {
        Set<String> changed = new HashSet<>(hidden(player));
        if (!changed.remove(owner)) {
            changed.add(owner);
        }
        take(player, changed);
        return hidden(player);
    }

    /** On the player's own thread: toggles and keeps it in their data. */
    public Set<String> toggle(Player player, String owner) {
        Set<String> now = toggle(player.getUniqueId(), owner);
        store.write(player, now);
        return now;
    }

    /** On the player's own thread: wears what their data says. */
    public void load(Player player) {
        take(player.getUniqueId(), store.read(player));
    }

    public Set<String> stored(Player player) {
        return store.read(player);
    }

    void take(UUID player, Set<String> off) {
        Set<String> fresh = Set.copyOf(off);
        Set<String> was = fresh.isEmpty() ? hidden.remove(player) : hidden.put(player, fresh);
        if (!fresh.equals(was == null ? Set.of() : was)) {
            redraw.accept(player);
        }
    }

    public void forget(UUID player) {
        hidden.remove(player);
    }
}
