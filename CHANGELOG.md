# Changelog

## [0.1.6]

### English

- Fixed the Spiritual Aura Guard virtual elytra conflicting with creative and realm flight. Ability flight now ends a gliding state before the player moves, the elytra can neither deploy nor boost while flying, and the second press of a flight double-tap goes to the flight toggle instead of folding the glide.
- Added two multiblock machines built around a Miniature Immortal Ruin: the Spirit Forging Compressor and the Spirit Decompressor. Eight World Barrier blocks and twelve iron chains ring the core, a World Shard Miner sits below and a Treasure Basin above; forming the structure opens a 4x3 input to a single output, and reversing the core switches the same build between the two machines.
- Ancient Jade documents the multiblock with a complete projection and layer diagrams, so the structure can be assembled straight from the handbook, and JEI and EMI list the compression and decompression production categories.
- The Spirit Forging Compressor converts source-defined resources into Immortal Yuan in whole batches, accepts datapack compression recipes, moves up to 64 items per tick on each of its six faces, can output directly into the owner's Xianqiao, and shares the simulated-machine face panel and three-state redstone control. The Spirit Decompressor runs one input into up to twelve outputs with atomic multiple results, and switching core polarity ejects both cached inventories.
- Added recipes worth building it for: quartz slabs return 2 quartz while blocks, stairs and chiseled variants return 4; an amethyst block returns 4 amethyst shards; ores split into their stone and their drops, so coal ore gives cobblestone and 4 coal and iron ore gives cobblestone and 4 raw iron; and one amethyst block or one inactive Nurturing Crystal Bedrock plus eight ImmortalPower activates the corresponding budding amethyst or Nurturing Crystal Bedrock. Compression matches counted multiple materials, including overlapping tag ingredients, and consumes nothing when the output is blocked.
- Added the missing Source Vein Manager and Xianqiao Redstone Interface crafting recipes together with their handbook pages, so both can be crafted again, and hollow-frame blocks no longer hide neighbouring block faces.
- Stages 6 and above now keep constant Night Vision. The effect renews before vanilla expiry, and topping it up never shortens a stronger, longer or infinite effect from another source.
- The Spirit Instrument and Immortal Artifact gained a resource locator: yellow outlines mark containers and loot, purple marks spawners and green marks archaeology, while a held Mine-mode artifact adds white ore outlines. Ore outlines follow a loot dictionary built from real loot tables at world load, so an off-hand drop or an ore selects every variant of its source family without name guessing or repeated world scans.
- Reworked item and block rendering: non-source block items reuse their placed model, block state and world renderer in the inventory and in hand, and the Miniature Immortal Ruin draws opaquely with a depth-writing shell and core over a black-and-white polarity texture.
- Also in this version: realm time acceleration guards repeated entity calls, honours acceleration-exclusion tags and isolates failures in extra ticks; the obsolete time-flow keybinding was removed; and handbook layer order, preview centring and culling were corrected so structures display in their real orientation.
- Updated the bilingual Ancient Jade handbook and the localization text for every change above.

### 简体中文

