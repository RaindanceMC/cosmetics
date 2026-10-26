package de.raindancer.modules.cosmetics.skins;

import de.raindancer.core.data.store.YamlStore;
import de.raindancer.core.platform.network.NetworkBus;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which skins each player has unlocked with their coffees, for good. One network document.
 *
 * <pre>
 * players:
 *   &lt;uuid&gt;:
 *     katana: 2026-10-26T12:00:00Z
 * </pre>
 */
public final class SkinUnlocks {

    private final YamlStore store;
    private final Map<UUID, Map<String, Instant>> unlocked = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Instant>> unwritten = new ConcurrentHashMap<>();
    private final Set<String> locked = ConcurrentHashMap.newKeySet();
    private volatile boolean readable = true;

    public SkinUnlocks(Path dataFolder) {
        this(YamlStore.network(dataFolder.resolve("skin-unlocks.yml")));
    }

    public SkinUnlocks(YamlStore store) {
        this.store = store;
    }

    public synchronized void load() {
        Optional<YamlConfiguration> read = store.readIfReadable();
        if (read.isEmpty()) {
            readable = false;
            return;
        }
        readable = true;
        Map<UUID, Map<String, Instant>> fresh = new HashMap<>();
        ConfigurationSection players = read.get().getConfigurationSection("players");
        if (players != null) {
            for (String id : players.getKeys(false)) {
                ConfigurationSection theirs = players.getConfigurationSection(id);
                if (theirs == null) {
                    continue;
                }
                try {
                    UUID who = UUID.fromString(id);
                    for (String skin : theirs.getKeys(false)) {
                        try {
                            fresh.computeIfAbsent(who, any -> new HashMap<>())
                                    .put(skin, Instant.parse(theirs.getString(skin, "")));
                        } catch (DateTimeParseException handEdited) {
                            fresh.computeIfAbsent(who, any -> new HashMap<>()).put(skin, Instant.EPOCH);
                        }
                    }
                } catch (IllegalArgumentException notAnId) {
                    // one broken line must not cost anybody else theirs
                }
            }
        }
        unwritten.forEach((who, theirs) -> fresh.computeIfAbsent(who, any -> new HashMap<>()).putAll(theirs));
        locked.forEach(key -> {
            int slash = key.indexOf('/');
            Map<String, Instant> theirs = fresh.get(UUID.fromString(key.substring(0, slash)));
            if (theirs != null) {
                theirs.remove(key.substring(slash + 1));
            }
        });
        unlocked.clear();
        fresh.forEach((who, theirs) -> unlocked.put(who, new ConcurrentHashMap<>(theirs)));
    }

    public boolean has(UUID player, String skin) {
        return unlocked.getOrDefault(player, Map.of()).containsKey(skin);
    }

    public int count(UUID player) {
        return unlocked.getOrDefault(player, Map.of()).size();
    }

    /** Oldest first. */
    public List<String> of(UUID player) {
        Map<String, Instant> theirs = new LinkedHashMap<>(unlocked.getOrDefault(player, Map.of()));
        return theirs.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.naturalOrder()))
                .map(Map.Entry::getKey).toList();
    }

    /** @return false when it was unlocked already. Held until {@link #save}. */
    public synchronized boolean unlock(UUID player, String skin, Instant at) {
        if (has(player, skin)) {
            return false;
        }
        unlocked.computeIfAbsent(player, any -> new ConcurrentHashMap<>()).put(skin, at);
        unwritten.computeIfAbsent(player, any -> new ConcurrentHashMap<>()).put(skin, at);
        locked.remove(player + "/" + skin);
        return true;
    }

    public synchronized boolean lock(UUID player, String skin) {
        Map<String, Instant> theirs = unlocked.get(player);
        if (theirs == null || theirs.remove(skin) == null) {
            return false;
        }
        Map<String, Instant> pending = unwritten.get(player);
        if (pending != null) {
            pending.remove(skin);
        }
        locked.add(player + "/" + skin);
        return true;
    }

    public boolean isDirty() {
        return !unwritten.isEmpty() || !locked.isEmpty();
    }

    public synchronized boolean save() {
        if (!readable) {
            load();
            if (!readable) {
                return false;
            }
        }
        if (!isDirty()) {
            return true;
        }
        Set<String> gone = new HashSet<>(locked);
        Map<UUID, Set<String>> sent = new HashMap<>();
        unwritten.forEach((who, theirs) -> sent.put(who, Set.copyOf(theirs.keySet())));
        boolean written = store.write(yaml -> {
            gone.forEach(key -> {
                int slash = key.indexOf('/');
                yaml.set("players." + key.substring(0, slash) + "." + key.substring(slash + 1), null);
            });
            unlocked.forEach((who, theirs) -> theirs.forEach((skin, at) ->
                    yaml.set("players." + who + "." + skin, at.toString())));
        });
        if (written) {
            sent.forEach((who, skins) -> {
                Map<String, Instant> pending = unwritten.get(who);
                if (pending != null) {
                    skins.forEach(pending::remove);
                    if (pending.isEmpty()) {
                        unwritten.remove(who);
                    }
                }
            });
            locked.removeAll(gone);
            load();
        }
        return written;
    }

    public NetworkBus.Subscription watch() {
        return store.onChange(() -> {
            synchronized (this) {
                save();
                load();
            }
        });
    }
}
