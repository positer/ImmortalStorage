# 世界碎片开采器：当前世界真实生成读取方案

## 目标与精确定义

“当前世界”不是模组内置默认表，也不是注册阶段看到的原始 JSON，而是服务端完成模组注册、数据包覆盖、标签绑定和 NeoForge Biome Modifier 后，当前存档实际使用的动态注册表与区块生成器。

本方案分别计算两类概率，禁止混成一个手写权重表：

- 矿物：`目标维度群系占比 × 该群系最终生成区块中的矿物密度`。
- 聚宝盆：`目标维度每区块未开启战利品容器出现率 × 当前战利品表的实际滚取结果`。

任意模组都可以用自定义 `Feature`、`PlacementModifier`、`RuleTest`、结构代码或运行时逻辑生成内容，因此不存在能够静态、通用且绝对精确地还原所有模组概率的公共 API。系统必须同时使用“当前注册表静态解析”和“不修改世界的最终区块被动校准”，并把覆盖度公开出来；不能再把未知修饰器默认为系数 `1` 后声称结果精确。

## 数据流水线

### 1. 创建世界生成纪元

服务端启动和 `/reload` 完成后，在服务端主线程从 `MinecraftServer.registryAccess()` 读取最终动态注册表：

- `LEVEL_STEM`、`BIOME`、`CONFIGURED_FEATURE`、`PLACED_FEATURE`；
- NeoForge 应用修改后的 `Biome#getGenerationSettings()`；
- 当前物品、方块、方块/物品标签以及战利品表注册表；
- 世界种子、目标维度 `ChunkGenerator` 和 `BiomeSource`。

对上述实际生效内容的稳定编码、目标模式 JSON、相关标签和加载模组集合计算 `WorldgenFingerprint`。指纹变化即创建新的 `epoch`，旧统计只归档而不参与新概率。构建完成后以单次原子替换安装不可变 `GenerationSnapshot`，机器一次工作周期只读取一个快照，避免 `/reload` 中途混用新旧表。

### 2. 计算全维度群系先验

不能把 `possibleBiomes()` 中每个群系视为等概率。对每个模式使用世界种子派生的固定低差异坐标序列，在目标维度的 `BiomeSource#getNoiseBiome` 上采样；该操作只查询气候/群系源，不创建、不加载区块。

- 水平坐标覆盖足够大的确定性窗口，并按维度逻辑高度分层采样。
- 结果为 `BiomePrior(biome, sampleCount, probability)`。
- 同一 `fingerprint + mode` 必须得到相同结果，默认建议至少 `65,536` 个 quart 样本。
- 允许外部 JSON 调整样本量和坐标窗口，但禁止注入运行时命令或 Java 类名。

### 3. 静态编译矿物候选

从每个目标群系最终的 `BiomeGenerationSettings` 遍历可达的 `PlacedFeature -> ConfiguredFeature` 图：

- 识别原版 `ORE`、`SCATTERED_ORE` 及可通过 Codec 安全读取的修饰器；
- 保留目标 `BlockState/Block`，不在此阶段错误地转换成方块物品；
- `COUNT`、`RARITY_FILTER`、高度范围等已知修饰器只形成启动估计；
- 自定义或无法解析的 Feature/PlacementModifier 标记为 `OPAQUE`，不使用虚假的 `1.0`；
- 候选还包括 `c:ores`、`forge:ores`（若存在）、配置 JSON 的显式方块/标签，解决不用原版 OreFeature 或缺少公共 Codec 的模组矿物。

静态结果仅是机器在校准前的 `BOOTSTRAP_ESTIMATE`，不能覆盖后续实际区块测量。

### 4. 被动观测最终生成区块

仅观察服务器自然生成、第一次进入可用状态的新区块；不扫描已被玩家修改的旧区块，不请求生成新区块，不增加强加载票。

