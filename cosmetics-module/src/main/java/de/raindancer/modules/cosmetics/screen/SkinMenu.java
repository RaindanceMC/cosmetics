package de.raindancer.modules.cosmetics.screen;

import de.raindancer.core.ui.effect.Cues;
import de.raindancer.core.ui.menu.Backdrop;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.MenuLayout;
import de.raindancer.core.ui.menu.PaginatedMenu;
import de.raindancer.modules.cosmetics.CosmeticsServices;
import de.raindancer.modules.cosmetics.skins.SkinCatalogue;
import de.raindancer.modules.cosmetics.skins.SkinItems;
import de.raindancer.modules.cosmetics.skins.SkinPack;
import de.raindancer.modules.cosmetics.skins.SkinService;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * {@code /cosmetics skins}: the skins for the weapon in your hand, by category — yours to put on with a click,
 * the rest to unlock with what you earned by supporting and playing, one at a time or at random.
 */
public final class SkinMenu extends PaginatedMenu<SkinCatalogue.Skin> implements ICosmeticsScreen {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final int MOST_AT_ONCE = 25;

    private final CosmeticsServices services;
    private final SkinService skins;
    /** Null: every category. */
    private String category;

    public SkinMenu(CosmeticsServices services, Player viewer, Menu parent) {
        super(viewer, services.brand(), parent);
        this.services = services;
        this.skins = services.skins();
    }

    @Override
    protected Component title() {
        return MINI.deserialize("<dark_gray>Weapon skins");
    }

    @Override
    public String breadcrumb() {
        return "Weapon skins";
    }

    @Override
    protected Backdrop backdrop() {
        return SkinPack.backdrop(layout().rows());
    }

    private String held() {
        return viewer.getInventory().getItemInMainHand().getType().getKey().getKey();
    }

    private List<SkinCatalogue.Skin> forHeld() {
        return skins.catalogue().offeredFor(held(), skins.settings().switchedOff());
    }

    @Override
    protected List<SkinCatalogue.Skin> entries() {
        List<SkinCatalogue.Skin> shown = new ArrayList<>(forHeld().stream()
                .filter(skin -> category == null || skin.category().equals(category)).toList());
        // Yours first, then by category in the menu's order, then by name.
        shown.sort(Comparator.comparing((SkinCatalogue.Skin skin) -> !skins.owns(viewer, skin.id()))
                .thenComparing(skin -> SkinCatalogue.CATEGORIES.indexOf(skin.category()))
                .thenComparing(SkinCatalogue.Skin::title));
        return shown;
    }

    @Override
    protected ItemStack emptyIcon() {
        if (SkinCatalogue.kindOf(held()).isEmpty()) {
            return Icons.of(Material.WOODEN_SWORD, "<white>Hold a weapon",
                    "<gray>Swords, axes, pickaxes, spears, maces,", "<gray>tridents, bows and crossbows have skins.",
                    "", "<dark_gray>Take one in your hand and open this again.");
        }
        return Icons.of(Material.COBWEB, "<gray>No skins for this one here",
                category == null ? "<gray>None are switched on for it." : "<gray>None in " + category + ".");
    }

    @Override
    protected ItemStack icon(SkinCatalogue.Skin skin) {
        boolean owns = skins.owns(viewer, skin.id());
        int left = skins.unlocksLeft(viewer);
        List<String> lore = new ArrayList<>();
        lore.add("<#C8A27A>" + skin.category());
        lore.add("<dark_gray>by " + skin.author() + " · " + skin.license());
        lore.add("");
        if (owns) {
            lore.add("<green>Yours.");
            lore.add("<dark_gray>Click to put it on what you hold.");
        } else if (left > 0) {
            lore.add("<#E8B98A>Locked.");
            lore.add("<dark_gray>Click to unlock it — " + (left == Integer.MAX_VALUE ? "as many as you like" : left + " left") + ".");
        } else {
            lore.add("<#E8B98A>Locked.");
            lore.add("<dark_gray>Earn unlocks with /support, and supporters");
            lore.add("<dark_gray>earn more just by playing.");
        }
        if (!services.core().resourcePacks().isWearing(viewer.getUniqueId())) {
            lore.add("");
            lore.add("<red>You need the server's resource pack to see skins.");
        }
        ItemStack icon = Icons.of(viewer.getInventory().getItemInMainHand().getType(), "<white>" + skin.title(), lore);
        skin.lookFor(held()).ifPresent(look -> icon.setData(DataComponentTypes.ITEM_MODEL, Key.key(look)));
        return icon;
    }

    @Override
    protected void onClick(SkinCatalogue.Skin skin, InventoryClickEvent event) {
        if (skins.owns(viewer, skin.id())) {
            put(skin);
            return;
        }
        if (skins.unlocksLeft(viewer) <= 0) {
            services.messages().send(viewer, "cosmetics.skins.none-left");
            return;
        }
        new ConfirmScreen(services, viewer, this, "Unlock " + skin.title() + "?",
                List.of("<gray>It is yours for good, on any of your weapons it fits.",
                        "<gray>You have " + leftText() + " unlock(s) left."),
                () -> {
                    if (skins.unlock(viewer, skin.id()) == SkinService.Outcome.WORN) {
                        services.messages().send(viewer, "cosmetics.skins.unlocked", "skin", skin.title());
                        services.core().effects().play(viewer.getUniqueId(), Cues.REWARD);
                        put(skin);
                    }
                    reopen();
                }).open();
    }

