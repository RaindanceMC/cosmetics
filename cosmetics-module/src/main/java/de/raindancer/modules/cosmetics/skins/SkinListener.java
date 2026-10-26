package de.raindancer.modules.cosmetics.skins;

import de.raindancer.core.platform.util.Scheduling;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Where a skinned item changes hands. The module's own timer goes over every player's inventory anyway; these make
 * the change show at once instead of within two seconds.
 */
public final class SkinListener implements Listener {

    private final Plugin plugin;
    private final SkinService skins;

    public SkinListener(Plugin plugin, SkinService skins) {
        this.plugin = plugin;
        this.skins = skins;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Scheduling.entityLater(plugin, player, 2, () -> skins.refresh(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            Scheduling.entityLater(plugin, player, 1, () -> skins.refresh(player));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Item dropped = event.getItemDrop();
        ItemStack stack = dropped.getItemStack();
        if (skins.refresh(stack, null)) {
            dropped.setItemStack(stack);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        var top = event.getView().getTopInventory();
        if (top.getType() != InventoryType.CRAFTING && top.getType() != InventoryType.CREATIVE
                && top.getHolder() != null && !(top.getHolder() instanceof de.raindancer.core.ui.menu.Menu)) {
            skins.refresh(top, null);
        }
        skins.refresh(player);
    }
}