在区块完成世界生成且尚未发生玩家交互的安全事件点，将只读快照排入有界队列；统计工作在快照上进行，不跨线程访问 `LevelChunk`：

- 按 quart/section 记录实际群系体积；
- 统计候选矿物 `BlockState` 数量，包含数据包、Biome Modifier 和自定义 Feature 最终留下的结果；
- 读取未开启容器 NBT 中的 `LootTable`/`LootTableSeed`，只记录表 ID、位置群系、维度和容器数，绝不解包或改写真实箱子；
- 每个区块只按 `dimension + chunkPos + epoch` 接受一次，去重集合持久化为压缩位图/分区摘要；
- 队列满时丢弃观测并增加 `droppedSamples`，绝不阻塞区块线程。

玩家探索会造成群系偏差，因此矿物最终权重不是直接使用观测总数，而是：

`weight(item) = Σ_b BiomePrior(b) × E[drop(item) | generated block volume in b]`

没有足够实测的群系使用静态启动估计，并在快照中标记；达到阈值后切换为 `CALIBRATED`。如果候选是由未知代码生成但最终方块具有矿物标签，它仍会被实测捕获。

### 5. 从矿石方块得到真实产物

矿物目录存储的是带权 `BlockState`，一次开采命中后才通过该方块当前战利品表生成 `ItemStack`：

- 使用固定的“普通开采”上下文和满足方块最低等级但无精准采集/时运的虚拟工具；
- 走服务端当前 LootTable/NeoForge 全局战利品修改链，保留数量和 Data Components；
- 空产物是合法结果；一个矿石掉出多个物品时全部进入本次输出事务；
- 外部 JSON 可把个别方块设为 `drop_self`、指定工具或显式输出，但默认不把矿石方块物品当作开采结果。

### 6. 聚宝盆的真实战利品率

战利品表只描述“箱内生成什么”，不描述“箱子在世界中多久出现一次”，因此当前 `loot_table + 固定 weight` 模型必须删除。

每个模式维护 `LootOccurrenceStats`：新生成区块中每个未开启 LootTable 容器的出现次数、采样区块数、群系/结构上下文摘要。聚宝盆每 2400 tick：

1. 按实测的“每区块出现率”选择 LootTable；
2. 使用该表当前 `epoch` 的服务端实例和 `CHEST` 上下文滚取一次；
3. 使用抽样到的真实来源位置上下文摘要（维度、群系、Y、结构标签），但不加载原区块；
4. 让当前 LootTable、条件、函数和 NeoForge GLM 决定最终 ItemStack；
5. 以一个事务写入世界碎片开采器的 27 格缓存，剩余空间不足则整次延后，禁止部分生成后丢失。

未达到最小新生区块/容器样本时，聚宝盆状态为 `CALIBRATING` 且不生成；整合包可用 JSON 提供明确标注的 `bootstrap_occurrence`，但 UI/日志必须显示这是覆盖值而非世界实测值。末地原版没有合格矿物时，矿物池为空是正确状态。

## 核心模型与边界

建议用下列服务端内部接口替换现有全局静态 Catalog；它们不是 Numen 或调试接口，也不暴露到客户端写操作：

```java
record WorldgenFingerprint(String sha256) {}

record GenerationSnapshot(
    long epoch,
    WorldgenFingerprint fingerprint,
    Map<ResourceLocation, ModeGenerationProfile> modes,
    Instant builtAt
) {}

record ModeGenerationProfile(
    ResourceKey<Level> dimension,
    WeightedBlockStatePool ores,
    WeightedLootTablePool lootTables,
    Coverage coverage
) {}

record Coverage(
    Status status,             // CALIBRATING, BOOTSTRAP_ESTIMATE, CALIBRATED, STALE
    long biomeSamples,
    long generatedChunks,
    long lootContainers,
    long opaqueFeatures,
    long droppedSamples
) {}

interface CurrentWorldGenerationIndex {
    GenerationSnapshot snapshot();
    void accept(NewChunkObservation observation);
}
```

