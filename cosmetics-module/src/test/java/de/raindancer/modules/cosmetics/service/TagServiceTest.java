package de.raindancer.modules.cosmetics.service;

import de.raindancer.core.ui.identity.NameBadges;
import de.raindancer.modules.cosmetics.store.TagChoices;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TagServiceTest {

    private static final UUID ALICE = UUID.nameUUIDFromBytes("alice".getBytes());
    private static final UUID BOB = UUID.nameUUIDFromBytes("bob".getBytes());

    private final List<UUID> redrawn = new ArrayList<>();
    private final TagService tags = new TagService(redrawn::add);

    @BeforeEach
    void setUp() {
        NameBadges.clear();
        NameBadges.register("guild", "Guild tag", player -> Optional.of(Component.text("[ABC]")));
        tags.install();
    }

    @AfterEach
    void tearDown() {
        tags.uninstall();
        NameBadges.clear();
    }

    private static String chat(UUID player) {
        return PlainTextComponentSerializer.plainText().serialize(NameBadges.before(player, NameBadges.Place.CHAT));
    }

    @Test
    @DisplayName("every tag is worn until the player takes it off")
    void wornByDefault() {
        assertThat(chat(ALICE)).isEqualTo("[ABC] ");
        assertThat(tags.wears(ALICE, "guild")).isTrue();
    }

    @Test
    @DisplayName("taking a tag off hides it for that player only and redraws their name; putting it on shows it again")
    void toggle() {
        Set<String> hidden = tags.toggle(ALICE, "guild");
        assertThat(hidden).containsExactly("guild");
        assertThat(chat(ALICE)).isEmpty();
        assertThat(chat(BOB)).isEqualTo("[ABC] ");
        assertThat(redrawn).containsExactly(ALICE);

        assertThat(tags.toggle(ALICE, "guild")).isEmpty();
        assertThat(chat(ALICE)).isEqualTo("[ABC] ");
    }

    @Test
    @DisplayName("what their data says is what they wear: a choice made on another server arrives with it")
    void fromData() {
        tags.take(ALICE, TagChoices.parse("guild,guild-honours"));
        assertThat(chat(ALICE)).isEmpty();
        assertThat(redrawn).containsExactly(ALICE);

        tags.take(ALICE, TagChoices.parse("guild,guild-honours"));
        assertThat(redrawn).as("nothing changed, nothing redrawn").containsExactly(ALICE);

        tags.forget(ALICE);
        assertThat(tags.wears(ALICE, "guild")).isTrue();
    }

    @Test
    @DisplayName("the stored list survives a round trip and ignores junk")
    void codec() {
        assertThat(TagChoices.parse(null)).isEmpty();
        assertThat(TagChoices.parse(" guild , ,guild-honours,guild")).containsExactly("guild", "guild-honours");
        assertThat(TagChoices.format(Set.of())).isNull();
        assertThat(TagChoices.parse(TagChoices.format(TagChoices.parse("b,a")))).containsExactly("a", "b");
    }

    @Test
    @DisplayName("uninstalled, everybody wears everything again")
    void uninstalled() {
        tags.toggle(ALICE, "guild");
        tags.uninstall();
        assertThat(chat(ALICE)).isEqualTo("[ABC] ");
    }
}
