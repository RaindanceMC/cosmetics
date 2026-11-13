package de.raindancer.modules.cosmetics.prime;

import de.raindancer.modules.cosmetics.util.PermissionNodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrimePerksTest {

    @Test
    @DisplayName("Prime-only: name colours, particles and teleport looks stop being everybody's")
    void gated() {
        assertThat(PrimePerks.GATED).contains(PermissionNodes.NAME_COLOUR, PermissionNodes.NAME_GRADIENT,
                PermissionNodes.NAME_ANY_COLOUR, PermissionNodes.NAME_ANIMATED, PermissionNodes.PARTICLES,
                PermissionNodes.TELEPORT, PermissionNodes.decoration(net.kyori.adventure.text.format.TextDecoration.BOLD));
        assertThat(PrimePerks.GATED).doesNotContain(PermissionNodes.USE, PermissionNodes.CLEAR,
                PermissionNodes.decoration(net.kyori.adventure.text.format.TextDecoration.OBFUSCATED));
    }

    @Test
    @DisplayName("Prime gets the gated ones and every weapon and armour skin; staff and creators get the gated ones too")
    void holders() {
        assertThat(PrimePerks.forPrime(true)).containsAll(PrimePerks.GATED)
                .contains(PermissionNodes.SKINS, PermissionNodes.SKINS_ALL);
        assertThat(PrimePerks.forPrime(false)).isEmpty();
        assertThat(PrimePerks.forPerksNode()).containsExactlyInAnyOrderElementsOf(PrimePerks.GATED);
    }

    @Test
    @DisplayName("Prime-only takes skins away from coffee supporters; off, they keep them")
    void supporters() {
        assertThat(PrimePerks.supporterKeepsSkins(true)).isFalse();
        assertThat(PrimePerks.supporterKeepsSkins(false)).isTrue();
    }
}
