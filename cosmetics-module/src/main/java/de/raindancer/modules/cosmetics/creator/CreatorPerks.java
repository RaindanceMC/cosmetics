package de.raindancer.modules.cosmetics.creator;

import de.raindancer.modules.cosmetics.model.Preset;
import de.raindancer.modules.cosmetics.util.PermissionNodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * What only creators have: everything behind {@link PermissionNodes#CREATOR}. The Twitch name presets live in the
 * shipped catalogue like any restricted preset; what makes them the creators' is that their nodes hang under the
 * creator node, that nobody gets them by default (operators neither) and that a name style equal to one is never
 * accepted by hand — see {@code NameStyleRule}.
 */
public final class CreatorPerks {

    /** The Twitch name presets: Twitch purple, and a glitched one with cyan and magenta in it. */
    public static final List<String> PRESETS = List.of("twitch", "twitch-glitch");

    private CreatorPerks() {
    }

    public static boolean isCreatorPreset(String id) {
        return id != null && PRESETS.contains(id.toLowerCase(Locale.ROOT));
    }

    public static List<String> presetNodes() {
        List<String> nodes = new ArrayList<>();
        for (String id : PRESETS) {
            nodes.add(Preset.PERMISSION_PREFIX + id);
        }
        return nodes;
    }

    /** What a Prime drop may never unlock, however it is written in prime-drops.yml. */
    public static Set<String> neverDropped() {
        Set<String> nodes = new java.util.LinkedHashSet<>(presetNodes());
        nodes.add(PermissionNodes.CREATOR);
        return nodes;
    }
}
