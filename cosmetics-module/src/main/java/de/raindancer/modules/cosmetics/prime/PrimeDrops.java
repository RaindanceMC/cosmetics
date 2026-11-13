package de.raindancer.modules.cosmetics.prime;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * The monthly Prime-only drops: each month names the cosmetics nodes it unlocks. Whoever was Prime for any part of a
 * month (Core's {@code PrimeMembers.months}) wears that month's drop whenever they are Prime — a month they did not
 * pay for stays locked for good.
 *
 * <pre>
 * drops:
 *   "2026-11":
 *     title: Harbor Sunset
 *     unlocks: [rainscosmetics.preset.harbor-sunset]
 * </pre>
 */
public record PrimeDrops(SortedMap<YearMonth, Drop> drops) {

    public record Drop(YearMonth month, String title, List<String> unlocks) {
    }

    public static final PrimeDrops NONE = new PrimeDrops(new TreeMap<>());

    /** Only cosmetics' own nodes — a typo or a pasted "*" must never become a staff power. */
    private static final String ALLOWED_PREFIX = "rainscosmetics.";
    private static final Set<String> NEVER = Set.of("rainscosmetics.admin", "rainscosmetics.clear.others",
            "rainscosmetics.free", "rainscosmetics.skins.all");

    public PrimeDrops {
        drops = java.util.Collections.unmodifiableSortedMap(new TreeMap<>(drops));
    }

    public static PrimeDrops read(YamlConfiguration yaml, Consumer<String> warn) {
        SortedMap<YearMonth, Drop> drops = new TreeMap<>();
        ConfigurationSection section = yaml.getConfigurationSection("drops");
        if (section == null) {
            return NONE;
        }
        for (String key : section.getKeys(false)) {
            YearMonth month;
            try {
                month = YearMonth.parse(key.trim());
            } catch (DateTimeParseException notAMonth) {
                warn.accept("prime-drops: '" + key + "' is not a month like 2026-11 — skipped.");
                continue;
            }
            ConfigurationSection drop = section.getConfigurationSection(key);
            if (drop == null) {
                continue;
            }
            List<String> unlocks = drop.getStringList("unlocks").stream().map(String::trim)
                    .filter(node -> {
                        boolean fine = node.startsWith(ALLOWED_PREFIX) && !node.contains("*") && !NEVER.contains(node)
                                && !de.raindancer.modules.cosmetics.creator.CreatorPerks.neverDropped().contains(node);
                        if (!fine) {
                            warn.accept("prime-drops " + key + ": '" + node + "' is not a cosmetic — left out.");
                        }
                        return fine;
                    }).toList();
            String title = drop.getString("title", "");
            drops.put(month, new Drop(month, title.isBlank() ? defaultTitle(month) : title.trim(), unlocks));
        }
        return new PrimeDrops(drops);
    }

    static String defaultTitle(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();
    }

    public Set<YearMonth> months() {
        return drops.keySet();
    }

    public Optional<Drop> of(YearMonth month) {
        return Optional.ofNullable(drops.get(month));
    }

    public Set<String> nodesFor(SortedSet<YearMonth> paid) {
        Set<String> nodes = new LinkedHashSet<>();
        for (YearMonth month : paid) {
            Drop drop = drops.get(month);
            if (drop != null) {
                nodes.addAll(drop.unlocks());
            }
        }
        return nodes;
    }

    /** What a fresh network starts with: the first two months' presets, shipped in the catalogue. */
    public static final String SHIPPED = """
            # Prime-only drops, one per month. Whoever was Prime for any part of a month keeps that month's
            # drop and wears it whenever they are Prime; a month they did not pay for stays locked.
            # unlocks: rainscosmetics.* nodes only — a restricted preset (rainscosmetics.preset.<id>, see the
            # presets in config.yml), a weapon/armour skin (rainscosmetics.skin.<id>), the coffee trail …
            drops:
              "2026-11":
                title: Harbor Sunset
                unlocks: [rainscosmetics.preset.harbor-sunset]
              "2026-12":
                title: First Frost
                unlocks: [rainscosmetics.preset.first-frost]
            """;
}
