package de.raindancer.modules.cosmetics.flair;

import de.raindancer.core.ui.effect.Effect;
import de.raindancer.core.ui.effect.Effects;
import de.raindancer.core.ui.effect.ParticleCue;
import de.raindancer.core.ui.effect.ParticleSequence;
import de.raindancer.core.ui.effect.SoundCue;
import de.raindancer.core.ui.effect.SoundSequence;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

/** Plays a {@link FlairEffect} through Core's {@link Effects}: on the thread that owns {@code at}. */
public final class FlairEffects {

    private FlairEffects() {
    }

    /** What Core plays for it: the sound, and the bursts a little above the ground. */
    public static Effect toCore(FlairEffect effect) {
        List<ParticleCue> bursts = effect.bursts().stream().map(burst -> new ParticleCue(burst.particle(),
                burst.count(), burst.spreadX(), burst.spreadY(), burst.spreadZ(), burst.speed(), burst.rgb())).toList();
        return Effect.of(sounds(effect), new ParticleSequence(bursts));
    }

    /** The sound alone, with its further notes after their delays. */
    public static SoundSequence sounds(FlairEffect effect) {
        List<SoundSequence.Step> steps = new java.util.ArrayList<>();
        steps.add(SoundSequence.Step.now(new SoundCue(effect.sound(), effect.volume(), effect.pitch())));
        for (FlairEffect.Note note : effect.then()) {
            steps.add(new SoundSequence.Step(new SoundCue(note.key(), note.volume(), note.pitch()), note.delayMillis()));
        }
        return new SoundSequence(steps);
    }

    /** What somebody hears when they pick {@code option} in a sound category: once, only to them. */
    public static void audition(Effects effects, Player who, FlairOption option) {
        if (option.effect() != null) {
            effects.play(who.getUniqueId(), Effect.of(sounds(option.effect())));
        } else if (option.text() != null) {
            effects.play(who.getUniqueId(), Effect.of(new SoundCue(option.text(), 0.7f, 1f)));
        }
    }

    /** The chime where they joined, heard by those near and by them. */
    public static void chime(Effects effects, Location at, FlairEffect effect) {
        if (at.getWorld() != null) {
            effects.playAt(at.getWorld().getName(), at.getX(), at.getY(), at.getZ(), Effect.of(sounds(effect)));
        }
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
