# Changelog

## [0.0.9] - 2026-08-08

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
- Bumped the version to `0.0.9`.

## [0.0.8] - 2026-08-07

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

## [0.0.7] - 2026-08-06

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

## [0.0.6] - 2026-08-01

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

## [0.0.5] - 2026-07-25

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
