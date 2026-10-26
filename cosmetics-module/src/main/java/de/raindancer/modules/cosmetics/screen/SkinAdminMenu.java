package de.raindancer.modules.cosmetics.screen;

import de.raindancer.core.ui.menu.Backdrop;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.PaginatedMenu;
import de.raindancer.modules.cosmetics.CosmeticsServices;
import de.raindancer.modules.cosmetics.skins.SkinCatalogue;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** {@code /cosmetics skins admin}: every skin, a click to switch it off or on again — the {@code skins.off} setting. */
public final class SkinAdminMenu extends PaginatedMenu<SkinCatalogue.Skin> implements ICosmeticsScreen {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final CosmeticsServices services;

    public SkinAdminMenu(CosmeticsServices services, Player viewer, Menu parent) {
        super(viewer, services.brand(), parent);
        this.services = services;
    }

    @Override
    protected Component title() {
        return MINI.deserialize("<dark_gray>Weapon skins: on and off");
    }

    @Override
    public String breadcrumb() {
        return "Skins on and off";
    }

    @Override
    protected Backdrop backdrop() {
        return Backdrop.workshop(layout().rows());
    }

    @Override
    protected List<SkinCatalogue.Skin> entries() {
        return services.skins().catalogue().all();
    }

    @Override
    protected ItemStack emptyIcon() {
        return Icons.of(Material.COBWEB, "<gray>No weapon skins in this build",
                "<gray>The catalogue ships inside the plugin;", "<gray>an empty one means a broken build.");
    }

    @Override
    protected ItemStack icon(SkinCatalogue.Skin skin) {
        Map.Entry<String, String> first = skin.looks().entrySet().iterator().next();
        Material material = Material.matchMaterial(first.getKey());
        boolean on = !services.skins().settings().switchedOff().contains(skin.id());
        List<String> lore = new ArrayList<>();
        lore.add(on ? "<green>On" : "<red>Off");
        lore.add("<gray>" + skin.category() + " · for " + String.join(", ", kinds(skin)));
        lore.add("<dark_gray>" + skin.packTitle() + " by " + skin.author() + " · " + skin.license());
        lore.add("<dark_gray>id: " + skin.id());
        lore.add("");
        lore.add(on ? "<dark_gray>Click to switch it off." : "<dark_gray>Click to switch it on again.");
        ItemStack icon = Icons.of(material == null ? Material.IRON_SWORD : material,
                (on ? "<white>" : "<gray><st>") + skin.title(), lore);
        icon.setData(DataComponentTypes.ITEM_MODEL, Key.key(first.getValue()));
        return icon;
    }

    private static Set<String> kinds(SkinCatalogue.Skin skin) {
        Set<String> kinds = new TreeSet<>();
        skin.looks().keySet().forEach(item -> SkinCatalogue.kindOf(item).ifPresent(kinds::add));
        return kinds;
    }

    @Override
    protected void onClick(SkinCatalogue.Skin skin, InventoryClickEvent event) {
        Set<String> off = new TreeSet<>(services.skins().settings().switchedOff());
        if (!off.remove(skin.id())) {
            off.add(skin.id());
        }
        services.skinSettings().set("skins.off", String.join(",", off));
        services.messages().send(viewer, "cosmetics.skins.toggled", "skin", skin.title(),
                "state", off.contains(skin.id()) ? "off" : "on");
        refresh();
    }
}