- 修复灵气护体虚拟鞘翅与创造飞行、仙窍飞行的冲突：主动飞行时会在移动前结束虚拟滑翔，飞行中既不能展开也不能加速鞘翅，双击飞行的第二次按键交还飞行切换，不再被鞘翅占用。
- 新增两台以迷你仙墟为核心的多方块机器：锻灵压缩机与锻灵解压器。八块世界胎壁与十二条铁链环绕核心，下方为世界碎片开采器、上方为聚宝盆；成型后打开 4x3 输入到单输出的界面，反转核心即可在同一结构上切换两台机器。
- 古玉指导书提供完整结构投影与逐层分解，照着手册即可搭建成型；JEI 与 EMI 列出压缩与解压两类生产配方。
- 锻灵压缩机按整批把源定义资源兑换为仙元，支持数据包压缩配方，六个面每 tick 可主动吞吐 64 件，可直接输出到放置者的仙窍，并复用模拟机器的六面面板与三态红石控件。锻灵解压器把单输入拆为最多十二输出，多结果原子化提交；切换核心极性会弹出两套缓存。
- 新增值得为此建机的配方：石英半砖拆解为 2 个石英，整块、楼梯与凿刻变体为 4 个；一块紫水晶块拆解为 4 枚紫水晶碎片；原矿拆为石料与掉落物，例如煤矿石产出圆石与 4 个煤炭、铁矿石产出圆石与 4 个粗铁；一块紫水晶块或失活的蕴灵晶母岩加八枚仙元，可激活为紫水晶芽床或蕴灵晶母岩。压缩支持计数多材料与重叠标签原料，输出被阻挡时不消耗材料。
- 补全缺失的源方块管理器与仙窍红石接口合成配方，并加入对应指导书页面，两者恢复可合成；中空框架不再隐藏相邻方块表面。
- 六阶及以上新增常驻夜视：在原版到期前续期，且续期不会缩短来自其他来源的更强、更长或无限时长的效果。
- 灵器与仙器新增有趣的增强——资源定位：黄色框标记容器与战利品、紫色框标记刷怪笼、绿色框标记考古，手持挖掘模式仙器再加白色矿物框。矿物定位依据世界载入时由真实掉落表建立的词典：副手掉落物或原矿会选中同一来源族的全部变体，不按名称猜测、不反复扫描世界。
- 重做物品与方块渲染：非源方块物品在物品栏与手持时复用放置后的模型、方块状态与世界渲染器；迷你仙墟改为不透明绘制，以黑白极性纹理保持外壳与核心的深度写入。
- 本版还包括：仙窍时间加速保护重复实体调用、遵守加速排除标签，并隔离额外 tick 的失败；移除废弃的时间流速快捷键；修正指导书层序、预览居中和遮挡剔除，使结构按真实朝向显示。
- 同步更新中英双语古玉指导书与本地化文本。

## [0.1.5]

### English

- Added stage-8 Soul Escape: the rebindable Soul Escape key (F7 by default) toggles spectator mode, and leaving restores the exact game mode you had before.
- Soul Escape needs neither cheats nor operator permissions, so it also works in hardcore and cheat-free worlds. A spectator state produced by another command is never treated as Soul Escape.
- Added warp travel to the Immortal Artifact's teleport mode: with ImmortalPower in the off hand, each item warps 100 blocks along the horizontal look direction, the whole off-hand stack is consumed, and the landing is the highest safe spot in the target column.
- A warp without a safe landing is cancelled and spends nothing, and a near-vertical look direction does not warp.
- Warp travel prepares its landing area before moving you, so the destination chunks are already loaded on arrival; the ordinary teleport stays a fast short hop and reports the distance covered.
- Fixed the Immortal Artifact's mode textures on the newer Minecraft line: the artifact now switches its mode sprite and its adaptive mining-tool sprite like the Spirit Instrument.
- The Spiritual Aura Guard virtual elytra now follows the vanilla elytra rules: it only deploys while you are not flying, and the second press of a flight double-tap always toggles flight instead of folding the glide.
- The Ancient Jade handbook documents Soul Escape, warp travel and the flight precedence in English and Simplified Chinese.

### 简体中文

- 八阶新增灵魂出窍：默认 F7（可自定义）进入与退出旁观者模式，退出时恢复进入前的游戏模式。
- 灵魂出窍不依赖作弊开关与操作员权限，极限模式与未开启作弊的世界同样可用；由其他命令产生的旁观者状态不会被当成灵魂出窍。
- 仙器传送模式新增折跃传送：副手持有仙元时，每枚仙元沿视线水平方向折跃 100 格，一次性消耗整叠副手仙元，落点取目标列最高的安全位置。
- 找不到安全落点时折跃取消且不消耗仙元；视线接近垂直时不会折跃。
- 折跃会先准备好落点区域再移动玩家，抵达时落点区块已经加载；普通传送保持轻量的短距跃迁，并提示本次传送距离。
- 修复新一代 Minecraft 中仙器的模式贴图：仙器的模式贴图与自适应挖掘工具贴图现在会像灵器一样切换。
- 灵气护体虚拟鞘翅改为按原版鞘翅规则工作：仅在未处于飞行时展开，双击切换飞行的第二次按键交还飞行，不再被鞘翅占用。
- 古玉手册以中英双语补充灵魂出窍、折跃传送与飞行优先级的说明页。

