package de.raindancer.modules.cosmetics.skins;

import java.util.Optional;
import java.util.UUID;

/** The decisions about skins, apart from items and players so they can be tested as plain values. */
public final class SkinRules {

    public static final String OUR_NAMESPACE = "rainsskins";

    private SkinRules() {
    }

    /**
     * How many skins somebody may unlock.
     *
     * @param cents        what they gave — after fees or before, as the owner chose
     * @param centsPerSkin what one skin costs; 0 means a supporter has every skin
     * @param bonus        unlocks given by hand ({@code rainscosmetics.skins.unlocks.<n>})
     */
    public static int unlocksAllowed(long cents, int centsPerSkin, int bonus, boolean supporter) {
        if (centsPerSkin <= 0) {
            return supporter ? Integer.MAX_VALUE : Math.max(0, bonus);
        }
        long total = Math.max(0, cents) / centsPerSkin + Math.max(0, bonus);
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    /** Active minutes per skin from playtime, before the owner's multiplier: fitted to the playtime on Lilly. */
    public static final int PLAYTIME_MINUTES_PER_SKIN = 300;

    /** Skins a supporter has earned by playing — active time only, away minutes do not count. */
    public static int playtimeUnlocks(long activeMinutes, double multiplier, boolean supporter) {
        if (!supporter || multiplier <= 0 || activeMinutes <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, (long) Math.floor(activeMinutes * multiplier / PLAYTIME_MINUTES_PER_SKIN));
    }

    /**
     * How much faster a supporter earns skins by playing, for what they gave: 2.5 times at {@code boostAtEuros} (one
     * skin every 2 hours instead of 5) and as fast as {@code fastestMinutes} per skin at {@code maxAtEuros}, on a
     * power curve through both, times a little for each further support and a running membership — never faster
     * than {@code fastestMinutes}.
     *
     * @param boostAtEuros 0 switches the boost off
     * @param perRepeat    added for each support after the first, up to {@code repeatCap}
     */
    public static double loyalty(long cents, int supports, boolean member, double boostAtEuros, double maxAtEuros,
                                 int fastestMinutes, double perRepeat, double repeatCap) {
        double fastest = Math.max(1.0, PLAYTIME_MINUTES_PER_SKIN / (double) Math.max(1, fastestMinutes));
        if (boostAtEuros <= 0) {
            return 1.0;
        }
        double given = 1.0;
        if (cents > 0) {
            double top = Math.max(boostAtEuros * 1.01, maxAtEuros);
            double power = Math.log(Math.max(1.0001, (fastest - 1) / 1.5)) / Math.log(top / boostAtEuros);
            given = 1 + 1.5 * Math.pow(cents / (boostAtEuros * 100.0), power);
        }
        double repeats = 1 + Math.min(Math.max(0, repeatCap), Math.max(0, perRepeat) * Math.max(0, supports - 1));
        return Math.min(fastest, given * repeats * (member ? 1.25 : 1));
    }

    /** Up to {@code count} skins from {@code pool} not yet in {@code owned}, each equally likely, none twice. */
    public static java.util.List<String> pickRandom(java.util.List<String> pool, java.util.Set<String> owned, int count,
                                                    java.util.Random random) {
        java.util.List<String> left = new java.util.ArrayList<>(pool.stream().filter(id -> !owned.contains(id)).distinct().toList());
        java.util.Collections.shuffle(left, random);
        return java.util.List.copyOf(left.subList(0, Math.max(0, Math.min(count, left.size()))));
    }

    public enum Apply { WEAR, LOCKED, SOMEBODY_ELSES }

    /** @param owns whether {@code who} has the skin — unlocked, or given by permission */
    public static Apply apply(Optional<SkinTag> onItem, UUID who, boolean owns) {
        if (onItem.isPresent() && !onItem.get().owner().equals(who)) {
            return Apply.SOMEBODY_ELSES;
        }
        return owns ? Apply.WEAR : Apply.LOCKED;
    }

    /** Whether an item's owner still has its skin: UNKNOWN when only a permission could say and they are away. */
    public enum Owns { YES, NO, UNKNOWN }

    /** What to do with an item's model: put the skin's on, take ours off, or leave it as it is. */
    public record Look(Kind kind, String model) {

        public enum Kind { SHOW, HIDE, LEAVE }

        public static final Look HIDE = new Look(Kind.HIDE, null);
        public static final Look LEAVE = new Look(Kind.LEAVE, null);

        public static Look show(String model) {
            return new Look(Kind.SHOW, model);
        }
    }

    /**
     * @param hideWithOthers hidden while a player who is not its owner carries it
     * @param showOutside    shown in containers, frames and on the ground
     */
    public record Visibility(boolean hideWithOthers, boolean showOutside) {

        /**
         * @param holder  who carries it, null when it lies in a container or on the ground
         * @param skinOn  whether the skin is switched on here
         * @param look    the skin's model for this item, null when it has none for it
         * @param current the model on the item now, null when none is set
         */
        public Look decide(SkinTag tag, UUID holder, Owns owns, boolean skinOn, String look, String current) {
            if (current != null && !current.startsWith("minecraft:") && !current.startsWith(OUR_NAMESPACE + ":")) {
                return Look.LEAVE;
            }
            if (owns == Owns.UNKNOWN) {
                return Look.LEAVE;
            }
            if (owns == Owns.NO || !skinOn || look == null) {
                return Look.HIDE;
            }
            if (holder == null) {
                return showOutside ? Look.show(look) : Look.HIDE;
            }
            if (!holder.equals(tag.owner()) && hideWithOthers) {
                return Look.HIDE;
            }
            return Look.show(look);
        }
    }
}
