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

窗口标题区位于原有齿轮与模式标签上方，界面不显示拖动说明、旁注或相关工具提示。原生标题区域也支持拖动，以便在较大 GUI 缩放下找回窗口位置。左键拖动条会整体移动配置窗口、按钮、筛选网格和真实容器槽位；释放后停止。位移只保留于当前打开的窗口，切换页签／重建控件保留位置，关闭再开恢复默认。移动不发送配置包，不改世界存档。低分辨率下可向左移动完整组合窗口以露出右侧设置。配置侧栏与机器主体作为同一窗口移动。
