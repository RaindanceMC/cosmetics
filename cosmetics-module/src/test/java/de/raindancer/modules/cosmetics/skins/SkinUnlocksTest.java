package de.raindancer.modules.cosmetics.skins;

import de.raindancer.core.data.store.Documents;
import de.raindancer.core.data.store.YamlStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SkinUnlocksTest {

    @TempDir
    Path folder;

    private final UUID player = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-26T12:00:00Z");

    private SkinUnlocks unlocks() {
        SkinUnlocks unlocks = new SkinUnlocks(new YamlStore(folder.resolve("skin-unlocks.yml"), null,
                Documents.Scope.NETWORK));
        unlocks.load();
        return unlocks;
    }

    @Test
    @DisplayName("an unlocked skin is theirs, counted once however often it is unlocked")
    void unlock() {
        SkinUnlocks unlocks = unlocks();

        assertThat(unlocks.unlock(player, "katana", now)).isTrue();
        assertThat(unlocks.unlock(player, "katana", now)).isFalse();

        assertThat(unlocks.has(player, "katana")).isTrue();
        assertThat(unlocks.has(player, "scythe")).isFalse();
        assertThat(unlocks.count(player)).isEqualTo(1);
        assertThat(unlocks.of(player)).containsExactly("katana");
    }

    @Test
    @DisplayName("staff can take one back, which frees the unlock")
    void lock() {
        SkinUnlocks unlocks = unlocks();
        unlocks.unlock(player, "katana", now);

        assertThat(unlocks.lock(player, "katana")).isTrue();
        assertThat(unlocks.has(player, "katana")).isFalse();
        assertThat(unlocks.count(player)).isZero();
        assertThat(unlocks.lock(player, "katana")).isFalse();
    }

    @Test
    @DisplayName("unlocks survive a restart, a lock included")
    void persists() {
        SkinUnlocks unlocks = unlocks();
        unlocks.unlock(player, "katana", now);
        unlocks.unlock(player, "scythe", now.plusSeconds(5));
        unlocks.lock(player, "scythe");
        unlocks.save();

        SkinUnlocks reread = unlocks();

        assertThat(reread.of(player)).containsExactly("katana");
    }
}
