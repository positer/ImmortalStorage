# 机器黑白名单与附属 API

本合同适用于世界碎片采集器、模拟灵田和模拟轮回炼化炉。默认不附带任何限制规则；已有模式、配方、燃料、强化、仙窍及六面输出路径继续工作。规则只限制未来生产，不追溯删除已有库存或待输出缓存。

## 机器、等级与模式

| machine | level | mode | 可用 target |
| --- | --- | --- | --- |
| `world_shard_miner` | 金字塔结构等级 1–4 | `WorldShardApi.minerModes()` 中的模式 ID，例如 `immortalstorage:overworld`、`immortalstorage:nether`、`immortalstorage:end` | `output_item` |
| `simulated_spirit_field` | 当前固定 1 | 实际作物方块 ID，例如 `minecraft:wheat`、`minecraft:chorus_flower` | `input_item` 种子、`input_block` 作物、`substrate_block` 实际工作基底、`output_item` 掉落 |
| `simulated_reincarnation_furnace` | 当前固定 1 | 实际实体类型 ID，例如 `minecraft:pig` | `input_item` 样本物品、`input_entity` 实体、`output_item` 掉落 |

模拟机器没有独立的多级方块或模式选择器。堆叠样本和强化插件改变倍率，不改变上述等级。灵田的普通泥土会转成湿润耕地工作，因此应以 `minecraft:farmland` 筛选该基底。

## 数据包

文件路径：`data/<namespace>/machine_policies/<name>.json`。规则 ID 来自文件的命名空间及相对路径，接受任意命名空间。与其他数据包一样，同 ID 文件由包优先级覆盖。

```json
{
  "machine": "world_shard_miner",
  "min_level": 2,
  "max_level": 3,
  "mode": "immortalstorage:nether",
  "target": "output_item",
  "whitelist": ["#c:ores", "minecraft:ancient_debris"],
  "blacklist": ["minecraft:nether_quartz_ore"]
}
```

- `machine` 和 `target` 必填，非法组合会被拒绝并记录规则 ID；等级必须是正整数，范围不能倒置。
- `mode` 省略表示所有模式；`min_level` 默认 1，`max_level` 默认无限制。
- 每条名单中的 ID 按对应物品、方块或实体注册表解释；`#` 前缀是该注册表的标签。未安装内容的合法 ID 与空标签匹配不到任何对象，不会扩展名单权限。
- 名单省略或为空时不施加该项限制。黑名单始终优先；多个适用规则同时成立，白名单取交集。具体模式规则不会放宽全局规则。
- `{"enabled": false}` 可以撤销同 ID 的 Java 默认规则或低优先级包规则。无需再写其他字段。
- `/reload` 重建完整快照；移除包后恢复 Java 默认值，原包限制不会残留。无规则时保持原抽样顺序和产量。
- 采集器先筛选带权矿池再抽样，因此剩余矿物保持相对权重和该等级的抽样数量；空池停产。两台模拟机器在扣燃料前检查输入，输出名单则裁掉新生成的对应掉落。输出全被禁止仍是一次有燃料成本的生产；炼化炉经验不属于物品名单。
- 无效 JSON 记录错误后跳过该文件；若存在同 ID 的 Java 默认规则，则保留它。部署后应检查日志，避免把格式错误当成限制已生效。

灵田示例：仅允许小麦在耕地上工作，分别写 `mode: "minecraft:wheat", target: "input_block", whitelist: ["minecraft:wheat"]` 与另一条 `target: "substrate_block", whitelist: ["minecraft:farmland"]`。炼化炉禁用猪：`machine: "simulated_reincarnation_furnace", target: "input_entity", blacklist: ["minecraft:pig"]`。

## Java API

公开入口在 `com.immortalstorage.immortalstorage.api.machine`。在附属的 `FMLCommonSetupEvent` 回调内直接注册；不要把注册再排进 `enqueueWork`，IS 会在 setup 的排队工作中冻结注册。重复 ID 与冻结后注册明确抛异常。

