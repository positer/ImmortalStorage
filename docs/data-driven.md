# ImmortalStorage data-driven formats

English first; 简体中文 below. Both maintained targets use singular `data/<namespace>/recipe/` folders. Minecraft 26.1.2 uses string ingredients; 1.21.1 uses ingredient objects.

## Source definitions

Load order is built-in defaults, `data/<namespace>/source_veins/<id>.json`, then `config/immortalstorage/source_veins/`. The configuration layer has highest priority. `/reload` installs an immutable definition snapshot. Invalid entries and conflicting outputs are logged and ignored individually.

```json
{
  "id": "immortalstorage:diamond",
  "type": "item",
  "item": "minecraft:diamond",
  "yuan_cost_per_batch": 2,
  "outputs_per_batch": 4,
  "min_stage": 8,
  "default_rate": 64,
  "max_rate": 9223372036854775807
}
```

This changes the diamond source to 2 Yuan per 4 diamonds. The compressor automatically derives the inverse, reduced to 2 diamonds per 1 Yuan. Free sources do not create Yuan by default; fluid definitions are not item-compression inputs. Definition ids remain stable when overriding a built-in output.

## Spirit compression recipes

Explicit recipes take priority over source inversion for matching inputs. `input_count` is an exact whole batch across the twelve input slots, from 1 to 768. `recipe` uses the native cooking recipe schema; processing occurs every tick rather than waiting for `cookingtime`. The output retains its components. Recipes producing Immortal Yuan accept only items present in source definitions, including when an ingredient tag also contains other items. Non-Yuan recipes can accept other ingredients.

Example: `data/example/recipe/diamond_compression.json` for 1.21.1:

```json
{
  "type": "immortalstorage:spirit_compression",
  "input_count": 8,
  "recipe": {
    "ingredient": { "item": "minecraft:diamond" },
    "result": { "id": "immortalstorage:immortal_yuan", "count": 3 },
    "cookingtime": 1
  }
}
```

For 26.1.2 replace `ingredient` with `"minecraft:diamond"`; tags use `"#namespace:tag"`. Keep the result and outer fields. The project's resource generator performs this conversion for built-in resources.

Non-Yuan recipe example:

```json
{
  "type": "immortalstorage:spirit_compression",
  "input_count": 9,
  "recipe": {
    "ingredient": { "item": "minecraft:iron_ingot" },
    "result": { "id": "minecraft:iron_block", "count": 1 },
    "cookingtime": 1
  }
}
```

Each tick prioritizes the largest craftable input batch and runs its maximum multiplier, then processes further results in deterministic order. The local output accumulates compatible bulk results; an incompatible result stops processing without consuming its inputs. Direct Xianqiao output processes all available recipes using native long counts, independently of the local output slot. Missing/offline owners or unavailable storage preserve inputs. Recipes and derived ratios are synchronized to JEI and, where installed, EMI on login and reload.

## Other existing data surfaces

- Native recipe types: `immortalstorage:immortal_furnace`, `immortalstorage:yuan_substitution_shaped`, `immortalstorage:yuan_substitution_shapeless`, standard crafting/cooking/smithing recipes and new `immortalstorage:spirit_compression`.
- `world_shard_miner` and `world_shard_loot`: miner modes and loot catalogs, validated by their existing reload listeners. World-generation pool changes may require a restart.
- Simulated spirit-field crop definitions: the existing crop catalog reload listener controls crop input/output.
- Block/item tags describe machine groups and source resources; registry ids remain code-defined. Datapacks do not create new registered block classes.
- The compressor structure is an exact code-defined fixture: 8 world barriers, 12 chains, 4 air cells, the ruin controller, a miner below and basin above. No cap-machine owner or enable-state condition is tested.

## 简体中文

源定义按“内置 → 数据包 → 配置目录”覆盖。配置目录为 `config/immortalstorage/source_veins/`，优先级最高；源数据包目录为 `data/<命名空间>/source_veins/`。重载创建新的不可变快照，单条非法定义不会让其余条目失效。修改内置源时保留原定义 id，输出物品不能与其他源定义重复。