## [0.1.4]

### English

- Changed the Spiritual Aura Guard virtual elytra boost from Ctrl+Z to double-tapping the forward key (W by default).
- Boost follows keyboard rebinding of the forward control. Each boost requires two presses within 350 ms; holding the key does not repeat it.
- Pending taps are cleared when virtual flight ends, a menu opens, the game pauses, or the window loses focus.
- Updated the bilingual Ancient Jade handbook with virtual elytra flight and boost controls.
- Rewrote the README as a concise bilingual overview of all items and the main progression.

### 简体中文

- 将灵气护体虚拟鞘翅的加速操作由 Ctrl+Z 改为双击前进键（默认 W）。
- 加速操作跟随前进键的键盘重绑定。每次加速需在 350 毫秒内按下两次，长按不会重复触发。
- 结束虚拟飞行、打开界面、暂停游戏或窗口失焦时，清除尚未完成的双击输入。
- 更新中英双语古玉手册，补充虚拟鞘翅的飞行与加速操作说明。
- 将 README 重写为简要的双语全物品与主要流程简介。

## [0.1.3]

### English

- Added machine-category tags and a shared `immortalstorage:machines` tag.
- Added three-state redstone control to ImmortalStorage machines, combined with existing machine switches using AND logic.
- Added the Xianqiao Redstone Interface with normal/inverted Schmitt-trigger monitoring, full redstone output, and active-state lighting.
- Added its compact two-row configuration UI, persistent thresholds, external-resource selection, player inventory, and expanded bilingual handbook instructions.
- Fixed its Blockbench-derived block, texture, state, inventory, and held-item models.
- Restored the intended Miniature Immortal Ruin projected silhouette and corrected redstone-control placement on affected settings screens.
- Maintained separate builds for both supported Minecraft/NeoForge generations.

### 简体中文

- 新增机器分类标签及统一管理用的 `immortalstorage:machines` 标签。
- 为仙藏机器新增三态红石控制，并与机器原有开关执行 AND 逻辑。
- 新增仙窍红石接口，支持正相/反相施密特触发、满强度红石输出和激活照明。
- 新增紧凑两行配置界面、持久化阈值、额外资源选择、玩家背包及扩写后的双语手册说明。
- 修复按 Blockbench 设计导出的方块、材质、状态、物品栏与手持模型。
- 恢复迷你仙墟应有的投影轮廓，并修正相关设置页的红石控件位置。
- 为两个受支持的 Minecraft/NeoForge 世代分别维护独立构建。

## [0.1.2]

### English

- Added the Immortal Art Remains and Immortal-Forged material progression chain.
- Added the Immortal One's Rule, Spiritual Aura Guard, virtual elytra flight, and the five-mode Immortal Artifact.
- Added the Information Storage Crystal for copying and restoring supported machine configurations.
- Improved tool reach, building, mining, teleportation, exploration, and cultivation equipment behavior.
- Fixed Create nurturing registration, embedded crafting and stonecutting continuity, recipe transfer, storage clicks, and several rendering issues.
- Expanded advancements and the bilingual Ancient Jade handbook.
- Added and stabilized the second supported Minecraft/NeoForge generation.

### 简体中文

- 新增仙法遗骸与仙锻材料进阶链。
- 新增仙师度、灵气护体、虚拟鞘翅飞行及五模式仙器。
- 新增信息存储水晶，可复制并恢复受支持机器的配置。
- 强化工具触及、建筑、挖掘、传送、探索与修仙装备行为。
- 修复机械动力蕴灵注册、内置合成与切石连续操作、配方转移、存储点击及多项渲染问题。
- 扩充成就与中英双语古玉手册。
- 新增并稳定维护第二个 Minecraft/NeoForge 支持世代。

