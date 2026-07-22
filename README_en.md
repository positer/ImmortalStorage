# ImmortalStorage

![ImmortalStorage Logo](immortalstorage-logo.png)

[简体中文](README.md) | **English**

ImmortalStorage (仙藏) is a progression, personal-storage, automation, and dimension mod for Minecraft. It turns cultivation into a complete survival path: awaken through the Ancient Jade, gather spiritual materials, advance through ten stages, expand a storage space bound to your character, construct a personal Xianqiao realm, automate resources, master specialized tools, and face tribulations that test each late-game breakthrough.

> The current release is 0.0.4, adding Stage-4 embedded smithing, Qi Collecting Bottles and Primordial Qi, an explicit storage magnet, linked/filtered ruins, Warp transport, and Spirit Staff teleportation.

The interface follows Minecraft's native pixel language while borrowing the information architecture of large storage networks: one continuous terminal combines storage, crafting, furnace processing, equipment, search, recipe-viewer interaction, and realm management without forcing the player through disconnected screens.

> Release 0.0.4 targets **Minecraft 1.21.1**, **NeoForge 21.1.235**, and **Java 21**. Other NeoForge version ranges are not claimed by this release.

> **Breaking brand migration:** this republished build changes the mod ID, resource namespace, Java package, network payload namespace, configuration files, command root, and artifact name to `immortalstorage`. It does not load old `cultivation` worlds or configuration. Delete test worlds and create a new world; never install an old `cultivation-*.jar` beside this build.

**Download:** [ImmortalStorage 0.0.4](https://github.com/positer/ImmortalStorage/releases/tag/0.0.4)

**Release JAR SHA256:** `2126107A6935EF55A97FB86F5F472ED4D3F33FAAEEF9E7703390B42DDB4A4A49`

## Highlights

- Ten cultivation stages with persistent benefits, pills, Yuan resources, ascension, and tribulations.
- Per-player Kongqiao/Xianqiao storage that follows the character rather than a single block.
- Smooth, searchable storage terminal with real crafting, furnace, armor, fluid, and magnet management.
- A UUID-bound personal realm whose usable space and time flow grow with cultivation stage.
- Source Veins and automation blocks for high-throughput item, fluid, energy, and optional-mod resources.
- Shared weapon attack projection writes paid resource and tempering growth into standard main-hand attributes so systems such as Apotheosis can read and multiply the real weapon damage.
- Bundled Patchouli Ancient Jade handbook with bilingual progression, recipes, and 0.0.3 mechanics; no separate Patchouli install is required.
- Optional JEI, EMI, AE2, Refined Storage, Mekanism, Botania, Ars Nouveau, Industrial Foregoing, and related integrations.
- Complete Simplified Chinese and English localization for gameplay and configuration.

## Requirements

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.235 |
| Java | 21 |
| ImmortalStorage | 0.0.4 |

No recipe viewer or storage mod is required. Optional integrations activate only when their target mod is installed.

## Installation

1. Install Minecraft 1.21.1 and NeoForge 21.1.235.
2. Use Java 21 for the client and dedicated server.
3. Download `immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.4.jar` from GitHub Releases.
4. Place the JAR in the instance or server `mods` directory.
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

### Pick

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
- Every tempering point adds 1% damage; after a hit, tempering points are reduced to 50%, rounded down.
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

Release 0.0.2 includes the adapters documented for the stated Minecraft/NeoForge target. Other planned loader lines remain unreleased until their own build and runtime gates are complete.

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
project/neoforge-1.21.1-mdk/build/libs/immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.4.jar
```

Useful verification tasks:

```powershell
.\gradlew.bat test build verifyProductionJarBoundary verifyVersionComposition verifyVersionArtifact --no-daemon --max-workers 1 --console=plain
```

The 0.0.4 release gate passes 671 tests on JDK 21, plus production-class, version-composition, and exact-artifact checks. Release JAR SHA256: `2126107A6935EF55A97FB86F5F472ED4D3F33FAAEEF9E7703390B42DDB4A4A49`.

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
