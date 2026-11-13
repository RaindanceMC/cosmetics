package de.raindancer.modules.cosmetics.prime;

import de.raindancer.core.data.store.YamlStore;
import de.raindancer.core.platform.network.NetworkBus;
import de.raindancer.core.social.support.PrimeMembers;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Puts each Prime member's drops on them as permission nodes while they are Prime and Prime-only mode is on;
 * {@code prime-drops.yml} is a network document, written with the first two months the first time.
 */
public final class PrimeDropService implements org.bukkit.event.Listener {

    private final Plugin plugin;
    private final YamlStore store;
    private final Supplier<PrimeMembers> members;
    private final BooleanSupplier primeOnly;
    private final Consumer<String> warn;
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> given = new ConcurrentHashMap<>();
    private volatile PrimeDrops drops = PrimeDrops.NONE;

    public PrimeDropService(Plugin plugin, YamlStore store, Supplier<PrimeMembers> members, BooleanSupplier primeOnly,
                            Consumer<String> warn) {
        this.plugin = plugin;
        this.store = store;
        this.members = members;
        this.primeOnly = primeOnly;
        this.warn = warn;
    }

    public void load() {
        store.writeTextIfAbsent(PrimeDrops.SHIPPED);
        YamlConfiguration read = store.readIfReadable().orElse(null);
        if (read == null) {
            warn.accept("prime-drops.yml could not be read — no drops until it is fixed.");
            drops = PrimeDrops.NONE;
            return;
        }
        drops = PrimeDrops.read(read, warn);
    }

    public NetworkBus.Subscription watch() {
        return store.onChange(this::load);
    }

    public PrimeDrops drops() {
        return drops;
    }

    /** On the player's own thread. */
    public void sync(Player player) {
        UUID who = player.getUniqueId();
        PrimeMembers prime = members.get();
        Set<String> should = primeOnly.getAsBoolean() && prime != null && prime.isPrime(who, Instant.now())
                ? drops.nodesFor(prime.months(who)) : Set.of();
        if (should.equals(given.getOrDefault(who, Set.of()))) {
            return;
        }
        forget(who);
        if (!should.isEmpty()) {
            PermissionAttachment attachment = player.addAttachment(plugin);
            should.forEach(node -> attachment.setPermission(node, true));
            attachments.put(who, attachment);
            given.put(who, Set.copyOf(should));
        }
        player.recalculatePermissions();
    }

    public void forget(UUID who) {
        given.remove(who);
        PermissionAttachment attachment = attachments.remove(who);
        if (attachment != null) {
            try {
                attachment.remove();
            } catch (IllegalStateException alreadyGone) {
                // the player has left
            }
        }
    }

    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.LOW)
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        sync(event.getPlayer());
    }

    @org.bukkit.event.EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        forget(event.getPlayer().getUniqueId());
    }

    public void forgetEveryone() {
        Set.copyOf(attachments.keySet()).forEach(this::forget);
    }
}
