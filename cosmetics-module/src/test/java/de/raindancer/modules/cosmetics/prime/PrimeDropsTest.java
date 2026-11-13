package de.raindancer.modules.cosmetics.prime;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class PrimeDropsTest {

    private static PrimeDrops read(String yaml, List<String> warnings) throws Exception {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString(yaml);
        return PrimeDrops.read(file, warnings::add);
    }

    @Test
    @DisplayName("each month lists what it unlocks; a bad month is warned about and skipped")
    void reads() throws Exception {
        List<String> warnings = new ArrayList<>();
        PrimeDrops drops = read("""
                drops:
                  "2026-11":
                    title: Harbor Sunset
                    unlocks: [rainscosmetics.preset.harbor-sunset]
                  "2026-12":
                    unlocks: [rainscosmetics.preset.first-frost, rainscosmetics.skin.icicle]
                  "someday":
                    unlocks: [x]
                """, warnings);

        assertThat(drops.months()).containsExactly(YearMonth.of(2026, 11), YearMonth.of(2026, 12));
        assertThat(drops.of(YearMonth.of(2026, 11)).orElseThrow().title()).isEqualTo("Harbor Sunset");
        assertThat(drops.of(YearMonth.of(2026, 12)).orElseThrow().title()).isEqualTo("December 2026");
        assertThat(warnings).hasSize(1).first().asString().contains("someday");
    }

    @Test
    @DisplayName("somebody gets the drops of the months they paid for, nothing else")
    void nodesFor() throws Exception {
        PrimeDrops drops = read("""
                drops:
                  "2026-11": {unlocks: [rainscosmetics.preset.a]}
                  "2026-12": {unlocks: [rainscosmetics.preset.b, rainscosmetics.preset.c]}
                  "2027-01": {unlocks: [rainscosmetics.preset.d]}
                """, new ArrayList<>());

        assertThat(drops.nodesFor(new TreeSet<>(List.of(YearMonth.of(2026, 11), YearMonth.of(2027, 1)))))
                .containsExactlyInAnyOrder("rainscosmetics.preset.a", "rainscosmetics.preset.d");
        assertThat(drops.nodesFor(new TreeSet<>())).isEmpty();
    }

    @Test
    @DisplayName("an unlock that is not a cosmetics node is refused — a drop must never hand out staff powers")
    void onlyCosmetics() throws Exception {
        List<String> warnings = new ArrayList<>();
        PrimeDrops drops = read("""
                drops:
                  "2026-11": {unlocks: [rainscosmetics.preset.ok, "*", rains.moderation.ban, rainscosmetics.admin]}
                """, warnings);

        assertThat(drops.nodesFor(new TreeSet<>(List.of(YearMonth.of(2026, 11)))))
                .containsExactly("rainscosmetics.preset.ok");
        assertThat(warnings).hasSize(3);
    }
}
