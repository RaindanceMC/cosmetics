package de.raindancer.modules.cosmetics.flair;

import de.raindancer.modules.cosmetics.flair.FlairEffect.Burst;
import de.raindancer.modules.cosmetics.util.PermissionNodes;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Everything on the flair page. Prime's options are behind {@link PermissionNodes#FLAIR}, the creators' Twitch extras
 * behind {@link PermissionNodes#CREATOR}. Looks only: there is nothing here that does damage, heals, moves anybody or
 * gives anything.
 */
public final class FlairCatalogue {

    private static final String PRIME = PermissionNodes.FLAIR;
    private static final String CREATOR = PermissionNodes.CREATOR;

    private static final List<FlairOption> ALL = List.of(
            FlairOption.tag("star", "Star", PRIME, "✦"),
            FlairOption.tag("diamond", "Diamond", PRIME, "◆"),
            FlairOption.tag("crown", "Crown", PRIME, "♛"),
            FlairOption.tag("moon", "Moon", PRIME, "☾"),
            FlairOption.tag("flower", "Flower", PRIME, "✿"),
            FlairOption.tag("bolt", "Bolt", PRIME, "⚡"),
            FlairOption.tag("play", "Play", CREATOR, "▶"),

            FlairOption.colour("gold", "Prime gold", PRIME, 0xFFC93C),
            FlairOption.colour("aqua", "Aqua", PRIME, 0x4DD0E1),
            FlairOption.colour("rose", "Rose", PRIME, 0xFF6F91),
            FlairOption.colour("violet", "Violet", PRIME, 0xB388FF),
            FlairOption.colour("mint", "Mint", PRIME, 0x69F0AE),
            FlairOption.colour("ember", "Ember", PRIME, 0xFF7043),
            FlairOption.colour("snow", "Snow", PRIME, 0xFFFFFF),
            FlairOption.colour("twitch", "Twitch purple", CREATOR, 0x9146FF),

            FlairOption.lines("sparkle", "Sparkle", PRIME,
                    "<#FFD966>✦ <player> <#FFF4C2>sparkles into the world",
                    "<#FFD966>✦ <player> <#FFF4C2>fades into stardust"),
            FlairOption.lines("royal", "Royal", PRIME,
                    "<#FFC93C>♛ <player> <#FFF4C2>arrives — make way",
                    "<#FFC93C>♛ <player> <#FFF4C2>leaves the court"),
            FlairOption.lines("moon", "Moonrise", PRIME,
                    "<#B388FF>☾ <player> <#D7C4FF>rises",
                    "<#B388FF>☾ <player> <#D7C4FF>sets"),
            FlairOption.lines("wanderer", "Wanderer", PRIME,
                    "<#69F0AE>✿ <player> <#C8F7DC>wanders in",
                    "<#69F0AE>✿ <player> <#C8F7DC>wanders off"),
            FlairOption.lines("twitch", "Going live", CREATOR,
                    "<#9146FF>▶ <player> <#BF94FF>logs in — chat, say hi",
                    "<#9146FF>▶ <player> <#BF94FF>signs off — thanks for watching"),

            FlairOption.effect(FlairCategory.KILL, "lightning", "Thunderstrike", PRIME,
                    new FlairEffect(List.of(Burst.of("ELECTRIC_SPARK", 24, 0.6, 0.2)),
                            "entity.lightning_bolt.thunder", 0.6f, 1.2f, true)),
            FlairOption.effect(FlairCategory.KILL, "fireworks", "Fireworks", PRIME,
                    new FlairEffect(List.of(Burst.of("FIREWORK", 40, 0.6, 0.12), Burst.of("END_ROD", 20, 0.8, 0.05)),
                            "entity.firework_rocket.blast", 1f, 1f, false)),
            FlairOption.effect(FlairCategory.KILL, "hearts", "Hearts", PRIME,
                    new FlairEffect(List.of(Burst.rising("HEART", 14, 0.02)),
                            "entity.player.levelup", 0.5f, 1.6f, false)),
            FlairOption.effect(FlairCategory.KILL, "souls", "Soul harvest", PRIME,
                    new FlairEffect(List.of(Burst.rising("SOUL", 20, 0.04), Burst.rising("SOUL_FIRE_FLAME", 10, 0.02)),
                            "particle.soul_escape", 1f, 1f, false)),
            FlairOption.effect(FlairCategory.KILL, "confetti", "Confetti", PRIME,
                    new FlairEffect(List.of(Burst.dust(0xFF6F91, 12, 0.7), Burst.dust(0xFFD966, 12, 0.7),
                            Burst.dust(0x4DD0E1, 12, 0.7), Burst.dust(0x69F0AE, 12, 0.7)),
                            "entity.firework_rocket.twinkle", 1f, 1.1f, false)),
            FlairOption.effect(FlairCategory.KILL, "glitch", "Glitch", CREATOR,
                    new FlairEffect(List.of(Burst.dust(0x9146FF, 22, 0.7), Burst.dust(0x00E5FF, 8, 0.9),
                            Burst.dust(0xFF3C78, 8, 0.9)),
                            "block.amethyst_block.resonate", 1f, 1.4f, false)),

            FlairOption.effect(FlairCategory.DEATH, "ghost", "Ghost", PRIME,
                    new FlairEffect(List.of(Burst.rising("SOUL", 16, 0.05), Burst.rising("CLOUD", 8, 0.02)),
                            "entity.vex.ambient", 0.7f, 1.4f, false)),
            FlairOption.effect(FlairCategory.DEATH, "poof", "Poof", PRIME,
                    new FlairEffect(List.of(Burst.of("POOF", 24, 0.4, 0.05), Burst.of("CLOUD", 10, 0.5, 0.02)),
                            "entity.chicken.egg", 0.8f, 0.8f, false)),
            FlairOption.effect(FlairCategory.DEATH, "stardust", "Stardust", PRIME,
                    new FlairEffect(List.of(Burst.rising("END_ROD", 24, 0.05), Burst.dust(0xFFD966, 20, 0.6)),
                            "block.amethyst_block.chime", 1f, 1.3f, false)));

    private FlairCatalogue() {
    }

    public static List<FlairOption> all() {
        return ALL;
    }

    public static List<FlairOption> of(FlairCategory category) {
        return ALL.stream().filter(option -> option.category() == category).toList();
    }

    public static Optional<FlairOption> find(FlairCategory category, String id) {
        if (id == null) {
            return Optional.empty();
        }
        String wanted = id.trim().toLowerCase(Locale.ROOT);
        return ALL.stream().filter(option -> option.category() == category && option.id().equals(wanted)).findFirst();
    }
}
