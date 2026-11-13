package de.raindancer.modules.cosmetics.screen;

import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.MenuLayout;
import de.raindancer.modules.cosmetics.CosmeticsServices;
import de.raindancer.modules.cosmetics.flair.FlairCatalogue;
import de.raindancer.modules.cosmetics.flair.FlairCategory;
import de.raindancer.modules.cosmetics.flair.FlairOption;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /cosmetics flair}: Prime's flair and the creators' Twitch extras, one row each — the tag, its colour, the
 * join and leave lines, the kill effect and the death effect. A click goes to the next option, a right-click back.
 */
public final class FlairMenu extends Menu implements ICosmeticsScreen {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final CosmeticsServices services;

    public FlairMenu(CosmeticsServices services, Player viewer, Menu parent) {
        super(viewer, services.brand(), parent);
        this.services = services;
    }

    @Override
    protected Component title() {
        return MINI.deserialize("<dark_gray>Your flair");
    }

    @Override
    public String breadcrumb() {
        return "Flair";
    }

    private static Material iconOf(FlairCategory category) {
        return switch (category) {
            case TAG -> Material.NAME_TAG;
            case COLOUR -> Material.PURPLE_DYE;
            case JOIN -> Material.OAK_DOOR;
            case JOIN_SOUND -> Material.NOTE_BLOCK;
            case TELEPORT -> Material.ENDER_PEARL;
            case KILL -> Material.FIREWORK_ROCKET;
            case DEATH -> Material.SKELETON_SKULL;
        };
    }

    @Override
    protected void render() {
        int column = 1;
        for (FlairCategory category : FlairCategory.values()) {
            String now = services.flair().effective(viewer.getUniqueId()).get(category).orElse("");
            List<String> lore = new ArrayList<>();
            lore.add(now.isEmpty() ? "<gray>Off right now." : "<gray>Now: <white>" + FlairCatalogue.find(category, now)
                    .map(FlairOption::title).orElse(now));
            lore.add("");
            for (FlairOption option : FlairCatalogue.of(category)) {
                boolean has = services.flair().rule().may(option, viewer::hasPermission);
                boolean on = option.id().equals(now);
                lore.add((on ? "<green>▶ " : has ? "<gray>  " : "<dark_gray>  ") + option.title()
                        + (has ? "" : " <dark_gray>(" + (option.node().endsWith("creator") ? "creators" : "Prime") + ")"));
            }
            lore.add("");
            lore.add("<dark_gray>Click for the next, right-click for the last.");
            band(MenuLayout.WHO, column, Icons.of(iconOf(category), "<white>" + category.title(), lore.toArray(String[]::new)),
                    click -> {
                        services.flair().cycle(viewer, category, click.isRightClick());
                        refresh();
                    });
            column++;
        }
    }

    @Override
    protected List<String> helpLines() {
        return services.messages().lines("cosmetics.help-flair").stream().map(MINI::serialize).toList();
    }

    @Override
    public String describe() {
        return "Prime's flair and the creators' Twitch extras";
    }
}