锻灵压缩机默认反向使用源定义的 `outputs_per_batch : yuan_cost_per_batch`，并约分为最小整数比率。免费源不默认产生仙元，流体源不进入物品压缩。上面的钻石源示例把源兑换改为“2仙元→4钻石”，压缩机自动得到“2钻石→1仙元”。

通过 `data/<命名空间>/recipe/<名称>.json` 注入 `immortalstorage:spirit_compression` 配方。`input_count` 是12输入槽合计每批消耗数，范围1–768；`recipe` 采用该代原生烹饪配方格式。机器按每tick批处理，`cookingtime` 不增加等待时间。1.21.1 输入写 `{ "item": "minecraft:diamond" }`，26.1.2 输入写 `"minecraft:diamond"`；标签分别使用对象 `tag` 字段和 `"#命名空间:标签"` 字符串。

显式配方会阻止相同输入回退到默认源兑换。仙元结果的输入会与源资源白名单求交集，标签中的非源物品不会因此被放行；非仙元配方允许其他原料。上面的铁锭示例是9铁锭压成1铁块。

每tick按每批输入需求从大到小选择可合成配方，计算最大完整倍率，随后依次处理其他结果。普通输出槽保存整批兼容产物；当前结果与输出不兼容或无容量时停止，保留对应材料。仙窍输出开启后，通过已有长整数接口在同tick输出全部可合成配方，绕过本地输出槽。所有者离线或仙窍不可用时不消耗输入。

机器所有者固定为迷你仙墟的放置者，界面开启不会转移认主；其他结构方块的工作状态和所有者不参与成型判断。六面主动传输每面每tick共64件；红石生产门禁复用已有三态配置。库存、输出总数、六面配置和认主随控制器持久化。

## Reversed core: Spirit Decompressor / 反转核心：锻灵解压器

The same formed structure becomes a Spirit Decompressor when its miniature ruin is reversed. Compression and decompression have separate recipe types and inventories; six-face routing, redstone mode, owner and direct Xianqiao output remain shared. The reverse panel has one input at the right and twelve outputs at the left. Each tick processes the entire craftable content of the single input slot. All results must fit together before materials are consumed. Direct Xianqiao output bypasses the local output slots and commits all results together.

核心反转后，同一结构变为锻灵解压器：左侧单输入→右侧4×3输出，箭头仍朝右，相对压缩机交换两组槽位。正常压缩与反转解压的库存和配方独立；六面、红石、所有者、仙窍输出开关共用。模式切换时弹出两套内部库存的输入和输出，已打开的旧模式菜单自动关闭；库存按各物品的正常堆叠上限分批掉落，其他配置保留。每tick处理输入格中全部完整批次；任一结果阻挡就不扣原料、不写入其他结果。仙窍模式不受本地槽位阻挡。

默认配方为16种主世界原版原矿及4种仙藏原矿：每个浅层原矿返还1圆石、每个深层原矿返还1深板岩，矿物固定为无时运掉落下限的四倍（铜8、红石16、青金石16，其余4）。整格64铜矿会同tick得到64圆石和512粗铜。下界矿等其他输入可通过数据包另行加入。

Use `data/<namespace>/recipe/<name>.json` with the independent `immortalstorage:spirit_decompression` type. `input_count` is 1–64. `recipe.result` is the first result; `additional_results` adds up to eleven more result stacks. Identical results combine during capacity planning. Overriding a built-in recipe uses its id, e.g. `immortalstorage:spirit_decompression/copper_ore`. JEI/EMI show compression and decompression in separate categories and show every result. Native recipes and the resolved catalogs synchronize on login/reload.

1.21.1 example / 示例：

```json
{
  "type": "immortalstorage:spirit_decompression",
  "input_count": 1,
  "recipe": {
    "ingredient": {"item": "minecraft:copper_ore"},
    "result": {"id": "minecraft:cobblestone", "count": 1},
    "experience": 0.0,
    "cookingtime": 1
  },
  "additional_results": [{"id": "minecraft:raw_copper", "count": 8}]
}
```

For 26.1.2, use `"ingredient": "minecraft:copper_ore"`; tags use `"#namespace:tag"`. The remaining fields are the same. Recipe types are independent: a compression override cannot change the decompressor catalog. 修改倍率、原料或多结果时使用本例格式；26.1.2 原料改为字符串，其余结构一致。压缩配方不会混入解压目录。

