package de.raindancer.modules.cosmetics.flair;

import de.raindancer.core.ui.effect.Effect;
import de.raindancer.core.ui.effect.Effects;
import de.raindancer.core.ui.effect.ParticleCue;
import de.raindancer.core.ui.effect.ParticleSequence;
import de.raindancer.core.ui.effect.SoundCue;
import de.raindancer.core.ui.effect.SoundSequence;
import org.bukkit.Location;

import java.util.List;

/** Plays a {@link FlairEffect} through Core's {@link Effects}: on the thread that owns {@code at}. */
public final class FlairEffects {

    private FlairEffects() {
    }

    /** What Core plays for it: the sound, and the bursts a little above the ground. */
    public static Effect toCore(FlairEffect effect) {
        List<ParticleCue> bursts = effect.bursts().stream().map(burst -> new ParticleCue(burst.particle(),
                burst.count(), burst.spreadX(), burst.spreadY(), burst.spreadZ(), burst.speed(), burst.rgb())).toList();
        return Effect.of(SoundSequence.of(new SoundCue(effect.sound(), effect.volume(), effect.pitch())),
                new ParticleSequence(bursts));
    }

    public static void play(Effects effects, Location at, FlairEffect effect) {
        if (at.getWorld() == null) {
            return;
        }
        effects.playAt(at.getWorld().getName(), at.getX(), at.getY() + 1, at.getZ(), toCore(effect));
        if (effect.lightning()) {
            at.getWorld().strikeLightningEffect(at);
        }
    }
}
