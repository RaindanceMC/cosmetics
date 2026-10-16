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
