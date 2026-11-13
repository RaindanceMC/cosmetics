package de.raindancer.modules.cosmetics.flair;

import de.raindancer.core.ui.identity.JoinLines;
import de.raindancer.core.ui.identity.NameBadges;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.cosmetics.util.PermissionNodes;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlairServiceTest {

    private final Map<UUID, FlairChoice> kept = new HashMap<>();
    private final List<UUID> redrawn = new ArrayList<>();
    private final Messages messages = mock(Messages.class);
    private final FlairService flair = new FlairService(new FlairStore() {
        @Override
        public FlairChoice read(Player player) {
            return kept.getOrDefault(player.getUniqueId(), FlairChoice.EMPTY);
        }

        @Override
        public void write(Player player, FlairChoice choice) {
            kept.put(player.getUniqueId(), choice);
        }
    }, redrawn::add, messages);

    private static Player player(String name, String... nodes) {
        Player player = mock(Player.class);
        UUID id = UUID.nameUUIDFromBytes(name.getBytes());
        Set<String> held = new HashSet<>(List.of(nodes));
        when(player.getUniqueId()).thenReturn(id);
        when(player.hasPermission(anyString())).thenAnswer(call -> held.contains(call.<String>getArgument(0)));
        return player;
    }

    @BeforeEach
    void setUp() {
        NameBadges.clear();
        JoinLines.provider(null);
        flair.install();
    }

    @AfterEach
    void tearDown() {
        flair.uninstall();
        NameBadges.clear();
    }

    private static String chat(UUID who) {
        return PlainTextComponentSerializer.plainText().serialize(NameBadges.before(who, NameBadges.Place.CHAT));
    }

    @Test
    @DisplayName("Prime picks a tag and a colour: it shows in chat and the name is redrawn")
    void primePicksATag() {
        Player prime = player("prime", PermissionNodes.FLAIR);

        assertThat(flair.choose(prime, FlairCategory.TAG, "star")).isTrue();
        assertThat(flair.choose(prime, FlairCategory.COLOUR, "rose")).isTrue();

        assertThat(chat(prime.getUniqueId())).isEqualTo("✦ ");
        assertThat(redrawn).contains(prime.getUniqueId());
        assertThat(kept.get(prime.getUniqueId()).format()).isEqualTo("tag=star;colour=rose");
        verify(messages, atLeastOnce()).send(eq(prime), eq("cosmetics.flair.set"), any(Object[].class));
    }

    @Test
    @DisplayName("the tag is a wearable one, so the Tags page can take it off")
    void tagIsWearable() {
        assertThat(NameBadges.wearables()).extracting(NameBadges.Wearable::owner).contains(FlairService.BADGE);
    }

    @Test
    @DisplayName("somebody without Prime is refused a Prime option, told so, and nothing is kept")
    void refusedWithoutPrime() {
        Player plain = player("plain");

        assertThat(flair.choose(plain, FlairCategory.KILL, "lightning")).isFalse();

        assertThat(kept).isEmpty();
        verify(messages).send(eq(plain), eq("cosmetics.flair.refused-prime"), any(Object[].class));
    }

    @Test
    @DisplayName("a Twitch option is refused to Prime, and to everybody else, with the creator wording")
    void refusedWithoutCreator() {
        Player prime = player("prime2", PermissionNodes.FLAIR);

        assertThat(flair.choose(prime, FlairCategory.KILL, "glitch")).isFalse();

        verify(messages).send(eq(prime), eq("cosmetics.flair.refused-creator"), any(Object[].class));
        assertThat(kept).isEmpty();
    }

    @Test
    @DisplayName("a creator, who is not Prime, may wear the Twitch extras and has an effect for kills")
    void creatorWearsTwitchExtras() {
        Player creator = player("creator", PermissionNodes.CREATOR);

        assertThat(flair.choose(creator, FlairCategory.TAG, "play")).isTrue();
        assertThat(flair.choose(creator, FlairCategory.COLOUR, "twitch")).isTrue();
        assertThat(flair.choose(creator, FlairCategory.KILL, "glitch")).isTrue();
        assertThat(flair.choose(creator, FlairCategory.DEATH, "ghost")).isFalse();

        assertThat(chat(creator.getUniqueId())).isEqualTo("▶ ");
        assertThat(flair.killEffect(creator.getUniqueId())).isPresent();
        assertThat(flair.deathEffect(creator.getUniqueId())).isEmpty();
    }

    @Test
    @DisplayName("an option nobody has heard of is said not to exist")
    void unknown() {
        Player prime = player("prime3", PermissionNodes.FLAIR);

        assertThat(flair.choose(prime, FlairCategory.JOIN, "nonsense")).isFalse();

        verify(messages).send(eq(prime), eq("cosmetics.flair.unknown"), any(Object[].class));
    }

    @Test
    @DisplayName("off takes one category off and leaves the rest")
    void off() {
        Player prime = player("prime4", PermissionNodes.FLAIR);
        flair.choose(prime, FlairCategory.TAG, "crown");
        flair.choose(prime, FlairCategory.KILL, "hearts");

        assertThat(flair.choose(prime, FlairCategory.TAG, "off")).isTrue();

        assertThat(chat(prime.getUniqueId())).isEmpty();
        assertThat(flair.killEffect(prime.getUniqueId())).isPresent();
        verify(messages).send(eq(prime), eq("cosmetics.flair.taken-off"), any(Object[].class));
    }

    @Test
    @DisplayName("their join and leave line goes to whoever says them, and only for them")
    void joinLines() {
        Player prime = player("prime5", PermissionNodes.FLAIR);
        flair.choose(prime, FlairCategory.JOIN, "royal");

        assertThat(JoinLines.custom(prime.getUniqueId(), JoinLines.Kind.JOIN)).isPresent()
                .hasValueSatisfying(line -> assertThat(line).contains("<player>"));
        assertThat(JoinLines.custom(prime.getUniqueId(), JoinLines.Kind.QUIT)).isPresent();
        assertThat(JoinLines.custom(UUID.randomUUID(), JoinLines.Kind.JOIN)).isEmpty();
    }

    @Test
    @DisplayName("when Prime runs out, what they picked stops showing at the next look, and is taken out of their data")
    void primeRunsOut() {
        Set<String> held = new HashSet<>(Set.of(PermissionNodes.FLAIR));
        Player prime = mock(Player.class);
        UUID id = UUID.nameUUIDFromBytes("expiring".getBytes());
        when(prime.getUniqueId()).thenReturn(id);
        when(prime.hasPermission(anyString())).thenAnswer(call -> held.contains(call.<String>getArgument(0)));
        flair.choose(prime, FlairCategory.TAG, "moon");
        flair.choose(prime, FlairCategory.DEATH, "ghost");
        assertThat(chat(id)).isEqualTo("☾ ");

        held.clear();
        flair.revalidate(prime);

        assertThat(chat(id)).isEmpty();
        assertThat(flair.deathEffect(id)).isEmpty();
        assertThat(kept.get(id).isEmpty()).isTrue();
        verify(messages).send(eq(prime), eq("cosmetics.flair.dropped"));
    }

    @Test
    @DisplayName("their data is read when they join, so a choice made on another server is worn here")
    void loadsFromData() {
        Player prime = player("prime6", PermissionNodes.FLAIR);
        kept.put(prime.getUniqueId(), FlairChoice.parse("tag=bolt;death=poof"));

        flair.load(prime);

        assertThat(chat(prime.getUniqueId())).isEqualTo("⚡ ");
        assertThat(flair.deathEffect(prime.getUniqueId())).isPresent();
        verify(messages, never()).send(eq(prime), eq("cosmetics.flair.dropped"));
    }

    @Test
    @DisplayName("a player who left is forgotten")
    void forgets() {
        Player prime = player("prime7", PermissionNodes.FLAIR);
        flair.choose(prime, FlairCategory.TAG, "star");

        flair.forget(prime.getUniqueId());

        assertThat(chat(prime.getUniqueId())).isEmpty();
    }

    @Test
    @DisplayName("a click goes through what they may pick — off first, round the end — and a creator never lands on a Prime option")
    void cycles() {
        Player prime = player("cycler", PermissionNodes.FLAIR);
        assertThat(flair.choices(prime, FlairCategory.KILL)).containsExactly("off", "lightning", "fireworks", "hearts",
                "souls", "confetti");

        flair.cycle(prime, FlairCategory.KILL, false);
        assertThat(flair.effective(prime.getUniqueId()).get(FlairCategory.KILL)).contains("lightning");
        flair.cycle(prime, FlairCategory.KILL, true);
        assertThat(flair.effective(prime.getUniqueId()).get(FlairCategory.KILL)).isEmpty();
        flair.cycle(prime, FlairCategory.KILL, true);
        assertThat(flair.effective(prime.getUniqueId()).get(FlairCategory.KILL)).contains("confetti");

        Player creator = player("cycler2", PermissionNodes.CREATOR);
        assertThat(flair.choices(creator, FlairCategory.KILL)).containsExactly("off", "glitch");
        assertThat(flair.mayOpen(creator)).isTrue();
        assertThat(flair.mayOpen(player("nobody"))).isFalse();
    }
}
