package de.raindancer.modules.cosmetics.prime;

import de.raindancer.modules.cosmetics.util.PermissionNodes;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Prime-only mode: what is everybody's on a normal server — a coloured name, particles, teleport looks — becomes a
 * perk of Prime, of staff ({@link #PERKS}, which Moderation seeds into every staff group) and of whoever has
 * {@code rainscosmetics.free} (creators, admins). Weapon and armour skins move from coffee supporters to Prime.
 * Off, everything is as declared in {@link PermissionNodes}.
 *
 * <p>Done by changing the nodes' defaults and parents at runtime, so one setting switches a whole network and no
 * LuckPerms group needs editing for it.
 */
public final class PrimePerks {

    /** Staff's way to the gated cosmetics. Operators by default. */
    public static final String PERKS = "rainscosmetics.perks";

    public static final Set<String> GATED = gated();

    private PrimePerks() {
    }

    private static Set<String> gated() {
        Set<String> nodes = new LinkedHashSet<>(List.of(PermissionNodes.NAME_COLOUR, PermissionNodes.NAME_GRADIENT,
                PermissionNodes.NAME_ANY_COLOUR, PermissionNodes.NAME_ANIMATED, PermissionNodes.PARTICLES,
                PermissionNodes.TELEPORT));
        for (TextDecoration decoration : TextDecoration.values()) {
            if (decoration != TextDecoration.OBFUSCATED) {
                nodes.add(PermissionNodes.decoration(decoration));
            }
        }
        return Set.copyOf(nodes);
    }

    /** The children {@code rains.prime} gets for cosmetics. */
    public static Set<String> forPrime(boolean primeOnly) {
        if (!primeOnly) {
            return Set.of();
        }
        Set<String> nodes = new LinkedHashSet<>(GATED);
        nodes.add(PermissionNodes.SKINS);
        nodes.add(PermissionNodes.SKINS_ALL);
        return nodes;
    }

    public static Set<String> forPerksNode() {
        return GATED;
    }

    public static boolean supporterKeepsSkins(boolean primeOnly) {
        return !primeOnly;
    }

    /** On the global thread. Idempotent; switching back restores the declared defaults. */
    public static void apply(PluginManager manager, boolean primeOnly) {
        Permission prime = de.raindancer.core.social.support.PrimePermission.permission(manager);
        Permission supporter = de.raindancer.core.social.support.SupporterPermission.permission(manager);
        Permission perks = manager.getPermission(PERKS);
        if (perks == null) {
            perks = new Permission(PERKS, "Prime-only cosmetics without Prime — staff", PermissionDefault.OP);
            manager.addPermission(perks);
        }
        Permission free = manager.getPermission(PermissionNodes.FREE);
        List<Permission> touched = new ArrayList<>(List.of(prime, supporter, perks));
        for (String node : GATED) {
            Permission gated = manager.getPermission(node);
            if (gated != null) {
                gated.setDefault(primeOnly ? PermissionDefault.FALSE : PermissionDefault.TRUE);
            }
            if (primeOnly) {
                perks.getChildren().put(node, true);
                if (free != null) {
                    free.getChildren().put(node, true);
                }
            } else {
                perks.getChildren().remove(node);
                if (free != null) {
                    free.getChildren().remove(node);
                }
            }
        }
        for (String node : List.of(PermissionNodes.SKINS, PermissionNodes.SKINS_ALL)) {
            prime.getChildren().remove(node);
        }
        GATED.forEach(prime.getChildren()::remove);
        forPrime(primeOnly).forEach(node -> prime.getChildren().put(node, true));
        if (supporterKeepsSkins(primeOnly)) {
            supporter.getChildren().put(PermissionNodes.SKINS, true);
        } else {
            supporter.getChildren().remove(PermissionNodes.SKINS);
        }
        if (free != null) {
            touched.add(free);
        }
        touched.forEach(Permission::recalculatePermissibles);
    }
}
