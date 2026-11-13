package de.raindancer.modules.cosmetics.creator;

import de.raindancer.modules.cosmetics.model.Catalogue;
import de.raindancer.modules.cosmetics.model.Preset;
import de.raindancer.modules.cosmetics.store.CatalogueFile;
import de.raindancer.modules.cosmetics.util.PermissionNodes;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CreatorPerksTest {

    @TempDir
    Path folder;

    @Test
    @DisplayName("the Twitch presets are creator-only: their node is derived from the id, and the node a creator holds")
    void presetNodes() {
        assertThat(CreatorPerks.isCreatorPreset("twitch")).isTrue();
        assertThat(CreatorPerks.isCreatorPreset("Twitch-Glitch")).isTrue();
        assertThat(CreatorPerks.isCreatorPreset("sunset")).isFalse();
        assertThat(CreatorPerks.presetNodes()).containsExactlyInAnyOrder("rainscosmetics.preset.twitch",
                "rainscosmetics.preset.twitch-glitch");
    }

    @Test
    @DisplayName("the shipped catalogue has the Twitch presets, restricted, in Twitch purple and animated")
    void shipped() {
        Catalogue loaded = CatalogueFile.in(folder).load(problem -> { });

        for (String id : CreatorPerks.PRESETS) {
            Preset preset = loaded.preset(id).orElseThrow(() -> new AssertionError(id + " is not shipped"));
            assertThat(preset.restricted()).as(id).isTrue();
            assertThat(preset.style().isAnimated()).as(id).isTrue();
            assertThat(preset.style().colours()).as(id).contains(TextColor.color(0x9146FF));
            assertThat(preset.permission()).isIn(CreatorPerks.presetNodes());
        }
    }

    @Test
    @DisplayName("a Prime drop can never hand out the creator node or a creator preset")
    void dropsCannotGrantThem() {
        assertThat(CreatorPerks.neverDropped()).contains(PermissionNodes.CREATOR)
                .containsAll(CreatorPerks.presetNodes());
    }
}
