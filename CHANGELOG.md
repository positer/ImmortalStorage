# Changelog

## 0.0.3 - development

- Fixed the Applied Botanics + Ars Nouveau capability-registration interaction through an AppBot-gated Botania official-lookup compatibility shim; the complete integration matrix now loads successfully.
- Bundled Patchouli 1.21.1-93 with NeoForge Jar-in-Jar and made the Ancient Jade handbook the only guide implementation; removed the legacy standalone guide UI, payload and maintenance tests.
- Added the Nurturing Crystal material and budding growth chain, Spirit Iron Nuggets, Substitute Puppet, Miniature Immortal Ruin, Stabilized Miniature Immortal Ruin, Premixed Heavy Compound, and Immortal-Ruin-Forged Spirit Sword.
- Fixed multi-stack Xianqiao Interface pulls through conservation-first transactions and redrew the interface faces with 90-degree rotational symmetry.
- Added deterministic texture generation from vanilla/user-provided source images with source provenance retained under the reference texture directory.
- Added owner-bound Substitute Puppet revival, recharge and respawn-anchor behavior, plus custom activation rendering.
- Added multiplayer-safe server-authoritative ruin fields, stabilized area collection/ejection, redstone gating, configuration persistence, wrench NBT pickup, and explicit NBT reset crafting.

All notable user-facing changes are documented here.

## [0.0.2] - 2026-07-20

### Breaking

- Renamed the public product to ImmortalStorage (仙藏), including mod ID and resource namespace `immortalstorage`, Java packages, network payload identifiers, configuration files, command root, project logo, and release artifact name.
- Old `cultivation` worlds and configurations are intentionally unsupported to avoid duplicate registration and namespace conflicts. Test worlds must be deleted and recreated, and old/new JARs must never be installed together.

### Added

- A rebindable grave-accent special-operation key for Spirit Instrument build-layer removal and server-authoritative Spirit Sword summon/return from the embedded Immortal Furnace.
- Persistent three-lane sword recall reservations that pause only the reserved lane, survive reconnects and restarts, and resume the exact recalled sword without duplication.
- Unified Xianqiao external-resource storage and terminal display for FE, Mekanism chemicals, Botania Mana, Ars Nouveau Source, and Industrial Foregoing Souls.
- Built-in AE2 external resource keys with installed-addon retreat bridges, plus Refined Storage resource types, so Xianqiao resources remain visible without requiring storage-addon mods.
- Building Gadgets 2 copy/paste access to held-player Kongqiao/Xianqiao storage and Create Schematicannon access through the Xianqiao Manager item capability.
- Compact inventory sort/deposit/withdraw controls and R-key sorter-compatible vanilla player slots.
- MEK-style 3x2 adjacent-face previews for interface modes, source modes, and per-cache output masks.

### Changed

- Xianqiao Interface active pull/push switches now control scheduled block automation only; passive pipes use matching caches and per-cache output face masks.
- External caches default to 1,000 units and enforce resource-appropriate server-side limits.
- Fluid and chemical containers configure the exact held container on left click and their contained resource on right click.
- Stage 9/10 uncapped Immortal Pills grant 2,000 ticks of natural ImmortalPower generation instead of restoring 50% of an unlimited cap.
- Beyond Dimensions remains an optional coexistence mod and no longer replaces or disables ImmortalStorage personal storage.

### Fixed

- FE and Mekanism chemical transfers now commit to the authoritative Xianqiao ledger and support both passive pipe access and configured active automation.
- Botania sparks persist across relogging and expose only configured interface Mana caches for extraction while still accepting input to storage.
- Ars Nouveau Dominion Wand binding records the interface without opening its UI; relay endpoint loops are repaired and Source transfer uses configured caches.
- Source Vein inventory rendering restores the base block model and draws the contained resource badge in the lower-right corner.
- External-resource names, chemical colors, animated Mana/Source textures, tooltips, and unified terminal entries now use the corresponding registered resources.

### Verification

- Minecraft: 1.21.1
- Loader: NeoForge 21.1.235
- Java: 21
- Tests: 658 passed
- Release JAR SHA256: `AD20A285B5F25942845642F3E49B472E8FB29CDF427D91E137144DECD468D297`

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
