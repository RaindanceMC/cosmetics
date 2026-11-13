package de.raindancer.modules.cosmetics.flair;

import java.util.List;

/**
 * A look and a sound, played once where something happened. Nothing in it touches anybody: particles, a sound and,
 * at most, a lightning bolt that is only seen and heard.
 *
 * @param sound a sound key such as {@code entity.firework_rocket.blast}
 */
public record FlairEffect(List<Burst> bursts, String sound, float volume, float pitch, boolean lightning,
                          List<Note> then) {

    public FlairEffect(List<Burst> bursts, String sound, float volume, float pitch, boolean lightning) {
        this(bursts, sound, volume, pitch, lightning, List.of());
    }

    /** A further sound of the same effect, {@code delayMillis} after it started. */
    public record Note(String key, float volume, float pitch, long delayMillis) {
    }

    /** Just sound — a chime — at {@code volume}, with more notes after it if given. */
    public static FlairEffect chime(String sound, float volume, float pitch, Note... then) {
        return new FlairEffect(List.of(), sound, volume, pitch, false, List.of(then));
    }

    /**
     * @param spread how far the particles scatter on each axis
     * @param rgb    for dust; null for a particle that takes none
     */
    public record Burst(String particle, int count, double spreadX, double spreadY, double spreadZ, double speed,
                        Integer rgb) {

        public static Burst of(String particle, int count, double spread, double speed) {
            return new Burst(particle, count, spread, spread, spread, speed, null);
        }

        public static Burst rising(String particle, int count, double speed) {
            return new Burst(particle, count, 0.3, 0.8, 0.3, speed, null);
        }

        public static Burst dust(int rgb, int count, double spread) {
            return new Burst("DUST", count, spread, spread, spread, 0, rgb);
        }
    }
}