持久化使用每个存档自己的 `SavedData`，按 `fingerprint/维度/群系/BlockState/LootTable` 分桶。不要把扫描统计写入通用配置目录，也不要跨存档复用。

## 外部 JSON

目录保持服务端数据包形式，建议迁移为：

- `data/<namespace>/cultivation/world_shard_mode/*.json`
- `data/<namespace>/cultivation/world_shard_generation_rules/*.json`

规则可配置：模式与目标维度、激活方块、候选矿物方块/标签、排除项、开采工具语义、群系采样数、校准阈值、显式启动估计和聚宝盆启动出现率。所有字段严格校验，未知字段、未知注册表 ID、负权重和过大采样预算整条拒绝；覆盖按资源包优先级而不是文件名排序决定，`replace` 必须显式。

旧的 `ore_weights` 与 `world_shard_loot.weight` 读取一版并记录弃用警告，只作为 `BOOTSTRAP_ESTIMATE`，不得伪装成自动读取结果。

## 世界优先与固定表配置

采集器和聚宝盆使用同一来源枚举，默认值必须是 world_first：

- world_first：当前世界索引是基线；配置可以对指定条目执行 set/remove/add/multiply，未提及条目保持世界读取结果。
- fixed_table：显式关闭该模式的世界读取权重，完全使用数据包固定表。用于空岛包、剧情地图和需要绝对可复现产出的整合包。
- replace_ore_weights=true 是 1.21.1 适配层已经实现的固定矿物表快捷写法；默认 false，ore_weights: 0 删除指定结果。
- 固定表仍必须验证注册表 ID、维度所有权、缓存事务和服务端随机数，不能绕过安全校验。
- 日志、管理界面与审计输出必须显示 WORLD_FIRST、WORLD_WITH_OVERRIDES 或 FIXED_TABLE，避免把固定表误报为实测世界概率。

建议配置示例：

    {
      "id": "cultivation:overworld",
      "activation": { "block": "minecraft:diamond_block" },
      "target_dimension": "minecraft:overworld",
      "beam_color": "#55FF55",
      "generation_source": "world_first",
      "replace_ore_weights": false,
      "ore_weights": {
        "minecraft:diamond": 250000,
        "example:disabled_ore": 0
      }
    }

切换固定表时改为 generation_source=fixed_table 或 replace_ore_weights=true，并列出完整目标表。

## Lootr 风格的聚宝盆生成边界

参考 Lootr 的 MIT 源码数据流，而不是复制其容器或建立硬依赖：

1. 通过版本适配器从原版、Lootr 或其他兼容容器读取尚未解包的 LootTable key 与 seed；适配器只读。
2. 用 server.reloadableRegistries().getLootTable(key) 获取当前数据包最终表；空或无法解析的表记录错误并跳过。
3. 构造 CHEST LootParams：真实来源 ORIGIN；有在线所有者时加入 THIS_ENTITY 与 luck；无人在线时使用明确的 ownerless 上下文，不伪造玩家。
4. 以 basin UUID + cycle + source seed + table key 派生确定性 seed，填充临时库存，再以单事务写入 27 格缓存。
5. 不触发或保存 Lootr 的“每玩家永久背包”；聚宝盆每周期是一轮新的服务器战利品生成。若 Lootr 已安装，只调用其公开 API/容器适配边界，禁止反射内部类。

参考实现：Lootr 1.21.1 DefaultLootFiller 使用当前 reloadable loot registry、CHEST 上下文、ORIGIN、THIS_ENTITY、luck 与表 seed；RandomizableContainerBlockEntityAdapter 只读原容器 LootTable/seed。

## NeoForge 1.21.1—26.2 多版本结构

该范围跨越多代 Minecraft 二进制 API，不能由一个 JAR 安全覆盖。采用“共享语义、多适配器、多产物”：

