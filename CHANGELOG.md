# Changelog

## [0.1.0] - 2026-08-14

### 简体中文（0.0.0 → 0.1.0 · 版本总览）

仙藏（ImmortalStorage）`0.1.0` 是首个大版本，面向 Minecraft 1.21.1 / NeoForge 21.1.235 与 Minecraft 26.1.2 / NeoForge 26.1.2.94 双版本同步交付。以下按系统汇总从首个公开构建到 `0.1.0` 的全部内容。

#### 一、修仙阶段与个人仙窍

- 修仙阶段 1-10 阶，进度显示使用可调按键（默认 T）；阶段决定仙窍权限、外部资源与时间流速上限。
- 个人仙窍：每位玩家独立的私人维度，绑定稳定持久玩家身份，可在其他维度间自由进出、传送至中心 (0,56,0)。
- 仙窍存储终端：物品、流体、长整型额外资源统一管理，支持整理、存入、筛选取出。
- 普通 / 高级仙窍接口：面向 AE2、Refined Storage 等外部存储的桥接。
- 渡劫：阶段突破玩法。
- 仙窍时间流速：加速仙窍内机器，跨维度生效并保持常加载；26.1.2 使用独立 world clock 隔离不同玩家。
- 领域展开（0.1.0 新增）：非仙窍维度 shift+V 展开 3×3×3 / 7×7×7 / 13×13×13 随身空间，中心即仙窍 (0,56,0)，无方块更新、无 NBT 变动双向搬移。

#### 二、能量与源方块

- 仙元 / 真元货币与能量体系。
- 源方块：将世界中的方块 / 流体 / 物品转化为资源，输出 FE、魔力（Mana）、魔源（Source）等。
- 免费资源源方块放置在仙窍中自动绑定所属玩家并进入存储索引。

#### 三、机器

- 模拟灵田：加速作物生长。
- 模拟轮回炼化炉：物品处理与淬火。
- 仙能水晶：电力 / 魔力 / 魔源三种，共享 800,000,000 容量与 1,000/tick 产出。
- 世界碎片采集器：按维度采集矿物。
- 聚宝盆：采集结构宝箱战利品，动态发现世界战利品表（覆盖远古城市、末地船、堡垒等）。
- 内置锻造台与内置切石机（0.1.0 新增）：空窍 / 仙窍终端内复用原版 UI。

#### 四、灵器与物品

- 灵剑：一气归元剑（0%/淬火点）、普通灵剑（1%/淬火点）、仙墟锻灵剑（1.5%/淬火点）。
- 拘灵器、替死傀儡、仙灵驱动器。
- 强化插件：次元窥令（×4）、次元平行法符（×16）、大千世界并行敕令（×256），加速机器处理。

#### 五、丹药

- 飞升丹：末地船宝箱 25% 产出，用于突破阶段。
- 突破丹、不朽丹、精炼丹：分别来自末地 / 下界 / 村庄战利品。
- 玉简：考古战利品。

#### 六、迷你仙墟

- 迷你仙墟系列：稳定化迷你仙墟、高级稳定化、纠缠、高级纠缠，作用范围 13×1×13 单层。

#### 七、世界胎壁与保护（0.1.0 新增）

- 世界胎壁：混元一气双向合成、仅玩家可破坏、硬度同干草垛。
- 仙窍维度禁止非玩家破坏（TNT / 苦力怕 / 凋零等）。

#### 八、附属兼容

- Applied Energistics 2、Refined Storage：长整型 FE / 额外资源存储与交换磁盘。
- Mekanism：化学品容器交互（阶段 8+）。
- Botania：仙能魔力水晶、火花联动；Ars Nouveau：仙能魔源水晶、支配之杖。
- JEI / EMI 配方查看；双语帕秋莉手册内置全部玩法说明。

#### 九、双版本与验证

- 两代同步交付：1.21.1 门禁 **222 suites / 807 tests / 0 failures**；26.1.2 门禁 **221 suites / 797 tests / 0 failures**，均通过生产 JAR 边界、版本组成、精确制品与无 AE2 运行时验证。
- 从旧版升级时请选择与游戏实例完全匹配的 JAR，并备份世界与配置。

### English (0.0.0 → 0.1.0 · release overview)

ImmortalStorage `0.1.0` is the first major release, delivered for both Minecraft 1.21.1 / NeoForge 21.1.235 and Minecraft 26.1.2 / NeoForge 26.1.2.94. The sections below summarize everything from the first public build through `0.1.0`.

#### 1. Cultivation stages and the personal realm

- Ten cultivation stages; the progression display uses a configurable key (default T). Stages gate realm access, external resources, and time-flow limits.
- Personal Xianqiao: a private dimension per player, bound to a stable persistent identity, with entry/exit and a center teleport to (0,56,0).
- Xianqiao storage terminal: unified items, fluids, and long-valued external resources with sort, deposit, and filtered withdraw.
- Plain / Advanced Xianqiao Interfaces bridge to AE2, Refined Storage, and other external storage.
- Tribulation: stage-breakthrough gameplay.
- Realm time flow accelerates realm machines across dimensions and stays loaded; 26.1.2 uses isolated world clocks per player.
- Domain Expansion (new in 0.1.0): shift+V outside the realm expands a 3×3×3 / 7×7×7 / 13×13×13 portable space centered on the realm, with no block updates and no NBT changes.

#### 2. Energy and source veins

- Immortal Yuan / True Yuan currency and energy.
- Source Veins convert world blocks / fluids / items into FE, Mana, Source, and other resources.
- Free-resource source veins placed in a realm bind to the owner and enter the storage index.

#### 3. Machines

- Simulated Spirit Field: accelerated crop growth.
- Simulated Reincarnation Furnace: item processing and tempering.
- Immortal Energy Crystals: electricity / mana / source variants sharing 800,000,000 capacity and 1,000/tick output.
- World Shard Miner: dimension-scoped ore collection.
- Treasure Basin: structure-chest loot collection with dynamic world loot-table discovery.
- Built-in smithing table and stonecutter (new in 0.1.0) inside the Kongqiao / Xianqiao terminals.

#### 4. Instruments and items

- Spirit swords: One-Qi Returning Origin Sword (0%/point), ordinary Spirit Sword (1%/point), Immortal-Ruin-Forged Spirit Sword (1.5%/point).
- Soul Catcher, Substitute Puppet, Spirit Drive.
- Reinforcement plugins: Dimensional Peeking Order (×4), Dimensional Parallel Talisman (×16), Great Thousand-World Parallel Edict (×256).

#### 5. Pills

- Ascension Dan: 25% from end-ship chests, for stage breakthrough.
- Breakthrough Pill, Immortal Pill, Refined Pill from end / nether / village loot.
- Jade Guide from archaeology.

#### 6. Miniature Immortal Ruins

- Stabilized, Advanced Stabilized, Entangled, and Advanced Entangled ruins, each influencing a 13×1×13 single layer.

#### 7. World Barrier and protection (new in 0.1.0)

- World Barrier: Primordial Qi two-way craftable, player-only breakable, hay-bale hardness.
- The realm dimension forbids non-player destruction (TNT, creepers, withers, etc.).

#### 8. Addon compatibility

- Applied Energistics 2 and Refined Storage: long-valued FE / external-resource storage and exchange media.
- Mekanism: chemical container interaction (stage 8+).
- Botania: Immortal Mana Crystal and Spark; Ars Nouveau: Immortal Source Crystal and Dominion Wand.
- JEI / EMI recipe viewing; a bilingual Patchouli handbook documents all systems.

#### 9. Dual generation and verification

- Delivered for both generations: the 1.21.1 gate passed **222 suites / 807 tests / 0 failures**; the 26.1.2 gate passed **221 suites / 797 tests / 0 failures**, each with production-JAR boundary, version-composition, exact-artifact, and no-AE2-runtime verification.
- When upgrading, install only the JAR matching your exact game instance and back up worlds and configuration first.

## [0.0.12] - 2026-08-13

### 简体中文（0.0.11 → 0.0.12）

本节完整记录从正式版 `0.0.11` 升级到 `0.0.12` 的用户可见变化，并单独说明 Minecraft 1.21.1 与 26.1.2 的迁移边界；不收录 0.0.12 开发期间的逐轮调试过程。

#### 版本迁移

- `0.0.12` 是仙藏首次正式提供双版本制品的发行版：`immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.12.jar` 面向 Minecraft 1.21.1 / NeoForge 21.1.235，`immortalstorage-neoforge-mc26.1.2-nf26.1.2.94-0.0.12.jar` 面向 Minecraft 26.1.2 / NeoForge 26.1.2.94。它们是同一功能版本的两个独立 JAR，不是可跨游戏版本安装的通用 JAR。
- 1.21.1 继续作为功能语义与资源表现基线；26.1.2 通过硬隔离的目标生成源码和版本适配层实现同等功能，不把 1.21.1 的 Minecraft、NeoForge 或联动模组类直接带入目标制品。
- 26.1.2 迁移覆盖注册与数据组件、菜单与输入事件、客户端提交式 GUI/物品/方块渲染、网络载荷、配方与战利品、世界生成、能力与存储传输、内置 Patchouli 手册以及同等发布测试。固定档位界面沿用 1.21.1 的尺寸、槽位和材质，不引入动态宽度布局。
- 26.1.2 正式联动矩阵使用目标版本官方接口：AE2 26.1.10-beta、Refined Storage 3.2.1 和 JEI 29.21.0.68。FE 能力与存储不依赖任何附属模组并始终注册；Mekanism、EMI 及其附属在没有可解析的 26.1.2 官方制品时保持可选且不启用，不借用旧版本 API 伪装兼容。
- 26.1.2 的仙窍存储、仙窍接口、高级仙窍接口、仙炉、模拟机器、仙能水晶和管理页面均按 1.21.1 固定档位重新适配坐标、裁剪、点击、拖动与滚动事件；物品、动态流体、FE/长整型额外资源、青白火焰和箭头进度继续使用原有材质与布局。本模组不再移动或替换 JEI 搜索组件，所有界面恢复 JEI 默认配置。
- 26.1.2 同步迁移了源方块动态悬浮、管理器动态物品预览、范围/双色面预览、非完整方块邻面渲染、聚宝盆模型与碰撞、世界碎片采集器玻璃颜色、仙窍时间流速和时间/天气锁定，以及灵铁/灵晶对应原版铁矿/钻石分布方式的世界生成规则。
- 1.21.1 的发布测试与迁移断言已同步到 26.1.2。最终门禁分别通过 219 suites / 792 tests 与 218 suites / 782 tests，均为 0 failures、0 errors、0 skipped，并通过生产 JAR 边界、版本组成、精确制品和无 AE2 运行时验证。
- 从 `0.0.11` 升级时必须选择与实例完全匹配的 JAR，并在替换前备份世界与配置；不要同时安装两个版本制品或保留旧版仙藏 JAR。

#### 功能与修复

- 修复 NeoForge 26.1.2 一气归元剑第一人称光束从画面下方偏置位置发射的问题。发射点现在取实际渲染剑模型的包围盒中心，并完整叠加实时手持、挥动、装备和视角变换；光束方向、武器模型与战斗逻辑保持不变。
- 修复模拟轮回炼化炉关闭仙窍输出后产物无法进入本地 4×3 缓存的问题。机器内部生产写入不再经过“输出槽禁止外部放入”的自动化过滤；管线输入限制保持不变，已有同类堆叠和空输出槽会按容量正常接收产物。
- 模拟灵田、模拟轮回炼化炉、聚宝盆和世界碎片采集器不再把缓存不足时的产物弹到世界。每轮产物依次进入本地槽、允许输出的临近目标和持久临时缓存；本地空间释放后优先回填临时内容，临时缓存清空前暂停下一轮，清空后自动恢复。开启仙窍输出时仍直接向所属仙窍结算，不受本地容量限制；暂时拒收的整批产物会安全等待，不会掉落或重复。
- 修复 NeoForge 26.1.2 仙窍内置仙炉在客户端错误查询服务端配方，以及仙炉配方返回未注册配方书分类的问题；淬火点满的灵剑可以正常取下，不再触发 `clientbound/minecraft:recipe_book_add` 编码异常。
- 双语内置帕秋莉手册补齐全部 10 条丹药配方，区分工作台、普通熔炉与仙炉处理，并明确飞升丹只来自末地战利品、没有合成表。
- 手册新增放置/内置仙炉强化边界，以及 AE2/RS 官方注册型附属存储、优先级、无附属回退和附属移除/重装行为说明。
- 修复 26.1.2 仙窍存储滚动半格时物品、流体、额外资源及长整型数量被错误裁剪而消失的问题。
- 仙窍内置仙炉新增与放置仙炉一致的强化插件槽及“插件/火焰/燃料”上中下排布；两者的插件均只加速处理，不增加产物、不延长燃料时间。
- AE2 与 Refined Storage 兼容层会主动读取官方注册表中新出现的附属资源/存储类型，并以可逆持久化键接入仙窍长整型额外资源账本；磁盘、存储总线和 RS 对等外部存储访问共享事务化快照与去重写入，避免重复挂载造成物品丢失。原有明确附属联动与无附属 FE/额外资源回退路径保持不变。
- 免费资源源方块放置在仙窍中会绑定维度所属玩家并进入仙窍存储索引；源方块和源方块管理器的移除、解绑与重复清理改为幂等异常安全，避免破坏方块时崩溃。