```java
MachinePolicyApi.register(ResourceLocation.parse("addon:no_diamond"),
    new MachinePolicyApi.Rule(
        MachinePolicyApi.Machine.WORLD_SHARD_MINER, 1, 2, null,
        MachinePolicyApi.Target.OUTPUT_ITEM,
        List.of(), List.of("minecraft:diamond_ore")));

SimulatedMachineApi.registerCrop(Items.WHEAT_SEEDS,
    Blocks.WHEAT.defaultBlockState(), List.of("minecraft:farmland"));
```

`MachinePolicyApi.rules()` 返回不可变快照；`generation()` 可作为服务端缓存失效标记。`allowsItem`、`allowsBlock`、`allowsEntity` 与运行中机器使用同一规则判定。`allowsBlock` 的带 `Target` 重载可以查询作物或工作基底。`allows` 的标签谓词必须查询当前服务器的相应注册表标签。`install` 是模组内部重载接线，请勿由附属调用它绕过数据包。

`SimulatedMachineApi.crop(seed)` 只查询显式作物映射，空值表示沿用原有自动推断；`cropGeneration()` 随重载变化，灵田据此丢弃旧映射缓存。映射数据包仍使用 `data/<namespace>/simulated_spirit_field_crops/*.json`，字段为 `seed`、`crop`、`substrates`，其值覆盖 Java 默认映射。

采集器新增完整模式仍使用既有 `api.worldshard.WorldShardApi.registerMinerMode`；本次未更改其签名。炼化炉自动支持已有刷怪蛋及缚魂样本中的实体类型，沿用真实实体战利品表，无须另注册静态掉落。

26.1.2 中 Minecraft 将 `ResourceLocation` 改名为 `Identifier`，该代的公开签名随平台映射；示例编译时也使用对应类型。

可编译示例见 [MachinePolicyAddon.java](examples/MachinePolicyAddon.java)。数据包只承载数据，不能分发附属 Java 实现。所有判定为服务端权威；客户端不从名单推断产物。

## 配置窗口

六面统一按上／北／下、 西／南／东排列，采用相同边框按钮，启用绿色、关闭灰色，并保留原有预览和提示。仙窍输出／自动输出开关直接显示状态。

所有配置页统一使用原生 17 像素标题行，标题下方不再保留第二行标题占位；标题与主体同色接合，主窗口和侧栏各保留一个标题。齿轮、页签和外部筛选框位于标题上方。锻灵／附魔压缩机及反转工况共用 `SpiritCompressorLayout`：整窗高度由原来的 199 像素（含额外标题行）收紧为 175，侧栏收窄 16 像素；只调整显示几何，槽位编号、库存所属、输入输出判定及扣料／结算逻辑不变。

Nemo’s Inventory Sorting 为可选客户端兼容：26.1.2 的 1.18 会向容器注入 `FilterBox` 与保留筛选条件的图钉，仙藏会将这两个原控件放在标题上方并避让自身按钮。拥挤工具条优先将搜索框收窄至可读宽度，仍不足时另起一行；保留输入、焦点和图钉行为，不修改 Nemo 配置。1.21.1 的 1.8.2.1 仅对箱子与潜影盒添加该筛选框，本轮机器页面不会凭空增加一个。兼容层通过模组存在性与控件标识识别，不引用 Nemo Java 类型，未安装时不生效。

移动属于隐式交互，界面不显示操作说明或相关提示。左键移动窗口标题时，窗口、按钮、筛选网格和真实槽位整体移动；释放即停止。切换页签／重建控件保留当前窗口位置，关闭再开恢复默认。移动不发送配置包、不改世界存档。居中弹窗使用独立坐标。


页面支持简体中文与英文：六面短名随语言切换，筛选页与双侧正常／反转模式具有独立标题；窄按钮采用简短英文，完整行为保留在提示中。标题过长时显示省略号，悬停查看完整名称。迷你仙墟五项状态与力度均使用翻译参数。筛选条件的“组件匹配”准确对应物品组件匹配，不再误称为 NBT。

仙藏自有语言资源共 777 个键，两语言键集及格式参数一致。可选 Nemo 的筛选提示与图钉提示由标准资源叠加补充，不修改第三方 JAR；没有安装时这些词条不会触发任何行为。
