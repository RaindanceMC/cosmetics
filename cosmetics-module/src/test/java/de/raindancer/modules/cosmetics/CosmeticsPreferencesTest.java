package de.raindancer.modules.cosmetics;

import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.cosmetics.service.ParticleService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CosmeticsPreferencesTest {

    private final Player player = mock(Player.class);
    private final ParticleService particles = mock(ParticleService.class);

    @Test
    void seeingParticlesIsASwitchThatGoesThroughTheService() {
        Preference seeing = CosmeticsPreferences.seeParticles(() -> particles);
        when(particles.sees(player)).thenReturn(true);
        assertThat(seeing.id()).isEqualTo("cosmetics:see-particles");
        assertThat(seeing.title()).isEqualTo("See other players' particles");
        assertThat(seeing.state(player)).isEqualTo("On");

        when(particles.sees(player)).thenReturn(false);
        assertThat(seeing.state(player)).isEqualTo("Off");
        when(particles.sees(player)).thenReturn(true);
        seeing.choose().accept(player, 1);
        verify(particles).toggleSeeing(player);
    }

    @Test
    void aSwitchToTheStateItAlreadyHasChangesNothing() {
        Preference seeing = CosmeticsPreferences.seeParticles(() -> particles);
        when(particles.sees(player)).thenReturn(true);
        seeing.choose().accept(player, 0);
        verify(particles, never()).toggleSeeing(player);
    }

    @Test
    void theCosmeticsPageOpensTheDialogForThePlayer() {
        AtomicReference<Player> opened = new AtomicReference<>();
        Preference page = CosmeticsPreferences.cosmetics(opened::set);
        assertThat(page.id()).isEqualTo("cosmetics:cosmetics");
        assertThat(page.title()).isEqualTo("Your cosmetics");
        assertThat(page.isPage()).isTrue();

        page.page().accept(player, null);
        assertThat(opened.get()).isSameAs(player);
    }
}