- generation-common：纯 Java 配置模型、权重合并、fingerprint、统计、选择算法、事务计划和 Golden 测试；不得 import Minecraft/NeoForge 类。
- platform-neoforge-<mc-version>：每个正式 Minecraft 版本线独立编译，适配 RegistryAccess、BiomeSource、Chunk/Structure 事件、LootParams、SavedData 与能力 API。
- cultivation-neoforge-<mc-version>：组合共享核心、对应平台适配器和该版本 UI/资源，产出带 Minecraft 版本后缀的独立 JAR。
- 版本清单由仓库内受审计的 supported_versions.json 驱动；从 1.21.1 到 26.2 的每个正式版本必须有 NeoForge 锁定版本、JDK、Lootr 参考分支、构建与冒烟结果。
- CI 对所有版本运行共享契约测试；至少 1.21.1、每个 API 断点版本和最新 26.2 运行专用服务器世界创建、数据包 reload、新区块采样、固定表切换与 Lootr 同装冒烟。
- 不用反射吞掉跨版本差异；API 改名只进入版本适配层。配置 schema 保持一致，增加字段必须向后兼容。

## 性能与一致性

- 群系先验只在新 `epoch` 计算一次，允许分 tick 完成。
- 区块快照队列、每 tick处理预算和 SavedData 刷盘间隔均有硬上限。
- 采样不得调用 `getChunk`、添加 ticket、写方块或打开真实容器。
- 统计采用整数计数和稳定排序；概率转换使用 64 位定点数，最终选择使用别名表或累计权重。
- 开采器缓存、仙窍路由和聚宝盆共享一个“先模拟容量、后一次提交”的输出事务。
- `/reload` 后旧机器不需要重放；下一工作周期读到新快照。若状态为 `STALE/CALIBRATING`，不产生来源不明的物品。

## 验收测试

1. 原版主世界/下界：实测目录包含最终矿石掉落，末地无矿物时为空。
2. 数据包增加、删除或替换 PlacedFeature；NeoForge Biome Modifier 增删矿物后，`fingerprint/epoch` 改变且目录重建。
3. 自定义无法解析 PlacementModifier：日志与 Coverage 显示 `OPAQUE`，最终新区块观测仍能校准其矿石。
4. 群系非均匀维度：结果按 BiomeSource 先验重加权，不按 `possibleBiomes` 等分，也不直接按玩家探索数量加权。
5. 矿石自定义方块战利品、时运/精准采集条件和 GLM：普通开采上下文输出与实际服务端一致。
6. 数据包替换箱表、模组新增 LootTable/GLM：聚宝盆下一 `epoch` 使用新结果，不保留内置固定权重。
7. 真实容器从未被打开或改写；采样不生成、不加载新区块。
8. 服务器重启后同 fingerprint 延续计数；修改数据包后旧计数隔离。
9. 队列溢出、缓存满、空矿池、校准不足和 `/reload` 竞态均不复制、不丢失既有物品、不阻塞服务器 tick。
10. Numen 仅从外部执行创建世界、探索新区块、放置/激活机器和检查输出；ImmortalStorage 内不得加入 Numen、MCP 或桥接端点。

## 官方依据

- NeoForge 1.21.1 动态注册表由数据包在世界加载时建立，应从世界的 `RegistryAccess` 获取：https://docs.neoforged.net/docs/concepts/registries/
- NeoForge Biome Modifier 会在世界加载时把数据包修改应用到目标群系，且同路径数据包可以覆盖模组定义：https://docs.neoforged.net/docs/1.21.1/worldgen/biomemodifier/
- LootTable 的池、条件、函数和滚取结果是数据驱动内容；表本身不包含结构/容器的世界出现频率：https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/

- Lootr 1.21.1 源码采用 common/NeoForge 多项目并以 MIT 发布；其默认填充器读取当前 LootTable、seed、CHEST 上下文、ORIGIN、THIS_ENTITY 与 luck：https://github.com/LootrMinecraft/Lootr