    private void put(SkinCatalogue.Skin skin) {
        SkinService.Outcome outcome = skins.wear(viewer, skin.id());
        services.messages().send(viewer, key(outcome), "skin", skin.title());
        refresh();
    }

    private String leftText() {
        int left = skins.unlocksLeft(viewer);
        return left == Integer.MAX_VALUE ? "unlimited" : String.valueOf(left);
    }

    @Override
    protected void render() {
        super.render();
        int row = MenuLayout.TOOLBAR_ROW * 9;
        ItemStack hand = viewer.getInventory().getItemInMainHand();
        boolean skinned = SkinItems.tagOf(hand).filter(tag -> tag.owner().equals(viewer.getUniqueId())).isPresent();
        set(row, hand.getType().isAir()
                        ? Icons.of(Material.BARRIER, "<gray>Nothing in your hand")
                        : Icons.showing(hand, null, List.of(
                                skinned ? "<#E8B98A>Click to take its skin off." : "<dark_gray>No skin on it yet.",
                                "<gray>Unlocks left: <white>" + leftText(),
                                "<gray>Unlocked so far: <white>" + skins.unlockedCount(viewer))),
                click -> {
                    if (skinned) {
                        services.messages().send(viewer, key(skins.takeOff(viewer)));
                        refresh();
                    }
                });
        List<String> categories = SkinCatalogue.categoriesOf(forHeld());
        set(row + 1, Icons.of(category == null ? Material.GLOW_ITEM_FRAME : Material.ITEM_FRAME,
                        (category == null ? "<#F3E3CC><b>" : "<white>") + "All", "<gray>" + forHeld().size() + " skin(s)"),
                click -> show(null));
        for (int i = 0; i < Math.min(6, categories.size()); i++) {
            String each = categories.get(i);
            long count = forHeld().stream().filter(skin -> skin.category().equals(each)).count();
            set(row + 2 + i, Icons.of(iconOf(each), (each.equals(category) ? "<#F3E3CC><b>" : "<white>") + each,
                    "<gray>" + count + " skin(s)"), click -> show(each));
        }
        set(row + 8, Icons.of(Material.DECORATED_POT, "<#E8B98A>Surprise me!",
                        "<gray>Unlocks skins you do not have yet, at random —",
                        "<gray>every skin as likely as any other, never twice.",
                        "<gray>Unlocks left: <white>" + leftText(), "",
                        "<dark_gray>Left-click: one · Right-click: five",
                        "<dark_gray>Shift-click: as many as you have (up to " + MOST_AT_ONCE + ")"),
                this::surprise);
    }

    private void show(String chosen) {
        category = chosen;
        refresh();
    }

    private void surprise(InventoryClickEvent event) {
        int wanted = event.isShiftClick() ? MOST_AT_ONCE : event.isRightClick() ? 5 : 1;
        if (skins.unlocksLeft(viewer) <= 0) {
            services.messages().send(viewer, "cosmetics.skins.none-left");
            return;
        }
        List<String> got = skins.unlockRandom(viewer, wanted);
        if (got.isEmpty()) {
            services.messages().send(viewer, "cosmetics.skins.random-none");
            return;
        }
        services.messages().send(viewer, "cosmetics.skins.random-head", "count", String.valueOf(got.size()));
        for (String id : got) {
            skins.catalogue().byId(id).ifPresent(skin -> services.messages().send(viewer, "cosmetics.skins.random-line",
                    "skin", skin.title(), "category", skin.category()));
        }
        services.core().effects().play(viewer.getUniqueId(), Cues.REWARD);
        refresh();
    }

    private static Material iconOf(String category) {
        return switch (category) {
            case "Katana & Blades" -> Material.IRON_SWORD;
            case "Scythe" -> Material.NETHERITE_HOE;
            case "Fantasy" -> Material.AMETHYST_SHARD;
            case "3D Classic" -> Material.DIAMOND_SWORD;
            case "Modern" -> Material.CROSSBOW;
            case "Cute & Fun" -> Material.PINK_PETALS;
            default -> Material.PAPER;
        };
    }

    static String key(SkinService.Outcome outcome) {
        return switch (outcome) {
            case WORN -> "cosmetics.skins.worn";
            case REMOVED -> "cosmetics.skins.removed";
            case LOCKED -> "cosmetics.skins.locked";
            case SOMEBODY_ELSES -> "cosmetics.skins.somebody-elses";
            case NOT_A_WEAPON -> "cosmetics.skins.not-a-weapon";
            case NOT_FOR_THIS_WEAPON -> "cosmetics.skins.not-for-this";
            case NONE_LEFT -> "cosmetics.skins.none-left";
            case OFF -> "cosmetics.skins.switched-off";
        };
    }
}
