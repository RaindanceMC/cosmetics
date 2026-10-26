package de.raindancer.modules.cosmetics.skins;

import de.raindancer.core.RainsCore;
import de.raindancer.core.platform.permission.NumberedLimit;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.modules.cosmetics.SkinSettings;
import de.raindancer.modules.cosmetics.util.PermissionNodes;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** Weapon skins for one server: who owns which, putting them on, and keeping every skinned item looking right. */
public final class SkinService {

    public enum Outcome { WORN, REMOVED, LOCKED, SOMEBODY_ELSES, NOT_A_WEAPON, NOT_FOR_THIS_WEAPON, NONE_LEFT, OFF }

    private final Plugin plugin;
    private final Server server;
    private final RainsCore core;
    private final SkinCatalogue catalogue;
    private final SkinUnlocks unlocks;
    private final Supplier<SkinSettings> settings;
    private final SecureRandom random = new SecureRandom();

    public SkinService(Plugin plugin, Server server, RainsCore core, SkinCatalogue catalogue, SkinUnlocks unlocks,
                       Supplier<SkinSettings> settings) {
        this.plugin = plugin;
        this.server = server;
        this.core = core;
        this.catalogue = catalogue;
        this.unlocks = unlocks;
        this.settings = settings;
    }

    public SkinCatalogue catalogue() {
        return catalogue;
    }

    public SkinSettings settings() {
        return settings.get();
    }

    public boolean mayUse(Player who) {
        return settings.get().enabled() && who.hasPermission(PermissionNodes.SKINS);
    }

    public boolean isOn(String skin) {
        return !settings.get().switchedOff().contains(skin) && catalogue.byId(skin).isPresent();
    }

    /** Unlocked with what they gave, or given by permission. */
    public boolean owns(Player who, String skin) {
        return unlocks.has(who.getUniqueId(), skin) || who.hasPermission(PermissionNodes.SKINS_ALL)
                || who.hasPermission(PermissionNodes.SKIN_PREFIX + skin);
    }

    /** For an item whose owner may be away: only a permission could say more, and only to somebody here. */
    SkinRules.Owns owns(UUID owner, String skin) {
        if (unlocks.has(owner, skin)) {
            return SkinRules.Owns.YES;
        }
        Player online = server.getPlayer(owner);
        if (online == null) {
            return SkinRules.Owns.UNKNOWN;
        }
        return owns(online, skin) ? SkinRules.Owns.YES : SkinRules.Owns.NO;
    }

    public int unlocksAllowed(Player who) {
        var supporter = core.supporters().of(who.getUniqueId());
        long cents = supporter.map(s -> settings.get().countAfterFees() ? s.netCents() : s.cents()).orElse(0L);
        int bonus = NumberedLimit.of(who, PermissionNodes.SKIN_UNLOCKS_PREFIX, null).highestOf(0);
        long active = core.playtime() == null ? 0 : core.playtime().activeMinutes(who.getUniqueId());
        SkinSettings now = settings.get();
        double loyalty = supporter.map(s -> SkinRules.loyalty(s.netCents(), s.supports(), s.member(),
                now.loyaltyAtEuros(), now.loyaltyMaxAtEuros(), now.fastestMinutes(), now.loyaltyPerRepeat() / 100.0, 0.25))
                .orElse(1.0);
        // The owner's multiplier goes on top, still never past the fastest rate.
        double rate = Math.min(SkinRules.PLAYTIME_MINUTES_PER_SKIN / (double) Math.max(1, now.fastestMinutes()),
                now.multiplier() * loyalty);
        int played = SkinRules.playtimeUnlocks(active, rate, supporter.isPresent());
        int given = SkinRules.unlocksAllowed(cents, settings.get().centsPerSkin(), bonus, supporter.isPresent());
        return given == Integer.MAX_VALUE ? given : (int) Math.min(Integer.MAX_VALUE, (long) given + played);
    }

