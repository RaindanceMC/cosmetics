package de.raindancer.modules.cosmetics.screen;

import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.ui.choose.ParticleCatalogue;
import de.raindancer.core.ui.choose.PlayerChooser;
import de.raindancer.core.data.settings.SettingsMenu;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.MenuLayout;
import de.raindancer.core.ui.text.NameStyle;
import de.raindancer.modules.cosmetics.CosmeticsServices;
import de.raindancer.modules.cosmetics.model.ClearScope;
import de.raindancer.modules.cosmetics.model.ParticleChoice;
import de.raindancer.modules.cosmetics.service.NameStyleService;
import de.raindancer.modules.cosmetics.util.PermissionNodes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/** {@code /cosmetics}: the doors to your name, tags and particle, each showing what you wear now. */
public final class CosmeticsMenu extends Menu implements ICosmeticsScreen {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** How many other plugins' doors fit: one beside the weapon skins and tags, five in the row below. */
    private static final int DOOR_PLACES = 6;

    private final CosmeticsServices services;

    public CosmeticsMenu(CosmeticsServices services, Player viewer) {
        super(viewer, services.brand(), null);
        this.services = services;
    }

    @Override
    protected Component title() {
        return MINI.deserialize("<dark_gray>Cosmetics");
    }

    @Override
    public String breadcrumb() {
        return "Cosmetics";
    }