## [0.1.1]

### English

- Published separate builds for both supported game generations.
- Restored the embedded stonecutter layout and recipe display.
- Fixed recipe selection and result continuity after taking crafted outputs.
- Improved compatibility behavior for migrated interfaces and interactions.

### 简体中文

- 为两个受支持的游戏世代分别提供独立构建。
- 恢复内置切石机的界面布局与配方显示。
- 修复取出产物后的配方选择与连续合成。
- 改进迁移界面与交互的兼容行为。

## [0.1.0]

### English

- Completed the core cultivation progression, personal Xianqiao realm, storage, energy, machine, equipment, pill, and Miniature Immortal Ruin systems.
- Added the World Barrier system for protecting personal-realm boundaries.
- Expanded automation through interfaces, managers, source veins, simulated machines, and stabilized ruin variants.
- Added broad optional integration with storage, recipe-viewer, technology, magic, and building mods.
- Completed bilingual interfaces, configuration text, advancements, and Ancient Jade documentation.

### 简体中文

- 完成修仙进阶、个人仙窍、存储、能量、机器、装备、丹药与迷你仙墟等核心系统。
- 新增用于保护个人仙窍边界的世界胎壁系统。
- 通过接口、管理器、源方块、模拟机器和稳定化仙墟系列扩展自动化能力。
- 广泛支持存储、配方查看、科技、魔法与建筑类可选模组联动。
- 完成双语界面、配置文本、成就与古玉手册说明。

## [0.0.12]

### English

- Added reinforcement plugins for supported machines with multiple processing-speed tiers.
- Added dynamic AE2 and Refined Storage support for registered addon resource types.
- Improved Immortal Furnace plugin handling, interface storage bridges, and source-resource behavior.
- Added the second supported game-generation implementation while preserving the original generation's behavior.
- Fixed multiple UI, rendering, storage, and compatibility issues across both generations.

### 简体中文

- 为受支持机器新增多档处理速度强化插件。
- 新增 AE2 与精致存储对附属注册资源类型的动态支持。
- 改进仙炉插件、接口存储桥与源资源行为。
- 在保持原世代行为的同时新增第二个受支持游戏世代实现。
- 修复两代中的多项界面、渲染、存储与兼容问题。

## [0.0.11]

### English

- Added FE, Mana, and Source Immortal Energy Crystals with Xianqiao storage interaction.
- Added long-valued external-resource storage and improved AE2/Refined Storage integration.
- Added new source blocks, simulated machines, and Miniature Immortal Ruin variants.
- Improved source-block rendering, interface transfer behavior, and bilingual handbook coverage.
- Fixed storage synchronization, capability, model, and compatibility issues.

### 简体中文

- 新增电力、魔力与魔源仙能水晶，并接入仙窍存储交互。
- 新增长整型额外资源存储并改进 AE2/精致存储联动。
- 新增源方块、模拟机器与迷你仙墟变体。
- 改进源方块渲染、接口传输行为与双语手册覆盖。
- 修复存储同步、能力、模型与兼容问题。

## [0.0.10]

### English

- Added Mekanism chemical-container interaction for Xianqiao storage.
- Improved sword tempering tooltips and percentage formatting.
- Reworked personal-realm time-flow controls into a clearer symmetric layout.
- Expanded bilingual Ancient Jade documentation.
- Fixed storage-addon compatibility, rendering, and configuration issues.

### 简体中文

- 新增仙窍存储与通用机械化学品容器的交互。
- 改进灵剑淬火提示与百分比格式。
- 将仙窍时间流速控件重排为更清晰的对称布局。
- 扩充中英双语古玉手册。
- 修复存储附属兼容、渲染与配置问题。

## [0.0.9]

