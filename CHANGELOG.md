# Changelog

All notable user-facing changes are documented here.

## [0.0.1] - 2026-07-18

### Added

- Ten-stage cultivation progression from mortal awakening to stage 10.
- Ancient Jade, a centered bilingual in-game guide with stage tasks, searchable chapters, real recipe rendering, and gameplay explanations.
- Per-player Kongqiao and Xianqiao storage with aggregated item counts, continuous scrolling, search, sorting, armor management, embedded crafting, embedded Immortal Furnace, magnet collection, and exact-component hand refill.
- UUID-bound personal Xianqiao realms with stage-dependent borders and dimension-local time gears from freeze to 32x.
- TruePower and ImmortalPower as physical items, fuels, crafting resources, storage balances, and placeable dyeable lights.
- Immortal Furnace with three processing lanes, TruePower/ImmortalPower fuel rules, automatic refill, and JEI/EMI recipe categories.
- Source Veins, Source Vein Manager, Xianqiao Manager, and configurable Xianqiao Interface automation.
- Spirit Instrument with Explore, Wrench, Pick, and Build modes.
- Spirit Sword with stage-based damage, Spirit Repair support, anvil enchantment, and repeatable tempering.
- World Shard Miner and Treasure Basin systems driven by world generation and loot rules.
- Tribulation encounters for stages 6-10, including protected failure recovery and configurable targets.
- Spirit Sage villager profession, pills, ores, storage blocks, enchantments, effects, recipes, loot injection, and administrator commands.
- Optional JEI, EMI, AE2, Refined Storage, Mekanism, Botania, Ars Nouveau, Flux Networks, Industrial Foregoing Souls, and related capability integrations isolated behind mod-presence checks.
- NeoForge Mod List configuration UI with complete Simplified Chinese and English labels/tooltips.

### Fixed

- Nested NeoForge configuration pages now retain full translation paths instead of displaying raw `cultivation.configuration...` keys.
- Advancement Weakness is registered and displayed as a real timed harmful effect with its own icon.
- Stage 10 defaults to finite generation of 256 ImmortalPower every 20 ticks; optional inexhaustible behavior is disabled by default.
- High-speed personal realms no longer invalidate terminal item interactions merely because displayed amounts changed.
- Storage, furnace, source, interface, armor, recipe-viewer, villager, tribulation, and persistent stage-effect regressions found during real-client QA.

### Compatibility

- Minecraft: 1.21.1
- Loader: NeoForge 21.1.235
- Java: 21
- Release version: 0.0.1

