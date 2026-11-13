package de.raindancer.modules.cosmetics;

import de.raindancer.core.ui.text.NameStyle;
import de.raindancer.modules.cosmetics.model.Preset;
import de.raindancer.modules.cosmetics.util.PermissionNodes;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The defaults, asserted: an unregistered node is operators-only, and FALSE means nobody at all —
 * neither mistake shows until a real player tries.
 */
class PermissionNodesTest {

    private static Map<String, PermissionDefault> defaults(List<Permission> nodes) {
        return nodes.stream().collect(Collectors.toMap(Permission::getName, Permission::getDefault));
    }

    @Test
    @DisplayName("everybody may colour their name; obfuscated, every preset and staff tools are operators'")
    void defaults() {
        Map<String, PermissionDefault> declared = defaults(PermissionNodes.declared());

        assertThat(declared).containsEntry(PermissionNodes.USE, PermissionDefault.TRUE)
                .containsEntry(PermissionNodes.NAME_COLOUR, PermissionDefault.TRUE)
                .containsEntry(PermissionNodes.NAME_GRADIENT, PermissionDefault.TRUE)
                .containsEntry(PermissionNodes.NAME_ANY_COLOUR, PermissionDefault.TRUE)
                .containsEntry(PermissionNodes.NAME_ANIMATED, PermissionDefault.TRUE)
                .containsEntry("rainscosmetics.name.decoration.bold", PermissionDefault.TRUE)
                .containsEntry("rainscosmetics.name.decoration.obfuscated", PermissionDefault.OP)
                .containsEntry(PermissionNodes.PRESET_ALL, PermissionDefault.OP)
                .containsEntry(PermissionNodes.PARTICLES, PermissionDefault.TRUE)
                .containsEntry(PermissionNodes.PARTICLES_ULTRA, PermissionDefault.OP)
                .containsEntry(PermissionNodes.CLEAR, PermissionDefault.TRUE)
                .containsEntry(PermissionNodes.CLEAR_OTHERS, PermissionDefault.OP)
                .containsEntry(PermissionNodes.ADMIN, PermissionDefault.OP);
        assertThat(declared).containsEntry(PermissionNodes.CREATOR, PermissionDefault.FALSE)
                .containsEntry(PermissionNodes.FLAIR, PermissionDefault.FALSE);
        assertThat(declared.entrySet().stream().filter(e -> e.getValue() == PermissionDefault.FALSE).map(Map.Entry::getKey))
                .as("only the two perks that are given, never assumed").containsExactlyInAnyOrder(
                        PermissionNodes.CREATOR, PermissionNodes.FLAIR);
    }

    @Test
    @DisplayName("rainscosmetics.creator carries the creator presets and nothing else — no particles, no skins, no flair")
    void creatorNode() {
        Permission creator = PermissionNodes.declared().stream()
                .filter(p -> p.getName().equals(PermissionNodes.CREATOR)).findFirst().orElseThrow();

        assertThat(creator.getDefault()).isEqualTo(PermissionDefault.FALSE);
        assertThat(creator.getChildren().keySet()).containsExactlyInAnyOrderElementsOf(
                de.raindancer.modules.cosmetics.creator.CreatorPerks.presetNodes());
        assertThat(creator.getChildren()).doesNotContainKeys(PermissionNodes.PARTICLES, PermissionNodes.SKINS,
                PermissionNodes.FLAIR, PermissionNodes.FREE, PermissionNodes.LIBRARY);
    }

    @Test
    @DisplayName("a creator preset is nobody's by default, not even an operator's; any other restricted one still is")
    void creatorPresetNodes() {
        List<Permission> nodes = PermissionNodes.declaredFor(List.of(
                new Preset("twitch", "Twitch", NameStyle.NONE, true),
                new Preset("vip", "VIP", NameStyle.NONE, true)));
        assertThat(defaults(nodes)).containsEntry("rainscosmetics.preset.twitch", PermissionDefault.FALSE)
                .containsEntry("rainscosmetics.preset.vip", PermissionDefault.OP);
    }

    @Test
    @DisplayName("the supporters' library: every particle and teleport look and any sound, operators' by default")
    void library() {
        Permission library = PermissionNodes.declared().stream()
                .filter(p -> p.getName().equals(PermissionNodes.LIBRARY)).findFirst().orElseThrow();

        assertThat(library.getDefault()).isEqualTo(PermissionDefault.OP);
        assertThat(library.getChildren()).containsEntry(PermissionNodes.PARTICLES, true)
                .containsEntry(PermissionNodes.TELEPORT, true)
                .containsEntry(PermissionNodes.TELEPORT_ANY_SOUND, true)
                .doesNotContainKey(PermissionNodes.PARTICLES_ULTRA);
    }

    @Test
    @DisplayName("only a restricted preset gets a node")
    void presetNodes() {
        List<Permission> nodes = PermissionNodes.declaredFor(List.of(
                new Preset("open", "Open", NameStyle.NONE, false),
                new Preset("vip", "VIP", NameStyle.NONE, true)));
        assertThat(defaults(nodes)).containsOnlyKeys("rainscosmetics.preset.vip")
                .containsEntry("rainscosmetics.preset.vip", PermissionDefault.OP);
    }
}
