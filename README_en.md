# ImmortalStorage

> **0.0.12 dual-version release (2026-08-13):** One GitHub Release provides two separate artifacts: Minecraft 1.21.1 / NeoForge 21.1.235 and Minecraft 26.1.2 / NeoForge 26.1.2.94. The 1.21.1 gate passed **219 suites / 792 tests**; its 5,382,838-byte JAR has SHA-256 `55B0816FC27F81A0E81E42517203E5FDE95CF97BB18676D06BB906B63CDCBE8A`. The 26.1.2 gate passed **218 suites / 782 tests** with the migration bypass disabled; its 5,397,569-byte JAR has SHA-256 `15C451F4C4C87E81462B310B33E0F0CDC279D80B555A718FAD090D57A5270D76`. Both runs had zero failures, errors, or skips and passed production-boundary, version-composition, exact-artifact, and no-AE2-runtime checks. Mekanism and EMI remain disabled optional integrations on 26.1.2 because no official target artifacts are available; no older API is borrowed. [Download 0.0.12](https://github.com/positer/ImmortalStorage/releases/tag/0.0.12).

> **0.0.12 NeoForge 26.1.2 One-Qi Returning Origin Sword muzzle fix (2026-08-13):** The migration removed the old `ItemRenderer` muzzle hook without connecting it to the new `ItemInHandRenderer` / `ItemStackRenderState` submission path, so the beam always fell back to an empirical hand offset near the bottom of the view. The target adapter now reads `getModelBoundingBox().getCenter()` immediately before the real item submission and transforms that exact model center through the live hand, swing, equip, and camera matrices. The item render submission itself is unchanged. A new regression failed before the hook was added and passes after the fix. The full 26.1.2 gate passed **218 suites / 782 tests / 0 failures / 0 errors / 0 skipped**; the final 5,397,569-byte JAR has SHA-256 `15C451F4C4C87E81462B310B33E0F0CDC279D80B555A718FAD090D57A5270D76`. The PCL2 version instance and global mods directory now each contain exactly one matching JAR; previous artifacts are backed up under `archive/pcl2-backups/2026-08-13-one-qi-muzzle-target-fix`. The 1.21.1 source and instance were not changed.

> **0.0.12 Simulated Reincarnation Furnace local-cache fix (2026-08-13):** Fixed completed drops remaining in the hidden pending list instead of entering the local 4×3 output area after Xianqiao Output was disabled. Internal production had incorrectly called `ItemStackHandler.insertItem`, which intentionally applies the external automation filter and rejects all output-slot insertion. A dedicated internal merge path now preserves component identity and slot limits while the public output slots remain extraction-only. Final gates: 1.21.1 **219 suites / 792 tests / 0 failures**, SHA-256 `D70C3DA883D4F9A48053A6A15E3AD4CB5C00B84C485476B430F6FB2FF49301E3`; 26.1.2 **217 suites / 781 tests / 0 failures**, SHA-256 `847A1B1A3801AAF04AA4F927A37371D669E1FEEFD16FE8FA1AFFDCE8E5B269E3`. Both PCL2 instances were replaced; live validation remains with the user.

> **0.0.12 persistent production-buffer and 26.1.2 embedded-furnace update (2026-08-13):** Simulated Spirit Fields, Simulated Reincarnation Furnaces, Treasure Basins, and World Shard Miners no longer eject batch overflow into the world. Local output slots fill first, enabled accepting faces are tried next, and any remainder is persisted inside the machine. Newly freed local space is reserved for that temporary output; no new production cycle starts until it clears. Direct Xianqiao output still bypasses local capacity, while a temporarily rejected direct batch remains pending without drops or duplication. NeoForge 26.1.2 also fixes client-side embedded-furnace recipe checks and the unregistered recipe-book category that prevented a fully tempered sword from being removed and caused recipe-book packet encoding failure. Final gates: 1.21.1 **219 suites / 791 tests / 0 failures**, SHA-256 `0FB00E6CE6A4A59825E20D68308586EEE0C31341855BA5EA6DF9789882C36102`; 26.1.2 **217 suites / 780 tests / 0 failures**, SHA-256 `1FADE31A5C44D19D505512AA52305DEF13E9479431FAEDDAB6D197BEB3931421`. Both PCL2 instances were updated; previous artifacts are under `archive/pcl2-backups/2026-08-13-persistent-production-temp-cache`. Live client validation remains with the user.

> **0.0.12 bilingual handbook and 26.1.2 half-row scrolling update (2026-08-13):** The bundled Patchouli guide now covers all ten real pill recipes, placed/embedded furnace reinforcement behavior, dynamic AE2 `AEKeyTypes` and RS `ResourceType`/`StorageType` addon storage, bridge priority, and no-addon fallbacks. The 26.1.2 terminal now converts foreground clipping to the local coordinates expected by `GuiGraphicsExtractor`, preventing items, fluids, external resources, and long amount labels from disappearing halfway through a row. Full gates passed with 786 tests on 1.21.1 and 773 tests on 26.1.2; both PCL2 instances were updated.

> **0.0.12 embedded-furnace and registered-addon storage update (2026-08-13):** The placed Immortal Furnace column is lowered again and the embedded furnace now uses the same top plugin / middle flame / bottom fuel layout. Both plugins accelerate processing only; they do not multiply output or fuel duration. AE2 addon key types are discovered from `AEKeyTypes`, while RS addon resource and storage types are discovered from its `ResourceType` and `StorageType` registries. Explicit addon bridges and ImmortalStorage's no-addon external-resource/FE fallback remain intact. Full gates passed with 786 tests on 1.21.1 and 772 tests on 26.1.2; both PCL2 instances were updated.

> **0.0.12 dual-version development build (2026-08-13):** Adds the 4x, 16x, and 256x reinforcement plugins across the Immortal Furnace, simulated machines, stabilized ruin family, crystal family, and Treasure Basin. Plugins never extend fuel duration; a higher-tier held plugin replaces a lower tier and returns the old item. The Treasure Basin now emits light level 15 and has face-output, Xianqiao-output, and plugin controls. The highest tier uses one thick pixel-lightning helix that starts in front at the bottom, passes behind the item, and returns to the front at the top. The 1.21.1 gate passed 780 tests and the 26.1.2 gate passed 766 tests, with zero failures. Both PCL2 version instances contain the new 0.0.12 artifacts; client visual QA remains pending.

![ImmortalStorage Logo](immortalstorage-logo.png)

> **0.0.11 release (2026-08-12):** The Minecraft 1.21.1 / NeoForge 21.1.235 release gate passed with **210 XML suites / 764 tests / 0 failures / 0 errors / 0 skipped**. The release artifact is `immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.11.jar`, 5,295,016 bytes, SHA-256 `60F0314381D714708FA9C7F29EFC4D8F50653E8FB6DDD40B5A6ED64541B37DF3`.

[简体中文](README.md) | **English**

ImmortalStorage (仙藏) is a progression, personal-storage, automation, and dimension mod for Minecraft. It turns cultivation into a complete survival path: awaken through the Ancient Jade, gather spiritual materials, advance through ten stages, expand a storage space bound to your character, construct a personal Xianqiao realm, automate resources, master specialized tools, and face tribulations that test each late-game breakthrough.

> The current release is **0.0.11**. Building on 0.0.10, 0.0.11 limits the Miniature Immortal Ruin to an exact `13×1×13` single-layer influence area and always excludes players holding a Miniature Immortal Ruin in either hand. The item tooltip and bilingual Patchouli handbook carry matching native usage guidance.
>
> 0.0.10 built on 0.0.9's persistent player identity and AE2/RS external-resource compatibility, fixed the tempering coefficients shown by the One-Qi Returning Origin and Immortal-Ruin-Forged sword tooltips, limited displayed tempering percentages to at most two decimal places, expanded the bilingual Patchouli handbook, added Mekanism chemical-container interaction with stored chemicals in the terminal, rearranged realm time-flow controls into a centered symmetric `- value +` row, and removed the control-text shadow/ghosting.
>
> **0.0.9 RS external-resource display and addon compatibility (2026-08-08):** With Refined Storage 2.0.9 alone, the Xianqiao exchange disk displays FE, Mana, Source, Souls, and Mekanism chemicals through ImmortalStorage's own `xianqiao_external` resource type. When Refined Types is installed, FE/Source/Souls use its native keys; when the official RS Mekanism Integration is installed, chemicals use its native keys. The built-in fallback remains readable and writable for old caches or addon removal, while each ledger resource is emitted only once. ExtraStorage-style addons remain compatible through RS's standard storage-container contract. See `archive/2026-08-08-rs-external-resource-display-and-addon-compat.md`.
>
> **0.0.8 realm identity unification (Bug 2, 2026-08-08):** launching the same test world through different launcher paths previously produced two isolated offline identities and personal realms. A chunk-level migration with "canonical wins" is complete: the canonical `00000fff-ffff-ffff-ffff-fffffff16c5c` is now the single identity, the old `00000000-0000-3003-998f-501bcc516c5c` realm chunks/advancements were merged in (+9 unique entity chunks, +14 advancements), canonical playerdata was kept, and the old-identity files plus the whole old realm folder were quarantined to `saves\Test\_quarantine_old_identity\` (pre-merge backup: `saves\_backups\Test_pre-realm-merge_20260808_141636`). All launcher bat files already hardcode the canonical `--uuid`, so no launcher changes were needed. See `archive/2026-08-08-realm-identity-unification-bug2.md`.
>
> **0.0.8 realm snow never counts toward forced chunk loading (design invariant, 2026-08-08):** realm snow is a client-only visual; the server SNOW weather mode deliberately does not enable `raining` (which would otherwise let vanilla lay snow layers across the whole realm). The "player-modified chunks" set that drives forced chunk loading is fed only by entity-driven break/place events, and vanilla snow uses a null-entity `setBlock`, so it can never be recorded as a player modification. The invariant is locked by the pure `RealmEnvironmentPolicy.requiresRain`/`requiresThunder` helpers and their regression tests.
>
> **0.0.8 AE2 client crash fix (2026-08-08):** fixed a `Missing render handler for channel immortalstorage:external_resource` crash when opening an AE2 terminal inside the merged Xianqiao realm. The server already registered the `immortalstorage:external_resource` AEKey channel, but the client never registered a render handler with `AEKeyRendering`, so rendering/hovering that external-resource entry threw in `getOrThrow`. The fix adds a client registration entry `Ae2ClientCompat` (reflected from `CompatManager.initializeClientIntegrations` under the `AE2_LOADED` guard) and `ImmortalStorageExternalResourceKeyRenderHandler` (drawing/naming reuse `ExternalResourceCatalog`, identical to the Xianqiao interface look); regression tests reproduce the original crash condition directly. Full gate at **191 suites / 706 tests / 0 failures**. See `archive/2026-08-08-ae2-missing-render-handler-crash-fix.md`.

The interface follows Minecraft's native pixel language while borrowing the information architecture of large storage networks: one continuous terminal combines storage, crafting, furnace processing, equipment, fluid and chemical-container interaction, search, recipe-viewer interaction, and realm management without forcing the player through disconnected screens.

> The republished 0.0.10 JAR uses **Minecraft 1.21.1**, **NeoForge 21.1.235**, and **Java 21** as its build baseline and officially declares the NeoForge range `[21.1.235,21.2)`; the range is active in this release asset.

> **Breaking brand migration:** this republished build changes the mod ID, resource namespace, Java package, network payload namespace, configuration files, command root, and artifact name to `immortalstorage`. It does not load old `cultivation` worlds or configuration. Delete test worlds and create a new world; never install an old `cultivation-*.jar` beside this build.

**Latest published version:** [ImmortalStorage 0.0.12](https://github.com/positer/ImmortalStorage/releases/tag/0.0.12)

**Release artifact SHA-256:**

- Minecraft 1.21.1 / NeoForge 21.1.235: `55B0816FC27F81A0E81E42517203E5FDE95CF97BB18676D06BB906B63CDCBE8A` (5,382,838 bytes)
- Minecraft 26.1.2 / NeoForge 26.1.2.94: `15C451F4C4C87E81462B310B33E0F0CDC279D80B555A718FAD090D57A5270D76` (5,397,569 bytes)

## Highlights

- Ten cultivation stages with persistent benefits, pills, Yuan resources, ascension, and tribulations.
- Per-player Kongqiao/Xianqiao storage that follows the character rather than a single block.
- Smooth, searchable storage terminal with real crafting, furnace, armor, fluid, Mekanism chemical-container, and magnet management.
- A UUID-bound personal realm whose usable space and time flow grow with cultivation stage.
- Source Veins and automation blocks for high-throughput item, fluid, energy, and optional-mod resources.
- Shared weapon attack projection writes paid resource and tempering growth into standard main-hand attributes so systems such as Apotheosis can read and multiply the real weapon damage.
- Bundled Patchouli Ancient Jade handbook with bilingual progression, real recipes, and complete 0.0.12 coverage for reinforcement plugins, machine output, addon storage, and every pill recipe. Locale-tree parity is enforced by tests; no separate Patchouli install is required.
- Optional JEI, EMI, AE2, Refined Storage, Mekanism, Botania, Ars Nouveau, Industrial Foregoing, and related integrations.
- Complete Simplified Chinese and English localization for gameplay and configuration.

## Requirements

| Minecraft | NeoForge | Java | 0.0.12 artifact |
| --- | --- | --- | --- |
| 1.21.1 | 21.1.235 (build baseline); supported range: `[21.1.235,21.2)` | 21 | `immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.12.jar` |
| 26.1.2 | 26.1.2.94 | 25 | `immortalstorage-neoforge-mc26.1.2-nf26.1.2.94-0.0.12.jar` |

No recipe viewer or storage mod is required. Optional integrations activate only when their target mod is installed. The republished 0.0.10 artifact also passed startup smoke tests in temporary 35-mod clients using both Sodium and Embeddium rendering stacks.

### Optimization-Mod Compatibility

- Sodium `0.8.12-alpha.4` with ModernFix `5.27.20`, FerriteCore `7.0.3`, ImmediatelyFast `1.6.11`, and Entity Culling `1.10.5` started successfully; Embeddium `1.0.15` with the same general-purpose optimization set also started successfully.
- Both temporary clients completed NeoForge mod loading, resource reload, sound-engine initialization, and ImmortalStorage registration for Mekanism, Botania, Industrial Foregoing Souls, AE2, Refined Storage, and Ars Nouveau without an optimization-mod or ImmortalStorage crash.
- Sodium and Embeddium are mutually exclusive rendering backends and must not be installed together. The ImmortalStorage JAR has no direct references to their classes and no mixins into their internal renderer; compatibility relies on standard Minecraft/NeoForge rendering entry points.

`CHANGELOG.md` records every user-visible release delta in Simplified Chinese and English. The 0.0.12 entry fully covers `0.0.11 → 0.0.12` and separately documents the Minecraft 1.21.1/26.1.2 migration boundary and dual-artifact installation rules.

## Installation

1. Confirm that the instance is Minecraft 1.21.1 / NeoForge 21.1.235 or Minecraft 26.1.2 / NeoForge 26.1.2.94.
2. Use Java 21 for 1.21.1 clients and servers, and Java 25 for 26.1.2.
3. Download the version-specific JAR from Release 0.0.12. The two artifacts are not cross-version compatible and must never be installed together.
4. Remove every older ImmortalStorage JAR from the instance, then place the matching new JAR in the client or server `mods` directory.
5. Start the game. New players receive an Ancient Jade by default; this can be changed from the NeoForge Mod List configuration screen.

The Mods screen displays **ImmortalStorage** (Chinese: **仙藏**). The administrator command root is `/immortalstorage`; configuration files are `immortalstorage-common.toml` and `immortalstorage-client.toml`.

Back up important worlds before adding or updating any content mod.

## Starting the Journey

The Ancient Jade is the player's main guide. By default, a new character receives one on first joining a world. It can also be crafted shapelessly from a book, emerald, Spirit Crystal, and ImmortalPower. Right-clicking it opens the bundled Patchouli handbook; the former standalone guide screen is no longer maintained.

Open the Jade to see:

- the next meaningful cultivation task for the current stage;
- detailed chapters for progression, storage, resources, tools, realms, and compatibility;
- real crafting-table and cooking diagrams sourced from the current world's recipes;
- stage-specific explanations that follow the active server configuration;
- searchable bilingual descriptions for every major item and machine.

The guide is intended to make a normal playthrough possible even when JEI and EMI are absent.

## ImmortalStorage Progression

ImmortalStorage runs from stage 0, the mortal state, through stage 10.

- Early stages revolve around awakening, Lingqi accumulation, Spirit Pills, and gradually expanding Kongqiao storage.
- Stage 5 is the final finite-storage stage and prepares the one-time ascension into Xianqiao.
- Ascension from stage 5 to 6 converts existing TruePower once; later TruePower is not silently converted.
- From stage 6 onward, the player gains Xianqiao storage, ImmortalPower systems, a personal realm, and late-game automation.
- Stages 6-10 advance through tribulations fought inside the player's own Xianqiao realm.
- Stage-derived permanent effects are restored after login, respawn, dimension travel, and game-mode changes, except while a tribulation deliberately suppresses them.

The server can configure the maximum normally reachable stage and the enemies used for each tribulation.

## TruePower and ImmortalPower

TruePower and ImmortalPower are real items rather than invisible-only counters. Other mods can see, extract, and use them through ordinary item automation.

- Total limits are calculated across the player and personal storage where a stage limit applies.
- Stage 6 and later no longer impose a TruePower maximum.
- Stage 10 normally generates 256 ImmortalPower every 20 ticks with unbounded storage.
- An optional configuration can replace that behavior with a non-consuming inexhaustible resource channel.
- One ImmortalPower can be crafted into 64 TruePower.
- Both items can act as furnace fuel.
- Both can place dyeable, hideable floating-core light blocks; the ImmortalPower light also suppresses natural hostile spawning in its own chunk.

## Kongqiao and Xianqiao Storage

Personal storage is opened with a configurable key binding. The default global storage shortcut can open above most non-text screens, and Escape returns to the previous screen.

### Kongqiao

- Available during stages 1-5.
- Uses finite physical slot capacity that grows by cultivation stage.
- Stores items only; fluids and Xianqiao Manager behavior are not available.
- Keeps the compact early-game inventory presentation appropriate to its smaller capacity.

### Xianqiao

- Available from stage 6.
- Aggregates equivalent items by item identity and complete Data Components while preserving server-authoritative entries.
- Displays long-valued totals and supports large catalogs without changing the underlying external item-handler contract.
- Supports item and fluid storage on the same terminal page; fluids are displayed in buckets.
- Can expose content to storage buses and other capability-based automation.

### Terminal Features

- Nine-column continuous storage view with smooth pixel scrolling.
- Search by localized name, tooltip/tag text, namespace, and ordinary text.
- Sorting by quantity, name, and mod ID.
- Correct item-count layering above item sprites.
- Player inventory, hotbar, and four real armor slots.
- Mouse-drag stack distribution and standard container interactions.
- Embedded 3x3 crafting with automatic ingredient refill from personal storage.
- Optional exact-NBT/Data-Components matching for crafting refill.
- Embedded three-lane Immortal Furnace with automatic input refill and direct storage output.
- Magnet management with enable state, whitelist/blacklist filtering, and direct pickup into Xianqiao.
- Exact-component held-stack refill after the current hand stack is consumed.
- On an external chemical row, right-clicking with a filled Mekanism chemical container deposits its chemical; left-clicking with an empty container fills it from the selected chemical. Stacked containers, long amounts, revision checks, and server-side rollback follow the fluid-container transaction model.
- Three compact 8x8 inventory-action icons (sort, deposit-all, and filtered withdraw) are centered in the gap between the Xianqiao storage grid and the player inventory, with hover descriptions and no overlap with item slots.

## Personal Xianqiao Realm

Every ascended player owns a stable UUID-bound personal dimension.

| Stage | Usable Area | Available Time Flow |
| --- | --- | --- |
| 6 | 3x3 chunks | 1x |
| 7 | 7x7 chunks | 0.5x, 1x, 2x, 4x |
| 8 | 19x19 chunks | 0.2x to 8x fixed gears |
| 9 | Unlimited | 0.1x to 16x fixed gears |
| 10 | Unlimited | 0x freeze to 32x fixed gears |

Stages 6-8 use a visible vanilla world border. Crossing the legal space returns the owner to the nearest valid position and displays a boundary warning. Boundary checks run only while the owner is inside that exact realm, avoiding unnecessary world-side overhead.

Time flow is dimension-local. Slowing or freezing the Xianqiao does not freeze the overworld and never removes the ability to leave the realm.

The Xianqiao Management page has side-by-side time and weather controls. The first toggles locked noon/midnight, while the second cycles clear, rain, thunder, and snow. The sky follows the selected time, both selections remain locked regardless of vanilla cycles or the realm time multiplier, and realm precipitation never changes Overworld weather.

Vanilla beds work inside a personal Xianqiao realm. Using a bed sleeps normally and records that exact personal dimension as the player's respawn point instead of causing a dimension explosion; the server restores the dynamic realm when the saved respawn target must be resolved.

Item-name colors use one progression-aware rarity policy: basic materials remain Common, mid-game crystals, spirit equipment, and machines are Uncommon, expensive Xianqiao, survival, and advanced resource devices are Rare, and dragon-egg/nether-star-class infinite sources or endgame ruin equipment are Epic.

## Tribulations

Late-stage advancement takes place only inside the player's own Xianqiao realm. ImmortalStorage-derived buffs and flight are temporarily suspended during the encounter.

Default encounters are:

| Advancement | Enemy |
| --- | --- |
| Stage 6 -> 7 | Armored Zombie |
| Stage 7 -> 8 | Armored Wither Skeleton |
| Stage 8 -> 9 | Armored Vindicator with Resistance I |
| Stage 9 -> 10 | Warden |

Each target permanently glows, has Strength III, and receives ten times its normal base health. Later fights add blindness and other pressure effects. Death during an active tribulation is intercepted in every game mode: the summoned target is removed, items are retained, the player revives in place, and current ImmortalPower is cleared. Failure does not lower cultivation stage.

Enemy registry IDs and the maximum normal stage are configurable for modpacks.

## Immortal Furnace

The Immortal Furnace is a three-lane cultivation furnace available both as a placed block and as an embedded Xianqiao module.

- Accepts supported vanilla furnace and blast-furnace recipes except recipes intentionally reserved by ImmortalStorage.
- Spirit Iron ores use blast-furnace or Immortal Furnace processing.
- Spirit Crystal ores use the Immortal Furnace only.
- TruePower processes one item in 50 ticks and supplies 150 furnace ticks in the Immortal Furnace rules.
- ImmortalPower processes an entire stack in 25 ticks and supplies 500 furnace ticks.
- Input automation enters from above.
- Embedded auto-fill restores a full previous input stack and sends completed output directly to personal storage.
- The Immortal Spirit Drive binds to a player and can pay a placed furnace from that player's storage, checking only when a real payment is required.
- JEI and EMI receive a dedicated Immortal Furnace recipe category and catalysts.

## Simulated Spirit Field (0.0.6)

The Simulated Spirit Field uses the unmodified vanilla Smooth Stone texture on a twelve-edge open frame whose geometry alone follows the Stabilized Miniature Immortal Ruin. Its internal substrate renders as the actual block state; ordinary soil becomes hydrated farmland, and the crop model advances through real age states over each 50-tick cycle.

- Its panel size, source/fuel/extra-tool positions, twelve output slots, and player-inventory layout match the Simulated Reincarnation Furnace.
- Only the top accepts seeds and the four horizontal sides accept True Yuan, Immortal Yuan, or a bound Spirit Drive; output extraction can be enabled independently on all six faces. The tool slot is manual-only.
- The seed is a permanent specimen and is never consumed. Every cycle evaluates the mature crop's real loot table with the shared tool and atomically inserts all drops into the twelve-slot output cache.
- Chorus fruit is accepted as a permanent seed only on end-stone-like substrate. A centered chorus flower grows continuously from 0.15x to 0.70x and each cycle yields exactly one chorus flower and two chorus fruit.
- True Yuan and Immortal Yuan use the Immortal Furnace's 150/500-tick burn durations. With an empty fuel slot inside Xianqiao, the field pays one Immortal Yuan directly from the realm owner.
- The upper-right gear opens the same six-face adjacent preview, per-face output, automatic-output, and experience-release settings as the Simulated Reincarnation Furnace. Automatic routing resolves the current realm owner first, the Spirit Drive owner second, then the twelve local output slots.
- Dirt/farmland, common soil tags, End Stone, and Soul Sand substrates can be replaced by right-clicking; the former substrate returns to the player's inventory. Chorus-like crops require End Stone and Nether Wart requires Soul Sand.
- Data packs can extend modded crops through the `immortalstorage:simulated_spirit_field_seeds` and `immortalstorage:simulated_spirit_field_substrates` tags plus seed-to-crop-to-substrate JSON files under `data/*/simulated_spirit_field_crops/`.
- The recipe yields four fields from Spirit Crystals, Primordial Qi, Spirit Iron Nuggets, Grass Block, and Nurturing Crystal. Its rarity is Uncommon.
- On 2026-08-01, the 0.0.6 release artifact was deployed to the 30-JAR full-mod PCL2 profile and passed Numen single-player QA under JDK 21 and `zh_cn`.

## Echo Shard Source Vein and Fan Catalysts (0.0.7)

- Added the Echo Shard Source Vein: a non-stacking source block feeding Source Vein Manager aggregation and per-face export, with an advancement, loot table, crafting recipe, and pickaxe mining.
- Water and Lava Source Veins work as Create blower-fan catalysts: fan washing and fan smelting are enabled through the `create:fan_processing_catalysts/*` tags, and both veins are registered as `create:fan_transparent` so fan wind passes straight through to the catalyst.

## Stabilized Miniature Ruin Container Scheduling (0.0.7)

- The Stabilized, Entangled, Advanced, and Advanced-Entangled container-facing variants share a 2×3 interaction face-mask grid (top row UP/NORTH/DOWN, bottom row WEST/SOUTH/EAST) with per-face toggles and translucent white preview highlights; six-off disables interaction on that side entirely.
- Each in-area position times each enabled face is an independent target resolved through the official NeoForge `Capabilities.ItemHandler.BLOCK, pos, face`.
- The scheduler interacts only with containers inside the preview box, never one layer outside; legacy "any face" (-1) saves migrate to the all-off mask.
- Entangled variants support normal/reversed buffering: the normal side collects area item entities into a 54-slot buffer, and the reversed side ejects that buffer to target containers by frequency, range, and filter.

## Source Veins and Resource Automation

Source Veins are individually configurable producer blocks with real long-valued caches.

- Every block owns its own cache and per-face output rate.
- Each face independently chooses Off, Push, or Bypass-Limit Push.
- Off disables active transfer only; passive storage-bus and pipe extraction remains available.
- Output uses cached resources first and performs ImmortalPower conversion only for a real deficit.
- Fluid source blocks accept and void arbitrary fluid input from all six sides.
- Int-only pipes receive saturated integer views while the authoritative ledger remains long-valued.

Source definitions are data/config driven. A configured output resource can have only one definition in the same active configuration.

### Source Vein Manager

- Can be placed only inside its owner's Xianqiao.
- Contains 72 single-item slots; each slot accepts one non-stackable Source Vein.
- Allows only one Source Vein of the same name per manager.
- Aggregates the installed veins into an extract-only resource view while keeping every member's cache and accounting independent.
- Binds the aggregate to Xianqiao storage statistics so terminals and storage buses can see the available maximum budget plus real cache.
- 0.0.8 visual rework: the `design/SourceVeinController.bbmodel` look (open black wire cage from 12 baked Edge beams) with 8 inner source-core cubes lighting up in an 8-segment blue/purple/red stair by fill level and rotating as one rigid body about the block center; hand/item rendering shows the same rotating core through a block-item BEWLR (state read from the persisted `DisplayState` when present), and the top/bottom ring beams gained `up`/`down` cap faces so the top frame never disappears.

### Xianqiao Manager

The Xianqiao Manager binds nearby automation to the owner's personal storage. Its visual identity is separate from Source Veins and it exposes standard item/fluid capability access without adding any Numen or HTTP debug interface to the production mod.

### Xianqiao Interface

The Xianqiao Interface can be placed outside the personal realm and cannot be claimed by another player.

- Configurable item and fluid cache targets.
- Default limits of 128 items per cache slot and 16 buckets per fluid slot, adjustable in configuration.
- JEI/EMI ghost dragging and a numeric cache-amount dialog.
- Per-target six-face interaction masks.
- Global active-pull and active-push switches plus per-face Pull, Push, or Off modes.
- Active pull drains all acceptable adjacent contents on enabled faces; active push supplies configured cached resources.
- Optional energy, chemical, mana, Source, and Soul Surge caches when their owning mods are installed.

## Spirit Instrument

The Spirit Instrument has five modes, selected with the configurable mode controls.

### Explore

Right-click air to toggle collection. While the enabled instrument remains in either hand, opening a chest, trapped chest, barrel, Lootr page, or registered loot container transfers the currently opened contents transactionally into personal storage. Anything that cannot fit remains in the source container.

### Wrench

Uses public wrench conventions for ImmortalStorage, Mekanism, AE2, Refined Storage, Create, and compatible machines. Shift-right-click safely dismantles owned ImmortalStorage machines and returns the block.

### Mining

Mines at netherite level. Ordinary left-click mining uses the instrument's current anvil enchantments and consumes durability normally. From stage 6, right-clicking a block spends one ImmortalPower to perform the instrument's built-in silk harvest; that special right-click path is independent of the tool's enchantments.

### Build

Shows a client preview derived from the same server placement plan, then batch-places a surface using materials from inventory and personal storage. The default action limit is 64 blocks and is configurable. Ctrl-right-click removes one planned layer without drops. Placement or removal consumes one durability per layer operation.

The Spirit Instrument cannot receive random enchanting-table enchantments, but anvil compatibility follows the current netherite-pickaxe enchantment rules, including enchantments added by other mods.

## Spirit Sword

The Spirit Sword's tooltip and combat code share the same damage model, so the displayed formula matches the server calculation.

- Stage-based bonus damage consumes the required TruePower or ImmortalPower on a successful hit.
- Insufficient balance falls back to base weapon damage.
- The sword supports anvil enchantments using the current netherite-sword rules but rejects random enchanting-table rolls.
- Spirit Repair can consume Yuan to restore durability.
- Repeated processing in a furnace, blast furnace, placed Immortal Furnace, or embedded Immortal Furnace adds one tempering point with no experience reward.
- Ordinary Spirit Swords gain 1% per tempering point; the One-Qi Returning Origin Sword gains 0%, and the Immortal-Ruin-Forged Spirit Sword gains 1.5%; after a hit, tempering points are reduced to 50%, rounded down.
- The current tempering bonus is visible in the tooltip.

## World Shard Miner and Treasure Basin

The World Shard Miner shares the beacon's complete same-block activation structure. A full diamond-block, ancient-debris, or purpur-brick base selects overworld, nether, or end mode. Levels 1-4 increase work rate.

By default, the miner reads the active world's ore-generation rules so datapacks and other mods influence its weighted output. Modpacks may switch to configurable fixed tables instead. Inside a Xianqiao realm, results enter that owner's personal storage; elsewhere, the miner fills an internal barrel-sized cache and stops when full.

The Treasure Basin is logically independent. It does not exchange messages with the miner: it only examines whether the block directly below is an active World Shard Miner and derives its loot mode from that observed state. It then samples loot-table behavior into its own cache and may block the beacon beam.

## Materials, Pills, Effects, and Villagers

- Spirit Iron, Crude Spirit Iron, Spirit Crystal, their ores, deep ores, and storage blocks form the main material chain.
- Spirit Core is a stackable-16 intermediate crafting material with no accessory behavior.
- Pills have short use times, can be consumed at full hunger, and describe their effects in their tooltips.
- Breakthrough Pill can fill stage-5 Lingqi progress without automatically crossing the ascension boundary.
- Advancement Weakness and Lingqi Saturation are registered visible effects with localized names and icons.
- Spirit Repair is a registered enchantment with an enchanted-book form.
- The Spirit Sage villager profession uses the Immortal Furnace as its workstation and provides cultivation trades.
- Standard villager purchases can draw exact required payment items from active personal storage; ImmortalPower is not silently substituted for TruePower.

## Recipe Viewers

JEI and EMI are optional. When installed, ImmortalStorage provides:

- R/U recipe lookup and ingredient highlighting from terminal entries;
- terminal search synchronization with loop prevention;
- exclusion areas for terminal panels;
- storage-backed crafting transfer and missing-material feedback;
- clickable item and fluid stack providers;
- dedicated Immortal Furnace recipe display and transfer behavior.

Installing neither viewer is supported. Installing both is also guarded against optional-class loading failures.

## Storage-Mod Compatibility

ImmortalStorage uses standard NeoForge item/fluid capability surfaces for ordinary automation. Optional integrations are isolated so an absent mod cannot cause a class-loading crash.

Implemented integration areas include AE2 and Refined Storage exchange media, storage-bus visibility, Mekanism energy/chemical access, Botania mana, Ars Nouveau Source, Flux Networks energy, and Industrial Foregoing Souls. Exact behavior depends on the installed mod version and the enabled server configuration.

The terminal also treats Mekanism chemical containers like fluid containers: an empty container left-clicks the selected chemical row to withdraw it, while a filled container right-clicks to deposit its registered chemical into Xianqiao. The optional bridge uses Mekanism's chemical capability boundary and server-side simulate/execute/rollback handling for stacked containers and long ledger amounts.

The republished 0.0.10 release declares the Minecraft 1.21.1 / NeoForge `[21.1.235,21.2)` adapter range, built against NeoForge 21.1.235; source gates on 21.1.236 and 21.1.248 plus Sodium/Embeddium client startup smoke tests are complete.

## Configuration

Open Minecraft's **Mods** screen, select **ImmortalStorage**, and choose **Configuration**. The built-in NeoForge configuration UI includes natural-language Simplified Chinese and English labels and tooltips.

Notable options include:

- whether new players receive Ancient Jade;
- loot injection probabilities and quantities;
- maximum normally reachable stage;
- optional stage-10 inexhaustible ImmortalPower behavior;
- tribulation enemy registry IDs;
- Source Vein ownership and destruction permissions;
- Xianqiao Interface item/fluid cache limits;
- Spirit Instrument build-mode action limit.

Server owners should stop the server and back up the world before making progression or compatibility changes.

## Key Bindings and Commands

All ImmortalStorage key bindings can be changed from Minecraft's Controls screen. Storage opening is global over most non-text screens; other actions remain gameplay-scoped to avoid stealing input from menus.

Permission-level-2 administrator commands:

```text
/immortalstorage stage <0..10> [player]
/immortalstorage unload [player]
/immortalstorage reload [player]
/immortalstorage speed <fixed-gear>x [player]
```

When the optional player argument is omitted, the command targets the executor.

## Performance and Safety

- Storage directories use stable entry IDs, revision validation, compact synchronization, and viewport-only rendering.
- Continuous-scroll rendering keeps full animation frame rate while unloading invisible rows.
- High realm time scales tolerate amount-only terminal updates without making entries impossible to click.
- Optional integrations are loaded behind explicit mod-presence boundaries.
- Long-valued resources expose safe saturated integer views to int-only APIs.
- Storage mutations, crafting refill, source conversion, interfaces, and furnace processing use server-authoritative transactions.
- Production ImmortalStorage contains no Numen, MCP, bearer-token, HTTP, or external-debug endpoint. Numen was used only as an external test tool.

## Building from Source

Clone the repository and run from Windows PowerShell:

```powershell
cd project/neoforge-1.21.1-mdk
$env:JAVA_HOME = "C:\path\to\jdk-21"
.\gradlew.bat clean build --no-daemon --max-workers 1 --console=plain
```

The release artifact is written to:

```text
project/neoforge-1.21.1-mdk/build/libs/immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.10.jar
```

Useful verification tasks:

```powershell
.\gradlew.bat test build verifyArsSourceAdapter verifyWithoutAe2Runtime verifyProductionJarBoundary verifyVersionComposition verifyVersionArtifact --no-daemon --max-workers 1 --console=plain
```

The republished 0.0.10 artifact passed **199 suites / 724 tests / 0 failures/errors/skips**, including the Ars Source API, no-AE2-runtime, production-boundary, version-composition, and exact-version-artifact checks. The built `immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.10.jar` is 5,163,055 bytes with SHA256 `EA09A8493367E4E05A4C04D520FCB6E74EBF6409DC103E5BE0A4AE2ACD6564B4`; NeoForge 21.1.236/21.1.248 source checks and Sodium/Embeddium optimization-stack client startup smoke tests also passed. This release covers sword-specific tempering coefficients, at-most-two-decimal tooltip percentages, the bilingual handbook entry-tree contract, Mekanism chemical-container terminal transactions, the centered realm time-flow layout, and the no-shadow text fix. This JAR is the sole asset for GitHub Release 0.0.10.

The 0.0.7 release gate passed 700 tests on JDK 21, plus production-class, version-composition, exact-artifact, Ars Source API, no-AE2-runtime, and single-player QA checks. Release JAR SHA256: `0547EFD1B1E75C9FE4305F3F6A48A79A9F5147FD42CA468AF716B87E91739B75`.

## Project Layout

```text
.
├── README.md                 Simplified Chinese default
├── README_en.md              English documentation
├── CHANGELOG.md
├── LICENSE
└── project/
    └── neoforge-1.21.1-mdk/
        ├── build.gradle
        ├── gradle.properties
        ├── gradle/
        └── src/
            ├── main/java/       ImmortalStorage implementation
            ├── main/resources/  assets, data, recipes, tags, and metadata
            └── test/java/       automated behavior and release-boundary tests
```

Reference mods, extracted third-party sources, local test instances, Numen bridges, logs, screenshots, archives, and built JARs are excluded from Git history. Compiled releases are distributed only through GitHub Releases.

## Support and Bug Reports

When reporting a problem, include:

- Minecraft, NeoForge, Java, and ImmortalStorage versions;
- whether the issue occurs on client, integrated server, or dedicated server;
- installed optional integration mods and their exact versions;
- a minimal reproduction sequence;
- the relevant `latest.log` excerpt without account tokens or private data.

## License

ImmortalStorage is distributed under the repository's All Rights Reserved license. Personal play and private modpack use are permitted; redistribution, commercial use, modification, and derivative publication require prior written permission.

Minecraft is a trademark of Mojang Studios. This project is independent and is not affiliated with Mojang Studios or Microsoft.
