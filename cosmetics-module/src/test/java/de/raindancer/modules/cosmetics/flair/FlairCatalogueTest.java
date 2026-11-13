package de.raindancer.modules.cosmetics.flair;

import de.raindancer.modules.cosmetics.util.PermissionNodes;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FlairCatalogueTest {

    @Test
    @DisplayName("every option has an id of its own within its category and sits behind the Prime or the creator node")
    void idsAndNodes() {
        Set<String> seen = new HashSet<>();
        for (FlairOption option : FlairCatalogue.all()) {
            assertThat(seen.add(option.category() + ":" + option.id())).as(option.id()).isTrue();
            assertThat(option.id()).matches("[a-z][a-z0-9-]*");
            assertThat(option.title()).isNotBlank();
            assertThat(option.node()).isIn(PermissionNodes.FLAIR, PermissionNodes.CREATOR);
        }
    }

    @Test
    @DisplayName("Prime has a real choice in every category")
    void enoughForPrime() {
        assertThat(primeIn(FlairCategory.TAG)).hasSizeGreaterThanOrEqualTo(5);
        assertThat(primeIn(FlairCategory.COLOUR)).hasSizeGreaterThanOrEqualTo(6);
        assertThat(primeIn(FlairCategory.JOIN)).hasSizeGreaterThanOrEqualTo(4);
        assertThat(primeIn(FlairCategory.KILL)).hasSizeGreaterThanOrEqualTo(4);
        assertThat(primeIn(FlairCategory.DEATH)).hasSizeGreaterThanOrEqualTo(3);
    }

    private static List<FlairOption> primeIn(FlairCategory category) {
        return FlairCatalogue.of(category).stream().filter(o -> o.node().equals(PermissionNodes.FLAIR)).toList();
    }

    @Test
    @DisplayName("creators have their own Twitch extras: a tag, its purple, a join line and a glitch kill effect")
    void creatorExtras() {
        assertThat(FlairCatalogue.find(FlairCategory.TAG, "play").orElseThrow().node()).isEqualTo(PermissionNodes.CREATOR);
        FlairOption purple = FlairCatalogue.find(FlairCategory.COLOUR, "twitch").orElseThrow();
        assertThat(purple.node()).isEqualTo(PermissionNodes.CREATOR);
        assertThat(purple.rgb()).isEqualTo(0x9146FF);
        assertThat(FlairCatalogue.find(FlairCategory.JOIN, "twitch").orElseThrow().node())
                .isEqualTo(PermissionNodes.CREATOR);
        assertThat(FlairCatalogue.find(FlairCategory.KILL, "glitch").orElseThrow().node())
                .isEqualTo(PermissionNodes.CREATOR);
    }

    @Test
    @DisplayName("a tag is one symbol and a colour is a colour")
    void tagsAndColours() {
        for (FlairOption tag : FlairCatalogue.of(FlairCategory.TAG)) {
            assertThat(tag.text().codePointCount(0, tag.text().length())).as(tag.id()).isEqualTo(1);
        }
        for (FlairOption colour : FlairCatalogue.of(FlairCategory.COLOUR)) {
            assertThat(colour.rgb()).as(colour.id()).isBetween(0, 0xFFFFFF);
        }
    }

    @Test
    @DisplayName("a join and a leave line both name the player, and read fine once the name is put in")
    void lines() {
        for (FlairOption option : FlairCatalogue.of(FlairCategory.JOIN)) {
            for (String line : new String[]{option.text(), option.text2()}) {
                assertThat(line).as(option.id()).contains("<player>");
                String plain = PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage()
                        .deserialize(line, Placeholder.unparsed("player", "Alex")));
                assertThat(plain).contains("Alex").doesNotContain("<").doesNotContain(">");
            }
        }
    }

    @Test
    @DisplayName("an effect is only a look and a sound — something to see, small enough not to lag a crowd")
    void effects() {
        for (FlairOption option : FlairCatalogue.all()) {
            if (option.category() != FlairCategory.KILL && option.category() != FlairCategory.DEATH) {
                assertThat(option.effect()).as(option.id()).isNull();
                continue;
            }
            FlairEffect effect = option.effect();
            assertThat(effect).as(option.id()).isNotNull();
            assertThat(effect.bursts().isEmpty() && !effect.lightning()).as(option.id() + " shows nothing").isFalse();
            assertThat(effect.bursts().stream().mapToInt(FlairEffect.Burst::count).sum()).as(option.id())
                    .isLessThanOrEqualTo(120);
            assertThat(effect.sound()).matches("[a-z0-9_.]+");
            effect.bursts().forEach(burst -> assertThat(org.bukkit.Particle.valueOf(burst.particle())).isNotNull());
        }
    }
}
