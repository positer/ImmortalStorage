# Changelog

## [0.0.3] - 2026-07-21

This section describes user-visible changes from the published `0.0.2` tag to `0.0.3`.

### Breaking

- Renamed the public product from Cultivation to ImmortalStorage (仙藏). The mod ID and resource namespace are now `immortalstorage`; Java packages, network payload IDs, configuration filenames, command root, mixin configurations, data-pack paths, and the release artifact name use the same identity.
- Existing `cultivation` worlds and configurations are not migrated. Remove every old `cultivation-*.jar`, delete test worlds created with the old namespace, and do not install the old and new builds together.

### Added

- Bundled Patchouli 1.21.1-93 through NeoForge Jar-in-Jar. Ancient Jade now opens one bilingual Patchouli handbook with progression, storage, automation, equipment, compatibility, and real recipe pages; players no longer need to install Patchouli separately.
- Added the Nurturing Crystal progression chain: Spirit Iron Nuggets, inactive and active Nurturing Crystal Bedrock, four crystal-growth stages, fortune-aware mature-cluster drops, Premixed Heavy Compound, and Immortal Furnace processing into a Heavy Core.
- Added the owner-bound Substitute Puppet. It can be recharged with Nurturing Crystals, activates from the player inventory, Kongqiao, or Xianqiao storage, consumes one of sixteen durability points per rescue, reproduces a totem-style activation, and may teleport the owner to a bound Respawn Anchor.
- Added the Miniature Immortal Ruin. Its server-authoritative 13x13 horizontal field can attract or repel entities, apply configurable damage, optionally affect players, and switch between multiple force strengths including teleport mode.
- Added the Stabilized Miniature Immortal Ruin with a 54-slot internal inventory, configurable 3D collection region and offset, adjustable execution interval, wireframe preview, item collection, reversed item ejection, sided automation, persistent wrench pickup, Silk Touch reset behavior, and a crafting recipe that clears stored configuration.
- Added the Immortal-Ruin-Forged Spirit Sword smithing upgrade. It preserves enchantments and tempering, increases each tempering point to 1.5% damage, expands sweep range by 50%, and spends five ImmortalPower to pull targets in a 13x13x13 area to the wielder.
- Added a common configuration option controlling whether the forged sword can affect other players. Successfully teleported targets have their velocity cleared and receive a one-second maximum-strength movement restraint.
- Personal Xianqiao realms now support vanilla beds. A bed no longer explodes, records the exact UUID-bound personal dimension as the player's respawn target, and causes the dynamic level to be restored before later respawn resolution.
- Assigned Common, Uncommon, Rare, and Epic vanilla rarity tiers to every ImmortalStorage item and block item according to recipe complexity, ingredient scarcity, progression stage, and functional power.

### Changed

- Ancient Jade no longer maintains the separate custom guide screen, guide payload, pagination/search implementation, or duplicate guide content; Patchouli is the sole handbook implementation.
- Source Vein, Stabilized Miniature Immortal Ruin, and Xianqiao Manager inventory previews now use NeoForge's standard item-decoration stage. Their dynamic badge/core rendering is shared by the player inventory, creative inventory, ImmortalStorage terminal, JEI, EMI, and hotbar while remaining below vanilla counts, durability, and tooltips.
- The terminal's sort, deposit-all, and filtered-withdraw icons were reduced to half-size artwork with compact spacing while retaining their interaction areas and hover descriptions.
- Substitute Puppet Respawn Anchor bindings are cleared when the bound anchor is mined or destroyed by an explosion. Stale bindings are also repaired during login, inventory ticks, and immediately before activation, including puppets stored in Kongqiao or Xianqiao.
- Miniature Immortal Ruin interaction rules are separated by held item: empty-hand use changes its reversed state, the Spirit Instrument wrench opens configuration, and Shift-wrench dismantling preserves the stabilized ruin's data.
- Stabilized ruin configuration uses synchronized numeric fields and +/- controls for size, offset, and interval. Boolean states use lit/unlit presentation, and the obsolete redstone-mode control was removed from the finalized interface.
- Spirit Sword and special-function tooltips now expose their real combat, tempering, teleport, and restraint behavior. Substitute Puppet owner display refreshes to the current player name while ownership remains UUID-based.
- Spirit Sword and Immortal-Ruin-Forged Spirit Sword now share one extensible weapon attack projection. Payable Yuan damage and tempering growth are written into the standard main-hand attack attribute before external attribute multipliers instead of being dealt as a separate damage instance.
- Item and block presentation was redrawn for the new progression content, including Nurturing Crystal growth, the black/white reversible ruin core, the crying-obsidian stabilized frame, the forged sword, and corrected transparent crystal render layers.

