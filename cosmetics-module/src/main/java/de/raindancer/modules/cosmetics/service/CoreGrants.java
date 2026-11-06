package de.raindancer.modules.cosmetics.service;

import de.raindancer.core.ui.choose.NameStyleGrants;
import de.raindancer.core.ui.choose.ParticleGrants;
import de.raindancer.core.ui.choose.StyleGrants;
import de.raindancer.modules.cosmetics.CosmeticsSettings;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Function;

/**
 * Offers what a player may paint their name with and which particles they may wear to everything else they own —
 * a pet's name, a pet's particles — through Core's {@link NameStyleGrants} and {@link ParticleGrants}.
 */
public final class CoreGrants implements ICosmeticsService {

    private final Function<Player, StyleGrants> styles;
    private final Function<Player, List<String>> particles;
    private Object styleHandle;
    private Object particleHandle;

    public CoreGrants(Function<Player, StyleGrants> styles, Function<Player, List<String>> particles) {
        this.styles = styles;
        this.particles = particles;
    }

    @Override
    public void settings(CosmeticsSettings fresh) {
        // asks the services every time, which hold their own snapshots
    }

    public synchronized void install() {
        uninstall();
        styleHandle = NameStyleGrants.provide(styles);
        particleHandle = ParticleGrants.provide(particles);
    }

    public synchronized void uninstall() {
        if (styleHandle != null) {
            NameStyleGrants.retract(styleHandle);
            styleHandle = null;
        }
        if (particleHandle != null) {
            ParticleGrants.retract(particleHandle);
            particleHandle = null;
        }
    }
}
