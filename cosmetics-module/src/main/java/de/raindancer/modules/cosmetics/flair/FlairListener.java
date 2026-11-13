package de.raindancer.modules.cosmetics.flair;

import de.raindancer.core.ui.identity.JoinLines;
import de.raindancer.modules.cosmetics.listener.ICosmeticsListener;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Flair at the moments it shows: joining and leaving where vanilla still says the line (where the welcome says it, it
 * asks {@link JoinLines} itself), and the effects where somebody wins a fight or falls. Never changes what happens,
 * only what is seen and heard.
 */
public final class FlairListener implements ICosmeticsListener {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final FlairService flair;
    private final Predicate<UUID> hidden;
    private final de.raindancer.core.ui.effect.Effects effects;

    /** @param hidden whether somebody is vanished — their fights must not give them away */
    public FlairListener(FlairService flair, de.raindancer.core.ui.effect.Effects effects, Predicate<UUID> hidden) {
        this.flair = flair;
        this.effects = effects;
        this.hidden = hidden;
    }

    /** After the vanish listener, which empties the line of somebody hidden; only a line still there is replaced. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        flair.load(event.getPlayer());
        if (event.joinMessage() != null) {
            say(event.getPlayer(), JoinLines.Kind.JOIN).ifPresent(event::joinMessage);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        if (event.quitMessage() != null) {
            say(event.getPlayer(), JoinLines.Kind.QUIT).ifPresent(event::quitMessage);
        }
    }

    private java.util.Optional<net.kyori.adventure.text.Component> say(Player who, JoinLines.Kind kind) {
        return JoinLines.custom(who.getUniqueId(), kind)
                .map(line -> MINI.deserialize(line, Placeholder.unparsed("player", who.getName())));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim) && !hidden.test(killer.getUniqueId())) {
            flair.killEffect(killer.getUniqueId()).ifPresent(effect ->
                    FlairEffects.play(effects, victim.getLocation(), effect));
        }
        if (!hidden.test(victim.getUniqueId())) {
            flair.deathEffect(victim.getUniqueId()).ifPresent(effect ->
                    FlairEffects.play(effects, victim.getLocation(), effect));
        }
    }

    @Override
    public void forget(UUID player) {
        flair.forget(player);
    }
}