- 新增次元窥令（×4）、次元平行法符（×16）、大千世界并行敕令（×256）三种强化插件，并入仙藏主分类；源方块改为独立分类。三件物品使用指定锻造顺序；普通库存可堆叠64，机器插件槽限制1。最高级插件采用紫底白核，并以像素条带雷电螺旋环绕核心。
- 手持插件右键支持的机器可直接安装；更高倍率可替换较低倍率，旧插件优先回到背包，背包已满时安全掉落。相同或更低倍率不会覆盖。
- 模拟灵田和模拟轮回炼化炉按处理槽堆叠数量一次结算等倍产物，再叠加强化插件倍率，不通过重复执行相同处理制造额外性能开销；插件与原工具/武器槽共用，不新增多余机器槽位。
- 仙炉新增强化槽并仅按倍率加快处理进度；燃料燃烧时间不受影响。仙能水晶系列按倍率增加每刻 FE/魔力/魔源产量；安装插件时充能物留在处理槽原位完成，并禁用类赛特斯石英充能配方。
- 稳定化迷你仙墟全系列新增过滤/六面配置页插件槽；反转模式按倍率尝试更多输出组，缓存为空或目标拒收时提前结束。聚宝盆与世界碎片采集器获得一致的六面输出、仙窍输出和插件配置；插件分别增加单轮战利品与单轮产矿。
- 两代聚宝盆亮度调整为15。修复 NeoForge 26.1.2 世界碎片开采器玻璃罩的错误红色染色，并同步双语内置帕秋莉手册。

### English (0.0.11 → 0.0.12)

This section records the complete user-visible upgrade from the `0.0.11` release to `0.0.12` and explicitly defines the Minecraft 1.21.1/26.1.2 migration boundary. Iterative internal debugging steps from 0.0.12 development are intentionally omitted.

#### Version migration

- `0.0.12` is the first ImmortalStorage release with two official version-specific artifacts: `immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.12.jar` targets Minecraft 1.21.1 / NeoForge 21.1.235, while `immortalstorage-neoforge-mc26.1.2-nf26.1.2.94-0.0.12.jar` targets Minecraft 26.1.2 / NeoForge 26.1.2.94. They implement the same feature release but are separate JARs, not universal cross-version builds.
- Minecraft 1.21.1 remains the behavioral and visual baseline. The 26.1.2 build reaches feature parity through hard-separated generated target sources and version adapters, without packaging or loading 1.21.1 Minecraft, NeoForge, or integration classes.
- The 26.1.2 migration covers registration and data components, menus and input events, the submitted GUI/item/block rendering pipeline, network payloads, recipes and loot, world generation, capabilities and storage transfer, the bundled Patchouli handbook, and equivalent release tests. Fixed-size screen tiers preserve the 1.21.1 dimensions, slots, and textures; dynamic-width layouts are not introduced.
- The released 26.1.2 integration matrix uses target-version official APIs for AE2 26.1.10-beta, Refined Storage 3.2.1, and JEI 29.21.0.68. FE capability and storage are always registered without requiring another mod. Mekanism, EMI, and their addons remain optional and disabled where no resolvable official 26.1.2 artifact exists; older APIs are never borrowed to claim compatibility.
- Xianqiao Storage, both interface tiers, Immortal Furnaces, simulated machines, Immortal Energy Crystals, and management screens were adapted to the fixed 1.21.1 screen tiers on 26.1.2, including coordinates, clipping, click/drag input, and scrolling. Items, dynamic fluids, FE/long-valued external resources, cyan-white flames, and progress arrows retain their original textures and layout. ImmortalStorage no longer moves or replaces JEI search widgets; every screen uses JEI's default configuration.
- The target migration also carries over animated source-vein cores, dynamic manager item previews, range and dual-color face previews, exposed faces beside full blocks, Treasure Basin models and collision, World Shard Miner glass color, Xianqiao time flow and time/weather locking, and world generation that follows vanilla iron distribution for Spirit Iron and diamond distribution for Spirit Crystal.
- The 1.21.1 publication tests and migration assertions were carried over to 26.1.2. Final gates passed 219 suites / 792 tests and 218 suites / 782 tests respectively, both with 0 failures, 0 errors, and 0 skipped tests, plus production-JAR boundary, version-composition, exact-artifact, and no-AE2-runtime verification.
- When upgrading from `0.0.11`, install only the JAR matching the exact game instance and back up worlds and configuration first. Do not install both artifacts together or leave an older ImmortalStorage JAR in the same instance.

#### Features and fixes

- Fixed the NeoForge 26.1.2 first-person One-Qi Returning Origin Sword beam spawning from an offset near the bottom of the view. Its muzzle now follows the exact rendered sword-model bounding-box center through live hand, swing, equip, and camera transforms without changing beam direction, item rendering, or combat behavior.
- Fixed Simulated Reincarnation Furnace products failing to enter the local 4×3 cache after Xianqiao Output was disabled. Machine-owned production writes no longer pass through the output slots' external-insertion filter; pipe input restrictions remain unchanged, while matching stacks and empty output slots accept completed drops normally.
- Simulated Spirit Fields, Simulated Reincarnation Furnaces, Treasure Basins, and World Shard Miners no longer eject output when a batch exceeds local capacity. Each batch routes through local slots, enabled accepting faces, and then a persistent temporary buffer. Newly freed local capacity is filled from that buffer first, and production pauses until it clears. Direct Xianqiao output still bypasses local capacity; a temporarily rejected batch remains pending without drops or duplicate settlement.
- Fixed NeoForge 26.1.2 embedded-furnace code querying server recipes on the client and returning an unregistered recipe-book category. Fully tempered swords can be removed normally without a `clientbound/minecraft:recipe_book_add` encoding failure.
- Expanded the bundled bilingual Patchouli handbook to cover all ten pill recipes, distinguishing crafting, vanilla smelting, and Immortal Furnace processing; Ascension Dan is explicitly End loot and has no fabricated recipe.
- Documented placed/embedded furnace reinforcement boundaries and official registered AE2/RS addon storage, priority, no-addon fallback, and addon removal/reinstallation behavior.
- Fixed NeoForge 26.1.2 half-row terminal scrolling incorrectly clipping items, fluids, external resources, and long-valued amount labels.
- The embedded Immortal Furnace now has the same reinforcement slot and top-plugin/middle-flame/bottom-fuel layout as the placed furnace. Both accelerate processing only and never multiply output or fuel duration.
- AE2 and Refined Storage now discover addon-defined resource/storage types through their official registries and map them reversibly into the long-valued Xianqiao external-resource ledger. Disks, storage buses, and equivalent RS external-storage access share transactional snapshots and deduplicated writes so duplicate mounts cannot consume items. Existing explicit addon integrations and no-addon FE/external-resource fallbacks remain available.
- Free-resource source veins placed inside a Xianqiao bind to the realm owner and enter the storage index. Source-vein and manager removal, unbinding, and repeated cleanup are now idempotent and exception-safe to prevent block-breaking crashes.

- Added three reinforcement plugins—Dimensional Peeking Order (×4), Dimensional Parallel Talisman (×16), and Great Thousand-World Parallel Edict (×256)—to the main ImmortalStorage category, while source veins now have their own category. The specified smithing sequences are retained; items stack to 64 normally while machine slots hold one. The highest tier uses a purple body, white core, and pixel-lightning bands spiralling around it.
- Using a plugin on a supported machine installs it directly. A higher tier replaces a lower tier and returns the old plugin to the inventory, safely dropping it only when the inventory is full. Equal or lower tiers do not overwrite it.
- Simulated Spirit Fields and Simulated Reincarnation Furnaces settle input-stack scaling and plugin scaling in one output batch instead of repeating the same processing operation. Their plugin shares the existing tool/weapon slot instead of adding another machine slot.
- The Immortal Furnace gains a plugin slot that multiplies processing progress only; fuel duration is unchanged. Crystals multiply FE/Mana/Source production. With a crystal plugin installed, chargeable items complete in place and Certus-like recipes are disabled.
- Every stabilized ruin variant gains a plugin slot below its filter/face controls and attempts more reversed output groups, stopping early when empty or blocked. The Treasure Basin and World Shard Miner gain matching face-output, Xianqiao-output, and plugin controls; plugins multiply per-round loot and mined resources respectively.
- Treasure Basin light level is 15 in both generations. Fixed the red-tinted World Shard Miner glass cover on NeoForge 26.1.2 and updated the built-in bilingual Patchouli handbook.

## [0.0.11]

本节完整列出 `0.0.10` → `0.0.11` 的用户可见变化，不包含内部调试过程、失败复现记录或构建过程。

### 简体中文

#### 新增

##### 仙能水晶系列

- 将原仙能水晶的用户可见名称改为“仙能电力水晶”，并新增条件注册的“仙能魔力水晶”和“仙能魔源水晶”。三种水晶共用同一套 Blockbench 结构、按水晶模型范围确定的碰撞箱、菜单、燃料规则、处理槽、额外槽、缓存和输出调度。
- 仙能电力水晶始终注册，不依赖 Mekanism 或其他用电模组即可声明和存储 FE；AE2、Refined Storage 与 NeoForge FE 能力读取同一份长整型 FE 账本。
- 安装 Botania 时注册绿色的仙能魔力水晶，支持魔力容器处理、魔力池等魔力容器的六面交互和火花联动；火花绑定在世界重载后保持，不重复挂载或掉落。
- 安装 Ars Nouveau 时注册紫色的仙能魔源水晶，支持魔源容器处理、魔源罐等魔源容器的六面交互和支配之杖指定魔源起始/重点；指定状态在世界重载后保持。
- 三种水晶共享默认 `800,000,000` 资源容量、燃烧时每 tick 产生 `1,000` FE/Mana/Source、相同燃料 tick 规则和可外部调整的容量/产出配置。仙元仅作为燃料使用；无源方块的旧额外资源仙元转化停用，源方块自身的仙元转化保持不变。
- 新增水晶组配方：已存在的变体之间可通过切石互转，也可在合成台用单个无序配方转换为顺序中的下一个已存在变体；Botania、Ars Nouveau 不存在时，对应水晶和配方不会注册。

##### 仙能水晶界面、充电和面向规则

- 仙能水晶界面复用模拟灵田/模拟轮回炼化炉的左侧三槽布局；右侧改为整块的大型资源仪表，按资源量从下向上填充，悬停显示当前量/上限，FE 读取使用长整型数值。处理箭头为静态指示，不把充电进度绘制到箭头中；运行时亮度为 15，停止时不发光。
- 处理槽支持可充电物品、类赛特斯石英以及 Botania/Ars Nouveau 对应的魔力/魔源容器；支持一组物品连续处理。完成的充电结果进入额外槽并同步显示，额外槽不允许主动放入。
- 额外槽中的充能结果不论仙窍输出是否开启都不会写入仙窍，也不会被六面自动输出到其他面；自动化只能从底面抽出。统一面向规则为顶部输入处理物，四个水平侧面输入燃料，底面不接受输入。
- 资源处理优先级统一为处理/充电、六面自动输出、仙窍输出；每个逻辑 tick 都会尝试把允许路径上的缓存全部传输，不再人为限制面输出、仙窍输出或充电输出速率。水晶可被存储管线、AE2、RS 和标准资源能力读取，绑定的仙窍缓存只按一个账本去重。

#### 变更与修复

##### 绑定、缓存和自动输出

- 仙能水晶、模拟灵田和模拟轮回炼化炉统一使用“个人仙窍维度优先，匹配仙灵驱动器回退”的绑定规则；不再使用放置者、最近玩家或不匹配启动器作为绑定对象。位于个人仙窍维度的仙能水晶优先绑定该仙窍所属玩家。
- 仙窍输出开关与六面自动输出开关完全独立。仙窍输出只控制向所属玩家仙窍输出，自动输出只控制向临近方块六面输出；三个机器的开关状态、设置界面文字和打开中的菜单同步更新。
- 仙窍输出只能在存在有效绑定对象和对应外部资源端点时开启。开启时先把本地资源缓存全部发送到所属仙窍并清零，再将资源容器读取绑定到仙窍账本；关闭时解除绑定并从归零的内部缓存重新累积，不把仙窍资源拉回机器。
- 仙窍输出开启后，界面和 FE/Mana/Source 能力显示绑定仙窍的实际资源量；超过水晶容量时按满槽显示。处理物充电优先使用本地缓存，剩余需求再使用已绑定的仙窍账本。
- 个人仙窍维度绑定的水晶关闭仙窍输出后，在燃料槽为空时停止自动仙元补货；仙灵驱动器绑定的水晶关闭仙窍输出不影响驱动器燃料替代和补货行为。直接放入的仙元/真元燃料仍按物品燃料规则工作。
- 六面输出、仙窍接口和三台模拟机器均改为每 tick 尝试向所有允许的面输出全部缓存，不限制单面输入输出速率；高倍速环境下仍按外部加速模组的实际倍速推进，不使用慢速追赶或跳帧补偿。

