package de.raindancer.modules.cosmetics;

import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.cosmetics.service.ParticleService;
import org.bukkit.entity.Player;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** What /preferences offers for cosmetics: the particle switch and a door to /cosmetics. */
public final class CosmeticsPreferences {

    private static final String GROUP = "Cosmetics";

    private CosmeticsPreferences() {
    }

    public static Preference seeParticles(Supplier<ParticleService> particles) {
        return Preference.toggle("cosmetics:see-particles", GROUP, "See other players' particles",
                "Only changes what you see. Your own particles are always shown to you.",
                player -> particles.get().sees(player),
                (player, on) -> {
                    ParticleService live = particles.get();
                    if (live.sees(player) != on) {
                        live.toggleSeeing(player);
                    }
                });
    }

    public static Preference cosmetics(Consumer<Player> openDialog) {
        return Preference.page("cosmetics:cosmetics", GROUP, "Your cosmetics",
                "Name style, particles, wings, tags and more.",
                (player, back) -> openDialog.accept(player));
    }
}
