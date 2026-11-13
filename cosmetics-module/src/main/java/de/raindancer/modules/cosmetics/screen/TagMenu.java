package de.raindancer.modules.cosmetics.screen;

import de.raindancer.core.ui.identity.NameBadges;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.MenuLayout;
import de.raindancer.modules.cosmetics.CosmeticsServices;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code /cosmetics tags}: every tag in front of your name you could wear — a guild's tag, its stars — each
 * a click to take off or put back on. Ranks keep their own page (/rank), linked from here.
 */
public final class TagMenu extends Menu implements ICosmeticsScreen {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    /** Columns of one band, left to right. */
    private static final int[] COLUMNS = {1, 2, 3, 4, 5, 6, 7};
    /** RanksModule.BADGE_OWNER: ranks are worn and chosen on /rank, shown here so they are found. */
    private static final String RANKS = "ranks";

    private final CosmeticsServices services;

    public TagMenu(CosmeticsServices services, Player viewer, Menu parent) {
        super(viewer, services.brand(), parent);
        this.services = services;
    }

    @Override
    protected Component title() {
        return MINI.deserialize("<dark_gray>Your tags");
    }

    @Override
    public String breadcrumb() {
        return "Tags";
    }

    /** The wearable tags this player has right now, shown or not. */
    static List<Offered> offered(java.util.UUID player) {
        List<Offered> found = new ArrayList<>();
        for (NameBadges.Wearable wearable : NameBadges.wearables()) {
            Optional<Component> badge = NameBadges.preview(player, wearable.owner(), NameBadges.Place.CHAT)
                    .or(() -> NameBadges.preview(player, wearable.owner(), NameBadges.Place.TABLIST));
            badge.ifPresent(component -> found.add(new Offered(wearable, component)));
        }
        return found;
    }

    record Offered(NameBadges.Wearable wearable, Component badge) {
    }

    /** The rank badge in front of this player's name (Ranks' own), as chat or else the tablist draws it. */
    static Optional<Component> rankOf(java.util.UUID player) {
        return NameBadges.preview(player, RANKS, NameBadges.Place.CHAT)
                .or(() -> NameBadges.preview(player, RANKS, NameBadges.Place.TABLIST));
    }

    @Override
    protected void render() {
        List<Offered> offered = offered(viewer.getUniqueId());
        Optional<Component> rank = rankOf(viewer.getUniqueId());
        boolean ranks = viewer.getServer().getCommandMap().getCommand("rank") != null;
        int shown = 0;
        if (rank.isPresent()) {
            band(MenuLayout.WHO, COLUMNS[shown++], Icons.of(Material.GOLDEN_HELMET, "<white>Your rank",
                            "<gray>Looks like: " + MINI.serialize(rank.get())
                                    // the badge's font stays open in MiniMessage: the name must not be drawn in it
                                    + "<font:minecraft:default> <white>" + viewer.getName(),
                            "<gray>Which rank you wear, its colours", "<gray>and where it shows: on /rank.",
                            "", ranks ? "<dark_gray>Click to open /rank." : ""),
                    click -> {
                        if (ranks) {
                            viewer.closeInventory();
                            viewer.performCommand("rank");
                        }
                    });
        }
        if (offered.isEmpty() && rank.isEmpty()) {
            band(MenuLayout.WHO, 4, Icons.of(Material.NAME_TAG, "<white>No tags yet",
                    "<gray>Tags come with what you are part of:",
                    "<gray>join a guild and its tag shows up here."));
        }
        for (Offered tag : offered) {
            if (shown == COLUMNS.length) {
                break;
            }
            String owner = tag.wearable().owner();
            boolean worn = services.tags().wears(viewer.getUniqueId(), owner);
            String looks = MINI.serialize(tag.badge());
            band(MenuLayout.WHO, COLUMNS[shown++], Icons.of(worn ? Material.NAME_TAG : Material.PAPER,
                            "<white>" + tag.wearable().label(),
                            "<gray>Looks like: " + looks + " <white>" + viewer.getName(),
                            worn ? "<green>Worn" : "<red>Taken off",
                            "",
                            worn ? "<dark_gray>Click to take it off." : "<dark_gray>Click to wear it again."),
                    click -> {
                        services.tags().toggle(viewer, owner);
                        refresh();
                    });
        }
        if (ranks && rank.isEmpty()) {
            band(MenuLayout.RULES, 4, Icons.of(Material.GOLDEN_HELMET, "<white>Your rank",
                            "<gray>Which rank you wear, and where,", "<gray>is on its own page.", "",
                            "<dark_gray>Click to open /rank."),
                    click -> {
                        viewer.closeInventory();
                        viewer.performCommand("rank");
                    });
        }
    }

    @Override
    protected List<String> helpLines() {
        return List.of("<gray>Tags are drawn in front of your name in chat,",
                "<gray>the tablist and above your head.",
                "<gray>Taking one off works on every server.");
    }

    @Override
    public String describe() {
        return "the tags in front of your name";
    }
}
