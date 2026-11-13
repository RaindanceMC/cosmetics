package de.raindancer.modules.cosmetics.screen;

import de.raindancer.core.ui.identity.NameBadges;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TagMenuTest {

    private final UUID ada = UUID.randomUUID();

    @AfterEach
    void clean() {
        NameBadges.unregister("ranks");
    }

    @Test
    @DisplayName("the rank someone wears is offered with its badge, the one Ranks draws in chat")
    void rankShown() {
        Component prime = Component.text("[PRIME]");
        NameBadges.registerPlaced("ranks", (player, place) ->
                player.equals(ada) && place == NameBadges.Place.CHAT ? Optional.of(prime) : Optional.empty());

        assertThat(TagMenu.rankOf(ada)).contains(prime);
        assertThat(TagMenu.rankOf(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("without Ranks there is no rank to show")
    void noRanks() {
        assertThat(TagMenu.rankOf(ada)).isEmpty();
    }
}
