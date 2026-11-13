package de.raindancer.modules.cosmetics.util;

import de.raindancer.core.ui.effect.ParticleShows;
import de.raindancer.modules.cosmetics.util.PermissionNodes;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Core's own particles (those {@link ParticleShows#isOwn}) and what each needs: the coffee beans a supporter's node —
 * Prime's with Prime-only on —, the Twitch trail the creator node, the stardust Prime's flair. Wearing one asks its
 * node and nothing else, so a creator who is not Prime still wears the Twitch trail.
 */
public final class OwnParticles {

    private static final Map<String, String> NODES = Map.of(
            ParticleShows.COFFEE, PermissionNodes.PARTICLE_COFFEE,
            ParticleShows.TWITCH, PermissionNodes.CREATOR,
            ParticleShows.STARDUST, PermissionNodes.FLAIR);

    /** Every particle with a gate here. */
    public static final Set<String> ALL = NODES.keySet();

    private OwnParticles() {
    }

    public static Optional<String> nodeFor(String particle) {
        return particle == null ? Optional.empty()
                : Optional.ofNullable(NODES.get(particle.trim().toUpperCase(Locale.ROOT)));
    }

    /** The message key for somebody refused {@code particle}. */
    public static String refusal(String particle, boolean primeOnly) {
        return switch (particle.trim().toUpperCase(Locale.ROOT)) {
            case ParticleShows.TWITCH -> "cosmetics.refused.particle-creators";
            case ParticleShows.STARDUST -> "cosmetics.refused.particle-prime";
            default -> primeOnly ? "cosmetics.refused.particle-prime" : "cosmetics.refused.particle-supporters";
        };
    }
}
