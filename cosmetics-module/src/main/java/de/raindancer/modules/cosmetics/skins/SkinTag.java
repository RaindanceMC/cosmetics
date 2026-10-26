package de.raindancer.modules.cosmetics.skins;

import java.util.UUID;

/** What a skinned item carries: which skin, and whose — the skin shows as long as its owner has it unlocked. */
public record SkinTag(String skin, UUID owner) {
}