##### 源方块、源方块管理器和动态渲染

- 源方块管理器的边框从建模到渲染直接复用源方块边框，只保留动态 core 与其相关渲染；原有八核、UV 分区、方块结构和六面中心对称关系不变。管理器的物品栏预览尺寸与源方块一致，并显示动态核心状态。
- 源方块管理器在世界/区块重载后继续显示服务端保存的成员进度，不再因客户端临时空成员数据而清零；源方块输出角标和动态物品栏结构在物品、创造栏、JEI/EMI 等标准预览路径保持一致。
- 方块和流体源方块悬浮核心先按目标方块/流体真实几何中心锁定，再进行缩放、浮动和缓慢转动；方块与流体保持半透明，流体每个面读取对应流体的动态材质。物品源方块使用目标物品真实几何中心，保持更大的不透明渲染，并固定竖直绕中心 Y 轴旋转。
- 源方块悬浮动画改为连续客户端时钟和稳定随机姿态，在其他模组加速时按倍速播放，避免加速后缓慢追赶、半秒跳帧、偏心和旋转漂移；外部注入的资源源方块使用同一套中心、透明度、材质和动画兼容规则。
- 源方块、源方块管理器和仙窍管理器的框架贴图增加符合原版风格的像素层次，保留黑色源方块/管理器框架、灰色仙窍管理器框架、原有模型结构和 UV 点对点映射；源方块管理器八个内部 core 保持半透明并增加层次；水晶晶体纹理增加分区切面层次，保留原色调、结构和 `80%` 半透明度。

##### 迷你仙墟、灵器与手册

- 迷你仙墟的作用范围固定为以方块为中心的 `13×1×13` 单层区域：水平方向各向外 6 格，垂直方向只覆盖所在方块层；上下相邻层不再受到吸引、排斥、传送或伤害。
- 主手或副手持有迷你仙墟的玩家始终不受其作用，无论玩家作用开关状态如何；该规则同时适用于普通作用和链接传送筛选。物品 Tooltip 与双语帕秋莉手册同步说明范围、持有者保护、玩家开关、吸引/排斥和链接传送用法。
- 灵器用户可见的“镐子模式”统一改名为“挖掘模式”；内部模式编号和资源键保持兼容，既有物品无需迁移，双语提示和古玉手册同步更新。
- 帕秋莉手册新增双语“仙能电力水晶行为”和“水晶联动”章节，完整说明三槽界面、绑定优先级、两种输出开关、缓存迁移、面向规则、充电/输出优先级、长整型资源读取，以及 Botania 魔力水晶、Ars Nouveau 魔源水晶、火花、支配之杖和条件配方。

### English

This section is the complete user-visible delta from `0.0.10` to `0.0.11`. It intentionally omits internal debugging, failure-reproduction, and build-process history.

#### Added

##### Xianeng crystal family

- Renamed the existing Xianeng Crystal to **Xianeng Electricity Crystal** and added conditionally registered **Xianeng Mana Crystal** and **Xianeng Source Crystal**. All three share the same Blockbench structure, model-bounded collision box, menu, fuel rules, processing slot, extra slot, cache model, and output scheduler.
- Xianeng Electricity Crystal is always registered and declares/stores FE without Mekanism or any other power mod. NeoForge FE, Applied Energistics 2, and Refined Storage read the same long-valued FE ledger.
- When Botania is installed, the green Xianeng Mana Crystal supports mana containers, mana pools and other registered mana containers on its faces, plus Spark integration. Spark attachment persists across reloads without duplicate attachment or item drops.
- When Ars Nouveau is installed, the purple Xianeng Source Crystal supports source containers and source jars on its faces, plus Dominion Wand source-start/priority selection. Those selections persist across reloads.
- All three crystals share a default capacity of `800,000,000`, produce `1,000` FE/Mana/Source per server tick while burning, use the same fuel-tick rules, and expose configurable capacity/output values. Immortal Yuan is fuel only; the old extra-resource Yuan conversion for non-source blocks is disabled while the Source Vein's own Yuan conversion remains active.
- Added crystal-family recipes: stonecutting converts between variants that exist, and a single shapeless recipe converts to the next existing variant in the family order. Botania- and Ars-dependent crystals and recipes are absent when their addons are absent.

##### Crystal UI, charging, and face rules

- The crystal screen reuses the left three-slot layout of the Simulated Spirit Field and Simulated Reincarnation Furnace. The right side is a single large resource meter filled from bottom to top, with a current/maximum long-value tooltip. The processing arrow is static, the running light level is 15, and an idle crystal is dark.
- The processing slot accepts rechargeable items, Certus-Quartz-like items, and the corresponding Botania/Ars Nouveau resource containers, and can process a full input stack continuously. Completed charges appear in the extra slot and synchronize to the open menu; the extra slot cannot be manually filled.
- Charged results in the extra slot never enter Xianqiao, regardless of the Xianqiao Output switch, and are never pushed through other faces. Automation may extract them only from the bottom. The common face rule is top processing input, four horizontal sides fuel input, and no bottom input.
- Processing/charging has priority over six-face automation, which has priority over Xianqiao output. Every logical tick attempts to transfer all available cache through every permitted path without an artificial face, Xianqiao, or charging rate cap. Storage pipes, AE2, RS, and standard resource capabilities can read the crystals, while bound Xianqiao caches are deduplicated to one ledger.

#### Changed and fixed

##### Binding, caches, and automation

- Xianeng crystals, Simulated Spirit Fields, and Simulated Reincarnation Furnaces now share the binding priority **personal Xianqiao realm first, matching Spirit Drive second**. Placement player, nearest player, and mismatched drives are not binding sources. A crystal placed in a personal realm binds that realm's owner first.
- Xianqiao Output and Automatic Output are independent switches. Xianqiao Output controls only transfer to the bound owner's personal realm; Automatic Output controls only six-face transfer to adjacent blocks. Switch state, settings labels, and open-menu data remain synchronized for all three machines.
- Xianqiao Output can be enabled only with a valid bound owner and external resource endpoint. Enabling flushes the local resource cache into the owner's Xianqiao and clears it before binding the resource container to the Xianqiao ledger. Disabling unbinds the external container and resumes accumulation from the zeroed local cache; it does not pull Xianqiao resources back into the machine.
- While bound, the crystal screen and FE/Mana/Source capabilities expose the owner's actual Xianqiao ledger, rendering full when it exceeds the crystal capacity. Charging consumes local cache first and then the bound Xianqiao ledger.
- For a realm-bound crystal, disabling Xianqiao Output stops automatic Immortal Yuan refilling when the fuel slot is empty. A Spirit-Drive-bound crystal keeps its drive-based fuel replacement behavior when Xianqiao Output is disabled. Direct True Yuan and Immortal Yuan item fuel remains independent of that switch.
- Six-face output, Xianqiao Interfaces, and the two simulated machines now attempt to output all available cache on every tick through every allowed face without an artificial per-face rate limit. Under high-speed acceleration, animation and machine work advance at the actual multiplier instead of slowly catching up or jumping frames.

##### Source Veins, Manager, and dynamic rendering

- Source Vein Manager now reuses the Source Vein frame directly in both model and rendering, retaining only the dynamic core renderer. The existing eight-core layout, UV islands, block structure, and six-face rotational symmetry are unchanged. Its inventory preview matches the Source Vein preview size and displays the dynamic core state.
- Source Vein Manager progress remains visible after a world or chunk reload from server-persisted member data; it no longer resets from empty client-side member data. Source output badges and dynamic item previews follow the same standard path in inventory, creative, JEI, and EMI.
- Block and fluid Source Vein floating cores are centered from the target block/fluid's real geometry before scaling, floating, or rotating. Blocks and fluids remain translucent, and fluids use the corresponding dynamic texture on every face. Item Source Veins use the target item's real geometry centre, a larger opaque preview, and fixed upright rotation around the Y axis.
- Source Vein animation uses a continuous client clock and stable random orientation, respecting external acceleration as a true multiplier instead of slowly catching up or jumping. Externally injected source resources use the same centre, alpha, texture, and animation compatibility rules.
- Source Vein, Source Vein Manager, and Xianqiao Manager frame textures gain vanilla-style pixel depth while preserving the black Source/Manager palette, grey Xianqiao Manager palette, original geometry, and point-to-point UV mapping; the Manager's eight internal cores remain translucent with layered shading. Crystal textures gain faceted section detail while preserving the original palette, structure, and `80%` alpha.

##### Miniature Immortal Ruin, Spirit Instrument, and handbook

- Miniature Immortal Ruin influence is now an exact single-layer `13×1×13` area: six blocks outward horizontally and only the ruin's own block layer vertically. Adjacent Y layers are no longer pulled, repelled, teleported, or damaged.
- Players holding a Miniature Immortal Ruin in either hand are always excluded from its effects, regardless of the player-affect switch. The rule also applies to ordinary effects and linked-teleport filtering. The item tooltip and bilingual Patchouli handbook document the range, holder protection, player switch, attraction/repulsion, and linked-teleport usage.
- Renamed the user-facing Spirit Instrument “Pick Mode” to **Mining Mode**. Internal mode ids and resource keys remain compatible, so existing items need no migration; bilingual prompts and the Ancient Jade handbook are synchronized.
- Added bilingual Patchouli chapters for **Xianeng Electricity Crystal Behavior** and **Crystal Integrations**, covering the three-slot UI, binding priority, independent output switches, cache migration, face rules, charging/output priority, long-valued resource access, Botania Mana Crystal, Ars Nouveau Source Crystal, Spark, Dominion Wand, and conditional recipes.

## [0.0.10]

本节完整列出 `0.0.9` → `0.0.10` 的用户可见变化。

### 简体中文

#### 新增与兼容

- 新增可选 Mekanism 化学品容器与仙窍终端的双向交互。阶段 8 及以上可右键手持已装容器将化学品存入仙窍，左键点击已选中的化学品条目并手持空容器取出化学品；操作遵循流体/流体容器的容器驱动语义。
- 化学品容器事务由服务端负责：校验实时菜单、阶段 8 外部资源权限、菜单 revision、化学品频道/注册表身份，并使用模拟、执行、返回容器与存储回滚处理，支持堆叠容器而不产生半完成转移。
- Mekanism 类仅位于可选兼容边界；未安装 Mekanism 时公共终端、网络和存储代码仍可加载，安装后才注册化学品容器能力桥。
- 补充双语帕秋莉古玉手册的缺失内容：新增仙窍管理器章节，扩展世界碎片开采器/聚宝盆的 27 格缓存、2400 刻战利品周期与数据包规则，并同步补充终端、源方块、仙窍接口、AE2/RS、可选联动和三类灵剑系数说明。
- 手册条目树契约现在检查中英文资源的结构一致性、JSON 合法性，以及新增系统与关键规则均已进入随 JAR 内置的 Patchouli 手册。

#### 优化与界面

- 统一三类灵剑的淬火系数来源：一气归元剑为 `0%/淬火点`，普通灵剑为 `1%/淬火点`，仙墟锻灵剑为 `1.5%/淬火点`；Tooltip 预览、标准主手攻击属性和战斗计算使用同一剑型规则。
- 仙窍存储的整理、全部存入、按筛选取出三个 `8×8` 操作按钮移动到存储网格与玩家背包之间的空隙，并保持原版背包槽和外部整理器识别契约。
- 仙窍管理时间流速改为居中的 `- 数值 +` 对称布局：标题独立居中，数值位于两个调速按钮之间，白天/天气控制保持在下一行。
- 时间流速标题和数值改用显式关闭阴影的原版文字绘制路径，消除截图中的文字重影并保持像素字体清晰。

#### 修复

- 修复一气归元剑 Tooltip 将每点淬火误显示为 `1%` 的问题；现在显示并使用 `0%/淬火点`。
- 修复仙墟锻灵剑 Tooltip 将每点淬火误显示为 `1%` 的问题；现在显示并使用 `1.5%/淬火点`，普通灵剑仍为 `1%`。
- 修复仙墟锻灵剑的 `1.5%` 淬火系数因浮点转换显示为 `1.49999……%` 的问题；Tooltip 百分比现在最多保留两位小数，并自动去除无意义的尾零。
- 固定外部资源阶段边界常量，避免能量、化学品、魔力和工业先锋灵魂等频道在阶段不足时通过不同入口出现不一致权限。

#### 支持范围、验证与发布

