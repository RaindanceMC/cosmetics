package de.raindancer.modules.cosmetics.rules;

import de.raindancer.core.platform.rule.Verdict;
import de.raindancer.modules.cosmetics.util.OwnParticles;

import java.util.Locale;
import java.util.Set;

/** Whether somebody may wear a particle, and whether a worn one is drawn right now. */
public final class ParticleRule implements ICosmeticsRule {

    /**
     * @param showable whether the particle exists and needs nothing beyond a colour — asked of Core's
     *                 {@code ParticleShows.canShow} by the caller, so this stays a plain function
     */
    public Verdict judge(String particle, boolean mayUse, Set<String> blocked, boolean showable) {
        return judge(particle, mayUse, blocked, showable, false);
    }

    /** As below, with Prime-only off. */
    public Verdict judge(String particle, boolean mayUse, Set<String> blocked, boolean showable, boolean ownHeld) {
        return judge(particle, mayUse, blocked, showable, ownHeld, false);
    }

    /**
     * @param ownHeld   whether they hold the node of Core's own particle they ask for (see {@link de.raindancer.modules.cosmetics.util.OwnParticles}):
     *                  the supporter's coffee, the creator's Twitch trail, Prime's stardust
     * @param primeOnly whether the coffee trail is Prime's rather than every supporter's — only words the refusal
     */
    public Verdict judge(String particle, boolean mayUse, Set<String> blocked, boolean showable, boolean ownHeld,
                         boolean primeOnly) {
        Verdict plain = judgePlain(particle, mayUse, blocked, showable);
        if (plain.isRefused()) {
            return plain;
        }
        if (de.raindancer.core.ui.effect.ParticleShows.isOwn(particle) && !ownHeld) {
            return Verdict.refused(OwnParticles.refusal(particle, primeOnly));
        }
        return plain;
    }

    private Verdict judgePlain(String particle, boolean mayUse, Set<String> blocked, boolean showable) {
        if (!mayUse) {
            return Verdict.refused("cosmetics.refused.particles");
        }
        String name = particle == null ? "" : particle.trim().toUpperCase(Locale.ROOT);
        if (!showable) {
            return Verdict.refused("cosmetics.refused.particle-unknown", name.toLowerCase(Locale.ROOT));
        }
        if (blocked.contains(name)) {
            return Verdict.refused("cosmetics.refused.particle-blocked", name.toLowerCase(Locale.ROOT));
        }
        return Verdict.allowed();
    }

    /**
     * How many to draw per point: what they chose, or the server's default — never past the server's
     * ceiling, and never none.
     */
    public int count(de.raindancer.modules.cosmetics.model.ParticleDensity chosen, int serverDefault, int ceiling) {
        if (chosen == de.raindancer.modules.cosmetics.model.ParticleDensity.ULTRA) {
            // Past the ceiling on purpose: only somebody given the Ultra node can have chosen it.
            return chosen.count();
        }
        int wanted = chosen == null ? serverDefault : chosen.count();
        return Math.max(1, Math.min(Math.max(1, ceiling), wanted));
    }

    /** The density somebody may actually have: Ultra falls back to the densest below it without the node. */
    public de.raindancer.modules.cosmetics.model.ParticleDensity allowed(
            de.raindancer.modules.cosmetics.model.ParticleDensity chosen, boolean mayUltra) {
        return chosen == de.raindancer.modules.cosmetics.model.ParticleDensity.ULTRA && !mayUltra
                ? de.raindancer.modules.cosmetics.model.ParticleDensity.VERY_DENSE : chosen;
    }

    /** Wings' density: Dense unless they chose one — the server's default is set for a ring or a trail. */
    public de.raindancer.modules.cosmetics.model.ParticleDensity wingDensity(
            de.raindancer.modules.cosmetics.model.ParticleDensity chosen) {
        return chosen == null ? de.raindancer.modules.cosmetics.model.ParticleDensity.DENSE : chosen;
    }

    /**
     * The animation frame to draw at draw number {@code tick}: slow lingers on a frame, fast skips
     * ahead. Null is normal.
     */
    public long frame(long tick, de.raindancer.modules.cosmetics.model.ParticleSpeed speed) {
        return (long) Math.floor(tick * (speed == null ? 1.0 : speed.factor()));
    }

    /**
     * Whether this is drawn at the wing pace ({@link #WING_STEP} ticks): coloured wings, whose points live exactly
     * until the next draw so a beating wing never shows two frames at once. A particle without a colour cannot be given a lifetime, and
     * drawn every tick it would pile up — so it keeps the server's pace.
     */
    public boolean everyTick(de.raindancer.core.ui.effect.ParticleShape shape, boolean takesColour, boolean natural) {
        return shape.isWings() && takesColour && !natural;
    }

    /** The worn particle and the wings, as one player wears them. */
    public record Worn(de.raindancer.modules.cosmetics.model.ParticleChoice particle,
                       de.raindancer.modules.cosmetics.model.ParticleChoice wings) {
    }

    /**
     * Wings chosen as the particle, before wings had a slot of their own, move to that slot — unless wings
     * are already chosen there — and the particle slot is free again either way.
     */
    public Worn split(de.raindancer.modules.cosmetics.model.ParticleChoice particle,
                      de.raindancer.modules.cosmetics.model.ParticleChoice wings) {
        if (particle.isNone() || !particle.shape().isWings()) {
            return new Worn(particle, wings);
        }
        return new Worn(de.raindancer.modules.cosmetics.model.ParticleChoice.NONE, wings.isNone() ? particle : wings);
    }

    /** Coloured wings are redrawn every this many ticks (about ten beats a second) instead of every tick. */
    public static final int WING_STEP = 2;

    /** Whether to draw on this game tick. */
    public boolean drawsNow(long tick, int every, boolean everyTick) {
        return drawsNow(tick, every, everyTick, 0);
    }

    /** @param phase which of the {@link #WING_STEP} offsets this wearer draws on, so they do not all draw on one tick */
    public boolean drawsNow(long tick, int every, boolean everyTick, int phase) {
        return everyTick ? (tick + phase) % WING_STEP == 0 : tick % Math.max(1, every) == 0;
    }

    public int phase(java.util.UUID wearer) {
        return Math.floorMod(wearer.hashCode(), WING_STEP);
    }

    /** How long a paced point lives: until the next draw, so wings neither flicker nor smear. */
    public int wingLifetime() {
        return WING_STEP;
    }

    /** The tick a shape's animation is at: one step per draw, however often that is. */
    public long animationTick(long tick, int every, boolean everyTick) {
        return everyTick ? tick : tick / Math.max(1, every);
    }

    /** Somebody hidden must not give themselves away by a ring of flames. */
    public boolean shows(boolean particlesOn, boolean vanished, boolean spectating, boolean invisible,
                         boolean dead) {
        return particlesOn && !vanished && !spectating && !invisible && !dead;
    }

    @Override
    public String describe() {
        return "who may wear which particle, and when it is drawn";
    }
}