## Multi-material compression / 多材料压缩

`additional_inputs` adds up to eleven counted ingredients to the existing primary `recipe.ingredient` / `input_count`. Each ingredient count is 1–768. Matching allocates actual input-slot stock across all ingredients, including overlapping tags, then selects the maximum whole multiplier. Materials are consumed together only after every output is accepted. All ingredients appear in JEI/EMI with their counts and are accepted by the machine's input ports.

```json
{
  "type": "immortalstorage:spirit_compression",
  "input_count": 1,
  "recipe": {
    "ingredient": {"item":"minecraft:amethyst_block"},
    "result": {"id":"minecraft:budding_amethyst", "count":1},
    "cookingtime": 1
  },
  "additional_inputs": [
    {"ingredient":{"item":"immortalstorage:immortal_yuan"}, "count":8}
  ]
}
```

26.1.2 uses item/tag strings in every `ingredient`, including additional inputs. 多材料配方也按最大完整倍率整批执行：紫水晶块或失活蕴灵晶母岩每块需8仙元，缺任一种材料不消耗；结果阻挡不扣任何原料。蕴灵晶母岩激活已从工作台移到压缩机，原工作台配方ID不再提供。

The built-in decompressor also accepts quartz blocks, chiseled quartz, pillars, quartz bricks, smooth quartz and both quartz stairs (4 quartz each), both quartz slabs (2 each), and amethyst blocks (4 shards). Stair recovery uses the reversible stonecutter quantity; it does not multiply ore drops. 解压新增石英系列整块/楼梯4石英、半砖2石英、紫水晶块4碎片，独立于原矿的四倍基础掉落规则。

## 工具定位（Tool locator）

默认渲染半径32格，模组配置`tool_locator.radius`范围1–48格。服务端预存每位有定位权限玩家周围半径3区块（7×7）已加载资源，不强制加载。正常配置使用内置`data/immortalstorage/tool_locator/settings.json`的`{"schema":1}`；数据包以相同路径覆盖为`{"schema":1,"radius":48}`时，显式半径优先于配置页。半径须为1–48整数，错误值记录日志并回退配置。客户端使用服务端同步半径。

四个方块标签：`immortalstorage:tool_locator/storage`（容器/战利品），`spawners`（刷怪笼），`archaeology`（考古可疑块），`ores`（矿物），均位于`tags/block/tool_locator/`。可用标准`replace`与`values`添加/替换；原生带随机战利品的容器实体与本地Lootr容器也归入存储类。不需要客户端安装额外规则包。

权限复用探索拉取条件：开启探索并手持灵器，或背包中开启探索的仙器；矿物提示要求手持挖掘模式仙器。死亡、旁观、无工具或模式未启用不创建扫描缓存。移动进入新区块、区块加载/卸载及方块/实体更新只刷新对应授权玩家窗口；方块事件增量更新变动位置，重叠干净区块保留。关闭功能撤销订阅、使待执行任务失效并清空客户端提示。

### 矿物独立半径与载入词典

矿物白框默认16格，配置`tool_locator.mining_radius`范围1–48；探索`tool_locator.radius`默认32。设置包可用`{"schema":1,"radius":32,"mining_radius":16}`独立覆盖，未指定项回退对应配置。

世界载入及全局数据重载时，服务端为`tool_locator/ores`识别的原矿建立真实掉落图：直接物品、物品标签、嵌套/引用表、条件分支和可识别替换函数。副手掉落物反查所有可能产出它的原矿；副手原矿按共享表ID或除丝触原矿项后的相同矿物产出集合匹配变体。名称、矿物族标签和模组ID不参与族推测。未加入标准c/forge矿石标签的原矿可通过`tool_locator/ores`补充；更改实际掉落表会在重载时自然更新对应映射。

游戏运行时不解析/查询掉落表，不模拟破坏方块。副手变化只读取载入时建立的不可变映射并发送筛选策略；客户端仅使用缓存坐标和原矿注册ID。矿词典是共享的注册表元数据，与只为有工具权限玩家建立的3区块资源预存区分；其构建不读取世界区块。