### English

- Added persistent player identity and migration support for personal storage.
- Expanded AE2 and Refined Storage disk, key, and addon-resource integration.
- Added the Xianling Drive and strengthened owner-bound machine behavior.
- Improved external-resource icons and rendering in storage interfaces.
- Fixed storage duplication, missing-renderer, compatibility, and handbook-coverage issues.

### 简体中文

- 新增个人存储持久身份与迁移支持。
- 扩展 AE2 与精致存储磁盘、资源键和附属资源联动。
- 新增仙灵驱动器并强化机器认主行为。
- 改进存储界面的额外资源图标与渲染。
- 修复存储重复、缺失渲染器、兼容与手册覆盖问题。

## [0.0.8]

### English

- Added the Advanced Xianqiao Interface with configurable regional container automation.
- Added region preview, per-face interaction modes, scheduling options, and extended resource support.
- Updated Xianqiao Interface and sword models from their design sources.
- Improved bilingual controls and interface inheritance for advanced variants.
- Fixed preview synchronization and optional resource-capability registration.

### 简体中文

- 新增高级仙窍接口，支持可配置范围内的容器自动化。
- 新增范围预览、逐面交互、调度选项与扩展资源支持。
- 按设计稿更新仙窍接口与灵剑模型。
- 改进高级变体的双语控件与界面继承结构。
- 修复预览同步和可选资源能力注册。

## [0.0.7]

### English

- Added Echo Shard Source Veins and new entangled and advanced Miniature Immortal Ruin variants.
- Added independent normal/inverted scheduling, shared buffers, per-face interaction, and configurable collection/ejection behavior.
- Added Create fan-processing catalyst support for water and lava ruins.
- Improved ruin settings, previews, filtering, item decoration, and scheduler behavior.
- Expanded optional storage-addon resource mapping.

### 简体中文

- 新增回响碎片源方块及纠缠、高级迷你仙墟变体。
- 新增正反相独立调度、共享缓冲、逐面交互和可配置收集/投出行为。
- 为水仙墟与岩浆仙墟新增机械动力风扇处理触媒支持。
- 改进仙墟设置、预览、过滤、物品装饰与调度行为。
- 扩展可选存储附属资源映射。

## [0.0.6]

### English

- Added the Simulated Spirit Field with data-driven crops, replaceable substrates, growth animation, and automated harvesting.
- Added personal-realm day/night and weather controls.
- Added sided input/output, experience handling, automatic output, and personal-storage fuel payment for simulated machines.
- Improved simulated-machine models, interfaces, previews, and persistence.
- Fixed crop output, special-crop handling, and realm-state synchronization.

### 简体中文

- 新增模拟灵田，支持数据驱动作物、可替换基底、生长动画与自动收获。
- 新增个人仙窍日夜与天气控制。
- 为模拟机器新增分面输入输出、经验处理、自动输出及个人存储燃料支付。
- 改进模拟机器模型、界面、预览与持久化。
- 修复作物产出、特殊作物处理与仙窍状态同步。

## [0.0.5]

### English

- Added the advancement tree, One-Qi-Origin Sword, Spirit Capturer, and Simulated Reincarnation Furnace.
- Added staged beam attacks, creature capture/release, simulated loot processing, experience storage, and automatic output.
- Expanded tempering, ImmortalPower payment, personal-storage routing, and sided automation.
- Improved machine models, particles, previews, and recipe-viewer integration.
- Fixed interface transfer direction, animation resets, and several combat and storage edge cases.

### 简体中文

- 新增成就树、一气归元剑、拘灵器与模拟轮回炼化炉。
- 新增分阶段光束、生物收纳释放、模拟战利品处理、经验存储和自动输出。
- 扩展淬火、仙元支付、个人存储路由与分面自动化。
- 改进机器模型、粒子、预览与配方查看器联动。
- 修复接口传输方向、动画重置及多项战斗与存储边界问题。

## [0.0.4]

### English

