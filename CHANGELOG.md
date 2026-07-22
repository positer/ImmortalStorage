# Changelog

## [0.0.4] - 2026-07-22

本节列出相对已发布 `0.0.3` 的完整用户可见变更。

### 简体中文

#### 新增

- 空窍与仙窍存储终端新增第四阶段解锁的内置锻造台，使用真实锻造配方管理器，并支持从个人存储、玩家背包以及 JEI/EMI 配方自动填充。
- 仙窍终端新增显式磁铁开关；开启后自动收取玩家周围精确 13×13×13 范围内的掉落物，关闭后停止自动吸取。
- 终端物品支持按住 Shift 显示精确存储数量；搜索框输入时会消费 E 键，不再关闭界面。
- 古玉加入全部原版可疑的沙子与可疑的沙砾考古战利品表，默认获取概率为 5%，可通过配置调整。
- 新增采气瓶：蕴灵晶、玻璃瓶、仙元无序合成，耐久 1024，不可堆叠，稀有度 Rare。
- 新增一次性采气瓶：灵晶、玻璃瓶、真元无序合成，最大堆叠 16，稀有度 Uncommon。
- 新增混元一气，最大堆叠 64，稀有度 Rare。在任意维度最低建筑高度以下或最高建筑高度上限处可采集，产物优先进入背包，再进入个人存储。
- 混元一气可令目标在 10 tick（0.5 秒）内连续缩小至零并以非击杀方式消失，同时掉落对应刷怪蛋。
- 刷怪蛋从运行时全局物品注册表中的全部 `SpawnEggItem` 子类解析，兼容模组生物与组件定义刷怪蛋，不依赖创造模式物品栏或硬编码名单。
- 新增混元一气实体黑名单配置；玩家始终不受作用，渡劫期间禁止使用；末影龙会先正确完成末地龙战流程再移除。
- 灵器新增第五种传送模式：每次使用消耗 1 仙元，沿面向方向传送 1–20 格，默认 20 格；按住可配置的特殊功能键（默认反引号）并滚动滚轮调节距离。
- 稳定化迷你仙墟新增可展开的 5×4 物品过滤格，支持手持物品点击、JEI/EMI 原料拖入、空手左键清除、组件/NBT 匹配以及白名单/黑名单模式。
- 稳定化迷你仙墟可通过灵器扳手链接同维度、相反状态的两个实例并共享同一容器；容量不叠加，首次合并溢出内容会掉落。
- 普通迷你仙墟可通过相同的扳手记录、清空、校验和白色连线流程建立链接。
- 已链接普通迷你仙墟新增 Warp 开关，每 tick 将接触正常状态端中心方块的合格生物与掉落物单向传送至反转状态端；玩家是否参与由“对玩家生效”开关决定。
- 新增完整中英文语言、配置说明、Patchouli 页面、物品模型、配方、战利品修改器与回归测试。

#### 行为调整

- 仙窍模块按解锁顺序排列为“内置合成 → 锻造台 → 内置仙炉 → 仙窍管理”。
- 加长仙窍管理侧栏及 JEI/EMI 避让区域，手持补充和磁铁按钮完整位于面板内。
- 灵器传送不检查沿途墙体或落点碰撞，严格执行设定距离，允许穿墙并进入会窒息的位置。
- 仙墟锻灵剑传送范围由 13×13×13 扩大为 27×27×27。
- 仙墟锻灵剑目标获得 40 tick（2 秒）绝对禁锢：速度清零、位置锁定，实体碰撞、挤压和外力均无法令其移动。
- 反转迷你仙墟现在把旧传送力度强制设为“无”，不再设为“强”。
- 普通迷你仙墟操作与稳定化版本同步：直接右键打开 UI，空手 Shift 右键切换状态，灵器扳手 Shift 右键拆下；拆下后不携带配置 NBT。
- 普通仙墟任意一端破坏时清除双方链接；稳定化仙墟按幸存端或同时破坏规则保存或掉落共享内容。
- 模组版本、网络协议与发布元数据更新至 0.0.4。

#### 修复

