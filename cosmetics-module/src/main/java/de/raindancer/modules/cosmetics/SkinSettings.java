package de.raindancer.modules.cosmetics;

import de.raindancer.core.data.settings.Describe;
import de.raindancer.core.data.settings.In;
import de.raindancer.core.data.settings.Key;
import de.raindancer.core.data.settings.Range;
import de.raindancer.core.data.settings.Settings;
import de.raindancer.core.data.settings.Title;
import de.raindancer.core.data.settings.Topic;
import org.bukkit.Material;

import java.util.List;
import java.util.Set;

/**
 * Weapon skins: a thank-you for supporting the server. A record of its own — the cosmetics settings are long, and
 * this is a separate decision.
 */
@Settings(id = "cosmetics-skins", topics = {
        @Topic(path = "cosmetics/skins", title = "Weapon skins", icon = Material.DIAMOND_SWORD),
})
public record SkinSettings(

        @In("cosmetics/skins") @Title("Weapon skins")
        @Describe("Whether players with the skins permission (every supporter has it) may put skins on their "
                + "weapons in /cosmetics skins. Off, every skin comes off until it is switched on again.")
        @Key("skins.enabled")
        boolean enabled,

        @In("cosmetics/skins") @Title("One skin unlocked per (cents)") @Range(min = 0, max = 100000)
        @Describe("How much somebody has to have given, in cents, for each skin they unlock — 50 is one skin per "
                + "50 ct. Zero: every supporter has every skin.")
        @Key("skins.cents-per-skin")
        int centsPerSkin,

        @In("cosmetics/skins") @Title("Count what reached you, after fees")
        @Describe("On: skins are counted from what reached you after Buy Me a Coffee's and the card's fees (the fee "
                + "settings are under Chat ➤ Support link). Off: from what they paid.")
        @Key("skins.count-after-fees")
        boolean countAfterFees,

        @In("cosmetics/skins") @Title("Skins from playtime (multiplier)")
        @Describe("Supporters also earn skins by playing: one per 5 hours of active play (away time does not count), "
                + "times this — 2 is twice as fast, 0.5 half, 0 off. Others earn none.")
        @Key("skins.playtime-multiplier")
        String playtimeMultiplier,

        @In("cosmetics/skins") @Title("One skin every 2 hours of play at (euros given)")
        @Describe("The more a supporter gave, the faster they earn skins by playing: 5 hours per skin to start, "
                + "2 hours at this amount, then faster up to the fastest rate below. 0: no boost.")
        @Key("skins.loyalty-at-euros")
        int loyaltyAtEuros,

        @In("cosmetics/skins") @Title("Fastest rate reached at (euros given)")
        @Describe("From this amount on, a supporter earns a skin every \"fastest\" minutes of active play.")
        @Key("skins.loyalty-max-at-euros")
        int loyaltyMaxAtEuros,

        @In("cosmetics/skins") @Title("Fastest: one skin per (active minutes)") @Range(min = 5, max = 300)
        @Describe("Nobody earns skins by playing faster than this, however much they gave.")
        @Key("skins.fastest-minutes-per-skin")
        int fastestMinutes,

        @In("cosmetics/skins") @Title("Each further support adds (percent)") @Range(min = 0, max = 100)
        @Describe("Supporting again speeds play up by this much each time, up to 25 %. A running membership adds 25 %.")
        @Key("skins.loyalty-per-repeat")
        int loyaltyPerRepeat,

        @In("cosmetics/skins") @Title("Hide a skin while somebody else carries it")
        @Describe("A skinned weapon looks normal in another player's inventory and shows its skin again once it is "
                + "back with its owner.")
        @Key("skins.hide-with-others")
        boolean hideWithOthers,

        @In("cosmetics/skins") @Title("Show skins in chests and on the ground")
        @Describe("Off: a skin shows only while its owner carries the weapon.")
        @Key("skins.show-outside-inventories")
        boolean showOutside,

        @In("cosmetics/skins") @Title("Skins switched off")
        @Describe("Skins nobody can pick or wear here, by id — easiest from /cosmetics skins admin.")
        @Key("skins.off")
        List<String> off) {

    public static final SkinSettings DEFAULTS = new SkinSettings(true, 50, true, "1", 40, 160, 30, 5, true, true, List.of());

    public double multiplier() {
        try {
            return Math.max(0, Double.parseDouble(playtimeMultiplier.strip().replace(',', '.').replace("x", "")));
        } catch (RuntimeException notANumber) {
            return 0;
        }
    }

    public Set<String> switchedOff() {
        return off == null ? Set.of() : Set.copyOf(off);
    }
}