    public int unlocksLeft(Player who) {
        int allowed = unlocksAllowed(who);
        return allowed == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, allowed - unlocks.count(who.getUniqueId()));
    }

    public int unlockedCount(Player who) {
        return unlocks.count(who.getUniqueId());
    }

    /** Unlocks one skin for good, if they have an unlock left. */
    public Outcome unlock(Player who, String skin) {
        if (!mayUse(who)) {
            return Outcome.OFF;
        }
        if (!isOn(skin) || owns(who, skin)) {
            return Outcome.OFF;
        }
        if (unlocksLeft(who) <= 0) {
            return Outcome.NONE_LEFT;
        }
        unlocks.unlock(who.getUniqueId(), skin, Instant.now());
        Scheduling.async(plugin, unlocks::save);
        return Outcome.WORN;
    }

    /** Up to {@code count} skins they do not have yet, chosen at random, each as likely as any other. */
    public List<String> unlockRandom(Player who, int count) {
        if (!mayUse(who)) {
            return List.of();
        }
        int take = Math.min(count, unlocksLeft(who));
        Set<String> owned = new HashSet<>();
        for (SkinCatalogue.Skin skin : catalogue.all()) {
            if (owns(who, skin.id())) {
                owned.add(skin.id());
            }
        }
        List<String> pool = catalogue.all().stream().map(SkinCatalogue.Skin::id).filter(this::isOn).toList();
        List<String> picked = SkinRules.pickRandom(pool, owned, take, random);
        picked.forEach(skin -> unlocks.unlock(who.getUniqueId(), skin, Instant.now()));
        if (!picked.isEmpty()) {
            Scheduling.async(plugin, unlocks::save);
        }
        return picked;
    }

    /** Puts {@code skin} on what they hold in their main hand. */
    public Outcome wear(Player who, String skin) {
        ItemStack held = who.getInventory().getItemInMainHand();
        String item = held.getType().getKey().getKey();
        if (SkinCatalogue.kindOf(item).isEmpty()) {
            return Outcome.NOT_A_WEAPON;
        }
        if (!mayUse(who) || !isOn(skin)) {
            return Outcome.OFF;
        }
        Optional<String> look = catalogue.byId(skin).flatMap(s -> s.lookFor(item));
        if (look.isEmpty()) {
            return Outcome.NOT_FOR_THIS_WEAPON;
        }
        switch (SkinRules.apply(SkinItems.tagOf(held), who.getUniqueId(), owns(who, skin))) {
            case LOCKED -> {
                return Outcome.LOCKED;
            }
            case SOMEBODY_ELSES -> {
                return Outcome.SOMEBODY_ELSES;
            }
            default -> {
            }
        }
        SkinItems.tag(held, new SkinTag(skin, who.getUniqueId()));
        refresh(held, who.getUniqueId());
        who.getInventory().setItemInMainHand(held);
        return Outcome.WORN;
    }

    /** Takes the skin off what they hold — only their own. */
    public Outcome takeOff(Player who) {
        ItemStack held = who.getInventory().getItemInMainHand();
        Optional<SkinTag> tag = SkinItems.tagOf(held);
        if (tag.isEmpty()) {
            return Outcome.NOT_A_WEAPON;
        }
        if (!tag.get().owner().equals(who.getUniqueId())) {
            return Outcome.SOMEBODY_ELSES;
        }
        SkinItems.apply(held, SkinRules.Look.HIDE);
        SkinItems.untag(held);
        who.getInventory().setItemInMainHand(held);
        return Outcome.REMOVED;
    }

    /**
     * Brings one item's look up to date.
     *
     * @param holder who carries it, null in a container or on the ground
     * @return whether the stack changed
     */
    public boolean refresh(ItemStack stack, UUID holder) {
        Optional<SkinTag> tag = SkinItems.tagOf(stack);
        if (tag.isEmpty()) {
            return false;
        }
        SkinSettings now = settings.get();
        String item = stack.getType().getKey().getKey();
        String look = catalogue.byId(tag.get().skin()).flatMap(s -> s.lookFor(item)).orElse(null);
        SkinRules.Look decision = new SkinRules.Visibility(now.hideWithOthers(), now.showOutside()).decide(tag.get(),
                holder, owns(tag.get().owner(), tag.get().skin()), now.enabled() && isOn(tag.get().skin()), look,
                SkinItems.currentModel(stack));
        return SkinItems.apply(stack, decision);
    }

    /** Every skinned item a player carries. On their own thread. */
    public void refresh(Player player) {
        refresh(player.getInventory(), player.getUniqueId());
    }

    /** Every skinned item in an inventory; {@code holder} null for a container. */
    public void refresh(Inventory inventory, UUID holder) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack != null && refresh(stack, holder)) {
                inventory.setItem(slot, stack);
            }
        }
    }

    public void refreshEveryone() {
        for (Player online : server.getOnlinePlayers()) {
            Scheduling.entity(plugin, online, () -> refresh(online));
        }
    }
}
