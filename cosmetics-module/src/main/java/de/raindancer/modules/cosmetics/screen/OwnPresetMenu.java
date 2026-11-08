package de.raindancer.modules.cosmetics.screen;

import de.raindancer.core.ui.identity.ReservedNames;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.PaginatedMenu;
import de.raindancer.core.ui.text.NameStyle;
import de.raindancer.modules.cosmetics.CosmeticsServices;
import de.raindancer.modules.cosmetics.service.NameStyleService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** The viewer's own collection of name styles — only somebody who has one is ever shown the way here. */
public final class OwnPresetMenu extends PaginatedMenu<ReservedNames.Preset> implements ICosmeticsScreen {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final CosmeticsServices services;

    public OwnPresetMenu(CosmeticsServices services, Player viewer, Menu parent) {
        super(viewer, services.brand(), parent);
        this.services = services;
    }

    @Override
    protected Component title() {
        return MINI.deserialize("<dark_gray>Your presets");
    }

    @Override
    public String breadcrumb() {
        return "Your presets";
    }

    @Override
    protected List<ReservedNames.Preset> entries() {
        return services.names().ownPresets(viewer);
    }

    @Override
    protected ItemStack emptyIcon() {
        return Icons.of(Material.COBWEB, "<gray>No presets of your own");
    }

    @Override
    protected ItemStack icon(ReservedNames.Preset preset) {
        NameStyle worn = services.names().current(viewer.getUniqueId());
        boolean wearing = worn.equals(preset.style());
        return Icons.of(wearing ? Material.GLOW_ITEM_FRAME : Material.AMETHYST_SHARD,
                MINI.serialize(NameStyleService.painted(preset.title(), preset.style())),
                "<gray>Your name:",
                MINI.serialize(NameStyleService.painted(viewer.getName(), preset.style())),
                "",
                wearing ? "<green>You are wearing this." : "<dark_gray>Click to wear it.");
    }

    @Override
    protected void onClick(ReservedNames.Preset preset, InventoryClickEvent event) {
        services.names().wear(viewer, preset);
        refresh();
    }

    @Override
    public String describe() {
        return "your own presets, painted in themselves";
    }
}
