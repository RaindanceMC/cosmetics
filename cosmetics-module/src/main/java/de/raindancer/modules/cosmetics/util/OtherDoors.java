package de.raindancer.modules.cosmetics.util;

import de.raindancer.core.ui.cosmetic.CosmeticDoors;
import de.raindancer.modules.cosmetics.service.ClearService;
import org.bukkit.entity.Player;

/**
 * The other plugins' cosmetics, through Core's doors. What came with Core 2.102.0 (wearing, taking off) is asked
 * for guarded, so the page still runs on an older Core, just without clearing the other plugins' cosmetics.
 */
public final class OtherDoors implements ClearService.Others {

    @Override
    public boolean wearing(Player player) {
        try {
            return CosmeticDoors.wearingAny(player);
        } catch (LinkageError olderCore) {
            return false;
        }
    }

    @Override
    public int takeOff(Player player) {
        try {
            return CosmeticDoors.takeOffAll(player);
        } catch (LinkageError olderCore) {
            return 0;
        }
    }
}