    @Override
    protected void render() {
        NameStyle style = services.names().current(viewer.getUniqueId());
        band(MenuLayout.WHO, 1, Icons.head(viewer.getUniqueId(),
                        MINI.serialize(NameStyleService.painted(viewer.getName(), style)),
                        "<gray>Your name", "<dark_gray>" + services.names().describe(style), "",
                        "<dark_gray>Click to change it."),
                click -> new NameStyleMenu(services, viewer, this).open());

        ParticleChoice particle = services.particles().current(viewer);
        Material icon = particle.isNone() ? Material.GLASS_BOTTLE
                : ParticleMenu.iconOf(particle.particle());
        band(MenuLayout.WHO, 3, Icons.of(icon, "<white>Particles",
                        particle.isNone() ? "<gray>None right now."
                                : "<gray>" + ParticleCatalogue.readable(particle.particle()) + ", "
                                        + particle.shape().title().toLowerCase(java.util.Locale.ROOT) + ".",
                        "", "<dark_gray>Click to pick one."),
                click -> new ParticleMenu(services, viewer, this).open());

        ParticleChoice wings = services.particles().wings().current(viewer);
        band(MenuLayout.WHO, 5, Icons.of(wings.isNone() ? Material.FEATHER : Material.ELYTRA, "<white>Wings",
                        wings.isNone() ? "<gray>None right now."
                                : "<gray>" + wings.shape().title() + ", " + ParticleCatalogue.readable(wings.particle())
                                        .toLowerCase(java.util.Locale.ROOT) + ".",
                        "<dark_gray>Worn on top of your particle.", "", "<dark_gray>Click to pick some."),
                click -> new ParticleMenu(services, viewer, this, services.particles().wings()).open());

        band(MenuLayout.WHO, 7, Icons.of(Material.ENDER_PEARL,
                        MINI.serialize(services.messages().get("cosmetics.teleport.menu.door")),
                        MINI.serialize(services.messages().get("cosmetics.teleport.menu.door-lore"))),
                click -> new TeleportMenu(services, viewer, this).open());

        int tags = TagMenu.offered(viewer.getUniqueId()).size();
        long off = TagMenu.offered(viewer.getUniqueId()).stream()
                .filter(tag -> !services.tags().wears(viewer.getUniqueId(), tag.wearable().owner())).count();
        band(MenuLayout.RULES, 4, Icons.of(Material.NAME_TAG, "<white>Tags",
                        tags == 0 ? "<gray>None right now." : "<gray>" + (tags - off) + " of " + tags + " worn.",
                        "<dark_gray>Your guild's tag and more,", "<dark_gray>in front of your name.", "",
                        "<dark_gray>Click to choose."),
                click -> new TagMenu(services, viewer, this).open());

        boolean flairHere = services.flair().mayOpen(viewer);
        ItemStack flairIcon = Icons.of(Material.NETHER_STAR, "<#FFC93C>Flair ✦",
                "<gray>A tag, your join and leave lines and", "<gray>effects for fights. Looks only.",
                "<gray>Prime has them all, creators get Twitch extras.", "", "<dark_gray>Click to choose.");
        band(MenuLayout.RULES, 7, flairHere ? flairIcon
                        : Icons.locked(flairIcon, MINI.serialize(services.messages().get("cosmetics.flair.locked"))),
                click -> {
                    if (flairHere) {
                        new FlairMenu(services, viewer, this).open();
                    }
                });

        boolean skinsHere = services.skins().mayUse(viewer);
        boolean skinsPrime = services.skins().primeOnly();
        ItemStack skinsIcon = skinsPrime
                ? Icons.of(Material.DIAMOND_SWORD, "<#FFC93C>Weapon skins ✦",
                        "<gray>A Prime perk:", "<gray>new looks for your weapons.", "",
                        "<dark_gray>Hold a weapon and click.")
                : Icons.of(Material.DIAMOND_SWORD, "<#E8B98A>Weapon skins ☕",
                        "<gray>A thank-you for supporting the server:", "<gray>new looks for your weapons.", "",
                        "<dark_gray>Hold a weapon and click.");
        band(MenuLayout.RULES, 2, skinsHere ? skinsIcon
                        : Icons.locked(skinsIcon, skinsPrime ? MINI.serialize(services.messages().get("cosmetics.flair.locked"))
                                : "For supporters. See /support"),
                click -> {
                    if (skinsHere) {
                        new SkinMenu(services, viewer, this).open();
                    }
                });

        // The other plugins' cosmetics, beside the weapon skins: chat colour, rod skins, elytra looks and whatever comes
        // next. More than fit: the last place leads to the rest, so none falls off the page.
        de.raindancer.modules.cosmetics.model.DoorLayout<de.raindancer.core.ui.cosmetic.CosmeticDoor> layout =
                de.raindancer.modules.cosmetics.model.DoorLayout.of(de.raindancer.core.ui.cosmetic.CosmeticDoors.doors(),
                        DOOR_PLACES);
        List<ItemStack> icons = new java.util.ArrayList<>();
        List<java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent>> clicks =
                new java.util.ArrayList<>();
        for (de.raindancer.core.ui.cosmetic.CosmeticDoor door : layout.onPage()) {
            icons.add(OtherDoorsMenu.iconOf(door, viewer));
            clicks.add(click -> OtherDoorsMenu.enter(services, door, viewer, this));
        }
        if (!layout.more().isEmpty()) {
            icons.add(Icons.of(Material.CHEST, "<white>More cosmetics",
                    "<gray>" + layout.more().size() + " more, from the server's other plugins.", "",
                    "<dark_gray>Click to see them."));
            clicks.add(click -> new OtherDoorsMenu(services, viewer, this, layout.more()).open());
        }
        if (!icons.isEmpty()) {
            band(MenuLayout.RULES, 6, icons.get(0), clicks.get(0));
        }
        List<Integer> columns = de.raindancer.modules.cosmetics.model.DoorLayout.rowColumns(icons.size() - 1);
        for (int n = 0; n < columns.size(); n++) {
            band(MenuLayout.LAND, columns.get(n), icons.get(n + 1), clicks.get(n + 1));
        }

        ItemStack unlocksIcon = Icons.of(Material.GOLD_INGOT, "<white>Unlocks",
                "<gray>What this server sells: colours,", "<gray>gradients, particles and more.", "",
                "<dark_gray>Click to see the prices.");
        boolean sold = services.unlocks().anySold();
        toolbar(2, sold ? unlocksIcon : Icons.locked(unlocksIcon, services.unlocks().selling()
                        ? "No price is set yet." : "Cosmetics are not for sale on this server."),
                click -> {
                    if (sold) {
                        new UnlockMenu(services, viewer, this).open();
                    }
                });
        if (services.mayOpenSettings(viewer)) {
            toolbar(6, Icons.of(Material.COMPARATOR, "<white>Server settings for cosmetics",
                            "<gray>Name styles, particles, teleport effects,", "<gray>and the prices with their switch.", "",
                            "<gray>Only people with the settings permission see this."),
                    click -> new SettingsMenu(viewer, services.brand(), services.core().chatFor(services.brand()),
                            services.core().settingsNavigation(), "cosmetics", this).open());
        }

        boolean mayClearOthers = services.clearing().may(viewer, false);
        toolbar(4, mayClearOthers,
                Icons.of(Material.SPONGE, "<white>Clear somebody else's",
                        "<gray>Takes everything they wear off.",
                        "<gray>Pick them from the list; you are asked first."),
                "Needs " + PermissionNodes.CLEAR_OTHERS,
                click -> new PlayerChooser(viewer, services.brand(), this, "Clear whose cosmetics?",
                        java.util.List.of(), person -> confirmClearing(person.id())).open());

        boolean mayClear = services.clearing().may(viewer, true);
        boolean wearing = !style.isEmpty() || !particle.isNone() || !wings.isNone()
                || new de.raindancer.modules.cosmetics.util.OtherDoors().wearing(viewer);
        ItemStack clearIcon = Icons.of(Material.BARRIER, "<red>Clear my cosmetics",
                "<gray>Takes off everything you wear:", "<gray>name style, particles, chat colour",
                "<gray>and the rest. Looks on items stay.", "", "<dark_gray>Asks first.");
        danger(mayClear && wearing ? clearIcon
                        : Icons.locked(clearIcon, !mayClear ? "Needs " + PermissionNodes.CLEAR
                                : "You are not wearing anything"),
                click -> {
                    if (!mayClear || !wearing) {
                        return;
                    }
                    new ConfirmScreen(services, viewer, this, "Clear your cosmetics?",
                            List.of("<gray>Your name goes back to plain, your particle", "<gray>is taken off, and so is "
                                    + "everything", "<gray>else you wear, like your chat colour.",
                                    "<gray>Looks on items stay. What you own stays yours."),
                            () -> services.clearing().clear(viewer, viewer, ClearScope.ALL)).open();
                });
    }

    /** Opened a tick late: the chooser goes back to this page right after answering, and would cover the question. */
    private void confirmClearing(UUID who) {
        OfflinePlayer target = services.server().getOfflinePlayer(who);
        String name = target.getName() == null ? who.toString() : target.getName();
        Scheduling.entityLater(services.plugin(), viewer, 1L, () ->
                new ConfirmScreen(services, viewer, this, "Clear " + name + "'s cosmetics?",
                        List.of("<gray>Their name goes back to plain. Their particle",
                                "<gray>and everything else they wear is taken", "<gray>off, if they are here.",
                                "<gray>It goes in the audit log."),
                        () -> services.clearing().clear(viewer, target, ClearScope.ALL)).open());
    }

    @Override
    protected List<String> helpLines() {
        return services.messages().lines("cosmetics.help-hub").stream().map(MINI::serialize).toList();
    }

    @Override
    public String describe() {
        return "the cosmetics, and what you wear now";
    }
}
