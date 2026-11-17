package de.raindancer.modules.cosmetics.screen;

import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.ui.choose.ParticleCatalogue;
import de.raindancer.core.ui.choose.PlayerChooser;
import de.raindancer.core.ui.cosmetic.CosmeticDoor;
import de.raindancer.core.ui.cosmetic.CosmeticDoors;
import de.raindancer.core.data.settings.SettingsMenu;
import de.raindancer.core.ui.dialog.DialogPage;
import de.raindancer.core.ui.dialog.DialogReturn;
import de.raindancer.core.ui.dialog.Faces;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.text.NameStyle;
import de.raindancer.core.ui.text.Text;
import de.raindancer.modules.cosmetics.CosmeticsServices;
import de.raindancer.modules.cosmetics.model.ClearScope;
import de.raindancer.modules.cosmetics.model.ParticleChoice;
import de.raindancer.modules.cosmetics.service.NameStyleService;
import de.raindancer.modules.cosmetics.util.OtherDoors;
import de.raindancer.modules.cosmetics.util.PermissionNodes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code /cosmetics} as the game's own window, like /help: your face and what you wear, then a button for everything
 * that can be worn, this plugin's and every other plugin's door. The pickers stay chest pages; their Back brings this
 * window back, built fresh, so it always says what is worn now.
 */
public final class CosmeticsDialog implements ICosmeticsScreen {

    static final String HEADING = "<gradient:#FFC93C:#C084FC>Cosmetics</gradient>";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** One button: what it says, what it does, and why somebody may not. */
    public record Entry(String label, String tooltip, boolean allowed, String refusal, Runnable press) {

        public static Entry open(String label, String tooltip, Runnable press) {
            return new Entry(label, tooltip, true, null, press);
        }

        public static Entry locked(String label, String tooltip, String refusal, Runnable press) {
            return new Entry(label, tooltip, false, refusal, press);
        }

        /** Another plugin's door, from its icon's name and lines. */
        public static Entry door(String name, List<String> lore, Optional<String> locked, Runnable open) {
            String tooltip = lore.isEmpty() ? null : String.join("\n", lore);
            return locked.map(reason -> locked(name, tooltip, reason, open)).orElseGet(() -> open(name, tooltip, open));
        }
    }

    private final CosmeticsServices services;
    private final Player viewer;

    public CosmeticsDialog(CosmeticsServices services, Player viewer) {
        this.services = services;
        this.viewer = viewer;
    }

    /** The parent for a chest page opened from here, or from another plugin's command: Back shows this window. */
    public static Menu returning(CosmeticsServices services, Player viewer) {
        return new DialogReturn(viewer, services.brand(), "Cosmetics", () -> new CosmeticsDialog(services, viewer).open());
    }

    @Override
    public void open() {
        // The face is fetched first, so the window opens with it rather than redrawing a moment later.
        Faces.load(viewer.getUniqueId()).thenRun(() -> Scheduling.entity(services.plugin(), viewer, () -> {
            if (viewer.isOnline()) {
                page().show(viewer);
            }
        }));
    }

    static DialogPage page(UUID viewer, String name, String wearing, List<Entry> entries) {
        DialogPage page = DialogPage.titled(HEADING).columns(2)
                .face(viewer, () -> Icons.head(viewer, "<white>" + Text.literal(plain(name))),
                        "<white><bold>" + name + "</bold>" + (wearing == null || wearing.isBlank() ? "" : "\n" + wearing));
        for (Entry entry : entries) {
            page.button(entry.allowed(), entry.label(), entry.tooltip(), entry.refusal(), answers -> entry.press().run());
        }
        return page;
    }

    static DialogPage confirmClear(String question, List<String> consequences, Runnable clear, Runnable back) {
        return DialogPage.titled(HEADING, "Clear").text("<white>" + question)
                .text(String.join("\n", consequences))
                .confirm("Yes, take it off", clear, "No, leave it", back);
    }

