package de.raindancer.modules.cosmetics.prime;

import de.raindancer.core.social.support.PrimePermission;
import de.raindancer.core.social.support.SupporterPermission;
import de.raindancer.modules.cosmetics.creator.CreatorPerks;
import de.raindancer.modules.cosmetics.util.PermissionNodes;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrimePerksTest {

    private org.mockito.MockedStatic<org.bukkit.Bukkit> bukkit;

    /** Changing a permission tells whoever holds it, which asks the server who that is: nobody. */
    @org.junit.jupiter.api.BeforeEach
    void noServer() {
        bukkit = org.mockito.Mockito.mockStatic(org.bukkit.Bukkit.class);
        org.bukkit.Server server = mock(org.bukkit.Server.class);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        bukkit.when(org.bukkit.Bukkit::getServer).thenReturn(server);
    }

    @org.junit.jupiter.api.AfterEach
    void serverGone() {
        bukkit.close();
    }

    /** A permission manager that remembers what was added, which is all {@link PrimePerks#apply} asks of one. */
    private static PluginManager manager() {
        Map<String, Permission> known = new HashMap<>();
        PluginManager manager = mock(PluginManager.class);
        when(manager.getPermission(anyString())).thenAnswer(call -> known.get(call.<String>getArgument(0)));
        doAnswer(call -> {
            Permission permission = call.getArgument(0);
            known.put(permission.getName(), permission);
            return null;
        }).when(manager).addPermission(any(Permission.class));
        PermissionNodes.declared().forEach(manager::addPermission);
        return manager;
    }

    @Test
    @DisplayName("Prime-only: name colours, particles and teleport looks stop being everybody's")
    void gated() {
        assertThat(PrimePerks.GATED).contains(PermissionNodes.NAME_COLOUR, PermissionNodes.NAME_GRADIENT,
                PermissionNodes.NAME_ANY_COLOUR, PermissionNodes.NAME_ANIMATED, PermissionNodes.PARTICLES,
                PermissionNodes.TELEPORT, PermissionNodes.decoration(net.kyori.adventure.text.format.TextDecoration.BOLD));
        assertThat(PrimePerks.GATED).doesNotContain(PermissionNodes.USE, PermissionNodes.CLEAR,
                PermissionNodes.decoration(net.kyori.adventure.text.format.TextDecoration.OBFUSCATED));
    }

    @Test
    @DisplayName("Prime gets the gated ones, every skin, the coffee trail and the flair; with Prime-only off, only the flair")
    void holders() {
        assertThat(PrimePerks.forPrime(true)).containsAll(PrimePerks.GATED)
                .contains(PermissionNodes.SKINS, PermissionNodes.SKINS_ALL, PermissionNodes.PARTICLE_COFFEE,
                        PermissionNodes.FLAIR);
        assertThat(PrimePerks.forPrime(false)).as("flair is new, so it is Prime's whether or not the rest is")
                .containsExactly(PermissionNodes.FLAIR);
    }

    @Test
    @DisplayName("staff get the gated ones and the flair, so they can try them; the coffee trail is theirs as operators")
    void staff() {
        assertThat(PrimePerks.forPerksNode()).containsAll(PrimePerks.GATED).contains(PermissionNodes.FLAIR)
                .doesNotContain(PermissionNodes.CREATOR);
    }

    @Test
    @DisplayName("Prime-only takes skins and the coffee trail away from coffee supporters; off, they keep them")
    void supporters() {
        assertThat(PrimePerks.supporterKeepsSkins(true)).isFalse();
        assertThat(PrimePerks.supporterKeepsSkins(false)).isTrue();
        assertThat(PrimePerks.supporterKeepsCoffee(true)).isFalse();
        assertThat(PrimePerks.supporterKeepsCoffee(false)).isTrue();
    }

    @Test
    @DisplayName("Prime-only on: the coffee trail is a child of rains.prime and no longer of rains.supporter")
    void coffeeBecomesPrimes() {
        PluginManager manager = manager();
        SupporterPermission.permission(manager).getChildren().put(PermissionNodes.PARTICLE_COFFEE, true);

        PrimePerks.apply(manager, true);

        assertThat(PrimePermission.permission(manager).getChildren()).containsEntry(PermissionNodes.PARTICLE_COFFEE, true);
        assertThat(SupporterPermission.permission(manager).getChildren())
                .doesNotContainKeys(PermissionNodes.PARTICLE_COFFEE, PermissionNodes.SKINS);
    }

    @Test
    @DisplayName("Prime-only off: the coffee trail is every supporter's again, as it always was, and not Prime's")
    void coffeeStaysTheSupporters() {
        PluginManager manager = manager();
        PrimePerks.apply(manager, true);

        PrimePerks.apply(manager, false);

        assertThat(SupporterPermission.permission(manager).getChildren())
                .containsEntry(PermissionNodes.PARTICLE_COFFEE, true).containsEntry(PermissionNodes.SKINS, true);
        assertThat(PrimePermission.permission(manager).getChildren()).doesNotContainKey(PermissionNodes.PARTICLE_COFFEE)
                .containsEntry(PermissionNodes.FLAIR, true);
    }

    @Test
    @DisplayName("the creator node is never a child of Prime, supporters, staff or free — in either mode")
    void creatorIsNobodyElsesPerk() {
        for (boolean primeOnly : new boolean[]{true, false}) {
            PluginManager manager = manager();
            PrimePerks.apply(manager, primeOnly);
            for (String parent : new String[]{PrimePermission.NODE, SupporterPermission.NODE, PrimePerks.PERKS,
                    PermissionNodes.FREE, PermissionNodes.LIBRARY}) {
                assertThat(manager.getPermission(parent).getChildren()).as(parent + " primeOnly=" + primeOnly)
                        .doesNotContainKey(PermissionNodes.CREATOR)
                        .doesNotContainKeys(CreatorPerks.presetNodes().toArray(String[]::new));
            }
        }
    }
}
