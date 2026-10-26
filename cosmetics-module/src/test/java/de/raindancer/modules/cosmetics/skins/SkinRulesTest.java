package de.raindancer.modules.cosmetics.skins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SkinRulesTest {

    private final UUID owner = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final SkinTag tag = new SkinTag("katana", owner);
    private static final String MODEL = "rainsskins:katana/diamond_sword";

    @Test
    @DisplayName("unlocks: one per 50 ct given, extras by hand, every skin when a skin is set to cost nothing")
    void unlocks() {
        assertThat(SkinRules.unlocksAllowed(430, 50, 0, true)).isEqualTo(8);
        assertThat(SkinRules.unlocksAllowed(49, 50, 0, true)).isZero();
        assertThat(SkinRules.unlocksAllowed(1000, 100, 0, true)).isEqualTo(10);
        assertThat(SkinRules.unlocksAllowed(0, 50, 2, false)).as("given by hand").isEqualTo(2);
        assertThat(SkinRules.unlocksAllowed(1, 0, 0, true)).as("free: every skin").isEqualTo(Integer.MAX_VALUE);
        assertThat(SkinRules.unlocksAllowed(0, 0, 0, false)).as("…but only for a supporter").isZero();
    }

    @Test
    @DisplayName("playtime: one skin per 5 active hours for a supporter, times the multiplier; none for others")
    void playtime() {
        assertThat(SkinRules.playtimeUnlocks(600, 1.0, true)).isEqualTo(2);
        assertThat(SkinRules.playtimeUnlocks(299, 1.0, true)).isZero();
        assertThat(SkinRules.playtimeUnlocks(600, 2.5, true)).isEqualTo(5);
        assertThat(SkinRules.playtimeUnlocks(600, 0, true)).as("multiplier 0: off").isZero();
        assertThat(SkinRules.playtimeUnlocks(6000, 1.0, false)).as("not a supporter").isZero();
    }

    private static double minutes(long cents, int supports, boolean member) {
        return SkinRules.PLAYTIME_MINUTES_PER_SKIN / SkinRules.loyalty(cents, supports, member, 40, 160, 30, 0.05, 0.25);
    }

    @Test
    @DisplayName("loyalty: 5 h per skin for any support, 2 h at €40, 30 min at €160, and never faster")
    void loyalty() {
        assertThat(minutes(0, 1, false)).isCloseTo(300.0, org.assertj.core.data.Offset.offset(0.5));
        assertThat(minutes(4000, 1, false)).isCloseTo(120.0, org.assertj.core.data.Offset.offset(0.5));
        assertThat(minutes(16000, 1, false)).isCloseTo(30.0, org.assertj.core.data.Offset.offset(0.5));
        assertThat(minutes(1000, 1, false)).isBetween(230.0, 250.0);
        assertThat(minutes(8000, 1, false)).isBetween(60.0, 70.0);
        assertThat(minutes(100000, 1, false)).as("capped").isCloseTo(30.0, org.assertj.core.data.Offset.offset(0.5));
    }

    @Test
    @DisplayName("loyalty: every further support adds 5 % up to 25 %, a running membership 25 % — still never past 30 min")
    void repeatsAndMembers() {
        assertThat(minutes(4000, 3, false)).isCloseTo(120.0 / 1.10, org.assertj.core.data.Offset.offset(0.5));
        assertThat(minutes(4000, 50, false)).isCloseTo(120.0 / 1.25, org.assertj.core.data.Offset.offset(0.5));
        assertThat(minutes(4000, 1, true)).isCloseTo(120.0 / 1.25, org.assertj.core.data.Offset.offset(0.5));
        assertThat(minutes(16000, 9, true)).isCloseTo(30.0, org.assertj.core.data.Offset.offset(0.5));
        assertThat(SkinRules.loyalty(4000, 1, false, 0, 160, 30, 0.05, 0.25)).as("boost switched off").isEqualTo(1.0);
    }

    @Test
    @DisplayName("random unlocks: only skins not yet theirs, no skin twice, never more than are left")
    void random() {
        var pool = java.util.List.of("a", "b", "c", "d", "e");
        var random = new java.util.Random(4);

        var three = SkinRules.pickRandom(pool, java.util.Set.of("b"), 3, random);
        assertThat(three).hasSize(3).doesNotHaveDuplicates().doesNotContain("b").isSubsetOf(pool);
        assertThat(SkinRules.pickRandom(pool, java.util.Set.of("a", "b", "c"), 10, random))
                .containsExactlyInAnyOrder("d", "e");
        assertThat(SkinRules.pickRandom(pool, java.util.Set.copyOf(pool), 1, random)).isEmpty();
        assertThat(SkinRules.pickRandom(pool, java.util.Set.of(), 0, random)).isEmpty();
    }

    @Test
    @DisplayName("every skin is as likely as any other — no rarities")
    void fair() {
        var pool = java.util.List.of("a", "b", "c", "d");
        var counts = new java.util.HashMap<String, Integer>();
        var random = new java.util.Random(7);
        for (int i = 0; i < 40_000; i++) {
            counts.merge(SkinRules.pickRandom(pool, java.util.Set.of(), 1, random).getFirst(), 1, Integer::sum);
        }
        counts.values().forEach(n -> assertThat(n).isBetween(9_400, 10_600));
    }

    @Test
    @DisplayName("putting a skin on: free on one's own item once unlocked; somebody else's skinned item stays theirs")
    void applying() {
        assertThat(SkinRules.apply(Optional.empty(), owner, true)).isEqualTo(SkinRules.Apply.WEAR);
        assertThat(SkinRules.apply(Optional.of(tag), owner, true)).isEqualTo(SkinRules.Apply.WEAR);
        assertThat(SkinRules.apply(Optional.empty(), owner, false)).isEqualTo(SkinRules.Apply.LOCKED);
        assertThat(SkinRules.apply(Optional.of(tag), other, true)).isEqualTo(SkinRules.Apply.SOMEBODY_ELSES);
    }

    @Test
    @DisplayName("seen: shown with its owner, hidden while somebody else carries it, back when it comes home")
    void visibility() {
        var rules = new SkinRules.Visibility(true, true);

        assertThat(rules.decide(tag, owner, SkinRules.Owns.YES, true, MODEL, null)).isEqualTo(SkinRules.Look.show(MODEL));
        assertThat(rules.decide(tag, other, SkinRules.Owns.YES, true, MODEL, null)).isEqualTo(SkinRules.Look.HIDE);
        assertThat(rules.decide(tag, null, SkinRules.Owns.YES, true, MODEL, null)).as("in a chest or on the ground")
                .isEqualTo(SkinRules.Look.show(MODEL));
    }

    @Test
    @DisplayName("the settings: shown with everybody, or hidden outside inventories")
    void settings() {
        assertThat(new SkinRules.Visibility(false, true).decide(tag, other, SkinRules.Owns.YES, true, MODEL, null))
                .isEqualTo(SkinRules.Look.show(MODEL));
        assertThat(new SkinRules.Visibility(true, false).decide(tag, null, SkinRules.Owns.YES, true, MODEL, null))
                .isEqualTo(SkinRules.Look.HIDE);
    }

    @Test
    @DisplayName("no longer theirs, switched off, or none for this weapon: hidden; not knowable right now: left as it is")
    void notWearable() {
        var rules = new SkinRules.Visibility(true, true);

        assertThat(rules.decide(tag, owner, SkinRules.Owns.NO, true, MODEL, null)).isEqualTo(SkinRules.Look.HIDE);
        assertThat(rules.decide(tag, owner, SkinRules.Owns.YES, false, MODEL, null)).isEqualTo(SkinRules.Look.HIDE);
        assertThat(rules.decide(tag, owner, SkinRules.Owns.YES, true, null, null)).isEqualTo(SkinRules.Look.HIDE);
        assertThat(rules.decide(tag, null, SkinRules.Owns.UNKNOWN, true, MODEL, null)).isEqualTo(SkinRules.Look.LEAVE);
    }

    @Test
    @DisplayName("another plugin's look on the item — an enchantment's — is left alone")
    void othersLook() {
        var rules = new SkinRules.Visibility(true, true);

        assertThat(rules.decide(tag, owner, SkinRules.Owns.YES, true, MODEL, "rainsenchants:mjolnir"))
                .isEqualTo(SkinRules.Look.LEAVE);
        assertThat(rules.decide(tag, owner, SkinRules.Owns.YES, true, MODEL, "minecraft:diamond_sword"))
                .as("the vanilla model is no other plugin's").isEqualTo(SkinRules.Look.show(MODEL));
        assertThat(rules.decide(tag, owner, SkinRules.Owns.YES, true, MODEL, "rainsskins:scythe/diamond_sword"))
                .as("an older skin of ours is replaced").isEqualTo(SkinRules.Look.show(MODEL));
    }
}
