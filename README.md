# RaindanceMC/cosmetics

- **`cosmetics-module`** — Rain's Flexible Modules — Cosmetics: Cosmetics players pick for themselves. First: their own name, painted in a colour, a gradient or a preset, with decorations. Stored in Core's identities, so chat, the tablist and nicknames all show it.
- **`cosmetics-standalone`** — Rain's Cosmetics: Rain's Cosmetics as a plugin of its own: the cosmetics module and the standard wrapper, shaded together.

Part of [RaindanceMC](https://github.com/RaindanceMC); built on [RainsCore](https://github.com/RaindanceMC/RainsCore) and the shared parent in [framework](https://github.com/RaindanceMC/framework). Split out of `rains-flexible-modules` on 16.10.2026, history kept.

## Build

```
mvn install
```

Needs Java 25 and read access to `packages.tstieh.de` (server ids `packages-releases` / `packages-snapshots` in `~/.m2/settings.xml`).

## Plugins

- `cosmetics-standalone/target/RainsCosmetics-0.18.1.jar` — also published as `de.raindancer.modules:cosmetics-standalone:0.18.1`

## Versions

A module's version is a property in this repository's root `pom.xml`, a plugin's is `plugin.version` in its own pom. CI publishes every version that is not in the registry yet on the next push to master.

## Prime, creators and flair (0.29.0)

- With `prime.only` on, everyday cosmetics, skins and the **coffee-bean trail** are Prime's (`rains.prime`); off, the
  trail stays every supporter's.
- `rainscosmetics.creator` (default false, a child of nothing): the Twitch trail (`TWITCH`, Core's own particle), the
  Twitch name presets `twitch` / `twitch-glitch`, and the Twitch options on the flair page.
- `rainscosmetics.flair` (child of `rains.prime` and of staff's `rainscosmetics.perks`): the stardust trail, a tag
  (`/cosmetics flair tag|colour`), join and leave lines (`join`), kill and death effects (`kill`, `death`). Looks only.
  Join/leave lines are said through Core's `JoinLines`, which essentials' welcome asks.
- **Creator skins (0.32.0):** seven existing skins are creator-only (`creator: true`, category Twitch, `rainscosmetics.creator`
  alone; not unlocks, not `skins.all`, not Prime): Shadowstone Tools, Dragon Blade, Glacial Gear and the armour sets Purple
  Diamond, Sakura Netherite, Valkyrian Netherite, Modern Netherite; since 0.38.0 also Ametrine Netherite, Cherry Diamond
  and VanillaFrost Diamond. `art/skins/creator.py` sets the flags; run it after `convert.py`/`armour.py`/
  `shields.py`.
- **Prime drop skins (0.38.0):** `"drop": "<month>"` in skins.json; only `rainscosmetics.skin.<id>` opens one (which
  prime-drops.yml gives that month's Prime members), not `skins.all`, not unlocks. 2027-01 and 2027-02 are shields
  (`art/skins/shields.py`: vanilla's shield renderer takes no texture, so a skin swaps in a cuboid model; banner
  patterns do not show on it), 2027-03 three armour sets. `art/skins/drops.py` sets the months and prints the
  prime-drops.yml entries; `PrimeDrops.SHIPPED` has them for fresh networks, a running network adds them by hand.
  `armour.py` now keeps the skins of packs that are not downloaded.
- **Prime sounds (0.33.0):** `/cosmetics flair sound <amethyst|bell|starfall|harp|off>` — a chime where you join, heard within
  about 32 blocks; `flair teleport <amethyst|bell|starfall|harp|allay|off>` — your departure and arrival sound (your own
  picks on the teleport page win). Creators have the extra join chime `twitch` (Going live).
