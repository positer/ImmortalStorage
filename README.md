# ImmortalStorage · 仙藏

**0.1.7 (development) · NeoForge** · [Latest release / 最新发布: 0.1.6](https://github.com/positer/ImmortalStorage/releases/tag/0.1.6) · [Changelog / 更新日志](CHANGELOG.md) · [English](#english) · [简体中文](#简体中文)

![ImmortalStorage](immortalstorage-logo.png)

## English

Workspace design reference: [Create: Entangled Factory complete implementation plan](../Minecraft Forge Workspace/docs/cef/CEF完整执行方案_Agent直接使用.md). The CEF design archive lives in the sibling development hub workspace, outside this repository. (Chinese; independent addon specification, not an ImmortalStorage feature or released mod). CEF now has a separate development repository; its current prototypes coordinate native assembly workpieces, real press cycles and the depot work-slot/in-place-upgrade path, while the complete playable feature set remains in development.

ImmortalStorage is a **cultivation mod for Minecraft** on NeoForge. It takes the xianxia ladder every reader of the genre already knows — awaken as a mortal, gather qi, refine pills, ascend, survive tribulations — and turns it into a working progression backbone. Three systems grow along it: ten stages of personal cultivation, a personal storage network that starts as Kongqiao and migrates into Xianqiao when you ascend, and a private realm whose endgame automation is built from source veins, interfaces, simulated machines and the Spirit Forging multiblock. Equipment carries the late game, and the bundled bilingual Ancient Jade handbook documents all of it.

### At a glance

| | |
| --- | --- |
| What it is | A cultivation (xianxia) progression mod whose late game is storage, equipment and automation |
| Progression | Ten stages: awaken, cultivate qi, refine pills, ascend at stage 5, then finish stages 7 to 10 through tribulations inside your own realm |
| Personal space | Kongqiao storage that grows from 9 to 72 slots, then Xianqiao storage plus a persistent personal dimension |
| Endgame | Source veins and interfaces, simulated machines, the Spirit Forging multiblock, three sword tiers and two artifact tiers |
| Supported targets | Minecraft 1.21.1 / NeoForge 21.1.235 and Minecraft 26.1.2 / NeoForge 26.1.2.94; no Forge loader line is maintained |
| In-game documentation | Bilingual Ancient Jade handbook, bundled through Patchouli |
| Registered content | 112 item registrations, or 40 standalone items and 72 block items |

### Install

| Minecraft | NeoForge | Java |
| --- | --- | --- |
| 1.21.1 | 21.1.235 | 21 |
| 26.1.2 | 26.1.2.94 | 25 |

Install the release JAR that matches your game version in the instance's mods directory, on both client and server, and keep one ImmortalStorage JAR per instance. Patchouli is embedded, so the handbook needs no separate installation. Back up worlds before updating.

### What is new in 0.1.7

| Change | In short |
| --- | --- |
| Enchantment multiblock | Put an Enchanting Table above the core instead of a Treasure Basin and the same shell runs the Enchantment Compressor, or the Enchantment Decompressor once the core is reversed. |
| Deferred material cost | The enchantment machines deduct their materials only when the product is taken out. Tearing the structure down, swapping the top block, reversing the core or breaking the block voids the uncollected product instead, so nothing drops and the materials stay in the machine. |
| Enchantment Compressor | Books, plain and enchanted alike, are materials only. Books alone merge into a single enchanted book; with an item present every enchantment moves onto the lowest-numbered non-book item and the other items are disenchanted where they stand. Conflicts are ignored and a repeated enchantment keeps its highest level. |
| Enchantment Decompressor | One composite enchanted book or enchanted item splits into single-enchantment books, up to eleven in a fixed order, with any leftover enchantments staying on the carrier. An item is always handed back; a spent book carrier is not returned as a plain book. |
| Handbook and particles | Ancient Jade documents both machines in English and Simplified Chinese, and the shared chain particles turn experience-rune green. |

The full list, including the deferred-cost rule and the split ordering, is in the [changelog](CHANGELOG.md).

### What is new in 0.1.6

| Change | In short |
| --- | --- |
| Spirit Forging multiblock | Around a Miniature Immortal Ruin core, eight World Barrier blocks and twelve iron chains form a working multiblock: the Spirit Forging Compressor converts source resources into Immortal Yuan in whole batches, and reversing the core turns the same structure into the Spirit Decompressor, which splits one input into up to twelve outputs. |
| Structure in the handbook | Ancient Jade carries the complete projection, the layer diagrams and every recipe; JEI and EMI list the compression and decompression production categories. |
| New recipes | Quartz and amethyst decomposition, ore splitting into its stone and its drops, and one base block plus eight Immortal Yuan to activate a budding amethyst or a Nurturing Crystal Bedrock. |
| Restored recipes | The Source Vein Manager and the Xianqiao Redstone Interface are craftable again, with matching handbook pages. |
| Resource locator | An enabled Spirit Instrument, or a carried Immortal Artifact, outlines containers and loot in yellow, spawners in purple and archaeology in green; holding an artifact in Mine mode adds white ore outlines. |
| Constant Night Vision | Stage 6 and above keeps Night Vision permanently, and renewing it never shortens a stronger, longer or infinite effect you already have. |
| Virtual elytra fix | Ability flight (creative or realm flight) now ends a glide before movement, so the jump key and the virtual elytra no longer fight each other. |
| Rendering | Non-source block items reuse their placed model, block state and world renderer in the inventory and in hand, and the ruins draw opaquely with a depth-writing shell over a black-and-white polarity texture. |

The full list, including the remaining fixes and the handbook work, is in the [changelog](CHANGELOG.md).

### Core systems

| System | What it does |
| --- | --- |
| Cultivation and stages | Ten stages driven by qi, pills and tribulations. Kongqiao opens crafting at stage 3, smithing at stage 4 and the Immortal Furnace at stage 5; stage 6 migrates storage into Xianqiao and unlocks the personal realm, and later stages add fluids, supported energy and Soul Escape. Stage 6 onwards grants constant Night Vision. |
| Personal realm | A persistent private dimension with day/night, weather and time-flow controls, respawn, World Barrier boundaries, Domain Expansion into the outside world, and isolated entity ticking with acceleration protection. |
| Personal storage | Search, sorting, a magnet, replenishment, and crafting, smithing, stonecutting and Immortal Furnace functions unlocked by stage. Xianqiao also stores supported energy, chemicals, Mana, Source, Souls and addon resource keys. |
| Equipment | Three spirit sword tiers, the five-mode Spirit Instrument and Immortal Artifact, and the Immortal One's Rule with Spiritual Aura Guard, virtual elytra flight, Soul Escape and warp travel. |
| Resource locator | An enabled Spirit Instrument, or a carried Immortal Artifact, outlines containers and loot in yellow, spawners in purple and archaeology in green; holding an artifact in Mine mode adds white ore outlines. |
| Automation and interfaces | Xianqiao and Advanced Xianqiao Interfaces, the Xianqiao Redstone Interface with hysteresis threshold control, the Source Vein Manager, and datapack-defined item, fluid and block sources. |
| Production machines | Immortal Furnace, Simulated Reincarnation Refining Furnace, Simulated Spirit Field, World Shard Miner and Treasure Basin, plus reinforcement plugins for 4x, 16x and 256x supported processing. |
| Spirit forging (0.1.6) | A Miniature Immortal Ruin core forms a multiblock whose Spirit Forging Compressor converts source resources in whole batches; reversing the core switches the same structure to the Spirit Decompressor. |
| Utility | Substitute Puppet, Soul Catcher and Information Storage Crystal for death protection, creature capture and machine-state copy and paste. |

### Complete item catalog

The following groups cover all 112 item registrations, including block items, conditional integrations and creative-only items. Availability depends on the target version and installed integrations. Placed Yuan lights and the temporary ruin block use their parent items; they are not extra inventory items. Tool modes and data-defined source variants share their base item registrations. The Spirit Forging Compressor and Spirit Decompressor are core-polarity modes of the Miniature Immortal Ruin, not extra items. Recipes and detailed interactions are available in Ancient Jade.

| Category | All items and block items | Purpose |
| --- | --- | --- |
| Guide and Yuan | Ancient Jade, TruePower, ImmortalPower | Handbook, cultivation resources, fuel and placeable lights. |
| Pills and progression | Crude Pill Embryo, Crude Pill, Refined Pill Embryo, Refined Pill, Breakthrough Pill Embryo, Breakthrough Pill, Immortal Pill, Ascension Dan, White Day Thunder | Pill crafting and cultivation; Immortal Pill enables normal ascension, Ascension Dan is rare End-ship loot, and White Day Thunder is a creative/admin shortcut. |
| Ores and basic materials | Spirit Iron Ore, Deepslate Spirit Iron Ore, Spirit Crystal Ore, Deepslate Spirit Crystal Ore, Crude Spirit Iron, Spirit Iron, Spirit Iron Nugget, Spirit Crystal, Crude Spirit Iron Block, Spirit Iron Block, Spirit Crystal Block, Spirit Core, Premixed Heavy Compound | Mining, smelting, compact storage and equipment/machine crafting. |
| Nurturing crystals | Nurturing Crystal, Inactive Nurturing Crystal Bedrock, Nurturing Crystal Bedrock, Small Nurturing Crystal Bud, Medium Nurturing Crystal Bud, Large Nurturing Crystal Bud, Nurturing Crystal Cluster | Activate the bedrock and grow buds into harvestable crystal clusters. |
| Immortal forging | Immortal Art Remains, Immortal-Forged Alloy Scrap, Immortal-Forged Alloy | Mine the End outer islands, fire the remains into scrap, then craft alloy for advanced equipment. |
| Qi and realm building | Qi Collecting Bottle, Disposable Qi Collecting Bottle, Primordial Qi, World Barrier | Collect qi for advanced crafting; Primordial Qi and World Barrier support realm construction. |
| Weapons and equipment | Spirit Sword, Immortal-Ruin-Forged Spirit Sword, One-Qi Returning Origin Sword, Spirit Instrument, Immortal Artifact, Immortal One's Rule | Three sword tiers, five-mode tools, and Spiritual Aura Guard with virtual elytra flight. |
| Utility items | Substitute Puppet, Soul Catcher, Information Storage Crystal | Death protection, creature capture, and copying/restoring supported machine state. |
| Storage and interfaces | Immortal Spirit Drive, Xianqiao Manager, Xianqiao Interface, Advanced Xianqiao Interface, Xianqiao Redstone Interface, Xianqiao Exchange Cell, Xianqiao Exchange Disk (RS) | Bind automation to personal storage, route resources, monitor thresholds with redstone, and connect AE2/RS through their respective disks. |
| Production machines | Immortal Furnace, Simulated Reincarnation Refining Furnace, Simulated Spirit Field, World Shard Miner, Treasure Basin | Refining and smelting, simulated mob/crop production, mineral extraction, and structure-loot production. |
| Energy crystals | Xianeng Electricity Crystal, Xianeng Mana Crystal, Xianeng Source Crystal | FE conversion; Mana and Source variants register only with Botania and Ars Nouveau respectively on supported targets. |
| Immortal ruins | Miniature Immortal Ruin, Stabilized Miniature Immortal Ruin, Entangled Stabilized Miniature Immortal Ruin, Advanced Stabilized Miniature Immortal Ruin, Advanced Entangled Stabilized Miniature Immortal Ruin | Portable ruin interactions, stabilized, entangled or advanced container scheduling, and the Spirit Forging multiblock core. |
| Reinforcement plugins | Dimensional Peeking Order, Dimensional Parallel Talisman, Great-Thousand-World Parallel Edict | Increase supported processing speed by 4x, 16x or 256x without multiplying outputs. |
| Source control | Source Vein Manager, Custom Source Vein | Configure source output and data-defined item/fluid sources. |
| Fluid sources | Water Source Vein, Milk Source Vein, Lava Source Vein | Supply the named fluids to automation. |
| Building sources | Cobblestone Source Vein, Stone Source Vein, Smooth Stone Source Vein, Dirt Source Vein, Oak Log Source Vein | Supply common building materials. |
| Concrete sources | White Concrete Source Vein, Orange Concrete Source Vein, Magenta Concrete Source Vein, Light Blue Concrete Source Vein, Yellow Concrete Source Vein, Lime Concrete Source Vein, Pink Concrete Source Vein, Gray Concrete Source Vein, Light Gray Concrete Source Vein, Cyan Concrete Source Vein, Purple Concrete Source Vein, Blue Concrete Source Vein, Brown Concrete Source Vein, Green Concrete Source Vein, Red Concrete Source Vein, Black Concrete Source Vein | All sixteen concrete colors. |
| Mineral and rare sources | Coal Source Vein, Raw Copper Source Vein, Raw Iron Source Vein, Raw Gold Source Vein, Lapis Source Vein, Redstone Source Vein, Crude Spirit Iron Source Vein, Spirit Crystal Source Vein, Diamond Source Vein, Emerald Source Vein, Echo Shard Source Vein, Ancient Debris Source Vein, Nether Star Source Vein, Enchanted Golden Apple Source Vein, Dragon Egg Source Vein | Supply the named mining and high-tier resources. |

### Main progression

1. **Awaken:** carry Ancient Jade for a full day or sleep with it to enter stage 1. Right-click it for the guide; mine Spirit Iron and Spirit Crystal to begin crafting.
2. **Cultivate:** accumulate qi and refine pills. Stages 1 to 5 expand Kongqiao from 9 to 72 slots with increasing stack capacity. Stage 3 unlocks crafting, stage 4 smithing, and stage 5 the built-in Immortal Furnace.
3. **Ascend:** at stage 5, fill qi and use an Immortal Pill. Ascension Dan from End-ship loot offers a rare shortcut. Stage 6 migrates storage into Xianqiao and unlocks a personal dimension; existing True Yuan (TruePower) converts once at 16:1 into Immortal Yuan (ImmortalPower). Later True Yuan stays independent; one Immortal Yuan can be crafted into 64 True Yuan.
4. **Expand:** stage 7 adds fluids; stage 8 adds supported energy and other resources and unlocks Soul Escape. Complete tribulations in your own realm to reach stages 7 to 10. Manage realm time and weather, build inside it, or use Domain Expansion to bring a region into the outside world.
5. **Automate and equip:** combine source blocks, interfaces and production machines; add reinforcement plugins and ruin scheduling. Mine Immortal Art Remains in the End outer islands, make alloy, and upgrade to the Immortal Artifact and Immortal One's Rule, which also gains warp travel in teleport mode.

### Spirit forging

The Spirit Forging multiblock is built around a Miniature Immortal Ruin: eight World Barrier blocks and twelve iron chains around the core, a World Shard Miner below and a Treasure Basin above. Ancient Jade shows the complete projection and every layer, so the structure can be assembled straight from the handbook. Forming it opens a 4x3 input to a single output. The compressor converts source-defined resources into Immortal Yuan in whole batches, accepts datapack compression recipes, moves up to 64 items per tick on each of its six faces, can output directly into the owner's Xianqiao, and shares the simulated-machine face panel and three-state redstone control.

Reversing the core turns it into the Spirit Decompressor with one input and twelve outputs, its own datapack recipes, ordinary ore processing and atomic multiple outputs; switching polarity ejects both cached inventories. Quartz slabs return 2 quartz, while blocks, stairs and chiseled variants return 4; an amethyst block returns 4 amethyst shards; an ore returns its stone and its drops, so coal ore gives cobblestone and 4 coal and iron ore gives cobblestone and 4 raw iron; an amethyst block or an inactive Nurturing Crystal Bedrock plus eight ImmortalPower becomes its budding or active form. Counted multiple materials, including overlapping tag ingredients, are matched without consuming input when the output is blocked. JEI and EMI list the compression and decompression production categories.

Replacing the Treasure Basin above the core with an Enchanting Table turns the same shell into the Enchantment Compressor, and reversing the core switches it to the Enchantment Decompressor. Both share the spirit machines' interface, six-face panel, three-state redstone control and direct Xianqiao output, and their chain particles glow in experience-rune green. They differ in one rule: their materials are deducted only when the product is taken out, while tearing the structure down, swapping the top block, reversing the core or breaking the block voids any uncollected product instead, so nothing drops and the materials stay in the machine. The compressor treats books of both kinds as materials, merging books alone into a single enchanted book and otherwise moving every enchantment onto the lowest-numbered non-book item while disenchanting the other items in place. The decompressor splits one composite book or enchanted item into single-enchantment books, up to eleven in registry order, leaving any remainder on the carrier; an item is always handed back, while a spent book carrier is not returned as a plain book.

### Resource locator

The Spirit Instrument and the Immortal Artifact can locate resources while they are in use: yellow outlines mark containers and loot, purple marks spawners and green marks archaeology. An artifact held in Mine mode adds white ore outlines. Ore outlines follow a loot dictionary built from real loot tables at world load, so an off-hand drop or an ore selects every variant sharing its source family, without name guessing or repeated world scans. The exploration radius defaults to 32 blocks and the ore radius to 16, both configurable up to 48 and overridable by a datapack.

### Everyday controls and systems

| Default input | Action |
| --- | --- |
| B | Open personal storage; search, sort, craft and manage equipment/resources. |
| T | Show cultivation progress. |
| V / Shift+V | Enter or leave your realm / toggle Domain Expansion outside it, or return to its center inside it. |
| Shift + mouse wheel | Cycle the held tool's modes: wrench, build, mine, teleport and explore. |
| Jump while airborne | Expand or fold the Spiritual Aura Guard virtual elytra; while actively flying (creative or stage 6+) the jump key stays with flight. |
| Double-tap W | Boost the expanded virtual elytra; follows the rebound forward keyboard key. Two presses within 350 ms, with no hold-to-repeat. |
| F7 | Toggle Soul Escape at stage 8 or later: spectator mode, then back to the exact mode you left. Rebindable, and available in hardcore or cheat-free worlds. |
| Teleport mode with off-hand ImmortalPower | Warp the Immortal Artifact 100 blocks per item along the horizontal look direction, spending the whole off-hand stack and landing on the highest safe spot. |

Personal storage includes search, sorting, a magnet, replenishment and unlocked crafting/smithing/stonecutting/furnace functions. Machines combine their own enable switch with redstone work modes. The redstone interface monitors high/low resource thresholds with normal or inverted hysteresis; Enter saves numeric edits. Spiritual Aura Guard spends True Yuan first, then Immortal Yuan, before unpaid damage reaches health. The resource locator outlines default to a 32-block exploration radius and a 16-block ore radius; both are configurable and can be overridden by a datapack. While ability flight (creative or realm flight) is active, a gliding state ends before movement and the virtual elytra cannot deploy or boost.

### Compatibility and help

Both targets integrate with their matching AE2, Refined Storage, JEI and Curios versions. Additional integrations on 1.21.1 include Mekanism, EMI, Create, Botania and Ars Nouveau; do not assume those integrations are available on 26.1.2. Energy conversion and machine behavior follow server configuration. Datapacks can extend supported recipes, sources, crop definitions, loot catalogs and locator radii; see [Data-driven formats](docs/data-driven.md). Non-source block items reuse their placed block model and animated renderer in the inventory and in hand. See Ancient Jade for recipes and full gameplay details. Report issues with the game/mod versions, reproduction steps and relevant logs at [GitHub Issues](https://github.com/positer/ImmortalStorage/issues). License: [All Rights Reserved](LICENSE).

---

## 简体中文

工作区设计资料：[CEF「机械动力：纠缠工厂」完整执行方案](../Minecraft Forge Workspace/docs/cef/CEF完整执行方案_Agent直接使用.md)（资料位于同级开发枢纽工作区，不在本仓库内），含实现合同与 pre-debug 清单；这是独立附属模组的设计文档，不属于仙藏已发布功能。CEF已在独立工程持续开发，置物台原型已接入原生序列、真实压机周期与网络工作槽/原位升级，完整可玩流程仍在开发。

仙藏是一款 **Minecraft 修仙模组**，运行于 NeoForge。它把修仙题材最经典的那条阶梯——凡人启灵、采气炼药、飞升渡劫——做成了一条真正可玩的进阶主脊，并沿脊挂上三套互相咬合的系统：十阶个人修行、从空窍起步并在飞升时迁入仙窍的随身存储网络，以及以源方块、接口、模拟机器与锻灵多方块搭建终局自动化的个人仙窍维度。后期由装备接棒，全部内容都写在随模组内置的中英双语古玉指导书中。

### 一眼概览

| | |
| --- | --- |
| 模组性质 | 修仙（仙侠）题材的进阶模组，终局落在存储、装备与自动化 |
| 主要流程 | 十阶修行：启灵、积气炼药、五阶飞升，之后在自己的仙窍内渡劫走完七至十阶 |
| 个人空间 | 空窍随身存储从 9 格扩展到 72 格，飞升后迁入仙窍，并开放持久化的个人维度 |
| 终局内容 | 源方块与接口、模拟机器、锻灵多方块、三档灵剑与两档仙器 |
| 支持版本 | Minecraft 1.21.1 / NeoForge 21.1.235，以及 Minecraft 26.1.2 / NeoForge 26.1.2.94；不维护 Forge 版本 |
| 内置说明 | 中英双语古玉指导书，通过内置帕秋莉提供 |
| 注册内容 | 112 项物品注册，即 40 个独立物品与 72 个方块物品 |

### 安装

| Minecraft | NeoForge | Java |
| --- | --- | --- |
| 1.21.1 | 21.1.235 | 21 |
| 26.1.2 | 26.1.2.94 | 25 |

将对应版本的发行 JAR 安装到客户端与服务端实例的 mods 目录，每个实例只保留一个仙藏 JAR。已内置 Patchouli，手册无需另装依赖。更新前请备份世界。

### 0.1.7 新增内容

| 改动 | 简要说明 |
| --- | --- |
| 附魔多方块 | 把核心上方的聚宝盆换成附魔台，同一外壳即运行附魔压缩机；反转核心则变为附魔解压器。 |
| 延迟扣料 | 附魔机只在产物被取出时才扣材料。拆除结构、更换顶层方块、反转核心或破坏方块会作废未取走的产物（什么都不掉落），材料留在机器内。 |
| 附魔压缩机 | 书（普通书与附魔书）一律只作材料。只有书时整合为一本附魔书；存在物品时把全部附魔转移到序号最小的非书物品上，其余物品在原地被清除附魔。无视冲突，同名附魔取最高等级。 |
| 附魔解压器 | 一本复合附魔书或一件附魔物品拆为单附魔书，按固定顺序最多十一本，未拆完的附魔留在载体上。物品始终返还，被消耗的书载体不会返还为普通书。 |
| 手册与粒子 | 古玉以中英双语记录两台机器；铁链粒子改为经验符文绿。 |

包含延迟扣料规则与拆分顺序的完整列表见[更新日志](CHANGELOG.md)。

### 0.1.6 新增内容

| 改动 | 简要说明 |
| --- | --- |
| 锻灵多方块 | 以迷你仙墟为核心，八块世界胎壁与十二条铁链搭成一座可运行的多方块：锻灵压缩机按整批把源资源兑换为仙元；反转核心后同一结构变为锻灵解压器，把单输入拆成最多十二输出。 |
| 指导书结构图 | 古玉提供完整结构投影、逐层分解与全部配方，JEI 与 EMI 列出压缩与解压两类生产配方。 |
| 新增配方 | 石英与紫水晶拆解、原矿拆为石料与掉落物，以及一块基材加八枚仙元把紫水晶块或失活蕴灵晶母岩激活为对应活跃形态。 |
| 补全配方 | 源方块管理器与仙窍红石接口恢复可合成，并配齐对应指导书页面。 |
| 资源定位 | 开启的灵器或随身携带的仙器可定位资源：黄色标记容器与战利品、紫色标记刷怪笼、绿色标记考古；手持挖掘模式仙器再加白色矿物框。 |
| 常驻夜视 | 六阶及以上常驻夜视，且续期不会缩短你已有的更强、更长或无限时长的效果。 |
| 虚拟鞘翅修复 | 主动飞行（创造飞行或仙窍飞行）会在移动前结束滑翔，跳跃键与虚拟鞘翅不再互相抢占。 |
| 渲染优化 | 非源方块物品在物品栏与手持时复用放置后的模型、方块状态与世界渲染器；仙墟改为不透明绘制，以黑白极性纹理保持外壳深度写入。 |

包含其余修复与手册改动的完整列表见[更新日志](CHANGELOG.md)。

### 核心系统

| 系统 | 作用 |
| --- | --- |
| 修行与境界 | 十阶修行由灵气、丹药与渡劫推进。三阶开放内置合成、四阶开放锻造、五阶开放内置仙炉；六阶把存储迁入仙窍并开放个人维度，此后逐阶加入流体、能量与灵魂出窍，六阶起常驻夜视。 |
| 个人仙窍 | 持久化个人维度：日夜、天气与时间流速控制，仙窍床重生，世界胎壁边界，领域展开，以及带加速保护的独立实体 tick 处理。 |
| 随身存储 | 搜索、排序、磁铁、自动补充，以及按境界开放的合成、锻造、切石与仙炉功能；仙窍还统一存储受支持的能量、化学品、魔力、魔源、灵魂与附属资源键。 |
| 装备 | 三档灵剑、五模式灵器与仙器、提供灵气护体的仙师度，以及虚拟鞘翅飞行、灵魂出窍与折跃传送。 |
| 资源定位 | 开启探索的灵器，或仅需背在身上的仙器，可用黄色标记容器与战利品、紫色标记刷怪笼、绿色标记考古；手持挖掘模式仙器再显示白色矿物框。 |
| 自动化与接口 | 仙窍接口与高级仙窍接口、带滞回阈值控制的仙窍红石接口、源方块管理器，以及数据包定义的物品、流体与方块源。 |
| 生产设备 | 仙炉、模拟轮回炼化炉、模拟灵田、世界碎片开采器与聚宝盆，并有 4 倍、16 倍、256 倍的强化插件。 |
| 锻灵多方块（0.1.6） | 以迷你仙墟为核心形成多方块：锻灵压缩机按整批把源资源兑换为仙元；反转核心后同一结构切换为锻灵解压器。 |
| 实用道具 | 替死傀儡、拘灵器与信息存储水晶，分别用于替死保护、生物拘取与机器状态复制粘贴。 |

### 全物品分类

以下分类覆盖全部 112 个物品注册项，包括方块物品、条件联动物品与创造用途道具；实际可用内容取决于游戏版本与已安装联动。放置后的元气光源、临时仙墟方块由对应道具产生，不是额外背包物品；工具模式和数据定义源变体共用基础注册项；锻灵压缩机与锻灵解压器是迷你仙墟核心的两种极性模式，也不是额外物品。具体配方与详细交互请查阅古玉。

| 分类 | 全部物品及方块物品 | 用途 |
| --- | --- | --- |
| 古玉与元气 | 古玉、真元、仙元 | 修行手册、修炼资源、燃料与可放置光源。 |
| 丹药与进阶 | 粗制聚灵丹胚、粗制聚灵丹、精致聚灵丹胚、精致聚灵丹、破限丹胚、破限丹、仙丹、飞升丹、白日惊雷 | 丹胚炼制与修行进阶；仙丹用于正常飞升，飞升丹为末地船稀有战利品，白日惊雷为创造或管理用捷径。 |
| 矿石与基础材料 | 灵铁矿、深层灵铁矿、灵晶矿、深层灵晶矿、粗灵铁、灵铁、灵铁粒、灵晶、粗灵铁块、灵铁块、灵晶块、灵核、预拌沉重混合物 | 采矿、烧炼、材料压缩及装备与机器合成。 |
| 蕴灵晶生长链 | 蕴灵晶、失活的蕴灵晶母岩、蕴灵晶母岩、小型蕴灵晶芽、中型蕴灵晶芽、大型蕴灵晶芽、蕴灵晶簇 | 激活母岩，让晶芽逐级生长为可采集晶簇。 |
| 仙锻材料 | 仙法遗骸、仙锻合金碎片、仙锻合金 | 在末地外岛采集遗骸，烧炼为碎片，再合成高阶装备所需合金。 |
| 采气与仙窍建材 | 采气瓶、一次性采气瓶、混元一气、世界胎壁 | 采气用于高阶合成，混元一气与世界胎壁用于仙窍建设。 |
| 武器与装备 | 灵剑、仙墟锻灵剑、一气归元剑、灵器、仙器、仙师度 | 三种灵剑、五模式工具，以及提供灵气护体与虚拟鞘翅的仙师度。 |
| 实用道具 | 替死傀儡、拘灵器、信息存储水晶 | 替死保护、生物拘取，以及受支持机器状态的保存与粘贴。 |
| 存储与接口 | 仙灵驱动器、仙窍管理器、仙窍接口、高级仙窍接口、仙窍红石接口、仙窍交流磁盘、仙窍交流磁盘（RS） | 绑定个人存储、调度资源、以红石监测阈值，并通过对应磁盘接入 AE2/RS。 |
| 生产设备 | 仙炉、模拟轮回炼化炉、模拟灵田、世界碎片开采器、聚宝盆 | 炼制与烧炼、模拟生物与作物生产、矿物开采及结构战利品生产。 |
| 仙能水晶 | 仙能电力水晶、仙能魔力水晶、仙能魔源水晶 | FE 转换；魔力与魔源变体仅在受支持版本安装对应 Botania 或 Ars Nouveau 时注册。 |
| 仙墟系列 | 迷你仙墟、稳定化的迷你仙墟、纠缠的稳定化迷你仙墟、高级稳定化迷你仙墟、高级纠缠的稳定化迷你仙墟 | 便携仙墟交互，稳定化、纠缠与高级容器调度，以及锻灵多方块核心。 |
| 强化插件 | 次元窥令、次元平行法符、大千世界并行敕令 | 为受支持设备提供 4 倍、16 倍、256 倍处理速度，不增加单次产物。 |
| 源方块控制 | 源方块管理器、自定义源方块 | 管理源方块输出，并承载数据定义的物品或流体源。 |
| 流体源 | 水源方块、奶源方块、岩浆源方块 | 向自动化设施提供对应流体。 |
| 建材源 | 圆石源方块、石头源方块、平滑石头源方块、泥土源方块、橡木原木源方块 | 持续提供常用建筑材料。 |
| 混凝土源 | 白色混凝土源方块、橙色混凝土源方块、品红色混凝土源方块、淡蓝色混凝土源方块、黄色混凝土源方块、黄绿色混凝土源方块、粉红色混凝土源方块、灰色混凝土源方块、淡灰色混凝土源方块、青色混凝土源方块、紫色混凝土源方块、蓝色混凝土源方块、棕色混凝土源方块、绿色混凝土源方块、红色混凝土源方块、黑色混凝土源方块 | 覆盖全部十六种混凝土颜色。 |
| 矿物与稀有资源源 | 煤炭源方块、粗铜源方块、粗铁源方块、粗金源方块、青金石源方块、红石源方块、粗灵铁源方块、灵晶源方块、钻石源方块、绿宝石源方块、回响碎片源方块、远古残骸源方块、下界之星源方块、附魔金苹果源方块、龙蛋源方块 | 提供对应矿产与高阶资源。 |

### 主要流程

1. **启灵入门：** 携带古玉一整日，或携玉入睡，进入一阶。右键查阅手册，采集灵铁与灵晶开始合成。
2. **积累修行：** 获取灵气并炼制丹药；一至五阶空窍从 9 格扩展到 72 格，单格堆叠量逐级增加。三阶开放内置合成，四阶开放锻造，五阶开放内置仙炉。
3. **飞升六阶：** 五阶灵气满额后使用仙丹；末地船中的飞升丹提供稀有捷径。飞升时空窍物品迁入仙窍并开放个人维度，既有真元按 16:1 一次性转为仙元；之后获取的真元保持独立，一枚仙元可合成为 64 真元。
4. **拓展仙窍：** 七阶开放流体，八阶开放受支持的能量及额外资源并解锁灵魂出窍；在自己的仙窍内渡劫，逐步达到十阶。可管理仙窍时间与天气、在其中建设，或通过领域展开将一片空间带到外界。
5. **自动化与仙锻：** 组合源方块、接口与生产机器，加入强化插件和仙墟调度；在末地外岛寻找仙法遗骸，炼制仙锻合金，升级仙器与仙师度，其中的仙器还会获得传送模式下的折跃传送。

### 锻灵多方块

锻灵结构以迷你仙墟为核心：八块世界胎壁与十二条铁链环绕核心，下方为世界碎片开采器，上方为聚宝盆。古玉提供完整结构投影与逐层分解，照着手册即可搭建。成型后打开 4x3 输入到单输出的界面。压缩机按整批把源定义资源兑换为仙元，支持数据包压缩配方，六个面每 tick 可主动吞吐 64 件，可直接输出到放置者的仙窍，并复用模拟机器的六面面板与三态红石控件。

反转核心后切换为锻灵解压器：左侧单输入、右侧十二输出，使用独立数据包配方，按原矿整格处理并原子化提交多结果；切换极性会弹出两套缓存。石英半砖拆解为 2 个石英，整块、楼梯与凿刻变体为 4 个；一块紫水晶块拆解为 4 枚紫水晶碎片；原矿拆为石料与掉落物，例如煤矿石产出圆石与 4 个煤炭、铁矿石产出圆石与 4 个粗铁；一块紫水晶块或失活的蕴灵晶母岩加八枚仙元可激活为紫水晶芽床或蕴灵晶母岩。压缩支持计数多材料与重叠标签原料，输出被阻挡时不消耗材料。JEI 与 EMI 列出压缩与解压两类生产配方。

把核心上方的聚宝盆换成附魔台，同一外壳即变为附魔压缩机；反转核心则切换为附魔解压器。两者与锻灵机共用界面、六面面板、三态红石控件与仙窍直出，铁链粒子改为经验符文绿。唯一的规则差异是扣料时机：材料只在产物被取出时才扣除，而拆除结构、更换顶层方块、反转核心或破坏方块会作废未取走的产物——什么都不掉落，材料留在机器内。压缩机把两种书都当作材料：只有书时整合为一本附魔书，存在物品时把全部附魔转移到序号最小的非书物品上，其余物品在原地被清除附魔。解压器把一本复合附魔书或一件附魔物品拆成单附魔书，按注册名顺序最多十一本，未拆完的附魔留在载体上；物品始终返还，被消耗的书载体不会返还为普通书。

### 资源定位

灵器与仙器在使用时可定位资源：黄色框标记容器与战利品、紫色框标记刷怪笼、绿色框标记考古。手持挖掘模式仙器再加白色矿物框。矿物定位依据世界载入时由真实掉落表建立的词典：副手掉落物或原矿会选中同一来源族的全部变体，不按名称猜测、不反复扫描世界。探索半径默认 32 格、矿物半径默认 16 格，均可在 48 格以内配置，并可被数据包覆盖。

### 常用操作与系统

| 默认操作 | 功能 |
| --- | --- |
| B | 打开个人存储，搜索、排序、合成及管理装备与资源。 |
| T | 查看修行进度。 |
| V / Shift+V | 进入或离开仙窍 / 在外界展开或收起领域，在仙窍内回到中心。 |
| Shift + 鼠标滚轮 | 切换手持工具的扳手、建筑、挖掘、传送与探索模式。 |
| 空中按跳跃键 | 展开或收起灵气护体虚拟鞘翅；正在飞行（创造或六阶以上）时跳跃键归飞行，不展开鞘翅。 |
| 双击 W | 为展开的虚拟鞘翅加速，跟随前进键的键盘重绑定；350 毫秒内按下两次，长按不重复触发。 |
| F7 | 八阶后可灵魂出窍：进入旁观者模式，再按一次回到进入前的游戏模式。可自定义，且极限模式或无作弊世界同样可用。 |
| 传送模式 + 副手仙元 | 仙器折跃传送：沿视线水平方向每枚仙元位移 100 格，一次性消耗整叠副手仙元，落点取目标列最高的安全位置。 |

个人存储提供搜索、排序、磁铁、自动补充，以及按进度开放的合成、锻造、切石和仙炉功能。机器自身开关与红石工作模式共同决定运行状态；仙窍红石接口按资源高低阈值执行正相或反相滞回控制，数字修改按 Enter 保存。灵气护体优先消耗真元，其次消耗仙元，不足的伤害才扣除生命。资源定位的探索半径默认 32 格、矿物半径默认 16 格，两者独立配置并可被数据包覆盖。处于创造飞行或仙窍飞行时，虚拟滑翔会在移动前结束，鞘翅既不能展开也不能加速。

### 兼容与帮助

两代版本均联动各自对应的 AE2、Refined Storage、JEI 与 Curios。1.21.1 另有 Mekanism、EMI、Create、Botania、Ars Nouveau 等联动，不代表这些联动在 26.1.2 也可用。能量转换与机器行为遵循服务器配置；数据包可扩展受支持的配方、源定义、作物、战利品目录与定位半径，格式见[数据驱动文档](docs/data-driven.md)。非源方块物品在物品栏与手持时复用放置后的方块模型与动画渲染。配方与完整玩法见游戏内古玉。反馈问题时请在 [GitHub Issues](https://github.com/positer/ImmortalStorage/issues) 附上游戏与模组版本、复现步骤及相关日志。许可：[保留所有权利](LICENSE)。


CEF optional follow-up design: [P01 Factory blueprints and shared Y rotation](../Minecraft Forge Workspace/docs/cef/patches/P01-blueprint-rotation/README.md). This is a separate, unimplemented patch specification.

CEF后续增量设计：[P01 工厂蓝图与网络整体Y旋转](../Minecraft Forge Workspace/docs/cef/patches/P01-blueprint-rotation/README.md)，独立待实现补丁，不替换现有执行方案。


CEF follow-up design / 后续增量设计：[P02 工作盆唯一导出、原生思索与JEI/EMI](../Minecraft Forge Workspace/docs/cef/patches/P02-basin-ponder-viewers/README.md)。独立待接入规格，未改变基础方案和P01。


CEF补丁P02已接收并完成[开发交接](../Minecraft Forge Workspace/docs/cef/HANDOVER.md)：唯一主动盆出口、原生Ponder与JEI/EMI仍为待实施规格，25项验收NOT_RUN；实际开发继续在独立CEF工程进行。


CEF follow-up design / 后续增量设计：[P03 原生配方、延迟空间、网络销毁与分类接口](../Minecraft Forge Workspace/docs/cef/patches/P03-recipes-lifecycle-ports/README.md)。含存储总线与内外物品/流体管道连接合同；独立待实现补丁，36项游戏验收尚未执行。


CEF当前实现对照：[2026-10-16 独立审查报告](../Minecraft Forge Workspace/docs/cef/reviews/2026-10-16-implementation-audit/REPORT.md)，包含蓝图扣料缺陷、P03差距与既有测试边界。

CEF审查整改顺序已由用户确认：先数据安全与玩法正确性，后引导和外观；阶段验收条件见上述独立审查报告。


CEF最新对照：[2026-10-17 实现复查及未知引用销毁裁定](../Minecraft Forge Workspace/docs/cef/reviews/2026-10-17-implementation-audit/REPORT.md)。

CEF全面核查：[全功能进度矩阵与玩家入口缺口](../Minecraft Forge Workspace/docs/cef/reviews/2026-10-17-implementation-audit/FULL_REPORT.md)。


## 2026-10-17 CEF 原生优先实现指导

已提供[15张执行卡与源码索引](../Minecraft Forge Workspace/docs/cef/implementation/2026-10-17-create-first/README.md)及[直接交付ZIP](../Minecraft Forge Workspace/docs/cef/implementation/CEF-Create-first-implementation-guide.zip)，覆盖材料事务、空间生命周期、原生盆台工艺、接口与引导。包含45项待执行行为验收；这是实施指导，不代表功能已完成。


CEF最新推进复核：[Agent反馈对照与修复顺序](../Minecraft Forge Workspace/docs/cef/reviews/2026-10-17-agent-progress-followup/GUIDANCE.md)。C01仍存在入口与持久化缺口，优先修安全与正确性，再处理延迟分配和外观。

CEF本轮进展（2026-10-17，独立工程提交 d093d1e，中断恢复）：打印事务的**端口回退并入事务**（返回失败**与抛异常**两条路径共用同一回退），探针以「蓝图携带与网络不同的端口表走一次被拒绝的打印、断言端口表逐项还原」把它变成可观测断言；探针 scratch 夹具锚点由 y=90 抬到 y=300——红灯根因是**随机种子世界的地形撞进夹具**（解码存档证据：该列整列 stone、地形顶 y=131、无任何 CEF 方块），**不是**打印路径缺陷。门禁 154/154 游戏测试、57 套件/317 单元/0 失败、探针 passed=true（59 项 check、双 JVM）。**C02 仍未完成**：指导包九条要求 0 条全满足，INVALID 计费、实体化前材料预检、持久托管记录与第三方来源身份仍欠。证据：独立工程 `archive/20261017-153500-c02-print-transaction-and-probe-fixture/` 与 `docs/development/2026-10-17-c02-probe-rca.md`、`...-gap-audit.md`。


CEF最新指导：[进度复核v2与执行顺序](../Minecraft Forge Workspace/docs/cef/reviews/2026-10-17-progress-recheck-v2/GUIDANCE.md)。用户已确认工厂放置即分配并常加载，覆盖旧延迟分配要求；优先接通T04–T06探针、闭环C01和材料事务。


## 2026-10-17 CEF复核v3

CEF最新复核：[v3推进指导](../Minecraft Forge Workspace/docs/cef/reviews/2026-10-17-progress-recheck-v3/GUIDANCE.md)。当前专服探针仍红，先查夹具现场，再修清理范围、票据释放和重启恢复。


## 2026-10-18 CEF复核v4

CEF最新：[v4复核与下一检查点](../Minecraft Forge Workspace/docs/cef/reviews/2026-10-18-progress-recheck-v4/GUIDANCE.md)，限定CP-CEF-04删除与访问安全闭环；全部7项通过后输出明确停止信号等待审查。


## 2026-10-18 CP04决策与CP05

CEF决策：[CP04有限范围审查与CP05指导](../Minecraft Forge Workspace/docs/cef/reviews/2026-10-18-cp04-decision-cp05/DECISION.md)。推荐C仅K5，先材料/工具事务安全；CP02实体效应例外保留。

## 2026-10-19 CEF CP05复核

最新[CP05返工指导](../Minecraft Forge Workspace/docs/cef/reviews/2026-10-19-cp05-review/GUIDANCE.md)：现有门禁通过，但工具回滚与托管持久恢复仍有缺口；先完成CP05第二版审查，再推进新玩法。

CEF工期规划：[剩余轮次估算](../Minecraft Forge Workspace/docs/cef/reviews/2026-10-19-cp05-review/REMAINING_ROUNDS.md)，功能约8–12轮，可发布验收累计约10–16轮；CP05与C08完成后重估。

## 2026-10-19 CP05阻塞后续

最新[CP05第三版推进指导](../Minecraft Forge Workspace/docs/cef/reviews/2026-10-19-cp05-blocked-followup/GUIDANCE.md)：先闭合退款与托管余额，再定位实体探针read失败；继续限定CP05，不启动C08。

## 2026-10-19 CEF接续开发：功能与材质优先

最新[接续指导](../Minecraft Forge Workspace/docs/cef/reviews/2026-10-19-feature-first-handoff/GUIDANCE.md)及[新对话Prompt](../Minecraft Forge Workspace/docs/cef/reviews/2026-10-19-feature-first-handoff/NEW_CHAT_PROMPT.md)已同步独立CEF仓库。按用户最新要求解除CP05全链前置冻结，做最低安全收尾后推进全功能全材质，专项验证留到集中验收。

## 2026-10-19 CP06两项集成收口

最新[收口指导](../Minecraft Forge Workspace/docs/cef/reviews/2026-10-19-cp06-integration-finish/GUIDANCE.md)：保留未提交成果，只补JEI/EMI与蓝图工作桌/分享入口，随后交功能材质完成审查。