- 版本更新为 `0.0.10`；构建基线为 Minecraft 1.21.1、NeoForge 21.1.235、Java 21，正式 NeoForge 支持范围扩展为 `[21.1.235,21.2)`，网络协议保持 8。
- 已采纳并合并 PR #2 与 PR #3：JEI 基线为 `19.27.0.343`，Refined Types 接受 `0.3.x`/`1.21.1-0.3.x`，并以 `[21.1.235,21.2)` 关闭 Issue #1；对应版本清单、适配器描述和 JAR 元数据保持一致。
- 兼容范围在 NeoForge `21.1.236` 与 `21.1.248` 上分别执行 `clean test`，每次均为 **724 tests / 0 failures / 0 errors / 0 skipped**；正式 21.1.235 基线完整门禁同样通过。
- 以合并后 0.0.10 JAR 在 35 模组临时客户端中分别启动 Sodium `0.8.12-alpha.4` 与 Embeddium `1.0.15` 栈，并加入 ModernFix `5.27.20`、FerriteCore `7.0.3`、ImmediatelyFast `1.6.11`、Entity Culling `1.10.5`；两次均完成 NeoForge 客户端启动、资源重载、声音引擎初始化和 ImmortalStorage 可选联动注册，无优化模组或仙藏崩溃。
- Sodium 与 Embeddium 分别作为二选一渲染后端验证，不能同时安装；生产 JAR 静态检查未发现 Sodium、Embeddium、Rubidium、Lithium、FerriteCore、ModernFix、ImmediatelyFast、Entity Culling、Iris、Oculus、Starlight 或相关优化 Mixin 的类名、引用或目标，仙藏继续只依赖标准 Minecraft/NeoForge 渲染入口。上述为启动、资源重载、声音引擎和联动注册烟测，不替代逐项玩法/UI 实机回归。
- 合并来源为 [PR #2](https://github.com/positer/ImmortalStorage/pull/2)（合并提交 `310a857ee624ed29b3f9f5fa75a08ca0d9431e72`）与 [PR #3](https://github.com/positer/ImmortalStorage/pull/3)（合并提交 `670b0968e00c951272418f1f0904c45e262bc8ec`）；[Issue #1](https://github.com/positer/ImmortalStorage/issues/1) 已关闭并标记为完成。
- 最终重发状态为提交 `13f896c85a45ca55fc9ac846fda6ac2a9aefd5da`、标签 `0.0.10` 和 [GitHub Release 0.0.10](https://github.com/positer/ImmortalStorage/releases/tag/0.0.10)。制品 `immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.10.jar` 大小为 `5,163,055` 字节，SHA-256 为 `EA09A8493367E4E05A4C04D520FCB6E74EBF6409DC103E5BE0A4AE2ACD6564B4`；远端重新下载哈希与本地干净构建完全一致。
- 完整门禁包含 `test`、`build`、Ars Source API、无 AE2 运行时、生产 JAR 边界、版本组成和精确版本产物检查，结果为 **199 suites / 724 tests / 0 failures / 0 errors / 0 skipped**。

### English

This section is the complete user-visible delta from `0.0.9` to `0.0.10`.

#### Added and compatibility

- Added optional Mekanism chemical-container interaction with the Xianqiao terminal. From stage 8 onward, right-clicking with a filled container deposits its chemical into Xianqiao, while left-clicking a selected chemical entry with an empty container withdraws it; the interaction follows the existing fluid/container model.
- Made the chemical-container transaction server-authoritative: it validates the live menu, stage-8 external-resource access, menu revision, chemical channel/registry identity, and uses simulate, execute, returned-container handling, and storage rollback to support stacked containers without partial transfers.
- Kept Mekanism classes behind the optional compatibility boundary. Common terminal, network, and storage code remains loadable without Mekanism; the chemical-container hook is installed only when Mekanism is present.
- Expanded the bilingual Patchouli Ancient Jade handbook with a dedicated Xianqiao Manager chapter, World Shard Miner/Treasure Basin 27-slot cache, 2400-tick loot cycle and datapack rules, plus deeper terminal, Source Vein, Xianqiao Interface, AE2/RS, optional-integration, and sword-coefficient guidance.
- Handbook contracts now enforce bilingual entry-tree parity, valid JSON, and inclusion of the new systems and key rules in the bundled Patchouli handbook.

#### Improvements and interface

- Unified all three sword tempering coefficient sources: One-Qi Returning Origin Sword `0%/tempering point`, ordinary Spirit Sword `1%/tempering point`, and Immortal-Ruin-Forged Spirit Sword `1.5%/tempering point`. Tooltip previews, standard main-hand attributes, and combat calculations now use the same item-specific rule.
- Moved the three Xianqiao storage `8×8` sort, deposit-all, and filtered-withdraw buttons into the gap between the storage grid and player inventory while preserving vanilla player slots and external sorter recognition.
- Reworked realm time flow into a centered symmetric `- value +` row: the title is centered separately, the value sits between the two controls, and day/weather controls remain on the following row.
- Switched the time-flow title and value to the explicit no-shadow vanilla text-rendering overload, removing the visible ghosting while retaining the pixel-font layout.

#### Fixed

- Fixed the One-Qi Returning Origin Sword tooltip showing `1%` per tempering point; it now shows and uses `0%/tempering point`.
- Fixed the Immortal-Ruin-Forged Spirit Sword tooltip showing `1%` per tempering point; it now shows and uses `1.5%/tempering point`; the ordinary Spirit Sword remains at `1%`.
- Fixed the Immortal-Ruin-Forged Spirit Sword `1.5%` tempering coefficient being rendered as `1.49999…%` after floating-point conversion; tooltip percentages now use at most two decimal places and trim insignificant trailing zeroes.
- Centralized the external-resource stage boundary so energy, chemicals, mana, Industrial Foregoing souls, and similar channels cannot expose inconsistent access through separate entry points below their unlock stage.

#### Support, verification, and release

- Bumped the version to `0.0.10`; the build baseline is Minecraft 1.21.1, NeoForge 21.1.235, and Java 21, while the official NeoForge support range is now `[21.1.235,21.2)`, with network protocol 8 unchanged.
- Accepted and merged PR #2 and PR #3: JEI is based on `19.27.0.343`, Refined Types accepts `0.3.x`/`1.21.1-0.3.x`, and `[21.1.235,21.2)` closes Issue #1; the supported-version manifest, adapter descriptor, and JAR metadata stay aligned.
- The compatibility range passed `clean test` separately on NeoForge `21.1.236` and `21.1.248`, with **724 tests / 0 failures / 0 errors / 0 skipped** each time; the formal 21.1.235 baseline also passed the complete gate.
- The merged 0.0.10 JAR started in two 35-mod temporary clients: a Sodium `0.8.12-alpha.4` stack and an Embeddium `1.0.15` stack, each also using ModernFix `5.27.20`, FerriteCore `7.0.3`, ImmediatelyFast `1.6.11`, and Entity Culling `1.10.5`. Both reached NeoForge client startup, resource reload, sound-engine initialization, and ImmortalStorage optional-integration registration without an optimization-mod or ImmortalStorage crash.
- Sodium and Embeddium were tested as mutually exclusive renderer backends; they must not be installed together. Static inspection of the production JAR found no Sodium, Embeddium, Rubidium, Lithium, FerriteCore, ModernFix, ImmediatelyFast, Entity Culling, Iris, Oculus, Starlight, or related optimization-mixin class names, references, or targets. ImmortalStorage continues to use standard Minecraft/NeoForge rendering entry points. This covers startup, resource reload, sound-engine, and integration-registration smoke checks rather than every gameplay/UI path.
- The merged sources are [PR #2](https://github.com/positer/ImmortalStorage/pull/2), merge commit `310a857ee624ed29b3f9f5fa75a08ca0d9431e72`, and [PR #3](https://github.com/positer/ImmortalStorage/pull/3), merge commit `670b0968e00c951272418f1f0904c45e262bc8ec`; [Issue #1](https://github.com/positer/ImmortalStorage/issues/1) is closed as completed.
- The final republish is commit `13f896c85a45ca55fc9ac846fda6ac2a9aefd5da`, tag `0.0.10`, and [GitHub Release 0.0.10](https://github.com/positer/ImmortalStorage/releases/tag/0.0.10). The artifact `immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.10.jar` is `5,163,055` bytes with SHA-256 `EA09A8493367E4E05A4C04D520FCB6E74EBF6409DC103E5BE0A4AE2ACD6564B4`; a fresh remote download matches the local clean build exactly.
- The complete gate includes `test`, `build`, the Ars Source API check, no-AE2 runtime verification, production-JAR boundary, version composition, and exact-version-artifact checks. It passes **199 suites / 724 tests / 0 failures / 0 errors / 0 skipped**.

## [0.0.9]

本节完整列出相对正式版 `0.0.8` 的全部用户可见变化。

### 简体中文

#### 新增与迁移

- 新增统一持久玩家身份：个人仙窍维度不再直接依赖启动器本次会话 UUID。首次加载 0.0.8 及更早存档时，依次从已保存的仙窍重生维度、仙窍退出维度或一致的旧绑定物品所有者执行一次性迁移；没有可靠旧证据时保留原 UUID 目录，不覆盖、复制或猜测维度数据。
- 仙窍存储磁盘、AE2 交换磁盘、RS 交换磁盘、仙灵驱动器、替死傀儡、仙窍/源方块管理器、普通/高级仙窍接口、源方块、世界碎片矿机与聚宝盆、轮回炼化炉、模拟轮回炼化炉、模拟灵田、渡劫与个人存储/燃料解析统一使用同一持久身份。
- 合法的旧会话 UUID 绑定会在首次交互时原地升级；磁盘独立 ID、物品内容与组件、耐久、替死傀儡锚点和机器数据全部保留。不同启动器启动同一用户时不再出现“物品栏/仙窍存储一致，但个人维度或绑定物品失联”。
- 完整补齐古玉指导书在后续版本中遗漏的双语章节：一气归元剑、拘灵器、仙灵驱动器、模拟轮回炼化炉、模拟灵田、回响碎片源方块、纠缠/高级/高级纠缠稳定化迷你仙墟，以及新版个人仙窍天气和持久身份迁移说明；灵剑、替死傀儡、源方块、兼容与个人仙窍旧章节同步校正。

#### 材质、模型与渲染

- 源方块管理器采用 `design/SourceVeinController.bbmodel` 新外观：12 根黑色 Edge 梁构成顶底开敞的镂空笼，内部 8 个 3×3×3 源核按 72 格容量映射为八段状态，使用空=蓝、使用中=紫、满=红的纯色不透明材质，并以 4°/tick 绕中心 Y 轴整体旋转。
- 源方块管理器移除旧六格指示器和 `source_vein_manager_side/top/front.png` 三张旧贴图，改用 `source_vein_manager_edge.png` 与三张独立核心材质；方块模型按世界方向映射 UV，内部开口保持无面遮挡。
- 源方块管理器手持/物品栏改为方块物品 BEWLR：标准笼模型上叠加与世界完全相同的八段旋转核心，优先读取拆下物品保留的 `DisplayState`，其次按序列化成员数推导。
- AE2 与 RS 的仙藏额外资源渲染统一复用 `ExternalResourceCatalog` 的名称、颜色和纹理定义。化学品严格采用仙藏一致的注册表取样纯色；FE、Mana、Source、Souls 等非纯色资源使用仙藏自带纹理，未安装资源渲染附属时不再借用交换磁盘模型。

#### 修复

- 修复摧毁源方块管理器时 `Missing id for entity in: {CachedUnits:...}` 导致的崩溃：管理器为新建/免费源成员写入 `CachedUnits` 时补齐方块实体字符串 `id`，已有 `id` 的成员只更新缓存；模拟轮回炼化炉拆下/保存同步改用含完整元数据的序列化路径。
- 修复源方块管理器顶面圆环不可见：顶部四根圆环梁补齐 `up` 面，底部四根补齐 `down` 面，复用 edge 贴图既有不透明采样区而不增加额外贴图。
- 修复源方块管理器手持/物品栏只显示空笼，动态核心、阶段颜色和拆下状态现在与世界内渲染一致。
- 修复 AE2 终端渲染或悬停 `immortalstorage:external_resource` 时因缺失客户端 `AEKeyRendering` 处理器而崩溃；新增受 AE2 可选加载边界保护的客户端注册，服务端与无 AE2 运行时不加载 AE2 客户端类。
- 修复 RS 合成终端触及 `RsExternalResource` 时因缺少网格仓库 Mapper 而断开连接；通过官方 `addGridResourceRepositoryMapper` 注册安全网格条目，名称、检索、数量、提示和提取分发均正常。
- 修复 RS 无额外资源附属时不显示仙藏额外存储，以及安装/移除附属后旧回退键可能失效的问题；同一账本资源只枚举一次，回退键继续可读写。
- 修复 RS 化学品渲染错误：`solidColor` 取样与纹理 `blit` 改为互斥分支，不再在绿色、灰色、粉色等化学品色块上叠加蓝色图标。
- 修复仙窍雪天气可能被误解为服务端降水并纳入强制加载的问题：SNOW 只保留客户端粒子，服务端不启用 rain/thunder；强制加载的玩家改动区块只接受玩家/实体驱动的破坏与放置事件。

#### 兼容与验证

- 仅安装 Refined Storage 2.0.9 时，仙窍 RS 交换磁盘以内置资源类型显示 FE、Mana、Source、Souls 与 Mekanism 化学品。
- 安装 Refined Types 0.3.x 时优先使用其 FE/Source/Souls 原生键；安装官方 RS Mekanism Integration 1.x 时优先使用其化学品原生键。仙藏回退键保留最低优先级，避免升级或移除附属后数据不可读。
- ExtraStorage 等标准 RS 容量附属继续通过官方 `StorageContainerItem` 契约共存，不需要硬依赖或专用分支；Refined Types 与 RS Mekanism Integration 仍是可选依赖。
- 正式支持范围保持 Minecraft 1.21.1、NeoForge 21.1.235、Java 21；网络协议保持 8。
- 完整门禁通过 **196 suites / 717 tests / 0 failures / 0 errors / 0 skipped**，并通过生产 JAR 边界、版本组成、精确版本制品、Ars Source API 与无 AE2 运行时验证；新增手册契约会检查中英条目树一致及后续版本主要系统覆盖。
- 30-JAR PCL2 全模组实例完成 JDK 21、`zh_cn` 实机验证：绑定旧 AE2/RS 磁盘可打开终端，RS 化学品取样颜色正确，`Missing render handler`、`No factory for class`、高级屏幕处理失败和仙藏外部资源纹理加载失败均为 0。
- 0.0.9 最终制品为 `immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.9.jar`，大小 `5,137,573` 字节，SHA-256 为 `79EECB2B262C0FCC27C0AA0CC7A67BE89A79458D03A8F8C426F98ED94B74B870`。
- 版本更新为 `0.0.9`。

### English

This section is the complete user-visible delta from release `0.0.8`.

#### Added and migrated

- Added one persistent player identity for personal realms. The realm key no longer depends directly on the launcher's current session UUID. On the first load of a 0.0.8-or-earlier save, a one-shot migration uses the saved realm respawn dimension, saved realm exit dimension, or one consistent legacy-bound item owner in that order; without reliable evidence, the existing UUID directory is preserved rather than copied or guessed.
- Xianqiao storage disks, AE2/RS exchange media, Spirit Drives, Substitute Puppets, Xianqiao/Source Vein Managers, plain/Advanced Xianqiao Interfaces, Source Veins, World Shard machines, reincarnation machines, Simulated Spirit Fields, tribulation ownership, and personal-storage/fuel resolution now share that same stable identity.
- Valid legacy session-UUID bindings upgrade in place on first interaction. Independent disk IDs, item contents/components, durability, puppet anchors, and machine data are preserved. Starting the same user through another launcher no longer separates the personal realm while inventory and Xianqiao storage remain shared.
- Completed the bilingual Ancient Jade handbook for systems missed by later updates: One-Qi Returning Origin Sword, Soul Catcher, Spirit Drive, Simulated Reincarnation Furnace, Simulated Spirit Field, Echo Shard Source Vein, Entangled/Advanced/Advanced Entangled Stabilized Ruins, plus current personal-realm weather and persistent-identity migration. The older sword, puppet, source, compatibility, and realm chapters were corrected at the same time.

#### Materials, models, and rendering

- Replaced the Source Vein Manager appearance with `design/SourceVeinController.bbmodel`: 12 black Edge beams form an open-top/open-bottom cage, while eight inner 3×3×3 cores map the 72-slot capacity to eight stages, use opaque empty-blue / used-purple / full-red materials, and rotate as one rigid body around Y at 4°/tick.
- Removed the old six-slot indicator and `source_vein_manager_side/top/front.png` textures. The new model uses `source_vein_manager_edge.png` plus three dedicated core textures, world-direction UV mapping, and an unobstructed inner opening.
- Hand/inventory rendering now uses a block-item BEWLR that overlays the same eight-stage rotating core on the standard cage model, reading persisted `DisplayState` first and serialized member count as fallback.
- AE2 and RS external-resource entries now share `ExternalResourceCatalog` names, colors, and textures. Chemicals use the same registry-sampled solid colors as Xianqiao; non-solid FE/Mana/Source/Souls entries use bundled ImmortalStorage textures when no resource-rendering addon is present.

#### Fixed

- Fixed `Missing id for entity in: {CachedUnits:...}` when destroying a Source Vein Manager. Fresh/free member data now receives the source-vein block-entity string `id`; existing IDs are preserved while cache data changes, and Simulated Reincarnation Furnace pickup/save uses full metadata.
- Fixed the Source Vein Manager top ring being invisible by adding `up` faces to the four top beams and `down` faces to the four bottom beams, reusing opaque regions of the existing edge texture.
- Fixed the Source Vein Manager hand/inventory item showing only an empty cage; its dynamic core, stage colors, and persisted pickup state now match world rendering.
- Fixed the AE2 terminal crash caused by a missing client `AEKeyRendering` handler for `immortalstorage:external_resource`; client registration remains behind the optional AE2 class-loading boundary and does not affect dedicated servers or no-AE2 runtimes.
- Fixed the RS crafting-terminal disconnect caused by the missing repository mapper for `RsExternalResource`. The official `addGridResourceRepositoryMapper` extension point now supplies safe grid entries with correct naming, search, amount, tooltip, and extraction dispatch.
- Fixed missing ImmortalStorage external resources with base RS and preserved readability when resource addons are installed or removed. Each ledger resource is emitted once and legacy fallback keys remain readable/writable.
- Fixed RS chemical rendering: sampled `solidColor` fills and textured `blit` rendering are now mutually exclusive, so no blue icon is overlaid on green, gray, pink, or other chemical swatches.
- Fixed personal-realm snow being eligible for server precipitation/forced-load interpretation. SNOW remains client particles only, rain/thunder stay disabled, and the forced-chunk set accepts only player/entity-driven break/place changes.

#### Compatibility and validation

- With Refined Storage 2.0.9 alone, Xianqiao RS exchange disks expose FE, Mana, Source, Souls, and Mekanism chemicals through the built-in resource type.
- Refined Types 0.3.x native FE/Source/Souls keys and official RS Mekanism Integration 1.x chemical keys take priority when installed. The ImmortalStorage fallback remains at lowest priority for upgrade/removal safety.
- Standard RS capacity addons such as ExtraStorage continue to coexist through the official `StorageContainerItem` contract; Refined Types and RS Mekanism Integration remain optional.
- Official support remains Minecraft 1.21.1, NeoForge 21.1.235, and Java 21; network protocol remains 8.
- The full gate passes **196 suites / 717 tests / 0 failures / 0 errors / 0 skipped**, plus production-JAR boundary, version composition, exact-version artifact, Ars Source API, and no-AE2 runtime verification. New handbook contracts enforce bilingual entry-tree parity and later-version major-system coverage.
- A 30-JAR full-mod PCL2 client passed JDK 21 / `zh_cn` live validation: bound legacy AE2/RS disks open their terminals, RS chemicals use the correct sampled colors, and there are zero `Missing render handler`, `No factory for class`, advanced-screen handling, or ImmortalStorage external-texture load failures.
- The final 0.0.9 artifact is `immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.9.jar`, `5,137,573` bytes, with SHA-256 `79EECB2B262C0FCC27C0AA0CC7A67BE89A79458D03A8F8C426F98ED94B74B870`.
- Bumped the version to `0.0.9`.

## [0.0.8]

本节完整列出相对正式版 `0.0.7` 的全部用户可见变化。

### 简体中文

#### 新增

- 新增高级仙窍接口（`advanced_xianqiao_interface`）：使用 `design/advanced-xianqiao-interface/` 的空心框架 + 浮空核心 + 顶部圆垫设计稿模型（默认 scale display）。保留普通仙窍接口的九个物品/流体/电力/化学品缓存槽与物主绑定，并额外支持对配置范围内容器的流体、电力、化学品输入/输出。
- 高级仙窍接口复用普通仙窍接口一致的六面（抽/推/关）三状态与左侧主动抽入/主动推出开关，但把这两种行为应用到可配置包围盒内的每一个容器：抽（PULL）从区域内每个容器经该配置面提取全部可交互内容（物品/流体/电力/化学品）进入仙窍；推（PUSH）从接口缓存中把允许该面交互的缓存槽按配置面向区域内容器推出。
- 高级仙窍接口新增与高级稳定化迷你仙墟系列一致的配置页（⚙ tag）：xyz/+xzy 范围尺寸与偏移、周期、预览框开关、启用、访问（轮询跳过/强制轮询）、均分（逐个/按组）、顺序（远先/近先）。
- 世界中可选白色选区线框 + 逐面高亮：交互面保持白色半透明，PULL 面叠加绿色半透明、PUSH 面叠加红色半透明，实时跟随服务端配置。
- 高级仙窍接口保留本方块的植物魔法火花/魔力交互、新生魔艺魔源交互、工业先锋灵魂与通用机械严格能量/化学品能力。
- 高级仙窍接口合成：`{灵晶，迷你仙墟，灵晶，回响碎片，仙窍接口，回响碎片，灵晶，村民刷怪蛋，灵晶}` 有序 3×3。

#### 变更

- 仙窍接口、灵剑、仙墟锻灵剑、一气归元剑的材质/方块模型改用 `design/` 下的美化设计稿：仙窍接口由 `minecraft:block/cube` 换成 13 元素六面内凹模型（12 棱边框架 + 内陷核心），旧 `xianqiao_interface_front.png` 移除；三把剑采用 16×16 原版风格像素材质（一气归元剑保持水平镜像、剑尖朝左上并带光束枪口）。方块模型均忽略设计稿缩放参数，使用默认 scale display，物品栏与手持按标准方块尺寸绘制。
- 仙窍接口六面按钮只显示方位字母，开启状态由边框颜色与悬停提示体现，不再溢出；高级仙窍接口的六面按钮沿用该样式（抽/推/关循环、绿/红/灰边框），但按钮内不显示相邻方块。
- `XianqiaoInterfaceBlockEntity` 与 `XianqiaoInterfaceMenu` 解除 final 以便高级变体继承；`XianqiaoInterfaceMenu` 增加类型感知构造与 `configurationDataCount`/`readExtraData`/`writeExtraData` 数据槽扩展点。
- 仙窍接口/高级仙窍接口的主动抽推按钮与缓存槽管道抽取提示改为完整双语翻译键，不再硬编码中文。
- 版本提升为 `0.0.8`（`gradle.properties` 与 `versions/supported_versions.json`）。

#### 修复

- 修复高级仙窍接口选中范围预览不渲染：接口方块实体补上 `getUpdatePacket` 并在面模式变化时同步客户端，预览框与绿/红面高亮即时跟随服务端配置。
- 修复高级接口调度器化学品面参数不一致（改用与物品/流体/电力相同的面方向）。
- 修复高级接口的植物魔法火花/魔力与工业先锋灵魂能力注册（对高级方块实体类型注册）。

#### 兼容与验证

- 官方支持范围：Minecraft 1.21.1、NeoForge 21.1.235、Java 21。模组 ID、资源命名空间、网络协议（8）不变。
- 700 项自动化测试全部通过（失败/错误/跳过为 0）；生产 JAR 边界、版本组成、精确版本产物、Ars Source API 与无 AE2 运行时校验通过。
- 0.0.8 在 30-JAR 全模组 PCL2 配置（含 Create 6.0.10）中使用 JDK 21、`zh_cn` 完成 2560×1504 大窗口启动并进入个人仙窍维度，无 ImmortalStorage 致命错误。
- 古玉手册新增高级仙窍接口条目，并补全仙窍接口的 Push（推出）与 Release（缓存返还）双语说明。

### English

This section is the complete user-visible delta from release `0.0.7`.

#### Added

- Added the Advanced Xianqiao Interface (`advanced_xianqiao_interface`) with the `design/advanced-xianqiao-interface/` hollow-frame / floating-core / top-puck model (default-scale display). It keeps the plain interface's nine mixed item/fluid/power/chemical cache slots and owner binding, and adds fluid, power, and chemical input/output over a configurable bounding box.
- The Advanced interface reuses the plain interface's six-face PULL/PUSH/OFF three-state modes and the left-side active pull/push toggles, but applies them to every container inside the configured box: PULL extracts all interactive content (items/fluids/power/chemicals) from each in-area container's configured face into Xianqiao; PUSH exports the cache slots that permit that face into the in-area containers.
- Added an advanced-stabilized-ruin-style configuration page (⚙ tag): xyz/+xzy range size and offset, frequency, preview toggle, enabled, access (poll-skip/force-poll), split (item-by-item/group-by-group), and order (far-first/near-first).
- Optional world selection box with per-face highlights: interaction faces stay white translucent, PULL faces gain green translucent and PUSH faces red translucent, live with server config.
- The Advanced interface keeps the block's own Botania spark/mana, Ars Nouveau source, Industrial Foregoing Soul, and Mekanism strict-energy/chemical interactions.
- Crafting: `{spirit crystal, miniature immortal ruin, spirit crystal, echo shard, xianqiao interface, echo shard, spirit crystal, villager spawn egg, spirit crystal}` ordered 3×3.

#### Changed

- The Xianqiao Interface and the Spirit Sword / Immortal-Ruin-Forged Spirit Sword / One-Qi Returning Origin Sword now use the `design/` beautified textures/models. The interface block switched from `minecraft:block/cube` to a 13-element six-face recessed model (12 edge beams + recessed core); the old `xianqiao_interface_front.png` was removed. The swords are 16x16 vanilla-style pixel icons (One-Qi kept horizontally mirrored with the beam muzzle at the upper-left tip). Both block models ignore the design scale and use default-scale display, so inventory and hand-held rendering are standard block size.
- Interface six-face buttons now show only the direction letter; state is conveyed by border color and tooltip (no overflow). The Advanced interface reuses that style (PULL/PUSH/OFF cycle, green/red/gray borders) without the adjacent-block preview.
- `XianqiaoInterfaceBlockEntity` and `XianqiaoInterfaceMenu` are no longer final; the menu gained a type-aware constructor and `configurationDataCount`/`readExtraData`/`writeExtraData` data-slot extension points.
- The active pull/push buttons and the cache-slot pipe-extraction tooltips on both interfaces now use full bilingual translation keys instead of hardcoded Chinese.
- Version bumped to `0.0.8`.

#### Fixed

- Fixed the Advanced interface range preview not rendering: the block entity now overrides `getUpdatePacket` and re-syncs face-mode changes, so the box and green/red highlights follow server config live.
- Fixed the advanced scheduler chemical face inconsistency (now uses the same face as items/fluids/energy).
- Restored Botania spark/mana and Industrial Foregoing Soul capability registration for the Advanced interface block entity type.

#### Compatibility and validation

- Official support: Minecraft 1.21.1, NeoForge 21.1.235, Java 21. Mod ID, resource namespace, and network protocol (8) unchanged.
- All 700 automated tests pass (0 failures/errors/skips); production-JAR boundary, version composition, exact artifact, Ars Source API, and no-AE2 runtime checks pass.
- 0.0.8 was validated in the 30-JAR full-mod PCL2 setup (incl. Create 6.0.10) with JDK 21 and `zh_cn` at 2560×1504, entering the personal Xianqiao realm with no ImmortalStorage fatal error.
- The Ancient Jade handbook gained an Advanced Xianqiao Interface entry and expanded bilingual Push (export) and Release (cache-return) coverage.

## [0.0.7]

本节完整列出相对正式版 `0.0.6` 的全部用户可见变化。

### 简体中文

#### 新增

- 新增回响碎片源方块：稀有稀有度的不可堆叠源方块，1 回响碎片 : 16 仙元的批量兑换，八阶解锁；接入源方块管理器聚合与六面推出。附带成就（`immortalstorage_stage_8` 条件）、战利品表、合成配方、方块/物品模型、确定性的色相旋转框架纹理，并加入 `source_veins` 标签与镐类挖掘标签。
- 新增纠缠稳定化迷你仙墟：正常/反向两套相互独立、每 tick 并行运行的配置组，共享一个 54 格暂存缓冲——正常侧把范围内掉落的物品实体收集进缓冲，反向侧按各自频率/范围/过滤器把缓冲投出到目标容器；两套配置各自拥有独立的 20 格过滤器与独立的范围、频率、预览与启用开关，界面可切换。
- 新增高级稳定化迷你仙墟：以稳定化迷你仙墟为基础、蓝色哭泣黑曜石配色框架，拥有可调范围；作为独立容器（自带 54 格物品栏），对范围内每一个物品处理器按独立容器调度——正常模式从每个容器逐格提取一个允许堆叠进入仙墟自身物品栏，反向模式把仙墟自身物品栏均分投出到各容器。循环行为可配置：访问策略（轮询跳过 / 强制轮询）、均分方式（逐件 / 逐组）与访问顺序（远先 / 近先，按曼哈顿距离并以位置决定平局）。不产生世界掉落，无法容纳的物品停留在仙墟自身物品栏。
- 新增高级纠缠稳定化迷你仙墟：两套相互独立的高级调度配置（正常/反向）各自拥有暂存缓冲，共享一个 54 格缓冲与两套可切换菜单。
- 新增 `AdvancedRuinScheduler` 共享纯逻辑扫描/均分器，供两个高级变体复用。
- 三个新变体的渲染器复用既有的 `MiniatureImmortalRuinRenderer.drawDisc` 与共享 `arcane_machine_frame` JSON 框架；物品预览新增三个动态装饰：高级稳定化复用 `RuinCoreItemDecorator`，纠缠/高级纠缠使用 `EntangledRuinCoreItemDecorator`（两个反向旋转的黑白球体），模拟灵田使用 `SimulatedSpiritFieldItemDecorator`（湿润基底带 + 脉动作物符号）。
- 纠缠与高级纠缠的预览框现在正确渲染（相对方块坐标、外扩 0.01 避免与方块面深度冲突）；新增 `RuinFaceHighlightRenderer`，按已启用面在预览框上逐面绘制半透明白色高亮面。
- 全部四个容器交互面变体新增 2×3 交互面掩码网格（上行 UP/NORTH/DOWN，下行 WEST/SOUTH/EAST）：`RuinFaceGrid` 提供 28×18 的六个开关按钮，`RuinFaceText` 用 U/D/N/E/W/S 单字母标注；逐面启用并渲染预览高亮，六格全关 = 该侧完全不与容器交互。
- 水仙墟/岩浆仙墟现在可作为机械动力（Create）鼓风机触媒：`create:fan_processing_catalysts/splashing`（水仙墟）与 `blasting`（岩浆仙墟）标签使两者充当洗炼与鼓风烧炼触媒，并新增 `create:fan_transparent` 白名单使鼓风机风可以穿过源方块（与 Create 自身把烈焰人燃烧器同时标记为透明和触媒的模式一致）。
- 新增纠缠侧感知的联网负载：`SetEntangledRuinFilter`、`ToggleEntangledRuinFilterMode` 与 `SetEntangledRuinValue`（侧感知），两个纠缠菜单暴露 `setAuthoritativeValue(side, index, value)`。
- 新增合成配方：纠缠变体为无序合成；高级变体为 2×2 有序合成；高级纠缠可从高级变体进行 2×2 有序合成或无序转换。
- 验证了 AE 附属自动适配：安装 Applied Flux、Applied Mekanistics、Ars Énergistique、Applied Botanics 等附属时，其现有资源键自动优先映射到同一仙窍账本；未安装对应附属时回退到 ImmortalStorage 自有键，无需额外中介模组。

#### 变更

- 稳定化/高级稳定化仙墟界面：⚙ 设置与 ▦ 过滤面板改为互斥（打开一个自动关闭另一个）；高级稳定化的三个调度按钮（访问/均分/顺序）移入设置子菜单。
- 纠缠与高级纠缠仙墟按用户要求重建为“每侧独立配置、共享 54 格缓冲”结构：行为/过滤器各侧独立，界面采用稳定化风格的 ⚙ 设置 / ▦ 过滤两按钮结构，面板内用 ◀/▶ 切换正常/反向侧；移除了旧的四页签布局。
- 高级稳定化与高级纠缠稳定化仙墟解耦自仙窍：移除全部 `PersonalStorageEndpoint`/`PersonalStorageApi` 引用、所有者 UUID 绑定流程与 `Owner` NBT；两个高级变体作为完全独立的容器运行。
- `AdvancedRuinScheduler.collect` 改为每个容器逐格提取一个允许堆叠直接进入仙墟自身 54 格物品栏；`eject` 为纯 `distribute` 分发；移除 `pullMemory`/`bufferInsert`/`reinsert` 辅助方法。
- 移除了迷你仙墟上被错误绑定的“力/伤害/扭曲”配置。
- 调度器改为严格的区内枚举：只与操作区域（预览盒）内的容器交互，不再越出预览盒一格与区域外相邻容器交互。
- 每个区内方块位置 × 每个已启用面 = 一个独立目标，统一通过 NeoForge 官方 `Capabilities.ItemHandler.BLOCK, pos, face` 的面参数访问容器能力。
- 旧存档的“任意面”(-1) 配置在加载时迁移为全关掩码；NBT 由单面 `Face` 迁移到 `FaceMask`（`-1` 或越界 → 全关，合法 ordinal → 对应位）。
- 稳定化的菜单、界面与方块实体改为可扩展（非 final/泛型化），供新变体继承，行为不变。
- 网络协议保持 8，联机要求不变。

#### 修复

- 修复源方块为完整方块、导致机械动力鼓风机风无法穿过的问题；源方块现已加入鼓风机透明白名单。
- 修复反向稳定化仙墟 `eject()` 发送循环中遗留的 `break` 只处理首格的问题，现在 54 格全部轮询且未被接受的多余物品回到原槽；纠缠方块实体的 `eject()` 应用同一修复。
- 修复调度器扫描循环包含排他上界（`dx <= sizeX`）、可能越出预览盒一格与区域外相邻容器交互的问题；`enumerate` 改为严格上界（`dx < sizeX`）并排除原点，与预览轮廓完全一致。
- （开发过程）修复 Gradle 增量编译曾下发陈旧类、导致部分新界面“未渲染/未实现”的问题；强制 `clean` 重建后新类与界面已确认存在于 0.0.7 制品。

#### 兼容与验证

- 正式支持范围保持 Minecraft 1.21.1、NeoForge 21.1.235 与 Java 21；模组 ID、配置路径与网络协议（8）不变。
- 700 项自动化测试全部通过，失败、错误与跳过均为 0；新增 `AdvancedRuinSchedulerContractTest`（9 项：排序、逐容器收集、轮询阻塞/跳过、均分投出、逐组投出、掩码解码、区内独占枚举）以及自检/资源审计/模型计数/成就计数/描述契约的同步更新。
- 生产 JAR 边界、版本组成、精确版本产物、Ars Source API 与无 AE2 运行时校验通过。
- 0.0.7 在 30-JAR 全模组 PCL2 配置（含 Create 6.0.10 及其 ponder/flywheel）中使用 JDK 21、`zh_cn` 完成启动与单人实机验证，进入个人仙窍维度无 ImmortalStorage 致命错误；部署后实例恢复既有 `Test` 存档并确认 Create 6.0.10 正常加载。

### English

This section is the complete user-visible delta from release `0.0.6`.

#### Added

- Added the Echo Shard Source Vein: a RARE, non-stacking source block exchanging 1 echo shard for 16 Immortal Yuan per batch, gated at stage 8, feeding Source Vein Manager aggregation and per-face export. It ships with an advancement (criterion `immortalstorage_stage_8`), loot table, crafting recipe, block/item models, a deterministic hue-rotated frame texture, the `source_veins` tag, and the pickaxe mineable tag.
- Added the Entangled Stabilized Miniature Immortal Ruin: two independent per-tick config groups (normal and reversed) sharing one 54-slot staging buffer. The normal side collects dropped item entities inside its range into the buffer; the reversed side ejects the buffer to its target block by its own frequency, range, and filter. Each side keeps an independent 20-slot filter, range, frequency, preview, and enabled switch, with a switchable interface.
- Added the Advanced Stabilized Miniature Immortal Ruin: a stabilized-ruin base with a blue Crying Obsidian frame recolor and an adjustable range. It runs as an independent container (own 54-slot inventory) and schedules every item handler in its range as an independent container: normal mode pulls one allowed stack per container into the ruin's own inventory; reversed mode distributes the ruin's own inventory across the containers. Looping is configurable: access (poll-skip / force-poll), split (item-by-item / group-by-group), and order (far-first / near-first by Manhattan distance with position tie-break). No world drops; unaccepted leftovers stay staged in the ruin's own inventory.
- Added the Advanced Entangled Stabilized Miniature Immortal Ruin: two independent advanced scheduling configs (normal and reversed) with their own staging buffers, sharing one 54-slot buffer and two switchable menus.
- Added the shared `AdvancedRuinScheduler` pure-logic scanner/splitter reused by both advanced variants.
- The three new variants' renderers reuse the existing `MiniatureImmortalRuinRenderer.drawDisc` and the shared `arcane_machine_frame` JSON frame. Item previews gained three dynamic decorators: the advanced stabilized reuses `RuinCoreItemDecorator`, the entangled/advanced entangled use `EntangledRuinCoreItemDecorator` (two counter-rotating black/white spheres), and the Simulated Spirit Field uses `SimulatedSpiritFieldItemDecorator` (hydrated-substrate band plus a pulsing crop glyph).
- The entangled and advanced entangled preview boxes now render correctly (relative to the block, inflated 0.01 to avoid z-fighting). A new `RuinFaceHighlightRenderer` draws a translucent white quad on the preview box per enabled face.
- All four container-facing variants gained a 2×3 interaction face-mask grid (top row UP/NORTH/DOWN, bottom row WEST/SOUTH/EAST): `RuinFaceGrid` provides six 28×18 toggle buttons and `RuinFaceText` labels them with single letters U/D/N/E/W/S. Faces toggle individually with preview highlights, and six-off disables interaction on that side entirely.
- Water and Lava Source Veins now work as Create blower-fan catalysts: the `create:fan_processing_catalysts/splashing` (water vein) and `blasting` (lava vein) tags mark them as fan-washing and fan-smelting catalysts, and a new `create:fan_transparent` whitelist lets fan wind pass through the vein blocks (mirroring Create's own pattern where the Blaze Burner is both transparent and a catalyst).
- Added side-aware network payloads: `SetEntangledRuinFilter`, `ToggleEntangledRuinFilterMode`, and `SetEntangledRuinValue`; both entangled menus expose `setAuthoritativeValue(side, index, value)`.
- Added recipes: the Entangled variant is shapeless, the Advanced variant is a 2×2 shaped recipe, and the Advanced Entangled is a 2×2 shaped or shapeless conversion from the Advanced variant.
- Verified automatic AE add-on adaptation: when add-ons such as Applied Flux, Applied Mekanistics, Ars Énergistique, or Applied Botanics are installed, their existing resource keys map preferentially onto the same Xianqiao ledger; without the matching add-on the mod falls back to its own keys, so no intermediary mod is required.

#### Changed

- The Stabilized/Advanced Stabilized ruin screens now make the ⚙ settings and ▦ filter panels mutually exclusive; the Advanced Stabilized ruin's three scheduling buttons moved inside the settings sub-menu.
- The Entangled and Advanced Entangled ruins were rebuilt so each side keeps its own behavior and filter configuration over a shared 54-slot buffer, using the stabilized ⚙ settings / ▦ filter two-button structure with a ◀/▶ side selector; the earlier four-tab layout was removed.
- The Advanced Stabilized and Advanced Entangled Stabilized ruins are decoupled from Xianqiao: all `PersonalStorageEndpoint`/`PersonalStorageApi` references, the owner UUID binding flow, and the `Owner` NBT were removed. Both advanced variants run as fully independent containers.
- `AdvancedRuinScheduler.collect` now inserts one allowed stack per container directly into the ruin's own 54-slot inventory; `eject` is a pure `distribute`; the `pullMemory`/`bufferInsert`/`reinsert` helpers were removed.
- Removed the wrongly bound "force/damage/warp" configuration from the mini ruins.
- The scheduler now enumerates strictly in-area positions, interacting only with containers inside the operation area (preview box); it no longer reaches one layer outside.
- Each in-area position times each enabled face is an independent target resolved through the official NeoForge `Capabilities.ItemHandler.BLOCK, pos, face`.
- Legacy "any face" (-1) saves migrate to the all-off mask on load; NBT migrated from the single-face `Face` key to `FaceMask` (-1/out-of-range maps to all-off, valid ordinals to their bit).
- The stabilized menu, screen, and block entity became extensible (non-final / generic) so new variants can subclass them, with no behavior change.
- The network protocol stays at 8; multiplayer requirements are unchanged.

#### Fixed

- Fixed the Source Vein blocks being full solid blocks that blocked Create blower-fan wind; the veins are now whitelisted as fan-transparent.
- Fixed a stray `break` in the reversed stabilized ruin `eject()` send loop that processed only the first slot; all 54 slots are now cycled and unaccepted remainders return to their originating slot. The same fix was applied to the entangled block entity's `eject()`.
- Fixed the scheduler scan loop including the exclusive upper bound (`dx <= sizeX`), which could reach one extra layer of containers outside the preview box; `enumerate` is now strict (`dx < sizeX`, origin excluded), matching the preview outline exactly.
- (Development) Fixed Gradle incremental compilation shipping stale classes, which made some new screens appear "not rendered/not implemented"; a forced `clean` rebuild confirmed the fresh classes are present in the 0.0.7 artifact.

#### Compatibility and verification

- Supported versions remain Minecraft 1.21.1, NeoForge 21.1.235, and Java 21. Mod ID, configuration paths, and network protocol (8) are unchanged.
- All 700 automated tests passed with zero failures, errors, or skips, including the new 9-case `AdvancedRuinSchedulerContractTest` (sort ordering, per-container collect, poll-block/poll-skip, equal-split and group-by-group eject, mask decoding, and in-area-exclusive enumeration) plus updated self-test, asset audit, model count, advancement count, and description contracts.
- Production-JAR boundary, version-composition, exact-version artifact, Ars Source API, and no-AE2-runtime checks passed.
- Version 0.0.7 launched under JDK 21 and `zh_cn` in the 30-JAR full-mod PCL2 profile (including Create 6.0.10 with ponder/flywheel) and passed single-player QA, entering the personal realm with no ImmortalStorage fatal errors; the instance later resumed the existing `Test` save with Create 6.0.10 confirmed loading.

## [0.0.6]

本节完整列出相对正式版 `0.0.5` 的用户可见变化。

### 简体中文

#### 新增

- 新增罕见稀有度的模拟灵田；工作台按“灵晶/混元一气/灵晶，灵铁粒/草方块/灵铁粒，灵晶/蕴灵晶/灵晶”合成 4 个。
- 模拟灵田外壳只复用稳定化迷你仙墟的 12 棱镂空几何，外壳纹理严格直接引用未经修改的原版平滑石；没有采集哭泣黑曜石颜色，也没有添加私有纹理细节。
- 新增与模拟轮回炼化炉完全同尺寸的 230×187 界面、相同的种源/燃料/额外工具槽位置、4×3 产物区与玩家背包布局。
- 新增数据包可扩展作物目录：`simulated_spirit_field_seeds` 种子标签、`simulated_spirit_field_substrates` 基底标签，以及 `data/*/simulated_spirit_field_crops/*.json` 种子—作物—基底清单。
- 新增可替换生长基底。玩家可用普通土壤、模组土壤/耕地、末地石类或灵魂沙类方块右键替换，旧基底优先返回背包；内部底层按对应湿润耕地或特殊基底渲染。
- 新增按处理进度绘制的作物生长动画，并为末地石紫颂和灵魂沙地狱疣提供专用基底与动画规则。
- 新增紫颂果特例：紫颂果作为不消耗种源，仅接受末地石类基底，内部持续放大紫颂花且最大为 0.70 倍，每轮固定产出 1 个紫颂花和 2 个紫颂果。
- 仙窍管理页新增并排的日夜与天气控制。时间在正午/午夜间切换；天气按无天气、下雨、雷雨、下雪循环，并为下雪提供仙窍内客户端雪花效果。

#### 变更

- 模拟灵田中的种子是永久种源，处理时永不消耗。每 50 tick 无视自然生长条件，使用成熟作物的真实战利品表和额外工具槽物品生成一次收获。
- 12 格产物区使用原子输出：只有完整产物清单均可容纳时才写入，避免部分插入或溢出丢失。
- 自动化分面固定为顶部仅输入种子、四个水平侧面仅输入真元/仙元/仙灵驱动器；六个面可分别启用产物抽取，额外工具槽只允许玩家手动交互。
- 真元与仙元分别提供与仙炉一致的 150/500 tick 燃烧时间；仙灵驱动器仍从绑定对象支付资源。
- 模拟灵田补齐模拟轮回炼化炉同款六面邻接预览、逐面输出、自动输出以及经验存储/提取设置。
- 模拟灵田自动输出对象顺序固定为“当前仙窍主人 → 仙灵驱动器绑定人 → 本地 12 格产物槽”。
- 放置在所有者仙窍内的仙炉、模拟轮回炼化炉和模拟灵田，在需要燃料时可直接从仙窍所有者的个人存储支付仙元，不要求燃料槽中预先放入物品。
- 日夜时刻和天气模式由服务器持久化并持续锁定，仙窍时间倍率、原版昼夜推进和天气倒计时均不会改变所选状态；旧存档缺少字段时默认白天、无天气。
- 雨、雷雨和雪只作用于对应个人仙窍，不会修改主世界天气。网络协议由 7 升级为 8，联机时客户端与服务端必须同时使用 0.0.6。

#### 修复

- 修复模拟灵田无法把紫颂果识别为合法种源、紫颂产物及生长动画不符合指定规则的问题。
- 修复仙窍维度类型中的 `fixed_time: 6000` 强制客户端持续渲染正午天空，导致按钮显示“黑夜”但天空盒不变化的问题；黑夜现锁定在 18000 的午夜星空。
- 修复个人仙窍天气状态委托主世界数据而无法独立持锁的问题；个人维度现在拥有独立的晴雨、雷雨计时与状态。

#### 兼容与验证

- 正式支持范围保持为 Minecraft 1.21.1、NeoForge 21.1.235 与 Java 21；模组 ID、配置路径和既有 0.0.5 世界格式不变。
- 691 项自动化测试全部通过，失败、错误与跳过均为 0；生产 JAR 边界、版本组成、精确版本产物、Ars Source API、无 AE2 运行时与专用服务端启动校验通过。
- 0.0.6 在 30-JAR 全模组 PCL2 配置中使用 JDK 21、`zh_cn` 完成启动与 Numen 单人实机验证；日夜控制完成“黑夜 → 白天 → 黑夜”往返并确认午夜天空盒。

### English

This section is the complete user-visible delta from release `0.0.5`.

#### Added

- Added the Uncommon Simulated Spirit Field. Its specified crafting-table recipe yields four blocks from Spirit Crystals, Primordial Qi, Spirit Iron Nuggets, Grass Block, and Nurturing Crystal.
- Reused only the Stabilized Miniature Immortal Ruin's twelve-edge open geometry. The frame directly references the unmodified vanilla Smooth Stone texture and takes neither color nor added private detail from Crying Obsidian.
- Added a 230×187 interface matching the Simulated Reincarnation Furnace, including identical source/fuel/extra-tool positions, a 4×3 output cache, and the same player-inventory layout.
- Added data-pack crop extension through the `simulated_spirit_field_seeds` seed tag, `simulated_spirit_field_substrates` substrate tag, and seed-to-crop-to-substrate files under `data/*/simulated_spirit_field_crops/*.json`.
- Added replaceable normal/modded soil and farmland, End Stone-like, and Soul Sand-like substrates. The former substrate returns to the inventory when possible, and the internal floor renders the matching hydrated farmland or special substrate.
- Added crop growth animation driven by processing progress, with dedicated End Stone Chorus and Soul Sand Nether Wart substrate/render rules.
- Added the Chorus Fruit special case: it is a permanent End Stone-only seed, renders a continuously scaling Chorus Flower capped at 0.70×, and yields exactly one Chorus Flower plus two Chorus Fruit per cycle.
- Added side-by-side environment controls to Xianqiao Management. Time toggles locked noon/midnight; weather cycles clear, rain, thunder, and snow, with a personal-realm snowflake effect for snow.

#### Changed

- Seeds in the Simulated Spirit Field are permanent specimens and are never consumed. Every 50 ticks, the field ignores natural growth conditions and evaluates the mature crop's real loot table using the extra-slot tool.
- The twelve output slots use atomic insertion: no drops are written unless the complete result list fits, preventing partial insertion and overflow loss.
- Sided automation is fixed to top-only seed input and four-horizontal-side True Yuan, Immortal Yuan, or Spirit Drive fuel input. Output extraction is configurable on all six faces, while the extra tool remains manual-only.
- True Yuan and Immortal Yuan retain the Immortal Furnace's 150/500-tick burn durations, and Spirit Drives continue paying from their bound player.
- Added the Simulated Reincarnation Furnace's six-face adjacent preview, per-face output, automatic-output, and stored-experience controls to the Simulated Spirit Field.
- Automatic output resolves targets in the fixed order: current realm owner, Spirit Drive owner, then the twelve local output slots.
- The Immortal Furnace, Simulated Reincarnation Furnace, and Simulated Spirit Field can pay Immortal Yuan directly from the personal storage of their realm owner when they need fuel inside that owner's realm.
- Day/night and weather are persisted and continuously locked server-side. Realm time multipliers, vanilla day progression, and weather timers cannot change the selection; older saves default to day and clear weather.
- Rain, thunder, and snow are local to the personal realm and never change Overworld weather. The network protocol advances from 7 to 8, so multiplayer clients and servers must both use 0.0.6.

#### Fixed

- Fixed Chorus Fruit not being accepted as a valid field seed and corrected its dedicated drops and growth animation.
- Removed `fixed_time: 6000` from the personal-realm dimension type, which previously forced a noon sky while the button displayed Night. Night now locks to a clear midnight sky at time 18000.
- Fixed personal-realm weather delegating to Overworld level data instead of retaining an independent locked rain/thunder state.

#### Compatibility and verification

- Supported versions remain Minecraft 1.21.1, NeoForge 21.1.235, and Java 21. The mod ID, configuration paths, and existing 0.0.5 world format are unchanged.
- All 691 automated tests passed with zero failures, errors, or skips. Production-JAR boundary, version-composition, exact-version artifact, Ars Source API, no-AE2-runtime, and dedicated-server startup checks passed.
- Version 0.0.6 launched under JDK 21 and `zh_cn` in the 30-JAR full-mod PCL2 profile and passed Numen single-player QA, including a Night → Day → Night round trip with the midnight sky confirmed.

## [0.0.5]

本节列出相对已发布 `0.0.4` 的完整用户可见变更。

### 简体中文

#### 新增

- 新增 16 节点原版成就树，覆盖启灵、升仙、丹药选择、迷你仙墟与稳定化仙墟、仙窍驱动器与接口、古玉、仙墟锻灵剑、混元一气、刷怪笼转化、十次模拟炼化及末影龙特殊目标；全部标题和说明均提供简体中文与英文翻译。
- 新增一气归元剑。使用灵核模板、仙墟锻灵剑与混元一气在锻造台合成，继承灵剑的境界基础伤害、煅烧和淬火体系，并加入普通熔炉、高炉及仙炉的连续淬火流程。
- 一气归元剑长按右键 5 tick 后发射贯穿实体的连续世界光束：5–25 tick 每 10 tick 造成 20% 攻击力伤害；25–50 tick 每 5 tick 造成 20%；50 tick 后每 5 tick 造成 50% 并消耗 10 淬火点。三个阶段分别消耗 1/2 仙元进入，第三阶段维持至松手或资源不足。
- 光束在所有可见玩家的世界视角中渲染；第一人称枪口逐帧读取原版实际手持物品模型中心，光线从相机近裁面外穿过剑身并沿十字准星视线延伸至视距外。第三阶段使用蓝色核心与白色外层，最粗状态仍显著细于信标光束。
- 新增拘灵器。使用灵核模板和混元一气强化栓绳获得，可收纳任意生物并在空地释放，完整保存生物数据，创造模式同样会正确写回手持物品；Tooltip 显示收纳对象，装有生物时显示附魔光效，渡劫期间禁止使用。
- 新增模拟轮回炼化炉方块、方块实体、菜单、屏幕、工作状态与独立 JEI/EMI 分类。手持混元一气、在副手持灵核对刷怪笼使用可完成转化。
- 炼化炉接受任意原版或模组刷怪蛋及装有生物的拘灵器作为不消耗原料，另有不消耗耐久的武器判定槽；每 50 tick 依照该武器生成一次真实实体战利品和经验。
- 炼化炉燃料槽接受真元、仙元和仙灵驱动器：每个真元提供 50 tick，每个仙元提供 500 tick，并支持从已绑定仙灵驱动器支付真元或仙元。
- 炼化炉新增 4x3、共 12 格只读输出缓存、经验缓存/直取按钮、自动输出开关，以及六个面的相邻方块预览和输出行为配置。
- 炼化炉原料槽放入刷怪蛋或拘灵器后即在方块内部持续旋转显示缩小的目标模型；青白火焰粒子和 Vault 发光面严格表示实际工作状态。

#### 行为调整

- 所有使用淬火点的武器统一获得 5,000 点上限；一气归元剑左键命中同样支付仙元并消耗当前 50% 淬火点，但淬火点不提供近战增伤。
- 一气归元剑持续蓄力期间，仙元/淬火点/同步组件变化不再触发重复放下和拿起动画；真实切换槽位或物品仍保留正常重装备动画。
- 模拟炼化不把临时生物加入世界，因此和平难度可用，并避免 AI、寻路、碰撞、实体 tick、追踪同步和经验球实体开销。
- 自动输出开启时，位于仙窍内的炼化炉将物品直接送入仙窍所有者的个人存储；世界中的炼化炉使用已绑定仙灵驱动器所属人作为目标。经验始终直接给予目标在线玩家；路由失败时内容保留在本地缓存。
- 渡劫期间仙窍时间流速强制锁定为 1x，不能调整；渡劫结束后恢复此前设置。
- 仙窍接口推出时只访问相邻方块对应面的输入能力，抽取时只访问对应面的输出能力，避免向熔炉输出槽塞入原料或从输入槽错误抽取。
- 仙墟锻灵剑的锻造配方交换为“灵核作模板、迷你仙墟作材料”。
- 蕴灵晶母岩被任何形式挖掘后固定掉落失活的蕴灵晶母岩。
- 为一气归元剑、拘灵器和模拟轮回炼化炉配置与其阶段定位相符的稀有度和创造模式目录入口。

#### 修复与美术

- 模拟轮回炼化炉改用原版宝库 `Vault` 六面模型和独立待机/工作模型；顶面、底面、正面、侧面及工作面分别从原材质换色，保留源透明蒙版和镂空结构。
- 重制炼化炉 230 像素原版风格 UI：使用熔炉底图、青白仙炉火焰、真实 50 tick 箭头进度、12 格输出区和项目统一的外置设置栏，移除槽位规模等调试说明文字。
- 拘灵器保留原版栓绳轮廓并换为青白色，增加两条更长、更宽、连续成片的授权云雾装饰。
- 一气归元剑采用用户提供的 32x32 像素底稿，仅进行确定性调色与最近邻中心缩放；成品占 26x26（0.8125 倍），外圈和原透明像素保持完全透明，整图不含半透明像素。
- 修复一气归元剑光束仅渲染在玩家脸部、起点镜像到左手、枪口与剑身错位、只显示粒子或短线、组件更新打断持续使用等问题。
- 修复模拟轮回炼化炉缺少创造目录入口、单面材质、工作状态不明确、输出区不是 4x3、进度条不同步和设置面板缺少六面预览等问题。

#### 验证

- 支持 Minecraft 1.21.1、NeoForge 21.1.235、Java 21。
- JDK 21 自动化测试、生产构建、版本组成和精确产物校验通过。
- 187 个测试套件、682 项自动化测试通过，0 failures、0 errors、0 skipped；生产 JAR 边界、Ars Source 发布 API 适配器及无 AE2 运行时边界校验通过。
- 全模组 PCL 实例以 `zh_cn` 启动，加载 30 个 JAR；ImmortalStorage 0.0.5 与 AE2、Refined Storage、Mekanism、Flux Networks、Botania、Ars Nouveau、Industrial Foregoing Souls 和 Beyond Dimensions 联动完成注册。
- 实机通过资源重载、OpenAL、声音引擎和纹理图集创建边界；未发现 `MixinApplyError`、`InvalidInjection`、`ModLoadingException` 或致命加载错误。
- 发布产物：`immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.5.jar`，4,882,189 字节，SHA256 `679B49B184561F0D06F17F209EFBAF9EC672D4BCBDE2AAAEDC23C7A47B6FE532`。

### English

This section contains the complete user-visible change set from the published `0.0.4` tag to `0.0.5`.

#### Added

- Added a 16-node vanilla advancement tree covering Awakening, Ascension, pill choices, ruin progression, Xianqiao technology, Primordial Qi, spawner conversion, ten simulated refinements, and the Ender Dragon special objective. Every title and description is localized in Simplified Chinese and English.
- Added the One Qi Returning Origin Sword, forged from a Spirit Core template, Immortal-Ruin-Forged Spirit Sword, and Primordial Qi. It inherits stage-scaled base damage and the heating/quenching loop and participates in continuous tempering in vanilla furnaces, blast furnaces, and the Immortal Furnace.
- Added a three-phase piercing continuous beam after five ticks of right-click charge. Phase one deals 20% attack damage every ten ticks, phase two deals 20% every five ticks, and phase three deals 50% every five ticks while consuming ten tempering points. Entering phases one and two costs one and two Immortal Yuan respectively.
- The beam is rendered in the world for every visible player. First-person rendering captures the live center of the actual vanilla held-item model every frame, enters from beyond the camera near plane, passes through the sword, and follows the crosshair sight axis beyond render distance. Phase three has a blue core and white shell while remaining much thinner than a beacon beam.
- Added the Soul Catcher, forged by upgrading a Lead with a Spirit Core template and Primordial Qi. It captures and releases arbitrary entities with full saved data, updates the held stack correctly in Creative mode, displays the contained entity in its tooltip, glints while occupied, and cannot be used during tribulation.
- Added the Simulated Reincarnation Refining Furnace block, block entity, menu, screen, working state, and dedicated JEI/EMI category. A spawner converts when used while Primordial Qi is held in the main hand and a Spirit Core in the off hand.
- The furnace accepts any vanilla or modded spawn egg or occupied Soul Catcher without consuming it, plus a non-damaging weapon slot used for loot evaluation. Every 50 ticks it produces one real entity-loot-table result and its experience.
- Added True Yuan, Immortal Yuan, and Spirit Drive fuel support: one True Yuan burns for 50 ticks, one Immortal Yuan for 500 ticks, and a bound Spirit Drive may pay either resource.
- Added a 4x3 twelve-slot output cache, stored-experience release, automatic-output toggle, and six adjacent-block preview/output controls.
- In-world rendering rotates a reduced specimen model as soon as the source slot is occupied. Cyan-white flame particles and active Vault faces independently and strictly represent actual processing.

#### Changed

- All tempering-point weapons now cap at 5,000 points. One Qi melee hits pay Immortal Yuan and consume 50% of current points, but tempering points provide no melee damage bonus for this sword.
- Resource/component updates during continuous One Qi charging no longer restart the equip animation; actual slot or item changes still animate normally.
- Simulated refinement never inserts the temporary entity into the world, so it works in Peaceful and avoids AI, pathfinding, collision, entity ticking, tracking packets, and experience-orb entities.
- With automatic output enabled, a furnace inside Xianqiao sends items to the realm owner's personal storage. Outside Xianqiao, a bound Spirit Drive selects its owner. Experience is granted directly to the online target player; failed routing retains items and experience in local caches.
- Xianqiao time flow is locked to 1x for the entire tribulation and the previous setting is restored afterward.
- Xianqiao Interface pushes now target only input capabilities on the adjacent face, and pulls target only output capabilities, preventing furnace ingredients from entering output slots or being extracted from input slots.
- Swapped the Immortal-Ruin-Forged Spirit Sword smithing order so the Spirit Core is the template and Miniature Immortal Ruin is the addition.
- Nurturing Crystal Bedrock now always drops Inactive Nurturing Crystal Bedrock when mined by any method.
- Assigned progression-appropriate rarities and creative-tab entries to the new sword, catcher, and furnace.

#### Fixed and Art

- Rebuilt the Simulated Reincarnation Refining Furnace from the vanilla Vault six-face model with separate idle/working models. Top, bottom, front, side, and active faces are palette recolors that preserve the source alpha mask and cutout geometry.
- Rebuilt the 230-pixel furnace screen around the vanilla furnace background, cyan-white Immortal Furnace fire, the real synchronized 50-tick progress arrow, twelve output slots, and the project's external settings rail. Removed prototype scale/debug labels.
- Preserved the vanilla Lead silhouette for the Soul Catcher recolor and added two authorized long, broad, continuous cloud bands.
- Refined the user-provided 32x32 One Qi sprite only through deterministic palette cleanup and nearest-neighbor center scaling. The final 26x26 footprint is 0.8125 scale; padding and source-transparent pixels remain fully transparent with no partial alpha.
- Fixed the beam rendering only near the player's face, mirrored left-hand origin, sword/muzzle misalignment, particle/short-line presentation, and component updates interrupting continuous use.
- Fixed missing furnace creative registration, single-face textures, ambiguous working visuals, incorrect output dimensions, unsynchronized progress, and missing six-face settings previews.

#### Verification

- Supported target: Minecraft 1.21.1, NeoForge 21.1.235, Java 21.
- JDK 21 automated tests, production build, version-composition audit, and exact-artifact audit passed.
- 187 test suites and 682 automated tests passed with 0 failures, 0 errors, and 0 skipped tests. Production-JAR boundary, published Ars Source API adapter, and no-AE2-runtime checks passed.
- A full-mod PCL instance launched in `zh_cn` with 30 JARs. ImmortalStorage 0.0.5 registered integrations for AE2, Refined Storage, Mekanism, Flux Networks, Botania, Ars Nouveau, Industrial Foregoing Souls, and Beyond Dimensions.
- The real client completed resource reload, OpenAL, sound-engine, and texture-atlas startup with no `MixinApplyError`, `InvalidInjection`, `ModLoadingException`, or fatal loading error.
- Release artifact: `immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.5.jar`, 4,882,189 bytes, SHA256 `679B49B184561F0D06F17F209EFBAF9EC672D4BCBDE2AAAEDC23C7A47B6FE532`.

## [0.0.4]

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

## [0.0.3]

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

## [0.0.2]

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

## [0.0.1]

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
