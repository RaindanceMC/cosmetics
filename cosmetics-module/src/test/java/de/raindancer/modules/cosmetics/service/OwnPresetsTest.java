package de.raindancer.modules.cosmetics.service;

import de.raindancer.core.ui.identity.Identities;
import de.raindancer.core.ui.identity.Nametags;
import de.raindancer.core.ui.identity.ReservedNames;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.core.ui.text.NameStyle;
import de.raindancer.modules.cosmetics.CosmeticsSettings;
import de.raindancer.modules.cosmetics.model.Catalogue;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OwnPresetsTest {

    private final Identities identities = mock(Identities.class);
    private final NameStyleService names = new NameStyleService(identities, mock(Nametags.class),
            mock(Messages.class), () -> new Catalogue(List.of(), List.of()), CosmeticsSettings.DEFAULTS);

    private Player player(UUID id) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        return player;
    }

    @Test
    @DisplayName("his own presets are his to see and to put on, whatever the server's grants say")
    void his() {
        Player tom = player(ReservedNames.RAINDANCER118);
        when(identities.setNameStyle(any(), any())).thenReturn(true);
        List<ReservedNames.Preset> own = names.ownPresets(tom);
        assertThat(own).isNotEmpty();
        NameStyle pick = own.get(3).style();
        assertThat(names.wearOwn(tom, pick)).isTrue();
        verify(identities).setNameStyle(ReservedNames.RAINDANCER118, pick);
    }

    @Test
    @DisplayName("somebody else has none, and cannot put one of his on this way")
    void nobodyElse() {
        UUID id = UUID.randomUUID();
        Player other = player(id);
        assertThat(names.ownPresets(other)).isEmpty();
        assertThat(names.wearOwn(other, ReservedNames.AMETHYST_FLOW)).isFalse();
        verify(identities, never()).setNameStyle(eq(id), any());
    }
}
