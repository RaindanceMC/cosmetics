package de.raindancer.modules.cosmetics.flair;

import de.raindancer.core.ui.identity.JoinLines;
import de.raindancer.core.ui.identity.NameBadges;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.cosmetics.CosmeticsSettings;
import de.raindancer.modules.cosmetics.service.ICosmeticsService;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Prime's flair and the creators' Twitch extras: a tag in front of the name, the line when they join and leave, and an
 * effect for where they win a fight or fall. What each player wears is held in memory, already checked against their
 * nodes — chat asks for the tag on every line, from its own thread — and filled from their data on join, when it
 * changes and every time Prime is looked at again.
 */
public final class FlairService implements ICosmeticsService {

    /** The tag's owner name in Core's {@link NameBadges}. */
    public static final String BADGE = "cosmetics-flair";

    private static final String OFF = "off";

    private final FlairStore store;
    private final Consumer<UUID> redraw;
    private final Messages messages;
    private final FlairRule rule = new FlairRule();
    private final Map<UUID, FlairChoice> worn = new ConcurrentHashMap<>();
    private volatile java.util.function.BiConsumer<Player, FlairOption> audition = (who, option) -> { };

    /** @param redraw draws a player's name again in chat, the tablist and above their head */
    public FlairService(FlairStore store, Consumer<UUID> redraw, Messages messages) {
        this.store = store;
        this.redraw = redraw;
        this.messages = messages;
    }

    @Override
    public void settings(CosmeticsSettings fresh) {
        // Nothing configurable: what a perk allows is the perk's.
    }

    /** Puts the tag in front of names and the lines in the welcome. */
    public void install() {
        NameBadges.register(BADGE, "Flair tag", id -> rule.tag(effective(id)));
        JoinLines.provider((id, kind) -> rule.line(effective(id), kind));
    }

    public void uninstall() {
        NameBadges.unregister(BADGE);
        JoinLines.provider(null);
        worn.clear();
    }

    public FlairChoice effective(UUID player) {
        return worn.getOrDefault(player, FlairChoice.EMPTY);
    }

    public Optional<FlairEffect> killEffect(UUID player) {
        return rule.effect(effective(player), FlairCategory.KILL);
    }

    public Optional<FlairEffect> deathEffect(UUID player) {
        return rule.effect(effective(player), FlairCategory.DEATH);
    }

    public Optional<FlairEffect> joinChime(UUID player) {
        return rule.effect(effective(player), FlairCategory.JOIN_SOUND);
    }

    public Optional<FlairOption> teleportSound(UUID player) {
        return rule.teleport(effective(player));
    }

    /** Called with what somebody has just picked in the sound categories, so it can be played to them once. */
    public void preview(java.util.function.BiConsumer<Player, FlairOption> audition) {
        this.audition = audition == null ? (who, option) -> { } : audition;
    }

    public FlairRule rule() {
        return rule;
    }

    /** On the player's own thread: wears what their data says they may. */
    public void load(Player player) {
        apply(player, rule.effective(store.read(player), player::hasPermission));
    }

    /** On the player's own thread: as {@link #load}, and takes out of their data what they may no longer wear. */
    public void revalidate(Player player) {
        FlairChoice stored = store.read(player);
        FlairChoice allowed = rule.effective(stored, player::hasPermission);
        if (!allowed.equals(stored)) {
            store.write(player, allowed);
            messages.send(player, "cosmetics.flair.dropped");
        }
        apply(player, allowed);
    }

    /** The stored choice, for the data watch to notice a change from elsewhere. */
    public String stored(Player player) {
        return store.read(player).format();
    }

    private void apply(Player player, FlairChoice allowed) {
        FlairChoice was = allowed.isEmpty() ? worn.remove(player.getUniqueId()) : worn.put(player.getUniqueId(), allowed);
        if (!allowed.equals(was == null ? FlairChoice.EMPTY : was)) {
            redraw.accept(player.getUniqueId());
        }
    }

    public void forget(UUID player) {
        worn.remove(player);
    }

    /**
     * On the player's own thread. {@code off} takes the category off.
     *
     * @return whether it was done; a refusal has been said to them
     */
    public boolean choose(Player player, FlairCategory category, String id) {
        FlairChoice stored = store.read(player);
        if (id == null || id.isBlank() || id.equalsIgnoreCase(OFF) || id.equalsIgnoreCase("none")) {
            store.write(player, stored.without(category));
            load(player);
            messages.send(player, "cosmetics.flair.taken-off", "category", category.title().toLowerCase());
            return true;
        }
        Optional<FlairOption> option = FlairCatalogue.find(category, id);
        if (option.isEmpty()) {
            messages.send(player, "cosmetics.flair.unknown", "detail", id);
            return false;
        }
        if (!rule.may(option.get(), player::hasPermission)) {
            messages.send(player, option.get().node().equals(de.raindancer.modules.cosmetics.util.PermissionNodes.CREATOR)
                    ? "cosmetics.flair.refused-creator" : "cosmetics.flair.refused-prime", "option", option.get().title());
            return false;
        }
        store.write(player, stored.with(category, option.get().id()));
        load(player);
        messages.send(player, "cosmetics.flair.set", "category", category.title(), "option", option.get().title());
        if (category == FlairCategory.JOIN_SOUND || category == FlairCategory.TELEPORT) {
            audition.accept(player, option.get());
        }
        return true;
    }

    /** What they may pick in a category, in the order of the page; {@code off} first. */
    public List<String> choices(Player player, FlairCategory category) {
        List<String> ids = new ArrayList<>(List.of(OFF));
        for (FlairOption option : FlairCatalogue.of(category)) {
            if (rule.may(option, player::hasPermission)) {
                ids.add(option.id());
            }
        }
        return ids;
    }

    /** One step through {@link #choices}, round the end; for a page's click. */
    public boolean cycle(Player player, FlairCategory category, boolean backwards) {
        List<String> ids = choices(player, category);
        String current = effective(player.getUniqueId()).get(category).orElse(OFF);
        int at = Math.max(0, ids.indexOf(current));
        int next = Math.floorMod(at + (backwards ? -1 : 1), ids.size());
        return choose(player, category, ids.get(next));
    }

    /** Whether they have anything on the page: Prime's flair or the creators' extras. */
    public boolean mayOpen(Player player) {
        return FlairCatalogue.all().stream().anyMatch(option -> rule.may(option, player::hasPermission));
    }
}