### Fixed

- Fixed Applied Botanics and Ars Nouveau startup interactions by initializing the Botania official capability lookup only when both AppBot and Botania are installed, and by making the later Botania registration idempotent.
- Fixed AE2 installed-addon bridges so Applied Flux, Applied Mekanistics, Ars Energistique, and Applied Botanics keys map onto the same authoritative Xianqiao ledger without duplicate terminal rows or double accounting; built-in keys remain fallback readers when an addon is absent.
- Fixed Ars Nouveau relay endpoint loops: rebinding an interface clears a conflicting opposite endpoint, and old same-position loops repair themselves on the next relay tick.
- Fixed Mekanism chemical transfer paths to accept the pipe API's typed and full-stack insertion/extraction calls, preserve each chemical's registered identity/color/name, and push into ordinary connected pipe networks from authorized interface faces.
- Fixed Xianqiao Interface pull semantics for items, fluids, FE, Mekanism chemicals, Botania Mana, Ars Source, and Industrial Foregoing Souls. Incoming resources commit to the authoritative personal ledger without requiring a preconfigured output cache; active block automation and passive capability access no longer incorrectly disable one another.
- Fixed multi-stack Xianqiao Interface pulls with conservation-first transactions so successful partial transfers cannot duplicate or delete items.
- Fixed interface cache configuration so left click records the exact fluid/chemical container item while right click records its contained resource; per-slot face selection supports repeated multi-face edits and server-side resource-family limits.
- Fixed Source Vein output badges and animated ruin/manager cores being visible in the hotbar but clipped or misplaced in inventories. Decorators now inherit the caller's final screen scissor instead of applying untransformed slot-local scissors.
- Fixed Source Vein item previews to retain their native block model and place the contained item or bucket badge in the lower-right corner without covering the base model or vanilla stack count.
- Fixed ruin-core overlay tinting, depth behavior, and reversed colors. The core now uses a solid black/white body with an occlusion-respecting opposite-color outline in world and item rendering.
- Fixed Nurturing Crystal buds and clusters rendering transparent pixels as black by explicitly using the cutout render type.
- Fixed Stabilized Miniature Immortal Ruin collection/ejection persistence, server/client setting synchronization, blocked-target handling, and NBT preservation when dismantled with the wrench.
- Fixed the Ancient Jade Heavy Core page to reference the vanilla `minecraft:heavy_core` item and corrected stale range descriptions to the implemented 13-block limits.
- Fixed the World Shard Miner excluding generated ores whose mods use a custom ore feature configuration. The scanner now reads structurally compatible target states and live supplier-backed vein sizes from final server worldgen; Mekanism osmium, fluorite, lead, tin, and uranium ores are discovered without hardcoded mod or ore IDs.
- Fixed client configuration rows displaying raw translation identifiers by explicitly binding and localizing terminal row count and recipe-viewer search synchronization in Chinese and English.
- Fixed debug Substitute Puppets with the vanilla unbreakable component consuming durability on activation while retaining the puppet item in the vanilla center-screen activation animation.
- Fixed Spirit Sword and Immortal-Ruin-Forged Spirit Sword being classified as `apotheosis:none`. Both now expose standard sword attack attributes, are recognized as `apotheosis:melee_weapon`, and accept Apotheosis reforging and affixes.

### Verification

- Supported target: Minecraft 1.21.1, NeoForge 21.1.235, Java 21.
- 662 automated tests passed with 0 failures, 0 errors, and 0 skipped tests.
- Clean JDK 21 build, production-boundary audit, version-composition audit, exact-artifact audit, Ars Source adapter check, and no-AE2-runtime check passed.
- Release artifact: `immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.3.jar`.
- SHA256: `DC0D9FD79CD557E9A8F69C795EDE62AD92524F9F051995683BB25527058215B6`.

All notable user-facing changes are documented here.

## [0.0.2] - 2026-07-20

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