- 修复铁傀儡无法掉落刷怪蛋；现在按全局注册刷怪蛋的真实默认堆栈解析实体类型，铁傀儡、末影龙及模组生物共用同一通用路径。
- 修复磁铁开关看似无法关闭；客户端现在逐 tick 同步按钮可见性、可点击状态和文字，开启与关闭均立即刷新。
- 修复 Warp 错误传送整个吸引区域内实体；Warp 与伤害现在共用精确的中心方块碰撞箱接触判定。
- 修复 Warp 在“对玩家生效”关闭时仍传送玩家的问题。
- 修复灵器传送被墙体或安全落点搜索阻挡的问题。
- 修复采气产物被背包部分或完整接收时的后续交付判断，保持“背包优先、个人存储其次”。
- 修复磁铁扫描范围，使其严格为 13×13×13。

#### 材质与手册

- 新增采气瓶、一次性采气瓶、灵器传送模式、混元一气与灵魂涌动的正式材质资源。

#### 验证

- 支持 Minecraft 1.21.1、NeoForge 21.1.235、Java 21。
- JDK 21 干净发布构建成功；671 项自动化测试通过，0 failures、0 errors；504 个资源 JSON 文件全部解析成功。
- 生产 JAR 边界、版本组成、精确产物、Ars Source 适配器与无 AE2 运行时校验全部通过。
- Numen 外部实机验证通过：第四阶段内置锻造、搜索框 E 键保留、模块顺序、磁铁面板完整布局，以及磁铁“关闭 → 开启 → 关闭”即时同步。
- 发布产物：`immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.4.jar`。
- SHA256：`2126107A6935EF55A97FB86F5F472ED4D3F33FAAEEF9E7703390B42DDB4A4A49`。

### English

This section contains the complete user-visible change set from the published `0.0.3` tag to `0.0.4`.

#### Added

- Added a Stage-4 embedded smithing-table module to both Kongqiao and Xianqiao storage terminals. Its template, base, addition, and result slots use the real smithing recipe manager and preserve normal recipe behavior.
- Added terminal smithing autofill from personal storage and player inventory, plus JEI and EMI smithing-recipe transfer support.
- Added an explicit Stage-4 personal-storage magnet switch. When enabled, dropped item entities inside the exact 13x13x13 area centered on the player are inserted directly into personal storage; disabling it stops automatic collection.
- Added exact stored-count tooltips while Shift is held over terminal entries.
- Added Ancient Jade to every vanilla suspicious-sand and suspicious-gravel archaeology loot table with a configurable chance, defaulting to 5%.
- Added the reusable Qi Collecting Bottle: unordered Nurturing Crystal, Glass Bottle, and Immortal Yuan recipe; 1,024 durability; non-stackable; Rare rarity.
- Added the Disposable Qi Collecting Bottle: unordered Spirit Crystal, Glass Bottle, and True Yuan recipe; stacks to 16; Uncommon rarity.
- Added Primordial Qi with a stack limit of 64 and Rare rarity.
- Added air collection in every dimension below the minimum build height or at/above the maximum build height. Reusable bottles lose one durability, disposable bottles consume one item, and output enters the player inventory before falling back to personal storage.
- Added Primordial Qi entity conversion. A successful use consumes one item, shrinks the target continuously to zero over 10 ticks (0.5 seconds), removes it without counting as a kill, and drops its matching spawn egg when one exists.
- Added runtime-global spawn-egg discovery across every registered `SpawnEggItem` subclass, including modded spawn eggs and component-defined egg types; the implementation does not depend on creative-tab contents or hardcoded entity lists.
- Added a configurable entity-type blacklist for Primordial Qi. Players remain protected, and Primordial Qi cannot be used while the player is undergoing tribulation.
- Added explicit Ender Dragon fight completion before a converted dragon is discarded, preventing the End from becoming stuck.
- Added a fifth Spirit Staff mode: teleportation. Each use spends one Immortal Yuan and moves the player in the exact facing direction.
- Added a configurable Spirit Staff teleport distance from 1 to 20 blocks, defaulting to 20. Hold the configurable special-operation key (grave accent by default) and scroll the mouse wheel to adjust it.
- Added a 5x4 filter grid to the Stabilized Miniature Immortal Ruin configuration screen, opened from a new button beside the existing settings.
- Added filter assignment by clicking with an item or dragging a JEI/EMI ingredient into a filter slot; empty-hand left click clears the slot.
- Added filter component/NBT matching and whitelist/blacklist switches. Normal mode either collects only whitelisted drops or excludes blacklisted drops; reversed mode either ejects only whitelisted contents or excludes blacklisted contents, and blocked entries are skipped during traversal.
- Added opposite-state Stabilized Miniature Immortal Ruin linking with the Spirit Staff wrench. Linked ruins share one authoritative container without stacking capacity; overflow produced during the initial merge drops into the world.
- Added same-dimension and opposite-state validation, wrench selection replacement/clearing behavior, white held-wrench link lines, survivor-retained contents when one stabilized ruin is broken, and correct drops when both ends are destroyed.
- Added opposite-state Miniature Immortal Ruin linking with the same wrench selection, validation, clearing, replacement, and white link-line workflow.
- Added a Warp switch to linked Miniature Immortal Ruins. Every tick, the normal-state side sends eligible living entities and dropped items touching its center to the reversed side in one direction.
- Added explicit Warp player control through the existing affect-players switch; dropped items and non-player living entities remain eligible.
- Added dedicated bilingual translations, Patchouli pages, item models, recipes, loot modifiers, configuration labels, and regression coverage for the 0.0.4 systems.

