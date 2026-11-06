package de.raindancer.modules.cosmetics.service;

import de.raindancer.core.ui.choose.NameStyleGrants;
import de.raindancer.core.ui.choose.ParticleGrants;
import de.raindancer.core.ui.choose.StyleGrants;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("what a player may paint and wear, offered to everything else they own")
class CoreGrantsTest {

    @AfterEach
    void clear() {
        NameStyleGrants.clear();
        ParticleGrants.clear();
    }

    @Test
    @DisplayName("while installed, Core answers with cosmetics' own judgement; stopped, nobody answers")
    void installed() {
        Player alice = mock(Player.class);
        CoreGrants grants = new CoreGrants(who -> StyleGrants.NOTHING.withColour(true), who -> List.of("FLAME"));

        grants.install();
        assertThat(NameStyleGrants.of(alice)).contains(StyleGrants.NOTHING.withColour(true));
        assertThat(ParticleGrants.of(alice)).contains(List.of("FLAME"));

        grants.uninstall();
        assertThat(NameStyleGrants.of(alice)).isEmpty();
        assertThat(ParticleGrants.of(alice)).isEmpty();
    }
}