    /** This window as it is right now. */
    public DialogPage page() {
        UUID id = viewer.getUniqueId();
        NameStyle style = services.names().current(id);
        ParticleChoice particle = services.particles().current(viewer);
        ParticleChoice wings = services.particles().wings().current(viewer);
        Menu here = returning(services, viewer);
        List<Entry> entries = new ArrayList<>();

        entries.add(Entry.open("<white>Name style", "<gray>" + services.names().describe(style)
                + "\n<dark_gray>Colours, gradients and decorations.", () -> new NameStyleMenu(services, viewer, here).open()));
        entries.add(Entry.open("<white>Particles", particle.isNone() ? "<gray>None right now."
                        : "<gray>" + ParticleCatalogue.readable(particle.particle()) + ", "
                        + particle.shape().title().toLowerCase(Locale.ROOT) + ".",
                () -> new ParticleMenu(services, viewer, here).open()));
        entries.add(Entry.open("<white>Wings", (wings.isNone() ? "<gray>None right now."
                        : "<gray>" + wings.shape().title() + ", "
                        + ParticleCatalogue.readable(wings.particle()).toLowerCase(Locale.ROOT) + ".")
                        + "\n<dark_gray>Worn on top of your particle.",
                () -> new ParticleMenu(services, viewer, here, services.particles().wings()).open()));
        entries.add(Entry.open(mini("cosmetics.teleport.menu.door"), mini("cosmetics.teleport.menu.door-lore"),
                () -> new TeleportMenu(services, viewer, here).open()));
        int tags = TagMenu.offered(id).size();
        long worn = TagMenu.offered(id).stream().filter(tag -> services.tags().wears(id, tag.wearable().owner())).count();
        entries.add(Entry.open("<white>Tags", (tags == 0 ? "<gray>None right now." : "<gray>" + worn + " of " + tags
                        + " worn.") + "\n<dark_gray>Your guild's tag and more, in front of your name.",
                () -> new TagMenu(services, viewer, here).open()));
        entries.add(new Entry("<#FFC93C>Flair ✦", "<gray>A tag, your join and leave lines and effects for fights."
                + " Looks only.\n<gray>Prime has them all, creators get Twitch extras.", services.flair().mayOpen(viewer),
                plain(mini("cosmetics.flair.locked")), () -> new FlairMenu(services, viewer, here).open()));
        boolean skinsPrime = services.skins().primeOnly();
        entries.add(new Entry(skinsPrime ? "<#FFC93C>Weapon skins ✦" : "<#E8B98A>Weapon skins ☕",
                (skinsPrime ? "<gray>A Prime perk: new looks for your weapons."
                        : "<gray>A thank-you for supporting the server: new looks for your weapons.")
                        + "\n<dark_gray>Hold a weapon first.", services.skins().mayUse(viewer),
                skinsPrime ? plain(mini("cosmetics.flair.locked")) : "For supporters. See /support",
                () -> new SkinMenu(services, viewer, here).open()));
        for (CosmeticDoor door : CosmeticDoors.doors()) {
            entries.add(doorEntry(door, here));
        }

        boolean sold = services.unlocks().anySold();
        entries.add(new Entry("<white>Unlocks", "<gray>What this server sells: colours, gradients, particles and more.",
                sold, services.unlocks().selling() ? "No price is set yet." : "Cosmetics are not for sale on this server.",
                () -> new UnlockMenu(services, viewer, here).open()));
        if (services.mayOpenSettings(viewer)) {
            entries.add(Entry.open("<white>Server settings", "<gray>Name styles, particles, teleport effects, and the"
                            + " prices with their switch.\n<dark_gray>Only people with the settings permission see this.",
                    () -> new SettingsMenu(viewer, services.brand(), services.core().chatFor(services.brand()),
                            services.core().settingsNavigation(), "cosmetics", here).open()));
        }
        entries.add(new Entry("<white>Clear somebody else's", "<gray>Takes everything they wear off.\n<gray>Pick them"
                + " from the list; you are asked first.", services.clearing().may(viewer, false),
                "Needs " + PermissionNodes.CLEAR_OTHERS,
                () -> new PlayerChooser(viewer, services.brand(), here, "Clear whose cosmetics?", List.of(),
                        person -> askClearing(person.id())).open()));
        boolean mayClear = services.clearing().may(viewer, true);
        boolean wearing = !style.isEmpty() || !particle.isNone() || !wings.isNone() || new OtherDoors().wearing(viewer);
        entries.add(new Entry("<red>Clear my cosmetics", "<gray>Takes off everything you wear: name style, particles,"
                + " chat colour and the rest.\n<gray>Looks on items stay.", mayClear && wearing,
                !mayClear ? "Needs " + PermissionNodes.CLEAR : "You are not wearing anything", this::askClearingMine));

        entries.add(Entry.open("<gray>How this works", "<gray>What each of these is, and the commands.", this::help));

        String painted = MINI.serialize(NameStyleService.painted(viewer.getName(), style));
        String wornNow = particle.isNone() ? "<gray>No particle." : "<gray>Particle: <white>"
                + ParticleCatalogue.readable(particle.particle()) + "</white>.";
        return page(id, painted, wornNow + "\n<dark_gray>Pick what to change. Back on any page comes here.", entries);
    }

    private void help() {
        DialogPage.titled(HEADING, "How this works")
                .text(String.join("\n", services.messages().lines("cosmetics.help-hub").stream().map(MINI::serialize)
                        .toList()))
                .back(this::open).show(viewer);
    }

    private Entry doorEntry(CosmeticDoor door, Menu here) {
        ItemStack icon = door.icon(viewer);
        String name = MINI.serialize(icon.effectiveName());
        List<Component> lore = icon.lore();
        List<String> lines = lore == null ? List.of() : lore.stream().map(MINI::serialize).toList();
        return Entry.door(name, lines, door.locked(viewer), () -> door.open(viewer, here));
    }

    private void askClearingMine() {
        confirmClear("Clear your cosmetics?", List.of("<gray>Your name goes back to plain, your particle is taken off,",
                        "<gray>and so is everything else you wear, like your chat colour.",
                        "<gray>Looks on items stay. What you own stays yours."),
                () -> services.clearing().clear(viewer, viewer, ClearScope.ALL), this::open).show(viewer);
    }

    /** Opened a tick late: the chooser goes back to its parent right after answering, and would cover the question. */
    private void askClearing(UUID who) {
        OfflinePlayer target = services.server().getOfflinePlayer(who);
        String name = target.getName() == null ? who.toString() : target.getName();
        Scheduling.entityLater(services.plugin(), viewer, 1L, () ->
                confirmClear("Clear " + Text.literal(name) + "'s cosmetics?",
                        List.of("<gray>Their name goes back to plain. Their particle and everything",
                                "<gray>else they wear is taken off, if they are here.", "<gray>It goes in the audit log."),
                        () -> services.clearing().clear(viewer, target, ClearScope.ALL), this::open).show(viewer));
    }

    private String mini(String key) {
        return MINI.serialize(services.messages().get(key));
    }

    private static String plain(String mini) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(MINI.deserialize(mini));
    }

    @Override
    public String describe() {
        return "the cosmetics, and what you wear now";
    }
}