#### Changed

- Reordered the Xianqiao module rail by progression: embedded crafting, embedded smithing, Immortal Furnace, then realm management.
- Enlarged the Xianqiao realm-management side panel and its JEI/EMI exclusion rectangle so hand-refill and magnet controls remain fully inside the frame.
- Terminal search fields now consume the inventory key while focused, so pressing E during text entry no longer closes the screen.
- Spirit Staff teleportation ignores intervening blocks and destination collision. It always applies the configured displacement and intentionally permits wall traversal and suffocating destinations.
- Expanded the Immortal-Ruin-Forged Spirit Sword pull volume from 13x13x13 to 27x27x27.
- Replaced the forged sword's previous slowdown with 40 ticks (2 seconds) of absolute restraint: velocity is cleared, position is pinned, and collision, pushing, and squeezing cannot move the target.
- Reversed Miniature Immortal Ruins now force the old teleport-strength setting to None instead of Strong.
- Miniature Immortal Ruin operation now matches the stabilized variant: direct right click opens configuration, empty-hand Shift-right-click toggles state, and wrench Shift-right-click dismantles it. A dismantled ordinary ruin does not retain configuration NBT.
- Breaking either end of an ordinary ruin link clears both link records; breaking either end of a stabilized link unlinks both ends while preserving the shared contents according to the survivor/simultaneous-break rules.
- Raised the ImmortalStorage network protocol and release metadata to 0.0.4.

#### Fixed

- Fixed Primordial Qi failing to drop an Iron Golem spawn egg by resolving each globally registered egg from its actual default stack. Registry-backed tests cover both Iron Golems and Ender Dragons, while the same generic path supports modded entities.
- Fixed the Xianqiao magnet switch appearing unable to turn off. The server state was changing, but the client omitted the magnet from its per-tick visibility, active-state, and label refresh; the label now updates immediately in both directions.
- Fixed Miniature Immortal Ruin Warp affecting the whole attraction field. Warp and damage now share the same exact center-block bounding-box contact test.
- Fixed Warp transporting players while player effects were disabled; player transport now follows the affect-players switch.
- Fixed Spirit Staff teleportation being stopped by walls or safe-destination searching.
- Fixed reusable bottle delivery when vanilla inventory insertion partially or fully accepted the produced Primordial Qi, preserving the inventory-first then storage-fallback contract.
- Fixed the explicit magnet volume to the required exact 13x13x13 cube.

#### Assets

- Added the final texture resources for the Qi Collecting Bottle, Disposable Qi Collecting Bottle, Spirit Staff teleport mode, Primordial Qi, and Soul Surge.

#### Verification

- Supported target: Minecraft 1.21.1, NeoForge 21.1.235, Java 21.
- Clean JDK 21 release build completed successfully.
- 671 automated tests passed with 0 failures, 0 errors, and 504 resource JSON files parsed successfully.
- Production-boundary, version-composition, exact-artifact, Ars Source adapter, and no-AE2-runtime checks passed.
- Numen real-client QA verified Stage-4 embedded smithing, search-field E-key retention, Xianqiao module order, the unclipped magnet panel, and immediate `off -> on -> off` magnet synchronization.
- Release artifact: `immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.4.jar`.
- SHA256: `2126107A6935EF55A97FB86F5F472ED4D3F33FAAEEF9E7703390B42DDB4A4A49`.

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
