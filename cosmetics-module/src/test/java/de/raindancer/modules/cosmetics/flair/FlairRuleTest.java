package de.raindancer.modules.cosmetics.flair;

import de.raindancer.core.ui.identity.JoinLines;
import de.raindancer.modules.cosmetics.util.PermissionNodes;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class FlairRuleTest {

    private static final Predicate<String> PRIME = Set.of(PermissionNodes.FLAIR)::contains;
    private static final Predicate<String> CREATOR = Set.of(PermissionNodes.CREATOR)::contains;
    private static final Predicate<String> NOBODY = node -> false;

    private final FlairRule rule = new FlairRule();

    private static FlairChoice everything() {
        return FlairChoice.EMPTY.with(FlairCategory.TAG, "star").with(FlairCategory.COLOUR, "gold")
                .with(FlairCategory.JOIN, "sparkle").with(FlairCategory.KILL, "lightning")
                .with(FlairCategory.DEATH, "ghost");
    }

    @Test
    @DisplayName("Prime keeps what it picked; without Prime nothing of it is worn")
    void primeOnly() {
        assertThat(rule.effective(everything(), PRIME)).isEqualTo(everything());
        assertThat(rule.effective(everything(), NOBODY).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("a creator without Prime keeps only the creator options; Prime without creator loses those")
    void creatorOptions() {
        FlairChoice mixed = everything().with(FlairCategory.KILL, "glitch").with(FlairCategory.COLOUR, "twitch");

        FlairChoice asCreator = rule.effective(mixed, CREATOR);
        assertThat(asCreator.get(FlairCategory.KILL)).contains("glitch");
        assertThat(asCreator.get(FlairCategory.COLOUR)).contains("twitch");
        assertThat(asCreator.get(FlairCategory.JOIN)).isEmpty();

        FlairChoice asPrime = rule.effective(mixed, PRIME);
        assertThat(asPrime.get(FlairCategory.KILL)).isEmpty();
        assertThat(asPrime.get(FlairCategory.COLOUR)).isEmpty();
        assertThat(asPrime.get(FlairCategory.TAG)).contains("star");
    }

    @Test
    @DisplayName("an option that no longer exists is dropped rather than crashing anything")
    void unknownOption() {
        assertThat(rule.effective(FlairChoice.parse("tag=gone;kill=lightning"), PRIME).format())
                .isEqualTo("kill=lightning");
    }

    @Test
    @DisplayName("the tag is the chosen symbol in the chosen colour; with no colour it is Prime gold; with no symbol there is none")
    void tag() {
        TextComponent star = (TextComponent) rule.tag(everything()).orElseThrow();
        assertThat(star.content()).isEqualTo("✦");
        assertThat(star.color().value()).isEqualTo(FlairCatalogue.find(FlairCategory.COLOUR, "gold").orElseThrow().rgb());

        TextComponent plain = (TextComponent) rule.tag(FlairChoice.EMPTY.with(FlairCategory.TAG, "crown")).orElseThrow();
        assertThat(plain.color().value()).isEqualTo(FlairRule.PRIME_GOLD);

        assertThat(rule.tag(FlairChoice.EMPTY.with(FlairCategory.COLOUR, "gold"))).isEmpty();
        assertThat(PlainTextComponentSerializer.plainText().serialize(star)).isEqualTo("✦");
    }

    @Test
    @DisplayName("the join and leave lines are the chosen style's, and none without a choice")
    void lines() {
        assertThat(rule.line(everything(), JoinLines.Kind.JOIN)).hasValueSatisfying(line -> assertThat(line)
                .isEqualTo(FlairCatalogue.find(FlairCategory.JOIN, "sparkle").orElseThrow().text()));
        assertThat(rule.line(everything(), JoinLines.Kind.QUIT)).hasValueSatisfying(line -> assertThat(line)
                .isEqualTo(FlairCatalogue.find(FlairCategory.JOIN, "sparkle").orElseThrow().text2()));
        assertThat(rule.line(FlairChoice.EMPTY, JoinLines.Kind.JOIN)).isEmpty();
    }

    @Test
    @DisplayName("may asks the node of the option, never more")
    void may() {
        FlairOption lightning = FlairCatalogue.find(FlairCategory.KILL, "lightning").orElseThrow();
        FlairOption glitch = FlairCatalogue.find(FlairCategory.KILL, "glitch").orElseThrow();

        assertThat(rule.may(lightning, PRIME)).isTrue();
        assertThat(rule.may(lightning, CREATOR)).isFalse();
        assertThat(rule.may(glitch, CREATOR)).isTrue();
        assertThat(rule.may(glitch, PRIME)).isFalse();
    }
}
