# 世界碎片采集器 / 聚宝盆：ATM 矿物、群系矿物与 Lootr 结构宝箱兼容调研

> 调研方式：Firecrawl 拉取 AllTheMods/AllTheModium（分支 `1.21.x`）与
> LootrMinecraft/Lootr（分支 `mdg-1.21.1`）的真实源码与数据包 JSON，
> 对照本模组 `WorldShardOreScanner` / `WorldShardStructureLootScanner` 的读取语义。

## 1. ATM（AllTheModium）矿物生成：不是结构包，而是「纯矿石生成 + NeoForge 群系修饰器」

### 源码事实（AllTheMods/AllTheModium，1.21.x）

- `data/allthemodium/worldgen/configured_feature/allthemodium_ore.json`：
  `type=minecraft:ore`，`size=4`，`targets` 用 `predicate_type=minecraft:tag_match`
  分别指向 `minecraft:stone_ore_replaceables → allthemodium:allthemodium_ore` 与
  `minecraft:deepslate_ore_replaceables → allthemodium:allthemodium_slate_ore`。
- `placed_feature/allthemodium_ore.json`：`count=10` + `in_square` +
  `height_range(above_bottom 0 … absolute 10)` + `environment_scan` +
  `random_offset` + 末尾 `minecraft:biome`（BiomeFilter，空配置）。
- `placed_feature/vibranium_ore.json`：`count=10`、`height 64..127`（下界）。
- `placed_feature/unobtainium_ore.json`：`count=3`、`height 0..128`（末地高地）。
- 所有矿物最终通过 `data/allthemodium/neoforge/biome_modifier/allthemodium/`
  （NeoForge Biome Modifier）注入目标群系的 `GenerationSettings#features()`。

### 结论

1.21.1 的 ATM **已不再使用结构包**（历史上「Registered structure pack:
allthemodium:allthemodium」在 1.20 被移除，日志里「Pure ore generation ended」即为明证）。
「远古城市里挖 allthemodium」是因为远古城市恰好位于 Deep Dark 群系，deepslate 层的
allthemodium 矿石在该群系自然生成，而不是矿石被塞进远古城市结构模板。

因此 `WorldShardOreScanner` **天然兼容** ATM 的三类矿物：它遍历
`biome.value().getGenerationSettings().features()`（最终结果，已包含 Biome Modifier
注入），`minecraft:tag_match` 的 RuleTest 只影响替换目标、不影响输出状态读取，
`minecraft:biome` 修饰器被按系数 1.0 处理。

### 已知边界（诚实标注，非「已精确读取」）

- 扫描器当前输出「矿石方块物品」（`state.getBlock().asItem()`），不是实际开采掉落；
  这是 `BOOTSTRAP_ESTIMATE`，与设计文档第 5 节「从矿石方块得到真实产物」是后续项。
- 若某整合包/模组真的把矿石塞进结构模板（非 PlacedFeature），当前扫描器看不到；
  逃生舱是模式 JSON 的 `ore_weights` 显式覆盖。设计文档第 4 节的被动区块观测
  才是这类结构矿物的完整解法。

## 2. 群系依赖矿物（其他模组）

多数模组矿物走标准 `PlacedFeature`（`minecraft:ore` / `scattered_ore`）+
`BIOME_FILTER` 或 NeoForge Biome Modifier。扫描器读取最终
`getGenerationSettings().features()`，对这两条路径均生效；未知
PlacementModifier 记为系数 1.0 并在日志给出 `ore_weights` 精确覆盖建议
（见 `WorldShardOreScanner.modifierFactor`）。

## 3. Lootr 结构附属宝箱：战利品表 ID 被完整保留

### 源码事实（LootrMinecraft/Lootr，mdg-1.21.1）

- `DefaultLootFiller.unpackLootTable`：
  1. `ResourceKey<LootTable> lootTable = provider.getInfoLootTable()`（原容器表 key）；
  2. `long seed = LootrAPI.getLootSeed(provider.getInfoLootSeed())`（原容器 seed）；
  3. `LootTable loottable = level.getServer().reloadableRegistries().getLootTable(lootTable)`；
  4. `LootParams`：`CHEST` 上下文 + `ORIGIN` + `THIS_ENTITY` + `luck`；
  5. `lootTable.fill(container, parameters, seed)`。
- `RandomizableContainerBlockEntityAdapter`：`getLootTable(entity) = entity.getLootTable()`、
  `getLootSeed(entity) = entity.getLootTableSeed()`。即 Lootr 只读原版
  `RandomizableContainerBlockEntity` 的表 key/seed，不改变表 ID。
- 结构箱子在结构生成阶段被替换（`mixin/structure_saving/MixinStructureTemplate`），
  但 `LootTable`/seed NBT 保留，因此 `minecraft:chests/*` 等表 ID 不变。

### 结论

聚宝盆 `rollOnce` 已按同一数据流实现：`chests/` 前缀动态发现（天然覆盖 Lootr
包裹的结构箱）→ `reloadableRegistries` 解析表 → `CHEST` 上下文 + `ORIGIN` +
`THIS_ENTITY` + `luck` → 派生确定性 seed → `CommonHooks.modifyLoot` 跑 GLM 链。
因此 Lootr 结构附属宝箱的战利品生成已兼容；唯一有意差异是聚宝盆每周期派生新 seed
（一轮新战利品），不触发/保存 Lootr 的「每玩家永久背包」。

## 4. 官方依据链接

- AllTheModium 源码：https://github.com/AllTheMods/AllTheModium （分支 `1.21.x`）
- Lootr 源码：https://github.com/LootrMinecraft/Lootr （分支 `mdg-1.21.1`）
- 本模组设计文档：`docs/world_shard_actual_generation_design.md`