- Added embedded smithing, a storage magnet toggle, exact-count tooltips, and improved search behavior.
- Added Ancient Jade archaeology loot, Qi Bottles, Primordial Qi, entity conversion, and teleport mode for the Spirit Instrument.
- Added stabilized-ruin filtering, linking, shared inventories, and one-way warp behavior.
- Expanded bilingual documentation, recipes, models, configuration, and compatibility.
- Fixed terminal interaction, external-resource handling, linking, rendering, and gameplay edge cases.

### 简体中文

- 新增内置锻造、存储磁铁开关、精确数量提示和改进后的搜索行为。
- 新增古玉考古战利品、采气瓶、混元一气、生物转化及灵器传送模式。
- 新增稳定化仙墟过滤、链接、共享容器与单向传送行为。
- 扩展双语说明、配方、模型、配置与兼容支持。
- 修复终端交互、额外资源、链接、渲染和玩法边界问题。

## [0.0.3]

### English

- Renamed the public product and namespace to ImmortalStorage.
- Bundled Patchouli and replaced the separate guide implementation with the bilingual Ancient Jade handbook.
- Added the Nurturing Crystal chain, Substitute Puppet, Miniature Immortal Ruin, Stabilized Miniature Immortal Ruin, and Immortal-Ruin-Forged Spirit Sword.
- Added personal-realm bed respawning, item rarity tiers, and shared inventory-decoration rendering.
- Improved terminal controls and fixed puppet bindings, ruin interaction, rendering, storage, and compatibility behavior.

### 简体中文

- 将公开产品名与命名空间统一更名为仙藏 ImmortalStorage。
- 内置帕秋莉，并以中英双语古玉手册替代独立指导界面。
- 新增蕴灵晶链、替死傀儡、迷你仙墟、稳定化迷你仙墟和仙墟锻灵剑。
- 新增个人仙窍床重生、物品稀有度与共享物品装饰渲染。
- 改进终端控件，并修复傀儡绑定、仙墟交互、渲染、存储与兼容行为。

## [0.0.2]

### English

- Added the special-operation key and persistent Spirit Sword recall reservations.
- Added unified Xianqiao storage for FE, chemicals, Mana, Source, Souls, and storage-addon resource keys.
- Added Building Gadgets and Create Schematicannon access to personal storage.
- Improved interface face controls, cache configuration, active automation, and passive capability access.
- Fixed external-resource transfers, Botania and Ars Nouveau behavior, source rendering, and compatibility issues.

### 简体中文

- 新增特殊操作键与可持久化的灵剑召回保留机制。
- 新增仙窍对电力、化学品、魔力、魔源、灵魂及存储附属资源键的统一存储。
- 新增建筑小帮手与机械动力蓝图大炮访问个人存储的能力。
- 改进接口逐面控制、缓存配置、主动自动化与被动能力访问。
- 修复额外资源传输、植物魔法、新生魔艺、源渲染及兼容问题。

## [0.0.1]

### English

- Added ten-stage cultivation progression and the bilingual Ancient Jade guide.
- Added per-player Kongqiao/Xianqiao storage and personal Xianqiao realms.
- Added TruePower, ImmortalPower, the Immortal Furnace, source systems, managers, and automation interfaces.
- Added the Spirit Instrument, Spirit Sword, World Shard Miner, Treasure Basin, tribulations, pills, ores, effects, and advancements.
- Added optional integration with major recipe-viewer, storage, technology, magic, and building mods.
- Added complete bilingual configuration and interface text.

### 简体中文

- 新增十阶段修仙进阶与中英双语古玉指导书。
- 新增每位玩家独立的空窍/仙窍存储与个人仙窍维度。
- 新增真元、仙元、仙炉、源系统、管理器与自动化接口。
- 新增灵器、灵剑、世界碎片采集器、聚宝盆、渡劫、丹药、矿物、效果与成就。
- 新增对主要配方查看、存储、科技、魔法与建筑类模组的可选联动。
- 新增完整中英双语配置与界面文本。
