package de.raindancer.modules.cosmetics.screen;

import de.raindancer.core.ui.cosmetic.CosmeticDoor;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.PaginatedMenu;
import de.raindancer.modules.cosmetics.CosmeticsServices;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/** The other plugins' cosmetics that did not fit on the cosmetics page. */
public final class OtherDoorsMenu extends PaginatedMenu<CosmeticDoor> implements ICosmeticsScreen {

    private final CosmeticsServices services;
    private final List<CosmeticDoor> doors;

    public OtherDoorsMenu(CosmeticsServices services, Player viewer, Menu parent, List<CosmeticDoor> doors) {
        super(viewer, services.brand(), parent);
        this.services = services;
        this.doors = List.copyOf(doors);
    }

    static ItemStack iconOf(CosmeticDoor door, Player viewer) {
        Optional<String> locked = door.locked(viewer);
        ItemStack icon = door.icon(viewer);
        return locked.isEmpty() ? icon : Icons.locked(icon, locked.get());
    }

    /** Opens the door, or says why it is locked. */
    static void enter(CosmeticsServices services, CosmeticDoor door, Player viewer, Menu from) {
        Optional<String> locked = door.locked(viewer);
        if (locked.isEmpty()) {
            door.open(viewer, from);
        } else {
            services.messages().send(viewer, "cosmetics.door-locked", "reason", locked.get());
        }
    }

    @Override
    protected Component title() {
        return MiniMessage.miniMessage().deserialize("<dark_gray>More cosmetics");
    }

    @Override
    public String breadcrumb() {
        return "More cosmetics";
    }

    @Override
    protected List<CosmeticDoor> entries() {
        return doors;
    }

    @Override
    protected ItemStack icon(CosmeticDoor door) {
        return iconOf(door, viewer);
    }

    @Override
    protected void onClick(CosmeticDoor door, InventoryClickEvent event) {
        Optional<String> locked = door.locked(viewer);
        if (locked.isEmpty()) {
            door.open(viewer, this);
        } else {
            services.messages().send(viewer, "cosmetics.door-locked", "reason", locked.get());
        }
    }

    @Override
    protected ItemStack emptyIcon() {
        return Icons.of(org.bukkit.Material.BARRIER, "<gray>Nothing more", "<gray>Every cosmetic is on the main page.");
    }

    @Override
    public String describe() {
        return "the other plugins' cosmetics";
    }
}
