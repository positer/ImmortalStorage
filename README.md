# 仙藏 ImmortalStorage

![仙藏 ImmortalStorage Logo](immortalstorage-logo.png)

> **领域展开边框固定、聚宝盆战利品检索与仙窍常加载修复（2026-08-14，本轮）：** 领域展开边框固定到展开位置（不再跟随玩家）；聚宝盆 `rollOnce` 补 `withQueriedLootTableId` 使飞升丹/丹药 GLM 注入生效（此前 `loot_table_id` 条件因 queriedLootTableId 为空永不匹配）；仙窍改为跨维度常加载（复用「中心区块 + 已修改区块」名单，1x 也强加载），其他维度调整流速持续作用于仙窍。1.21.1 **222/807/0**，JAR SHA-256 `9E0280899912B7B6FFA7C5EAF8BE4CFF78108B923EB37073B4F01EC1D6C5ABE1`；26.1.2 **221/797/0**，JAR SHA-256 `F5269439A1CF2A6275767F810F7B1E88EEDB8BD6A8BE445DC78794D4CEE1074B`。PCL2 三处已替换。详情见 `archive/2026-08-14-worldshard-basin-realm-fixes.md`。

> **世界碎片采集器/聚宝盆读取优化与附属 API（2026-08-14，本轮）：** 聚宝盆每周期战利品 roll 从「每次查 reloadable registry」改为「重载时预解析 WorldShardLootCatalog 缓存」；公开 `api.worldshard.WorldShardApi` + `WorldShardAddonRegistry` 供附属在 commonSetup 前注册矿物模式/战利品定义，reload listener 合并 addon 与 datapack 覆盖。飞升丹末地船 25% 契约锁定；调研确认 ATM 1.21.1 已改纯群系矿石（非结构包）故采集器天然兼容、Lootr 保留结构箱战利品表 ID 故聚宝盆天然覆盖。1.21.1 **222 suites / 807 tests / 0 failures**，JAR 5,431,173 字节、SHA-256 `BE20F1A78DDACC4F9CC9188CA754B3E4C408F9C22AE5A79ACF2BBCF10BB50C92`；26.1.2 **221 suites / 797 tests / 0 failures**，JAR 5,442,708 字节、SHA-256 `78DB596097B060C83E137E402DF32EA3A580C83440E17B3C35124D4B218CC651`。未 bump、已替换 PCL2 三处、未启动客户端。详情见 `archive/2026-08-14-worldshard-api-loot-atm-lootr-compat.md`。

> **0.1.0 五项更新 + 四项追加（2026-08-14，本轮）：** 飞升丹末地船产出改为 25%；聚宝盆改为从世界 LOOT_TABLE 注册表动态发现结构宝箱战利品表（非白名单，天然覆盖远古城市等）；空窍/仙窍终端新增内置切石机（与内置锻造台共用 tag 内按钮切换，完整复刻原版结果网格与滚动选择）；新增「领域展开」随身空间（非仙窍维度 shift+V 展开/收起，按阶段 3×3×3 / 7×7×7 / 13×13×13，中心即仙窍 (0,56,0)，无方块更新、无 NBT 变动的双向搬移，与「玩家在仙窍」互斥）；1-5 阶进度显示改默认 T 键，仙窍内 shift+V 传送至 (0,56,0)。追加世界胎壁（混元一气双向合成、仅玩家破坏、硬度干草垛）、禁止仙窍非玩家破坏（TNT/creeper/凋零等）、领域展开移走填胎壁、切石机完整复刻原版 UI。1.21.1 **219 suites / 792 tests / 0 failures**，JAR 5,418,088 字节、SHA-256 `C9EF9EB899B32249CCE4E094761B6777E1103E95ECDB3DFCD85C4D88C9E6C290`；26.1.2 **218 suites / 782 tests / 0 failures**，JAR 5,433,752 字节、SHA-256 `46E5F2576AE3EE34A5821025DE5498443F713D32B4B32BFD2A45F204FFD70901`。版本号未 bump、未装 PCL2、未启动客户端，由用户验收。详情见 `archive/2026-08-14-0.1.0-five-updates-plus-four.md`。

> **0.0.12 双版本正式发布（2026-08-13）：** 同一 GitHub Release 分别提供 Minecraft 1.21.1 / NeoForge 21.1.235 与 Minecraft 26.1.2 / NeoForge 26.1.2.94 两个独立 JAR。1.21.1 门禁通过 **219 suites / 792 tests**，制品 5,382,838 字节，SHA-256 `55B0816FC27F81A0E81E42517203E5FDE95CF97BB18676D06BB906B63CDCBE8A`；26.1.2 在关闭迁移旁路后通过 **218 suites / 782 tests**，制品 5,397,569 字节，SHA-256 `15C451F4C4C87E81462B310B33E0F0CDC279D80B555A718FAD090D57A5270D76`。两代均为零失败、零错误、零跳过；生产边界、版本组成、精确制品与无 AE2 运行时门禁通过。26.1.2 的 Mekanism/EMI 因没有官方目标制品继续作为未启用的可选联动，不混用旧代 API。[下载 0.0.12](https://github.com/positer/ImmortalStorage/releases/tag/0.0.12)。

> **0.0.12 NeoForge 26.1.2 一气归元剑发射点修复（2026-08-13）：** 26.1.2 迁移时移除了旧版 `ItemRenderer` 发射点钩子，却没有接入新版 `ItemInHandRenderer` / `ItemStackRenderState` 提交链，光束因而始终退回经验手位并从画面下方偏置发射。目标版现于物品实际提交前读取 `getModelBoundingBox().getCenter()`，再叠加实时手持、挥动、装备与视角矩阵，按屏幕中真实剑身中心生成光束；物品提交本身保持原样。新增回归先在缺失钩子时失败、修复后通过；26.1.2 完整门禁为 **218 suites / 782 tests / 0 failures / 0 errors / 0 skipped**，最终发布 JAR 5,397,569 字节，SHA-256 `15C451F4C4C87E81462B310B33E0F0CDC279D80B555A718FAD090D57A5270D76`。26.1.2 PCL2 版本实例及全局模组目录均已替换且各仅保留一个仙藏 JAR，旧包备份于 `archive/pcl2-backups/2026-08-13-one-qi-muzzle-target-fix`；1.21.1 源码与实例未改动。详情见 `archive/2026-08-13-one-qi-muzzle-target-fix.md`。

> **0.0.12 模拟轮回炼化炉本地缓存修复（2026-08-13）：** 修复关闭仙窍输出后，炼化产物无法进入本机 4×3 输出区而滞留在隐藏待输出列表的问题。根因是内部生产错误调用了受外部自动化输入规则约束的 `ItemStackHandler.insertItem`，而输出槽按设计拒绝外部放入。现新增机器内部专用合并路径：保留输出槽对管线的只读/可抽取规则，同时允许生产结果按组件一致性和槽位上限正确堆叠。1.21.1 **219 suites / 792 tests / 0 failures**，SHA-256 `D70C3DA883D4F9A48053A6A15E3AD4CB5C00B84C485476B430F6FB2FF49301E3`；26.1.2 **217 suites / 781 tests / 0 failures**，SHA-256 `847A1B1A3801AAF04AA4F927A37371D669E1FEEFD16FE8FA1AFFDCE8E5B269E3`。两代 PCL2 实例已替换，客户端运行验收由用户执行。详情见 `archive/2026-08-13-simulated-reincarnation-local-cache-fix.md`。

> **0.0.12 生产临时缓存与 26.1.2 内置仙炉修复（2026-08-13）：** 模拟灵田、模拟轮回炼化炉、聚宝盆和世界碎片采集器不再把单轮溢出产物弹到世界；本地输出槽先收纳，允许输出的临近目标随后接收，仍未容纳的余量持久保存在机器临时缓存中。后续腾出的本地空间优先回填临时内容，临时缓存清空前暂停下一轮生产，清空后自动恢复。开启仙窍输出时仍绕过本地容量直接结算到所属仙窍；临时拒收只会保留整批待输出内容，不会掉落或重复。26.1.2 同步修复内置仙炉客户端配方判定及未注册配方书分类导致的淬火满级灵剑无法取下/数据包编码失败。最终门禁：1.21.1 **219 suites / 791 tests / 0 failures**，SHA-256 `0FB00E6CE6A4A59825E20D68308586EEE0C31341855BA5EA6DF9789882C36102`；26.1.2 **217 suites / 780 tests / 0 failures**，SHA-256 `1FADE31A5C44D19D505512AA52305DEF13E9479431FAEDDAB6D197BEB3931421`。两代 PCL2 实例已替换，旧包见 `archive/pcl2-backups/2026-08-13-persistent-production-temp-cache`；客户端运行验收由用户执行。详情见 `archive/2026-08-13-persistent-production-output-cache.md`。

> **0.0.12 双语手册与 26.1.2 半格滚动修复（2026-08-13）：** 内置帕秋莉手册补齐丹药的 10 条真实配方说明，并完整记录放置/内置仙炉强化规则、AE2 `AEKeyTypes` 与 RS `ResourceType`/`StorageType` 动态附属存储、明确桥优先级及无附属回退。26.1.2 仙窍存储前景裁剪改用与 `GuiGraphicsExtractor` 当前矩阵一致的局部坐标，修复滚动半格时物品、流体、额外资源和 long 数值同时消失。1.21.1 **218 suites / 786 tests / 0 failures**，SHA-256 `5065F52DB3D2B5E333DFC76D30E0F97F938E98BFE311464D4F01D9121456141B`；26.1.2 **215 suites / 773 tests / 0 failures**，SHA-256 `FA1224FA9B62DD8E6A71E2E306028CE425D700D685FD3C506AC77376C77AE622`。两代 PCL2 实例已替换，客户端滚动与手册翻页由用户实机验收。详情见 `archive/2026-08-13-patchouli-pill-guide-and-half-scroll.md`。

> **0.0.12 内置仙炉强化与 AE2/RS 动态附属存储兼容（2026-08-13）：** 放置仙炉的插件/火焰/燃料列再次下移并保持上中下排布；仙窍内置仙炉新增同款插件槽和排布，两者的强化插件都只加速处理进度，不增加产物且不改变燃料时间。AE2 现通过官方 `AEKeyTypes` 注册表动态识别附属新增键类型，RS 通过官方 `ResourceType`/`StorageType` 注册表动态识别附属新增资源与存储类型；原有明确附属桥和无附属自带额外资源键/FE 存储回退完整保留。动态键使用可持久化、可逆的 Codec 载荷，重载后仍能恢复原生键。1.21.1 **218 suites / 786 tests / 0 failures**，SHA-256 `275A8BA061B52B6FC161FB38F8DE5DFC0F3938F121599EB47F16C18458EDBB03`；26.1.2 **214 suites / 772 tests / 0 failures**，SHA-256 `2E327B1AC7AAC040F7B858A8C946C42E553D4063C37EC670A5730852F44D19C3`。两代 PCL2 实例均已替换；客户端联机兼容表现由用户实机验收。详情见 `archive/2026-08-13-registered-addon-storage-and-embedded-furnace.md`。

> **0.0.12 插件槽、仙窍存储渲染与源方块绑定修复（2026-08-13）：** 模拟灵田/模拟轮回炼化炉的强化插件改为与工具/武器槽共用，并安全迁移此前 16 槽存档；聚宝盆设置页状态同步到服务端，4×/16×/256×插件倍率能够参与一次产物结算。26.1.2 仙窍存储改为先提交物品、后提交 long 数值/流体覆盖层，保留半格滚动缓冲行；仙炉右侧三元素整体下移 4 像素。强化插件并入主分类，源方块独立分类。免费源方块放在仙窍中会绑定维度所属玩家并进入存储索引，源方块及管理器移除改为幂等异常安全。1.21.1 **217 suites / 783 tests / 0 failures**，SHA-256 `474BE8730B13D7DC19018D04F6C9AFCAB5C5399CF3C7B15E65659EEE0B425471`；26.1.2 完整迁移构建通过，SHA-256 `1CE6DC8976505BB6A5A6864E6FF2873C33CC86F8B713A4223D44CEAED23B3FC5`。两代 PCL2 实例已替换，客户端表现由用户实机验收。详情见 `archive/2026-08-13-plugin-slot-storage-render-source-binding.md`。

> **0.0.12 旧存档模拟轮回炼化炉插件槽崩溃修复（2026-08-13）：** `错误报告-2026-8-13_17.20.55.zip` 中“打开聚宝盆闪退”实际由附近模拟轮回炼化炉的服务端 tick 触发：0.0.12 新增第 16 个强化插件槽，但旧世界 NBT 的物品处理器仍记录 `Size=15`，反序列化后访问索引 15 越界。两代现在加载旧 NBT 时先把槽位声明扩展至当前 `SLOT_COUNT=16`，保留原 15 槽物品并留下空插件槽，无需拆除机器。新增真实旧 NBT 回归测试；1.21.1 **216 suites / 781 tests / 0 failures**，SHA-256 `320DD0194E617D0A2A66BB36F92EC6E3D76D308FED5708CDD706AF5595AB4898`；26.1.2 **212 suites / 767 tests / 0 failures**，SHA-256 `3DCFEB050117F24ECF3F8514CEE7E5976F006FFB0984ED5583E048B0FB35689E`。两代 PCL2 实例已替换，旧 JAR 备份于 `archive/pcl2-backups/2026-08-13-1720-basin-crash`；客户端由用户实机验收。详情见 `archive/2026-08-13-treasure-basin-open-crash.md`。

> **0.0.12 双版本强化插件开发构建（2026-08-13）：** 新增次元窥令（4×）、次元平行法符（16×）与大千世界并行敕令（256×），支持仙炉、模拟机器、稳定化迷你仙墟系列、仙能水晶系列和聚宝盆；插件不改变燃料燃烧时间，高级插件可手持右键替换低级插件并返还旧插件。模拟灵田/轮回炼化炉按处理栈数量一次结算倍数产物；聚宝盆提升至 15 级亮度并获得六面输出、仙窍输出和插件配置；26.1.2 同步修复世界碎片开采器玻璃着色、机器火焰/箭头坐标和存储流体材质。最高级插件为紫底白核，粗像素雷电从底部正面绕至背后，再于顶部回到正面，只环绕一圈。1.21.1 **215 suites / 780 tests / 0 failures**，SHA-256 `3C427948588D0FEE90EA718499D69CC6D841812359C94A14D3C6CEE03B864EAD`；26.1.2 **211 suites / 766 tests / 0 failures**，SHA-256 `F9ED5206460F39769A6AD438F5F2A3CC62F32B876B91880C7AB0D0BD7C137A81`。两个 PCL2 版本实例已替换，0.0.11 旧包保存在 `archive/pcl2-backups/2026-08-13-0.0.12`；客户端 UI 与视觉表现由用户实机验收。

> **两代 JEI 默认布局恢复与 26.1.2 GUI 动态层修复（2026-08-13，本轮）：** 已从 1.21.1 与 26.1.2 完全移除本模组对 JEI IngredientListOverlay 的 Mixin 搬移/隐藏、整屏 `getGuiExtraAreas` 排除和专用定位策略，所有仙藏界面重新采用 JEI 自身默认配置；JEI 配方分类、R/U 可点击材料、幽灵材料与配方转移继续保留。26.1.2 机器青白火焰及箭头进度改为按目标坐标直接裁剪实际纹理文件，避免局部 sprite 提取在左上角聚集；仙窍存储、普通/高级仙窍接口的流体改从 26.1.2 `FluidStateModelSet` 读取 still sprite 与栈颜色，不再显示桶。1.21.1 **214 suites / 775 tests / 0 failures**，JAR SHA-256 `9054561D3B582A246920E3EBAC86FC1DA8CFD3E302248EBA66644B44E7B319C3`；26.1.2 **207 suites / 755 tests / 0 failures**，JAR SHA-256 `CC7D62F61BA0782C233914DA396CA656ECCD6144112383F42291274F6ACD2BB8`。两代实例及 26.1.2 全局 `mods` 已替换，旧包备份于 `archive/pcl2-backups/2026-08-13-jei-default-machine-fluid-render`；本轮未启动客户端，由用户实机验收。详情见 `archive/2026-08-13-jei-default-machine-fluid-render.md`。

> **两代聚宝盆固定 8 级光照（2026-08-13，本轮）：** 聚宝盆原有 4 级方块光照不足以抵消模型 AO 与世界方向光照造成的暗淡，现将 1.21.1 canonical 注册、26.1.2 generated 注册及兼容审计树统一改为固定 `lightLevel(state -> 8)`。模型、UV、金色贴图、10×10×10 外形与碰撞箱均未改动。1.21.1 **214 suites / 775 tests / 0 failures**，JAR SHA-256 `E513565242D4F17C1F5181F6D7FC8EA55090AAEAB448C30AC1D3A260D02E31EA`；26.1.2 **207 suites / 754 tests / 0 failures**，JAR SHA-256 `1C5F8F207DD2C9F8EEB53B694EA924EDBB6B634A6444B6A2C3DFA33C8E930F15`。两代实例及 26.1.2 全局 `mods` 已替换，旧包备份于 `archive/pcl2-backups/2026-08-13-treasure-basin-light-8`；本轮未启动客户端，由用户实机验收。详情见 `archive/2026-08-13-treasure-basin-light-8.md`。

> **26.1.2 源方块悬浮同步、JEI 启动崩溃与聚宝盆亮度修复（2026-08-13，本轮）：** 保留用户已调整的 1.21.1 源方块悬浮实现，并按目标渲染 API 等价同步到 26.1.2：方块核心缩放为 `0.36`、透明度为 `166/255`，物品核心放大为 `0.48`，按实际模型包围盒竖直居中且只绕 Y 轴旋转，不加入俯仰或翻滚。错误报告中的启动崩溃定位为 JEI 注入代码直接引用 Mixin 包内普通策略类，现将策略类迁出 Mixin package，同时保留可选 JEI 边界。聚宝盆继续使用原版炼药锅结构、UV 与 AO，只提高金色色阶下限以避免世界方向光照后近黑。1.21.1 **214 suites / 775 tests / 0 failures**，JAR SHA-256 `205511650F957443999395230452A88CC80F49B803E0DC9B1A30A3EBABABF91A`；26.1.2 **207 suites / 754 tests / 0 failures**，JAR SHA-256 `0F5E1D542176A30985515ED071484306E08D303D14E9261D11F96C02835D95DD`。两代实例及 26.1.2 全局 `mods` 已替换，旧包备份于 `archive/pcl2-backups/2026-08-13-jei-crash-basin-source-sync`；本轮未启动客户端，由用户实机验收。详情见 `archive/2026-08-13-jei-crash-basin-source-sync.md`。

> **两代 JEI 固定定位、范围预览与聚宝盆光照收口（2026-08-13，本轮）：** 仙窍存储页的 JEI `Filter...` 与固定按钮改由可选 JEI Mixin 相对容器 `guiLeft/guiTop` 定位，不再受存储行数或窗口尺寸漂移；稳定化迷你仙墟、高级稳定化、纠缠及高级纠缠页面统一上移 4 GUI 像素。仙窍接口/高级仙窍接口、模拟灵田与模拟轮回炼化炉的 JEI 搜索显示已彻底关闭。26.1.2 范围预览不再引用失效的 `minecraft:textures/misc/white.png`，迷你仙墟六面预览及高级仙窍接口白/绿/红双色面统一使用存在的白色纹理；反转迷你仙墟核心保持白色。聚宝盆只恢复原版炼药锅 AO 层次光照，金色贴图、UV、10×10×10 模型和碰撞箱均未改变。1.21.1 **214 suites / 774 tests / 0 failures**，JAR SHA-256 `38E14521A68DB0BC58FDCDE0C86E501D169E3C9B5335621DFF17B9AB72363F54`；26.1.2 **207 suites / 753 tests / 0 failures**，JAR SHA-256 `DB518CC8A39DE9E3C742D9BE15600EC0ED75F7EA6EE8A8B802EA863F9B8F0063`。两代实例及 26.1.2 全局 `mods` 已替换，旧包备份于 `archive/pcl2-backups/2026-08-13-jei-preview-lighting`；本轮未启动客户端，由用户实机验收。详情见 `archive/2026-08-13-jei-preview-basin-lighting.md`。

> **接口搜索定位与两代金色炼药锅聚宝盆（2026-08-13，本轮）：** 普通/高级仙窍接口的 JEI 可用区域按用户截图目标增加 **106 GUI 像素右移避让**，底部避让由 24 提升为 42，使搜索框向界面右上方移动；1.21.1 与 26.1.2 使用同一固定档位规则。聚宝盆不再使用自定义 5 长方体模型，两个版本都直接继承 `minecraft:block/cauldron`，完整复用原版炼药锅的 13 元素结构、面、UV、剔除与 AO 规则。四张原版炼药锅 16×16 纹理逐像素保留透明遮罩和明暗顺序，只映射为固定金色梯度；生成过程固化在 `tools/generate_golden_cauldron_textures.py`。贴图审计未发现本轮新增问题；1.21.1 **211 suites / 768 tests / 0 failures**，JAR SHA-256 `7B08BE2AA15F6F2B10E36B7674DB4C29E893EDA58A6474D27ECBEAB848D96CA4`；26.1.2 **205 suites / 749 tests / 0 failures**，JAR SHA-256 `9109E868E4FCBBF8D679EF49A590565321D0A757A17567FB4CFCE8EA07887846`。两代实例及 26.1.2 全局 `mods` 已替换，旧包备份于 `archive/pcl2-backups/2026-08-13-0015-golden-cauldron`；客户端视觉位置由用户继续验收。详情见 `archive/2026-08-13-interface-search-golden-cauldron.md`。

> **26.1.2 槽位光标与聚宝盆目标渲染修正（2026-08-12，本轮）：** 上一轮仅移除模组自绘高亮，未触及 Minecraft 26.1.2 `AbstractContainerScreen` 在旧菜单槽位坐标生成的私有 24×24 前后高亮，因此白色光标仍会跟随错误位置。目标适配层现按原版顺序完整提取控件、标签、槽位与前后高亮，并统一从终端重定位后的可视槽位边界判定悬停。聚宝盆保持 1.21.1 的 5 个长方体、UV 与贴图引用完全不变，仅在 26.1.2 资源覆盖中关闭环境光遮蔽以隔离目标版本烘焙光照伪影。定向契约、**205 suites / 748 tests / 0 failures / 0 errors / 0 skipped** 与 26.1.2 `clean test build` 均通过；JAR SHA-256 `4497413ADB7C34C2FA3CEF012093F95328B6BF4C78D95864E796C42E37B273C8`，已替换实例和全局 `mods`，旧包备份于 `archive/pcl2-backups/2026-08-12-2348-cursor-basin`。本轮未启动客户端，由用户实机验收。详情见 `archive/2026-08-12-cursor-basin-target-fix.md`。

> **AE2 事务访问丢物与 26.1.2 渲染收口（2026-08-12，本轮）：** 26.1.2 的 NeoForge `ResourceHandler` 现按官方事务规范桥接旧 `IItemHandler`：探测/模拟事务在回滚时不再即时扣除物品，根事务提交后才执行不可逆写入；动态槽位在提交前再次校验资源键，同一事务多次插入会累计已承诺容量，避免 AE2 得到虚假的接受数量。AE2 网格在存储总线接入管理器时立即停用同 owner 的磁盘 wrapper，关闭一 tick 双重挂载窗口；1.21.1 同步具备该去重。26.1.2 同时移除重复的自绘槽位高亮、将源方块悬浮中心恢复为 `Y=0.5`，并修正世界碎片采集器把方块坐标误当玻璃 outline 颜色的问题。完整门禁：1.21.1 **768 tests / 0 failures**，26.1.2 **747 tests / 0 failures**。1.21.1 JAR SHA-256 `723E8D6A0AC0947CF6ECCA23899FABC4E00CAD1CAD17F28000285B7899830BCC`；26.1.2 JAR SHA-256 `30D0BFFEC69A11550A807BD92A99EA3F86EF265388F01F7A0948034234D72C6C`。两代已替换 PCL2 对应实例，26.1.2 同步替换全局 `mods`；旧文件备份在 `archive/pcl2-backups/2026-08-12-2330-ae2-transfer`。本轮未启动客户端，由用户继续实机校验。详情见 `archive/2026-08-12-ae2-transaction-render-fixes.md`。

> **仙窍接口/高级仙窍接口与源方块迁移修复（2026-08-12，本轮）：** 普通仙窍接口和高级仙窍接口均接入固定档位 JEI 搜索区域避让，避免搜索框压住界面；AE2 交换网在同一仙窍管理器同时被磁盘和存储总线访问时，按管理器 owner 去重，避免重复挂载导致物品丢失。灵器建筑模式按方块本体识别，箱子等方块状态变化后仍属于同一建造面；26.1.2 的目标渲染器崩溃入口补齐线宽初始化。源方块目录显示改为“当前缓存 + 当前仙元可兑换量”，仙元仍单独显示总量，不改变实际抽取语义；相邻完整方块的悬浮面使用微小外移避免深度缓冲吞面。1.21.1 完整门禁为 **211 XML suites / 768 tests / 0 failures / 0 errors / 0 skipped**；26.1.2 完整门禁为 **203 XML suites / 740 tests / 0 failures / 0 errors / 0 skipped**。1.21.1 JAR 为 5,299,692 字节，SHA-256 `44CFE06E1AFB496A406FB335DE43B74F495B60948D0DBDA06852915130870CC4`；26.1.2 JAR 为 5,298,622 字节，SHA-256 `B4F5835C6D28C9E7183173CB534DBBA8362353510BDD182755B053885D4621AD`。两代已替换到 PCL2 对应实例，26.1.2 同步更新全局 `mods`，替换前文件备份在 `archive/pcl2-backups/20260812-2245-batch-jei-storage-fixes`；本轮未启动客户端，等待用户实机/UI校验。详情见 `archive/2026-08-12-jei-storage-state-render-fixes.md`。

> **源方块/仙窍管理器中心渲染修复（2026-08-12，本轮）：** 修复仙窍管理器中心白色方块丢失材质：共享悬浮方块渲染器改用存在的原版 `minecraft:textures/block/white_concrete.png`，管理器仍只复用既有 core 渲染。源方块悬浮方块改为从实际 BakedQuad 几何顶点求中心，悬浮物品在 26.1.2 特殊物品模型管线中读取 `ItemStackRenderState.getModelBoundingBox()` 后再居中，避免外层 `-0.5` 变换造成二次偏移。水源方块渲染传入世界与方块位置，使用对应流体 still sprite 和 `BiomeColors` 动态水色，不再因缺少 tint 上下文变白；1.21.1 与 26.1.2 均增加渲染契约测试。1.21.1 完整门禁为 **211 XML suites / 766 tests / 0 failures / 0 errors / 0 skipped**；26.1.2 完整门禁为 **203 XML suites / 738 tests / 0 failures / 0 errors / 0 skipped**。1.21.1 JAR 为 5,296,256 字节，SHA-256 `0A6082ABBE91DA2177A1ED17435C52ABB6E210C14B392DAC3CF6D5743AE8FF3F`；26.1.2 JAR 为 5,295,159 字节，SHA-256 `BB8F4ECED3B16B46591EE3DF84C18575FEBA045D84F8430C2A02E503AB40445A`。两代制品已替换到 PCL2 对应实例，替换前文件备份于 `archive/pcl2-backups/20260812-2025-source-floating-render-fix`；本轮未启动客户端，等待用户实机校验。详情见 `archive/2026-08-12-source-floating-render-material-fix.md`。

> **高级仙窍接口外部资源配置修复（2026-08-12，本轮）：** 修复高级/普通仙窍接口缓存配置槽右键无法打开外部资源菜单的问题。右键现在优先进入资源配置；已配置外部资源时普通右键可直接更换目标，Shift+右键保留为数量编辑。外部资源目录始终提供内置 FE；Botania Mana、Ars Nouveau Source 与 Industrial Foregoing Soul 仅在对应模组加载时出现，因此无可选附属时菜单只显示 FE。1.21.1 与 26.1.2 兼容源已同步，新增高级界面契约测试；两代完整构建、测试、版本组成与生产 JAR 边界校验通过。1.21.1 JAR 为 5,295,542 字节，SHA-256 `E5E0AC4C32B7A6E6C8C0DF0D5BEBE1F7BC809C049C8E2F909C353C7E5F26A280`；26.1.2 JAR 为 5,290,293 字节，SHA-256 `A008AFEA4E5B58A0E90D23853D7EC705604832EF564BDA535A07534BAC080E2E`。两者均已替换对应 PCL2 实例，旧制品在 `archive/pcl2-backups/` 中按本轮时间戳备份。详情见 `archive/2026-08-12-advanced-xianqiao-resource-menu.md`。

> **26.1.2 仙窍时间/天气锁定迁移修复（2026-08-12）：** 仙窍不再向已失效的 `ServerLevelData` 时间/天气字段写值；目标专用 `PersonalRealmServerLevel` 改用 26.1.2 `WorldClock` 网络状态和独立 `WeatherData`，仅向仙窍所属玩家同步固定昼/夜与晴/雨/雷雨状态，避免污染主世界或其他玩家仙窍。时间流速仍由仙窍独立 tick budget 控制。顺带修正目标描述文件的 JEI 版本范围为 `29.21.0.68`。26.1.2 完整门禁为 **733 tests / 0 failures / 0 errors / 0 skipped**；新 JAR 为 5,289,881 字节，SHA-256 `3F484998425DA74541798FA19AB8ACBD2416A2C238BF10F64B80C84DB75DD7BA`，已备份旧制品并替换指定 PCL2 实例。本轮未启动游戏，等待用户实机校验。

> **26.1.2 GUI 纹理坐标与输入事件兼容修复（2026-08-12）：** 修正 `GuiGraphicsExtractor.blit` 的目标终点坐标和归一化 UV 终点转换，终端、仙能水晶、FE 额外存储图标及所有共用纹理层不再随界面位置拉伸偏移；新版 `MouseButtonEvent` 点击/拖动/释放及键盘事件现会回派给原 1.21.1 屏幕覆写，终端自定义长整型存储取出逻辑重新生效。新增目标回归断言，26.1.2 全量结果为 **731 tests / 0 failures / 0 errors / 0 skipped**，完整构建与生产门禁通过；JAR 内确认包含原 `ae2_fe.png`。

> **0.0.11 正式发布（2026-08-12）：** Minecraft 1.21.1 / NeoForge 21.1.235 发行线已完成正式门禁并发布。制品 `immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.11.jar` 为 5,295,016 字节，SHA-256 为 `60F0314381D714708FA9C7F29EFC4D8F50653E8FB6DDD40B5A6ED64541B37DF3`。

> **26.1.2 源码迁移基线已冻结（2026-08-12，本轮）：** 已将当前未提交工作树中的 26.1.2 迁移输入、共享核心、兼容生成器/矩阵、目标生成与覆盖源码、测试/资源和相关设计源文件保存为独立文件级 fork：[`archive/26.1.2-source-fork-20260812`](archive/26.1.2-source-fork-20260812)。该快照包含 2,680 个文件并通过逐文件 SHA-256 校验；后续 1.21.1 升级只作用于 live canonical 源码，重新迁移 26.1.2 时以该快照为输入，避免连带改动。由于当前工作树有未提交改动，本轮没有强行提交 Git 分支，也没有修改或安装 PCL2 实例。详情见 [`archive/2026-08-12-26.1.2-source-fork.md`](archive/2026-08-12-26.1.2-source-fork.md)。

> **26.1.2 错误报告修复与资源迁移收口（2026-08-12，本轮）：** 针对 `错误报告-2026-8-12_0.36.29.zip` 修复退出世界时空维度键触发的 `ResourceKey.identifier()` 空指针；26.1.2 的配方、战利品修改器与 JEI 访问路径按目标接口迁移：配方 Ingredient 使用字符串/`#tag`，附魔组件使用直接等级映射，战利品修改器使用逐文件 `type` 与 `item.id`，旧全局 loot 索引不进入目标 JAR；JEI 改用 `OnDatapackSyncEvent#sendRecipes` → `RecipesReceivedEvent` 客户端缓存，不再强转 `ClientRecipeContainer` 为 `RecipeManager`。矿物范围按用户更正仅保留世界生成方式与采集器/聚宝盆读取，未添加任何 `minecraft:iron_ores`/`minecraft:diamond_ores` 标签。26.1.2 目标测试为 **199 XML suites / 729 tests / 0 failures / 0 errors / 0 skipped**；1.21.1 全量测试为 **210 XML suites / 764 tests / 0 failures / 0 errors / 0 skipped**；最终目标 JAR 为 4,655,177 字节，SHA-256 `8A40172D7CDCF42EB14C91D3ECEF47513E6DF2F9BF189FA81879009E246D91B7`，已替换 PCL2 全局 `mods` 与 `versions\\neoforge-26.1.2.94\\mods`，旧 JAR 备份在 `archive/pcl2-backups/2026-08-12-26.1.2-report-fix`。本轮未自动启动客户端。详情见 `archive/2026-08-12-26.1.2-error-report-fix.md`。

> **范围更正（2026-08-12）：** 上一条世界生成记录中关于新增原版矿物标签的描述已被本条 supersede；当前实现只处理矿物生成方式，不改动矿物标签集合。该历史条目的旧标签描述仅保留为操作留痕，不代表最终源码或 JAR。

> **测试计数复核（2026-08-12）：** 上方错误报告修复条目的 1.21.1 计数已由新增空维度键回归用例复核为 **210 XML suites / 764 tests / 0 failures / 0 errors / 0 skipped**；26.1.2 目标计数保持 **199 XML suites / 729 tests / 0 failures / 0 errors / 0 skipped**。

> **历史条目说明（2026-08-12）：** 下方世界生成迁移条目保留旧操作留痕，其中曾出现的原版矿物标签扩展已撤销；最终实现仅保留矿物生成方式与既有采集读取路径。

> **26.1.2 世界生成迁移、矿物生成方式与断言（2026-08-12，历史检查点；标签范围已撤销）：** 根据 PCL2 世界加载日志定位到 26.1.2 对旧 `dimension_type`/`biome` 数据形状的拒绝：旧 `bed_works`、维度 `effects`、`carvers: {}` 和生物群系 `mood_sound` 均不能直接沿用。目标资源新增 `attributes`、`default_clock`、`has_ender_dragon_fight`、`timelines`、`min_y=-64`/`height=384`，并将生物群系 carving 改为 12 段数组、环境音改到 attributes；Gradle 明确在 canonical 数据之后覆盖目标数据，避免两代格式串包。矿物范围在该历史检查点中曾误扩展到原版方块/物品标签，已按用户更正撤销；当前只保留原有 `minecraft:ore` configured/placed feature 生成目标。世界碎片采集器的 26.1.2 适配改为从最终 `PlacedFeature` Holder 的 `value().config()`、生物群系注册表 tag 读取矿物；聚宝盆继续通过 `WorldShardLootCatalog` 和 reloadable registry 的 `ResourceKey< LootTable >` 读取真实战利品表，不使用硬编码掉落。早期 `WorldgenMinerDictionaryTargetContractTest` 的标签断言已由当前制品的“无四个原版标签”反断言替代。1.21.1 全量测试为 **210 XML suites / 764 tests / 0 failures / 0 errors / 0 skipped**；该历史检查点的 26.1.2 门禁为 **197 XML suites / 726 tests / 0 failures / 0 errors / 0 skipped**。历史 JAR 为 4,654,727 字节，SHA-256 `A4470097EB723877567516F6DF0008F759830F29CDA98544E9559DADFE9B0DC0`，已由本轮错误报告修复后的目标制品替换；旧制品备份在 `archive/pcl2-backups/2026-08-12-26.1.2-worldgen-assertions`。适配依据记录于 [NeoForge 26.1 迁移说明](https://docs.neoforged.net/primer/docs/26.1/) 与 [NeoForge 生物群系修改器文档](https://docs.neoforged.net/docs/1.21.1/worldgen/biomemodifier/)。

> **26.1.2 启动崩溃修复与实例更新（2026-08-11，本轮）：** 根据 23:23:51 与 23:24:18 两份错误报告，修复迁移生成器对同一按键分类重复 `KeyMapping.Category.register` 的问题；现在四个按键共享一个分类实例。新增目标回归测试，26.1.2 完整门禁通过 **196 XML suites / 722 tests / 0 failures / 0 errors / 0 skipped**。新 JAR 为 4,653,510 字节，SHA-256 `F2804449F489E2D1999DB1167474FCDE9D85F21BF08267F31D1B878BEDF5E21F`，已替换 PCL2 全局 `mods` 和 `versions\neoforge-26.1.2.94\mods`；两处各 13 个 JAR 且逐文件一致。旧 JAR 已备份到 `archive/pcl2-backups/2026-08-11-26.1.2-key-category-fix-20260811-233548`。本轮未自动启动游戏。详情见 `archive/2026-08-11-26.1.2-key-category-crash-fix.md`。

> **26.1.2 构建与全模组 PCL2 实例更新（2026-08-11，本轮）：** 已从 canonical 源码重新生成兼容层并构建 `immortalstorage-neoforge-mc26.1.2-nf26.1.2.94-0.0.11.jar`；目标测试汇总为 **721 tests / 0 failures / 0 errors / 0 skipped**，生产 JAR 边界、版本组合、精确版本产物和无 AE2 运行时门禁通过。新 JAR 为 4,653,477 字节，SHA-256 `EDA336DD9454F1A70B37358EE546F527EAF3B46EE9AD0D97E61852739C44A6D7`，已替换 PCL2 全局 `mods` 与 `versions\neoforge-26.1.2.94\mods` 两处目标；两处各 13 个 JAR 且逐文件一致。替换前制品已备份到 `archive/pcl2-backups/2026-08-11-26.1.2-refresh-20260811-231519`。本轮未启动游戏，26.1.2 仍等待实机互操作测试。详情见 `archive/2026-08-11-26.1.2-build-and-pcl2-refresh.md`。

> **0.0.10 → 0.0.11 Changelog（2026-08-11）：** 已将本版本用户可见变更整理到根目录 [`CHANGELOG.md`](CHANGELOG.md)，覆盖仙能水晶系列、FE/AE2/RS 长整型存储、仙窍绑定与输出、源方块动态渲染、26.1.2 兼容迁移、迷你仙墟、灵器名称和双语帕秋莉手册；该 changelog 不包含 0.0.11 内部调试过程。

> **帕秋莉手册水晶章节更新（2026-08-11，本轮）：** 新增双语“仙能电力水晶行为”章节，完整说明三槽、绑定优先级、仙窍/六面独立开关、缓存迁移、顶部处理/侧面燃料/底面抽取、充电优先级、每刻全量输出、额外槽边界、FE长整型读取与配置。新增“水晶联动”章节，集中说明Botania仙能魔力水晶、Ars Nouveau仙能魔源水晶、火花/支配之杖持久化、条件注册及水晶组转换配方；机器绑定总则不再混写两种可选水晶的具体行为。67个手册JSON解析通过，双语条目树一致，1.21.1帕秋莉契约测试通过 **6 tests / 0 failures / 0 errors / 0 skipped**。26.1.2无独立手册资源覆盖，下一次目标构建会从canonical资源树同步；本轮未重建或替换目标JAR。详情见 `archive/2026-08-11-patchouli-crystal-behavior.md`。

> **26.1.2 错误报告定位（2026-08-11，本轮）：** 分析 `C:\Users\12252\Desktop\Files\Minecraft\PCL\.minecraft\错误报告-2026-8-11_21.58.16.zip` 确认启动失败的第一原因是迁移生成器将四个 ImmortalStorage 按键分别展开为四次 `KeyMapping.Category.register(immortalstorage:immortalstorage)`；第二个按键在 `ImmortalStorageKeybinds.<clinit>` 抛出 `Category already registered`，后续 `NoClassDefFoundError` 均为连锁异常。根因位于 `project/version-compat/generate-compat-source.ps1:1289-1291`，不是仙能水晶、AE2/RS 或渲染逻辑。本轮只完成证据归档，未修改源码、未重编译、未替换实例；修复方案记录于 `archive/2026-08-11-26.1.2-error-report-key-category.md`。

> **仙能水晶额外槽与面向规则收口（2026-08-11，本轮）：** 当前版本为 **0.0.11**。三类仙能水晶的充能结果永远留在额外槽，不论仙窍输出开关状态都不会写入所属仙窍；自动化只能从底面抽出，其他面不能抽取额外槽。统一面输入规则为：顶部仅输入处理物，四个水平侧面仅输入燃料，底面不接受输入。仙窍输出仍只处理 FE/Mana/Source 资源缓存，资源能力的既有六面输出和优先级不变。1.21.1 全量 `test` 通过 **763 tests / 0 failures / 0 errors / 0 skipped**；26.1.2 迁移 `test` 通过，两个 JAR 均构建成功。当前 JAR：1.21.1 为 5,287,603 字节、SHA-256 `AE33FCC52F85D6463BE46AB2C3DE9653CC6896783756C88C212604586C048E03`；26.1.2 为 4,646,106 字节、SHA-256 `4E4E90DC0B5A4D427C32ADF473D94AD5C62390E2DFEF4C600F5F09D12B245639`。PCL2 已同步：1.21.1 实例 30 个 JAR；26.1.2 全局 `mods` 与 `versions\neoforge-26.1.2.94\mods` 各 13 个 JAR，目标两处逐文件一致。替换前文件保存在 `archive/pcl2-backups/2026-08-11-extra-slot-output-rule`。本轮未启动游戏。

> **1.21.1/26.1.2 管理器 GUI 预览收口（2026-08-11，本轮）：** 已排查“源方块管理器 GUI 物品栏表现没有丝毫变化”的根因：管理器 BEWLR 内部缩放并不是最终 GUI 尺寸，物品模型缺少独立 `display.gui` 时会沿父模型链继承 `arcane_machine_frame` 的 `0.82` 变换，因此此前只改渲染器不会改变槽内大小。现将 `source_vein_manager.json` 的 GUI 旋转、平移和缩放逐项设为与 `source_vein.json` 完全一致的 `[30,225,0]`、`[0,0,0]`、`[0.625,0.625,0.625]`；管理器方块模型仍继承 `custom_source_vein`，BEWLR 只追加动态 core，未重复缩放或改变结构。新增资源契约测试已在两代源码树验证，最终 1.21.1 `build` 与 26.1.2 `build` 均通过；当前目标测试结果为 **713 tests / 0 failures / 0 errors / 0 skipped**。1.21.1 最终制品 `immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.11.jar` 为 5,244,333 字节，SHA-256 `3FF9D35ED95815A3389C6AB3440584A9B0DECBC3354492787C0C88BFF0B7B0D8`，已替换到 PCL2 1.21.1 实例；26.1.2 制品为 4,617,509 字节，SHA-256 `DA97982B8E5BFE11F3925225B32797D07944D760104D683DED7DA3E14BD598C5`，并已与 AE2、Refined Storage、JEI 的目标矩阵文件一同布置到 PCL2 全局 `mods`。26.1.2 仍是 development 目标，尚未宣称正式发布：缺少目标客户端/服务端/Numen 与 AE2/RS 实机互操作证据，且没有可解析的官方 26.1.2 Mekanism、EMI、Refined Types、RS Mekanism Integration 制品。构建接口依据已核对 [NeoForge 26.1 迁移说明](https://docs.neoforged.net/primer/docs/26.1/)、[NeoForge 事务接口](https://docs.neoforged.net/docs/inventories/transactions/)、[AE2 API](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/API.md)、[Refined Storage Storage API](https://refinedmods.com/javadoc/refinedstorage2/com/refinedmods/refinedstorage/api/storage/Storage.html) 与 [Mekanism 26.1 ResourceHandler](https://github.com/mekanism/Mekanism/blob/26.1/src/api/java/mekanism/api/resource/IMekanismResourceHandler.java)。

> **1.21.1 仙能水晶仙窍维度优先绑定与燃料补货修正（2026-08-11，本轮）：** 本轮只更新 canonical `project/neoforge-1.21.1-mdk`，26.1.2 目标与用户恢复的 `design/EnergyCristal-Half.bbmodel` 均未修改。仙能水晶放置在个人仙窍维度时直接优先绑定维度所属玩家，即使燃料槽没有仙灵驱动器也可正确解析仙窍 FE；只有不在仙窍维度时才回退到已绑定仙灵驱动器。仙窍来源使用精确的 owner-realm FE endpoint，因此开启仙窍输出后仪表、FE capability、AE2/RS 长整型读取可显示同一仙窍 FE。仙窍维度绑定的水晶关闭仙窍输出后停止自动仙元燃料补货；仙灵驱动器绑定的水晶关闭仙窍输出不影响其燃料补货，直接放入的仙元/真元燃料仍按物品燃料规则工作。1.21.1 全量测试通过 **752 tests / 0 failures / 0 errors / 0 skipped**，生产构建及兼容/版本/无 AE2 门禁通过。当前 0.0.11 构建与 PCL2 部署 JAR 均为 5,232,412 字节，SHA-256 `1CE9B010AD77976965E28E4C1F4E76EC5C532B5F8C8E15EF0584CF920FD2B6B9`；替换前 JAR 已备份为 `archive/2026-08-11-pcl2-backup-immortalstorage-0.0.11-pre-realm-priority-binding.jar`，SHA-256 `622C23CA4ACE0E0A20149A1E818DA154D0D07DD8A778DDD9E380AB3362F5E55E`。

> **1.21.1 当前最终收口（2026-08-11，本轮）：** 本轮仍只更新 canonical `project/neoforge-1.21.1-mdk`，26.1.2 目标与用户恢复的 `design/EnergyCristal-Half.bbmodel` 均未修改。源方块管理器的方块/物品模型现在直接继承 `custom_source_vein`，边框不再保留第二套建模或渲染；管理器 BE renderer 只绘制 core，客户端世界重载不会再用空成员把显示进度清零。仙能水晶仙窍输出默认关闭且无有效绑定时禁止开启；开启前将内部 FE 缓存全量迁移并清零，开启后 FE capability 与右侧仪表读取绑定玩家仙窍 FE，超过 800M 按满槽显示；关闭时只解除外部绑定，不把仙窍 FE 拉回、不额外改写缓存，内部缓存从 0 重新累积。方块源方块悬浮核心为真实几何中心、`0.36` 半透明，流体为动态材质半透明，物品为真实中心、`0.48` 且不额外降 alpha。1.21.1 全量 `test` 通过 **752 tests / 0 failures / 0 errors / 0 skipped**，生产构建及兼容/版本/无 AE2 门禁通过。当前 0.0.11 构建与 PCL2 部署 JAR 均为 5,229,723 字节，SHA-256 `622C23CA4ACE0E0A20149A1E818DA154D0D07DD8A778DDD9E380AB3362F5E55E`；替换前 JAR 已备份为 `archive/2026-08-11-pcl2-backup-immortalstorage-0.0.11-pre-xianqiao-binding-manager-frame-fix.jar`，SHA-256 `27092E2726A3E4CF5E7306CE7A56D96AD795ABF23ED6D55E77E77A91A67CA9F7`。

> 下方同日段落保留为历史检查点；其中与仙窍绑定来源、燃料补货、仙窍关闭动作或管理器边框模型有关的旧描述，以本段最新规则为准。

> **1.21.1 源方块悬浮透明度与仙能水晶仙窍 FE 回灌修正（2026-08-11，本轮）：** 本轮只更新 canonical `project/neoforge-1.21.1-mdk`，没有写入 `project/version-compat/neoforge/mc-26.1.2-nf-26.1.2.94`，也没有读取或修改用户恢复的 `design/EnergyCristal-Half.bbmodel`。方块源方块悬浮核心改为按真实 BakedQuad 几何中心缩放到 `0.36` 并统一使用 alpha `166/255` 的半透明缓冲；流体源方块继续读取对应流体的动态静态材质并以同等半透明规则铺满六面；物品源方块改为真实几何中心、`0.48` 缩放和原生不额外降 alpha 渲染。仙能水晶的绑定策略改为通过统一 `PersistentPlayerIdentity` 解析个人仙窍 owner 与已绑定灵器，兼容稳定身份及迁移期旧 session UUID；FE 读写改经统一 `PersonalStorageApi.resolveXianqiao(...).externalResourceStorage()`，因此开启仙窍输出、处理槽优先取仙窍 FE、关闭开关/换绑/拆除时缓存回灌都使用同一个长整型 FE 账本，AE2/RS 仍读取同一账本。新增/更新渲染与绑定回归契约。源码强制编译、测试源码编译、生产 JAR 边界、版本组合、兼容矩阵、Ars Source 探针和无 AE2 运行时探针均通过；完整 NeoForge `test` 本轮未能进入用例阶段，因测试前置重复下载已存在的 Minecraft 资产索引，定向跳过下载又缺少 `minecraft_assets.properties`，因此不冒充完整测试通过。当前 0.0.11 构建与 PCL2 部署 JAR 均为 5,229,146 字节，SHA-256 `27092E2726A3E4CF5E7306CE7A56D96AD795ABF23ED6D55E77E77A91A67CA9F7`；替换前 JAR 已备份为 `archive/2026-08-11-pcl2-backup-immortalstorage-0.0.11-pre-source-render-xianqiao-cache-fix.jar`，SHA-256 `2CF75D0070FE0E2941F15FBBDC598FA2EC67772A61260F0B29C01BD0E1676E5F`。

> **1.21.1 绑定规则、FE缓存、输出开关与渲染修正（2026-08-11，本轮）：** 本轮只更新 canonical `project/neoforge-1.21.1-mdk`，`project/version-compat/neoforge/mc-26.1.2-nf-26.1.2.94` 未写入，用户恢复的 `design/EnergyCristal-Half.bbmodel` 未读取、未写入。仙能水晶、模拟灵田、模拟轮回炼化炉现在统一只在“位于玩家个人仙窍维度 + 燃料槽为该玩家绑定的玩家仙灵启动器”时绑定玩家；放置者、最近玩家、旧存档 owner 或不匹配启动器都不会产生玩家绑定。仙能水晶开启仙窍输出后，内部 FE 缓存与绑定玩家的仙窍 FE 存储关联；关闭开关或拆除时先将缓存回灌仙窍，处理可充电物品时优先消耗本地缓存，再按开关状态读取仙窍 FE，剩余量保持在本地缓存中。三台机器的仙窍输出与六面自动输出是独立开关，`ContainerData`、打开中的菜单广播和设置面板文字均同步，输出开关标题不再与面按钮重叠。源方块物品显示改用原始 BakedQuad 列表渲染，避免原版 ItemRenderer 的二次半方块平移造成中心偏移；加速动画按实际逻辑 tick 倍速即时播放，不保留慢速追赶尾巴或跳帧。1.21.1 全量 `check` 为 **751 tests / 0 failures / 0 errors / 0 skipped**。当前构建与 PCL2 JAR 均为 5,225,674 字节，SHA-256 `2CF75D0070FE0E2941F15FBBDC598FA2EC67772A61260F0B29C01BD0E1676E5F`；部署前制品已备份为 `archive/2026-08-11-pcl2-backup-immortalstorage-0.0.11-pre-xianqiao-binding-cache-sync.jar`，SHA-256 `52222B4365547119819371AC8830B4D99C27F67EBB2D149A6CDF7A903DC87C13`。

> **当前 1.21.1 修订状态（2026-08-11）：** 本轮只更新 canonical `project/neoforge-1.21.1-mdk`，26.1.2 目标目录未写入，用户恢复的 `design/EnergyCristal-Half.bbmodel` 未读取、未写入。源方块管理器模型的 `edge` 现在直接引用 `source_vein_frame`，旧 `source_vein_manager_edge.png` 仅作为像素一致的兼容别名；12 根梁、56 个面、UV 坐标和结构不变。源方块/流体世界显示以方块中心 `(0.5,0.5,0.5)` 固定旋转枢轴，按位置稳定随机姿态慢转；物品显示以物品原点为枢轴，仅保持竖直 Y 轴慢转，真实 BakedQuad 几何中心先归中。仙能水晶额外槽的客户端同步拦截已修复：仍禁止主动放入和非下方输出，但允许菜单同步包写入客户端只读槽位。全量 `check` 为 **749 tests / 0 failures / 0 errors / 0 skipped**。当前 PCL2 目标 JAR 为 5,223,233 字节，SHA-256 `52222B4365547119819371AC8830B4D99C27F67EBB2D149A6CDF7A903DC87C13`；部署前文件备份为 `archive/2026-08-11-pcl2-backup-immortalstorage-0.0.11-pre-direct-frame-orientation-slot-sync.jar`。纹理审计保留 6 项既有告警，未引入本轮结构性纹理问题。

下方同日的 748-test 段落是本次修订前的检查点记录；当前状态以本段为准。

> **1.21.1 源方块管理器/悬浮动画/仙能水晶显示修正（2026-08-11，本轮）：** 本轮只更新 canonical `project/neoforge-1.21.1-mdk`，不写入 26.1.2；用户手动恢复的 `design/EnergyCristal-Half.bbmodel` 保持原样。源方块与仙窍管理器框架贴图保持黑/灰主题，只做逐像素换色与层次纹理；源方块、源方块管理器、仙窍管理器的整张框架贴图均按 90° 旋转中心对称验收，源方块管理器模型实际采样的六组 2×2 UV 面岛也分别保持中心对称，避免梁面重叠采样造成材质爆炸。源方块/物品悬浮显示现在读取 BakedQuad 顶点包围盒，以目标方块或物品的真实几何中心旋转；加速世界使用单调时钟观测逻辑 tick 倍速并连续推进，不把多 tick 更新直接变成单帧跳跃。仙能水晶处理完成后直接写入额外槽的产物会广播到打开中的 `EnergyCrystalMenu`，因此槽位可见性与鼠标取出状态一致。新增回归契约覆盖真实模型中心、加速时钟、管理器 UV 映射、90° 对称与额外槽同步；1.21.1 全量 `check` 为 **748 tests / 0 failures / 0 errors / 0 skipped**。当前 PCL2 1.21.1 已部署 JAR（5,220,671 字节，SHA-256 `2C7BD2C9665403831A2750523C6F2A15913331B1DEF10D029F36BDEDFA3A7419`），部署前旧文件备份为 `archive/2026-08-11-pcl2-backup-immortalstorage-0.0.11-pre-source-manager-animation-slot-fix.jar`；26.1.2 本轮不写入。

> **1.21.1 仙能水晶与源方块显示修正（2026-08-11，本轮）：** 已手动恢复的 `design/EnergyCristal-Half.bbmodel` 保持原样，本轮不读取、不写入；仅更新运行时 `energy_crystal_crystal.png`，将参考水晶的多级三角切面像素结构换色到原青白/蓝青色调，并铺满现有 5 个晶体长方体的每个面；主晶体与交叉晶体的顶/底面按原分区 UV 岛的位置、方向和 2×2 尺寸适配，全部 16×16 像素为 alpha 204（80% 不透明）。源方块框架和源方块管理器框架保持黑/炭黑层次，仙窍管理器框架保持灰色层次；三者依据既有 Blockbench/模型 UV 采样位置逐像素换色，尺寸、透明布局、模型结构、8 个源核和未点名资源不变。源方块世界/物品的方块与物品显示先回到自身 `[0,1]` 模型中心再叠加浮动、旋转，动画改为双精度连续客户端时间并钳制 partial tick，修复偏心与周期性跳帧。1.21.1 全量 `check` 为 **746 tests / 0 failures / 0 errors / 0 skipped**；当前 PCL2 1.21.1 实例已部署该 JAR（5,212,495 字节，SHA-256 `B5B6921E14E6593D86E9B7E6F2A3463D8C6E3D70BA740E63771634042189D48D`），旧文件可从 `archive/2026-08-11-pcl2-backup-immortalstorage-0.0.11-pre-crystal-uv-fix.jar` 恢复。26.1.2 本轮不写入，继续等待 1.21.1 实机反馈。

> 当前正式发布版本为 **0.0.11**。本版本在 0.0.10 基础上将迷你仙墟作用范围严格限定为 `13×1×13` 单层区域，并使主手或副手持有迷你仙墟的玩家始终排除在作用对象外；物品 Tooltip 与双语帕秋莉手册同步给出原生使用说明。
>
> **26.1.2 兼容迁移与同等发布门禁（2026-08-10）：** 1.21.1 的 190 份测试已迁移到 26.1.2 目标，目标全量为 **698 tests / 0 failures / 0 errors / 0 skipped**；两代均通过项目 `check` 门禁，覆盖 `test`、`jar`、生产 JAR 边界、版本组合、精确版本产物和无 AE2 运行时。1.21.1 全量为 **740 tests / 0 failures / 0 errors / 0 skipped**，并额外通过 Ars Source API 探针；26.1.2 的 Ars 探针因目标官方制品不可解析而按构建规则跳过。26.1.2 仍标记为 `development`、未发布：正式声明还需要目标客户端/服务端、Numen、AE2/RS 实机互操作证据，且官方 26.1.2 Mekanism/EMI/Refined Types/RS Mekanism Integration 目标制品仍无可解析版本。
>
> 26.1.2 FE 额外存储使用共享长整型账本，通过 `ExternalResourceKeyBridge`、AE2 `MEStorage`/`AEKeyRendering` 与 RS long 资源/`ResourceRendering` 访问和渲染；AE2/RS 读取按修订号复用不可变长整型快照，避免重复扫描并在外部账本变化、写入或端点替换后及时刷新。新增仙能水晶：默认 800M FE 容量、燃料运行时 1k FE/t，兼容 FE 能力且不依赖 Mekanism；其 UI 完全沿用模拟灵田/模拟轮回炼化炉左侧三格布局，右侧 3×4 区域替换为 72×54 的 MEK 风格整块 FE 仪表，悬停显示长整型当前值/上限。仅停用无源方块的旧额外资源仙元转化，`SourceVeinBlockEntity` 自身的 `SourceChargeRegistry.IMMORTAL_YUAN` 源方块转化保持不变。灵器用户可见的“镐子模式”已统一改名为“挖掘模式”，内部模式编号保持不变；高级机器的面向能力、存储持久化、掉落缓存、配方/世界碎片/仙窍状态与 1.21.1 测试均纳入目标测试树。目标客户端物品资源已补齐 105 份 26.1 item definitions：源方块及自定义源方块使用 `source_vein` special renderer，源方块管理器使用 `source_vein_manager`，稳定化仙墟与仙窍管理器复现动态物品栏预览，`RegisterItemDecorationsEvent` 保留源方块输出角标。1.21.1 产物 `immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.11.jar` 为 5,209,850 字节、SHA-256 `A601DFDDE59FC0AC41060B7D06C60FC82F789DCC31F44D078145196EDBF4D59F`；26.1.2 产物 `immortalstorage-neoforge-mc26.1.2-nf26.1.2.94-0.0.11.jar` 为 4,591,188 字节、SHA-256 `5FE08A575AD95E4D71C96328519ACB0CF0012041FCDF9032764C5E0715F476E1`。

> **1.21.1 全模组 PCL2 实例配置（2026-08-10）：** 已更新 `C:\Users\12252\Desktop\Files\Minecraft\PCL\.minecraft\versions\1.21.1-NeoForge_21.1.235` 中的 ImmortalStorage 为当前 0.0.11 制品，实例保持 30 个 JAR；部署文件大小为 5,209,850 字节，SHA-256 为 `A601DFDDE59FC0AC41060B7D06C60FC82F789DCC31F44D078145196EDBF4D59F`。旧部署制品已保存到 `archive/2026-08-10-pcl2-backup-immortalstorage-0.0.11-pre-refresh.jar`。PCL 全局 Java 列表已登记 JDK 21；当前只完成实例配置，等待用户启动 1.21.1 实测后再配置 26.1.2。
>
> 0.0.10 在 0.0.9 的持久玩家身份、AE2/RS 额外资源兼容和崩溃修复基础上，修复一气归元剑与仙墟锻灵剑的淬火点 tooltip 系数显示，将淬火百分比限制为最多两位小数，补齐扩展后的双语帕秋莉手册内容，新增 Mekanism 化学品容器与终端内储存化学品的双向交互，并将仙窍管理的时间流速控件重排为居中的 `- 数值 +` 对称布局，同时修复控件文字重影。
>
> **0.0.9 RS 额外资源显示与附属兼容（2026-08-08）：** 仅安装 Refined Storage 2.0.9 时，仙窍交换磁盘会用仙藏自有 `xianqiao_external` 资源类型在 RS 网格中显示 FE、Mana、Source、Souls 与 Mekanism 化学品。安装 Refined Types 时 FE/Source/Souls 自动改用其原生键，安装官方 RS Mekanism Integration 时化学品自动改用其原生键；仙藏回退键继续可读写，避免升级、移除附属或网络缓存中的旧条目失效，并保证同一账本资源只枚举一次。ExtraStorage 等使用 RS 标准存储容器协议的扩容附属可与仙窍交换磁盘共存。详见 `archive/2026-08-08-rs-external-resource-display-and-addon-compat.md`。
>
> **0.0.9 RS 合成网格闪退修复（2026-08-08）：** 修复打开含仙藏额外资源的 RS 合成网格时 `No factory for class ... RsExternalResource` 导致连接中断。资源类型序列化与图标渲染此前均已注册，但遗漏了 RS 网格仓库的 `ResourceRepositoryMapper`；现补齐 `addGridResourceRepositoryMapper` 及安全网格条目。绑定旧磁盘的 `Test` 世界已通过 Numen 实机复测，`CraftingGridScreen` 正常显示资源且无高级屏幕处理失败。详见 `archive/2026-08-08-rs-grid-mapper-crash-fix.md`。
>
> **0.0.8 崩溃修复（2026-08-08）：** 修复摧毁源方块管理器时 `Missing id for entity in: {CachedUnits:...}` 闪退。根源是原版 `block_entity_data` 组件编码要求携带字符串 `id` 键，而管理器为新建/免费源成员写入 `CachedUnits` 时未补 `id`，导致成员序列化报错。现统一在写入侧补齐 `id`，且对已有 `id` 的成员（挖掘入仓）只更新缓存不覆盖；模拟轮回炼化炉的拆下/保存路径同步改用含 `id` 的 `saveWithFullMetadata`。详见 `archive/2026-08-08-source-vein-manager-missing-entity-id-crash-fix.md`。
>
> **0.0.8 源方块管理器控制器动效（2026-08-08）：** 源方块管理器改用 `design/SourceVeinController.bbmodel` 设计稿——12 根黑色 Edge 梁组成顶底开敞的镂空笼子（静态烘焙模型），内部 8 个 3x3x3 源核小立方按 72 格→8 段阶梯从空（蓝）→用（紫）→满（红）点亮，并作为刚体整体绕块中心 y 轴缓慢旋转（自定义 BlockEntityRenderer 动画）。手持/物品栏按原版默认渲染（空笼）。详见 `archive/2026-08-08-source-vein-manager-controller-bbmodel-animation.md`。
>
> **0.0.8 源方块管理器实机修复（2026-08-08）：** 实机验证发现两个显示缺陷并已修复：① 顶面圆环消失——顶/底各四根圆环梁补齐 `up`/`down` 封口面（复用既有 edge 贴图的备用不透明 2x2 采样区，贴图零改动），顶部框架不再消失；② 手持/物品栏只显示空笼——改为方块物品 BEWLR，在标准方块模型之上叠加与世界中完全一致的 8 段旋转源核（状态读取拆下时保留的 `DisplayState`，缺失时按成员数推导），与世界中渲染像素一致。详见 `archive/2026-08-08-source-vein-manager-bugfix-frame-and-item-core.md`。
>
> **0.0.8 仙窍身份统一（Bug 2，2026-08-08）：** 不同启动方式进入同一测试世界会产生两条互相隔离的离线身份与个人仙窍。已按“规范身份获胜”完成区块级迁移：规范 `00000fff-ffff-ffff-ffff-fffffff16c5c` 作为唯一身份，旧身份 `00000000-0000-3003-998f-501bcc516c5c` 的仙窍区块/成就并入（entities +9 独有区块、成就 +14 条），玩家数据保留规范身份，旧身份文件与旧仙窍目录整体隔离到 `saves\Test\_quarantine_old_identity\`，迁移前全量备份见 `saves\_backups\Test_pre-realm-merge_20260808_141636`。所有启动批处理均已硬编码规范 `--uuid`，无需改启动脚本。详见 `archive/2026-08-08-realm-identity-unification-bug2.md`。
>
> **0.0.8 仙窍雪不计入强制加载（设计约束，2026-08-08）：** 仙窍下雪仅为客户端粒子视觉，服务端 SNOW 天气不置 `raining=true`（否则 vanilla 会在整个仙窍铺雪层）；驱动强制加载的“玩家改动区块”集合只由玩家/实体驱动的破坏/放置事件喂养，vanilla 下雪用 null 实体的 `setBlock` 永远不会进入该记录路径。该不变式已通过 `RealmEnvironmentPolicy.requiresRain/requiresThunder` 纯函数与 `RealmEnvironmentPolicyTest` 回归测试锁定。
>
> **0.0.8 AE2 客户端闪退修复（2026-08-08）：** 修复在合并后的仙窍内打开 AE2 终端时 `Missing render handler for channel immortalstorage:external_resource` 闪退。根源是服务端已注册 `immortalstorage:external_resource` AEKey 通道，但客户端从未向 `AEKeyRendering` 注册渲染处理器，AE2 终端渲染/悬停该外部资源条目时 `getOrThrow` 抛异常。现新增客户端注册入口 `Ae2ClientCompat`（在 `CompatManager.initializeClientIntegrations` 中于 `AE2_LOADED` 守护下反射调用）与 `ImmortalStorageExternalResourceKeyRenderHandler`（绘制与命名复用 `ExternalResourceCatalog`，与仙窍接口视觉一致），并由回归测试直接复现原崩溃条件。全门禁 **191 suites / 706 tests / 0 failures**。详见 `archive/2026-08-08-ae2-missing-render-handler-crash-fix.md`。


**简体中文** | [English](README_en.md)

仙藏（ImmortalStorage）是一个面向 Minecraft 生存流程的修仙、个人存储、自动化与专属维度模组。玩家会从凡人启灵开始，通过古玉了解修行道路，积累灵气与材料，逐步突破十个阶段，扩展与角色绑定的空窍/仙窍存储，建设属于自己的仙窍维度，并在后期通过渡劫完成境界提升。

模组界面采用 Minecraft 原版像素语言，并借鉴大型存储网络的信息架构：存储、检索、合成、仙炉、装备、流体、化学品容器、磁铁和仙窍管理集中在连续终端中，不需要频繁打开互不关联的独立窗口。

> 重发后的 0.0.10 JAR 以 **Minecraft 1.21.1、NeoForge 21.1.235、Java 21** 为构建基线，正式声明 NeoForge 兼容范围 `[21.1.235,21.2)`；该范围已随本次发布资产生效。

> **破坏性品牌迁移：** 本次重发将模组 ID、资源命名空间、Java 包、网络 Payload、配置文件、命令和制品名全部改为 `immortalstorage`。不兼容旧 `cultivation` 世界或配置；测试旧世界必须删除后新建世界。不要同时安装任何旧 `cultivation-*.jar`。

**最新已发布版本：**[仙藏 ImmortalStorage 0.0.12](https://github.com/positer/ImmortalStorage/releases/tag/0.0.12)

**发行制品 SHA-256：**

- Minecraft 1.21.1 / NeoForge 21.1.235：`55B0816FC27F81A0E81E42517203E5FDE95CF97BB18676D06BB906B63CDCBE8A`（5,382,838 字节）
- Minecraft 26.1.2 / NeoForge 26.1.2.94：`15C451F4C4C87E81462B310B33E0F0CDC279D80B555A718FAD090D57A5270D76`（5,397,569 字节）

## 模组特色

- 从凡人到十阶的完整修行阶段，包含灵气、丹药、真元、仙元、飞升和渡劫。
- 与玩家身份绑定的空窍/仙窍存储，不依赖某一个固定放置的箱子或机器。
- 原版风格连续滚动终端，支持搜索、排序、合成、仙炉、装备、流体、Mekanism 化学品容器与磁铁管理。
- 每位玩家独有的仙窍维度，空间边界和时间流速会随阶段成长。
- 源方块、源方块管理器、仙窍管理器和仙窍接口组成的大宗资源自动化系统。
- 探索、扳手、挖掘、建筑、传送五模式灵器，以及可淬火成长的灵剑。
- 模组武器通过统一攻击投影将资源支付与成长增伤写入标准主手攻击属性，便于神化等属性系统读取并参与乘算。
- 内置帕秋莉运行时的古玉手册，内含阶段流程、功能说明和真实配方图，无需另装手册模组；0.0.12 手册完整说明强化插件、机器输出、附属存储及全部丹药配方。
- 可选兼容 JEI、EMI、AE2、RS、通用机械、植物魔法、通量网络与工业先锋灵魂等模组；Iron's Spells 玩家魔力不接入仙窍存储。
- 完整的简体中文与英文游戏文本、Tooltip 和模组配置说明。

## 环境要求

| Minecraft | NeoForge | Java | 0.0.12 制品 |
| --- | --- | --- | --- |
| 1.21.1 | 21.1.235（构建基线）；支持范围：`[21.1.235,21.2)` | 21 | `immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.12.jar` |
| 26.1.2 | 26.1.2.94 | 25 | `immortalstorage-neoforge-mc26.1.2-nf26.1.2.94-0.0.12.jar` |

JEI、EMI 和其他存储/科技模组均不是必需依赖。可选联动只会在目标模组实际安装时启用，未安装的联动不会造成类加载冲突。0.0.10 重发资产还在临时 35 模组客户端中分别通过了 Sodium 与 Embeddium 渲染栈启动烟测。

### 优化模组兼容性

- Sodium `0.8.12-alpha.4` 与 ModernFix `5.27.20`、FerriteCore `7.0.3`、ImmediatelyFast `1.6.11`、Entity Culling `1.10.5` 组合启动成功；Embeddium `1.0.15` 与同一组通用优化模组也启动成功。
- 两个临时客户端都完成 NeoForge 模组加载、资源重载、声音引擎初始化和 ImmortalStorage 的 Mekanism、Botania、Industrial Foregoing Souls、AE2、RS、Ars Nouveau 注册；没有优化模组或仙藏崩溃。
- Sodium 与 Embeddium 是二选一的渲染后端，不应同时安装。仙藏 JAR 不直接引用这些优化模组的类，也没有针对其内部渲染实现的 Mixin；兼容性依赖标准 Minecraft/NeoForge 渲染入口。

### 0.0.7 发行验证

- 回响碎片源方块与四个容器交互面变体的调度器、掩码解码、区内独占枚举、排序/收集/反向/投出等 9 项契约测试全部通过。
- 全部 700 项自动化测试通过，失败、错误与跳过均为 0；生产 JAR 边界、版本组成、精确版本产物、Ars Source API 与无 AE2 运行时校验通过。
- 0.0.7 在 30-JAR 全模组 PCL2 配置（含 Create 6.0.10 及其 ponder/flywheel）中使用 JDK 21、`zh_cn` 完成启动与单人实机验证，进入个人仙窍维度无 ImmortalStorage 致命错误。
- 鼓风机源方块触媒：`create:fan_processing_catalysts/splashing`（水仙墟）、`blasting`（岩浆仙墟），并补充 `create:fan_transparent` 白名单使风机风穿过源方块。
- 发行包 SHA256 为 `0547EFD1B1E75C9FE4305F3F6A48A79A9F5147FD42CA468AF716B87E91739B75`。

### 0.0.6 发行验证

- 模拟灵田的永久种源、50 tick 处理、真实作物战利品、特殊基底、紫颂/地狱疣规则、六面输出、自动输出与经验配置均有自动化契约测试。
- 仙炉、模拟轮回炼化炉和模拟灵田可在所有者仙窍内直接从个人存储支付仙元；联网协议已同步升级为 8。
- 仙窍日夜和天气由服务器持久锁定；Numen 实机完成“黑夜 → 白天 → 黑夜”往返并确认午夜天空盒、雨与四态天气切换。
- 2026-08-01 全量 691 项测试、生产 JAR 边界、版本组成、精确版本产物、Ars Source API、无 AE2 运行时及专用服务端启动校验通过。
- 发行包 SHA256 为 `21B27726DA0647A121A1E58CBF655AC2CA1B89DE4ADDE954FDE76C2F24C3DD89`。
- `CHANGELOG.md` 用中英文按版本记录用户可见变化；0.0.12 条目完整覆盖 `0.0.11 → 0.0.12`，并单列 Minecraft 1.21.1 / 26.1.2 迁移边界与双制品安装规则。

## 安装方法

1. 确认实例是 Minecraft 1.21.1 / NeoForge 21.1.235，或 Minecraft 26.1.2 / NeoForge 26.1.2.94。
2. 1.21.1 客户端与服务端使用 Java 21；26.1.2 使用 Java 25。
3. 从 0.0.12 Release 下载与实例完全匹配的独立 JAR。两个制品不能跨版本使用，也不能同时安装。
4. 删除同一实例中的旧版仙藏 JAR，再把新 JAR 放入客户端或服务端的 `mods` 文件夹。
5. 启动游戏。默认情况下，新玩家首次进入世界会获得一本古玉指导书。

模组列表显示名为 **ImmortalStorage**，中文名为 **仙藏**；管理员命令根节点为 `/immortalstorage`，配置文件为 `immortalstorage-common.toml` 和 `immortalstorage-client.toml`。

安装或更新任何内容模组前，请先备份重要世界。

## 古玉与入门流程

古玉是玩家的主要修行指导书。默认情况下，新角色首次进入世界时会获得一本；也可以使用书、绿宝石、灵晶和仙元进行无序合成。

古玉会根据玩家当前阶段显示：

- 当前最适合完成的修行目标；
- 从启灵到十阶的阶段路线；
- 空窍、仙窍、源方块、机器、灵器、灵剑和个人维度的详细说明；
- 从当前世界配方管理器读取并渲染的真实合成台与烧炼配方；
- 与服务器实际配置一致的十阶仙元、渡劫和边界说明；
- 可搜索的中英文物品、方块与功能章节。

指导书以正常玩家的游玩顺序编写，不是开发计划或功能清单。即使没有安装 JEI/EMI，也应当能够依靠古玉完成基础到后期的正常流程。

古玉只维护一份双语帕秋莉手册。ImmortalStorage 发行 JAR 通过 NeoForge Jar-in-Jar 内置 Patchouli 1.21.1-93，玩家无需单独安装；古玉右键直接打开该手册，不再包含或维护旧独立指导界面。手册提供六类目录、真实配方页面，并覆盖 0.0.3–0.0.11 的主要玩法：新增独立的仙窍管理器、世界碎片开采器/聚宝盆数据包扩展、终端管理页、外部资源与可选联动边界，以及一气归元剑和仙墟锻灵剑的实际淬火系数；同时说明迷你仙墟的 `13×1×13` 单层作用边界与主/副手持有者排除规则，并保留替死傀儡、拘灵器、仙灵驱动器、模拟生产设备、四类稳定化仙墟、源方块/管理器、个人仙窍天气与持久身份迁移说明。中英条目树与后续版本系统覆盖均由自动契约检查。

## 修行阶段

修行从零阶凡人开始，最高正常达到十阶。

- 一至五阶围绕启灵、灵气积累、丹药、真元与逐步扩展的空窍展开。
- 五阶是有限存储的最后阶段，需要满足飞升条件才能进入六阶。
- 五升六时会一次性处理既有真元；六阶以后新获得的真元不会继续被主动转换。
- 六阶开始开放仙窍存储、仙元、个人仙窍维度和后期自动化。
- 六至十阶通过在个人仙窍内完成渡劫进阶。
- 阶段常驻效果会在登录、复活、切换维度和改变游戏模式后重新校准；渡劫期间则会暂时失效。

整合包作者可以配置正常流程最高阶数以及各阶段渡劫敌人的注册 ID。

## 真元与仙元

真元与仙元均为真实物品，而不是只能在界面中查看的隐藏数值，因此其他模组可以通过普通物品能力读取、抽取并用于合成或机器处理。

- 有阶段上限时，限制按玩家身上与个人存储中的总量共同计算。
- 六阶以后真元不再具有上限。
- 十阶默认每 20 tick 生成 256 仙元，存储无上限。
- 配置中可开启十阶无消耗仙元通道；该选项默认关闭。
- 一个仙元可以无序合成 64 个真元。
- 真元和仙元都可以作为普通熔炉燃料。
- 两者都可放置可染色、可隐藏模型的悬浮核心光源；仙元光源还会抑制其当前区块的敌对生物自然生成。

## 空窍与仙窍存储

个人存储通过可自定义快捷键打开。存储快捷键可以覆盖大多数非文本输入界面全局打开，按 Esc 会返回之前的界面。

### 空窍

- 在一至五阶使用。
- 采用随阶段提升的有限物理槽位容量。
- 只存储物品，不支持流体。
- 不支持仙窍管理器等飞升后设备。
- 低阶段会限制空白显示行数，避免小容量存储出现过多空行。

### 仙窍

- 六阶开始使用。
- 按物品身份和完整 Data Components 聚合同类物品。
- 以长整型显示大宗数量，同时保持外部自动化的标准能力语义。
- 物品与流体共用同一存储页面，流体按桶数显示。
- 可以被存储总线、管道与其他标准能力自动化读取。

### 终端功能

- 九列连续存储页面和平滑像素滚动。
- 按本地化名称、Tooltip、标签、命名空间或普通文本搜索。
- 支持按数量、名称和模组 ID 排序。
- 物品数量显示在材质上层，不会被图标遮挡。
- 可直接管理玩家背包、快捷栏与四件真实装备槽。
- 支持鼠标拖动均分等原版容器交互。
- 内置真实 3x3 合成台，材料不足时自动从个人存储补充。
- 可切换是否要求自动补料完全匹配 NBT/Data Components。
- 内置三工作格仙炉，支持自动补入上一组原料并将输出送回存储。
- 磁铁支持开关、黑名单/白名单与掉落物直接入库。
- 手持物品耗尽后可按完整数据组件自动补充一组。
- 在外部资源化学品行上，右键手持已装 Mekanism 化学品容器入库；左键手持空容器从选中的化学品行取出，支持堆叠容器、长数量和服务端事务回滚。

## 个人仙窍维度

每位飞升玩家拥有一个按稳定 UUID 绑定的独立仙窍维度。

| 阶段 | 可用空间 | 可用时间流速 |
| --- | --- | --- |
| 六阶 | 3x3 区块 | 1x |
| 七阶 | 7x7 区块 | 0.5x、1x、2x、4x |
| 八阶 | 19x19 区块 | 0.2x 至 8x 固定档位 |
| 九阶 | 无限 | 0.1x 至 16x 固定档位 |
| 十阶 | 无限 | 0x 冻结至 32x 固定档位 |

六至八阶会显示原版世界边界。玩家越过允许空间时会收到边界提醒，并被送回最近的合法位置。边界检测只在玩家位于自己的仙窍时运行，离开维度后会停止相关检查。

时间倍率只作用于对应仙窍维度。减速或完全冻结仙窍不会影响主世界，也不会阻止玩家正常离开。

仙窍存储的“仙窍管理”页提供并排的时间与天气控制：时间按钮在白天/黑夜间切换，天气按钮按无天气、下雨、雷雨、下雪循环。个人仙窍会持续锁定所选正午/午夜时刻与天气，天空盒会同步切换而不受原版昼夜、天气倒计时或仙窍时间倍率影响；雨雪状态不会修改主世界天气。

个人仙窍支持原版床。右键床会正常睡眠并将该床设置为玩家在其个人仙窍中的重生点，不会触发维度床爆炸；服务器会在需要解析该重生点时恢复对应的动态个人维度。

物品名称颜色采用统一稀有度：基础材料保持普通，中期灵晶、灵器和机器为罕见，高成本仙窍、保命与高级资源设施为稀有，龙蛋/下界之星级无限源和终局仙墟装备为史诗。

## 渡劫

六阶以后的渡劫只能在玩家自己的仙窍中开始。渡劫期间，ImmortalStorage 提供的常驻增益与飞行能力会暂时失效。

默认敌人如下：

| 突破 | 默认敌人 |
| --- | --- |
| 六升七 | 全套附魔下界合金装备的僵尸 |
| 七升八 | 全套附魔下界合金装备的凋零骷髅 |
| 八升九 | 全套附魔下界合金装备、抗性提升 I 的卫道士 |
| 九升十 | 坚守者 |

渡劫目标会永久发光，获得力量 III，并拥有原生基础生命值的十倍。后期渡劫还会对玩家施加失明、凋零等压力效果。

无论游戏模式如何，渡劫期间死亡都会被拦截：召唤物会被清除，玩家保留物品并在原地复活，当前仙元清零，但阶段不会下降。

## 仙炉

仙炉拥有三个并行工作格，既可以作为世界方块放置，也可以作为仙窍终端内置模块使用。

- 支持符合设计范围的原版熔炉与高炉配方。
- 灵铁矿和深层灵铁矿只支持高炉或仙炉烧炼。
- 灵晶矿和深层灵晶矿只支持仙炉烧炼。
- 使用真元时，每个工作格一次烧炼一个物品，耗时 50 tick。
- 使用仙元时，每次烧炼一组物品，耗时 25 tick。
- 自动化从方块上方输入。
- 内置仙炉自动填充会补入完整一组上一轮原料，并将输出直接存入个人存储。
- 仙灵驱动器可绑定玩家并从绑定人的空窍/仙窍支付仙元或真元燃料，仅在真实需要付款且余额不足时进入五 tick 重试。
- JEI 和 EMI 中拥有独立仙炉配方分类和配方转移。

## 模拟灵田（0.0.6）

模拟灵田使用平滑石原材质的 12 棱镂空框架；几何只参考稳定化迷你仙墟，未从哭泣黑曜石采色。内部基底按实际方块渲染，普通土壤显示为湿润耕地，作物模型依 50 tick 处理进度显示实际生长阶段。

- UI 的尺寸、种源/燃料/额外工具位置、12 格产物区和玩家背包布局与模拟轮回炼化炉一致。
- 顶面只输入种子，四个水平侧面只输入真元、仙元或已绑定仙灵驱动器；六个面可分别启用产物抽取，工具槽只允许手动交互。
- 种子作为永久种源，处理永不消耗；每 50 tick 使用成熟作物方块的真实战利品表和工具槽物品生成一次收获，并以 12 格原子输出逻辑避免溢出丢失。
- 紫颂果可作为永久种源，只能配合末地石类基底；内部以居中紫颂花从 0.15 倍持续放大至 0.70 倍显示生长，每轮固定产出 1 个紫颂花和 2 个紫颂果。
- 真元与仙元分别提供与仙炉一致的 150/500 tick 燃烧时间；仙窍内燃料槽为空时直接向该仙窍所有者支付 1 仙元。
- 右上角齿轮展开与模拟轮回炼化炉相同的六面邻接预览、逐面输出、自动输出和经验提取设置；自动输出对象严格按“当前仙窍主人、仙灵驱动器绑定人、本地 12 格产物槽”依次解析。
- 泥土、耕地、通用土壤/耕地标签、末地石和灵魂沙类基底可右键替换；旧基底返还玩家背包。紫颂类作物只接受末地石类基底，地狱疣只接受灵魂沙。
- 数据包可通过 `immortalstorage:simulated_spirit_field_seeds`、`immortalstorage:simulated_spirit_field_substrates` 标签，以及 `data/*/simulated_spirit_field_crops/*.json` 的种子—作物—基底清单扩展模组作物。
- 工作台配方为“灵晶/混元一气/灵晶，灵铁粒/草方块/灵铁粒，灵晶/蕴灵晶/灵晶”，每次合成 4 个；物品稀有度为罕见（UNCOMMON）。
- 2026-08-01 已将 0.0.6 正式制品部署到 30-JAR 全模组 PCL2 配置并用 JDK 21、`zh_cn` 完成 Numen 单人实机验证。

## 回响碎片源方块与鼓风机触媒（0.0.7）

- 新增回响碎片源方块：不可堆叠的源方块，接入源方块管理器聚合与六面推出，附带成就、战利品表与合成配方，可被镐子采集。
- 水仙墟与岩浆仙墟可作为机械动力（Create）鼓风机触媒：洗炼（splashing）与鼓风烧炼（blasting）通过 `create:fan_processing_catalysts/*` 标签启用；两源方块同时注册 `create:fan_transparent`，使鼓风机风能直接穿过源方块到达触媒。

## 稳定化迷你仙墟容器调度（0.0.7）

- 基础稳定化、基础纠缠、高级稳定化与高级纠缠四个容器交互面变体统一采用 2×3 交互面掩码网格：上行 UP/NORTH/DOWN、下行 WEST/SOUTH/EAST，逐面启用并显示半透明白色预览高亮；六格全关 = 该侧完全不交互。
- 每个操作区域内的方块位置 × 每个已启用面 = 一个独立目标，通过 NeoForge 官方 `Capabilities.ItemHandler.BLOCK, pos, face` 面参数访问容器能力。
- 调度器严格只与预览盒内的容器交互，不会越出区域外一格与相邻容器交互；旧存档“任意面”(-1) 配置加载时迁移为全关掩码。
- 纠缠变体支持正常/反向双向：正常侧把区域内的掉落物收集进 54 格缓冲，反向侧把缓冲按频率、范围与过滤器投出到目标容器。

## 源方块与自动化

每个源方块都有独立的长整型真实缓存和独立的六面设置。

- 六个面分别设置关闭、推出或绕过上限推出。
- 每个面拥有独立的每 tick 推出预算和数值输入。
- 关闭只停止主动抽取/推出，不会拒绝存储总线或管道的被动访问。
- 外部抽取优先消耗真实缓存，缓存不足时才进行仙元兑换。
- 流体源方块始终接受六面输入的任意流体并直接销毁。
- 面对只支持整形数量的管道时，对外提供安全饱和的整形视图，内部账本仍保持长整型。

源定义支持通过配置文件扩展。相同资源在同一有效配置中只能存在一个源定义，避免重复定义导致预算与统计冲突。

### 源方块管理器

- 只能放置在所有者自己的仙窍维度内。
- 拥有 72 个单件槽，每格只能放入一个不可堆叠源方块。
- 同名源方块在同一个管理器中只能存在一个。
- 每个成员源仍独立工作和保存缓存，管理器仅聚合其对外取出能力。
- 聚合视图会绑定到仙窍存储统计，使终端和存储总线能读取最大预算与真实缓存。
- 成员源方块物品的持久缓存写在 `block_entity_data` 组件上；管理器会在写入 `CachedUnits` 时确保该组件带合法的方块实体 `id`（`immortalstorage:source_vein`），避免摧毁管理器或保存时触发原版 `Missing id for entity` 序列化错误。
- 0.0.8 改用 `design/SourceVeinController.bbmodel` 设计稿外观：黑色镂空笼子（12 根 Edge 梁，顶/底圆环梁已补齐 `up`/`down` 封口面）静态渲染，内部 8 个 3x3x3 源核小立方按 72 格→8 段阶梯从空（蓝）→用（紫）→满（红）点亮，并作为刚体绕块中心缓慢旋转；手持/物品栏通过方块物品 BEWLR 叠加同一旋转源核（状态读取拆下保留的 `DisplayState`），与世界中渲染一致。

### 仙窍管理器

仙窍管理器把相邻自动化与所有者的个人仙窍存储绑定，对外提供标准物品/流体能力。正式模组不包含任何 Numen、HTTP 或外部调试接口。

### 仙窍接口

仙窍接口可以放置在其他维度，且不能被其他玩家重新认领。

> **0.0.8：** 仙窍接口方块已改用 `design/xianqiao-interface/` 的六面内凹设计稿——12 根深色金属棱边框架住内陷核心（上表面金色、其余五面面板），以 13 元素独立方块模型渲染；渲染采用默认 scale display（忽略设计稿的缩放参数）。设计源文件与接入说明见 `design/xianqiao-interface/README.md` 与 `archive/2026-08-07-immortalstorage-0.0.8-development.md`。

- 配置物品与流体缓存目标。
- 默认每个物品槽上限 128，每个流体槽上限 16 桶，可在配置中调整。
- 支持从 JEI/EMI 拖动设置幽灵目标，并通过小窗口输入缓存数量。
- 空手左键已配置缓存槽会安全返还缓存并清除设置；空手右键打开数量与逐槽六面掩码窗口。
- 每个缓存目标都拥有独立的六面交互掩码。
- 具有主动抽入、主动推出总开关，以及每面的抽入、推出、关闭设置。
- 主动抽入会从开启面的相邻容器提取所有能够存入个人存储的内容，而不是只提取当前配置目标。
- 主动推出会从真实缓存向对应面输出配置资源。
- 安装对应模组后可配置电力、化学品、魔源、魔力和灵魂涌动等额外资源缓存。
- FE、植物魔法 Mana 与新生魔艺魔源均先消耗真实缓存，缓存不足时才按配置原子消耗仙元兑换缺口。

### 高级仙窍接口（0.0.8 开发中）

高级仙窍接口（`advanced_xianqiao_interface`）采用 `design/advanced-xianqiao-interface/` 的空心框架+浮空核心+顶部圆垫设计稿（默认 scale display）。它保留普通仙窍接口的九个混合缓存槽与物品/流体/电力/化学品输入输出能力，但**拒绝接口自身原有的六面主动推拉功能**——六面区域改为配置与高级稳定化迷你仙墟系列一致的容器交互面掩码网格，主动调度改为对配置范围内的容器进行：

- **xyz / +xzy 配置栏**：范围尺寸 `sizeX/Y/Z`（1–13）与偏移 `offsetX/Y/Z`（-13–13）。
- **预览框与面预览渲染与开关**：`preview` 开关在世界上绘制白色选区线框与每个启用面的半透明高亮。
- **轮询相关选项**：周期 `frequency`、启用总开关、访问（轮询跳过/强制轮询）、均分（逐个/按组）、顺序（远处/近处优先），以及通过潜行右键或设置面板按钮切换的**汇入/推出**模式。
- 汇入模式把范围内容器中可抽取的物品、流体、电力与化学品输入所有者的仙窍；推出模式把配置的缓存槽资源按面掩码推入范围内的容器。
- 设置面板（齿轮）不含六面行为设置，六面行为只在主界面六面区域配置。
- 合成：`{灵晶，迷你仙墟，灵晶，回响碎片，仙窍接口，回响碎片，灵晶，村民刷怪蛋，灵晶}` 有序 3×3。

## 灵器

灵器拥有探索、扳手、挖掘、建筑四种模式。

### 探索模式

对空气右键开关自动收纳，启用时显示附魔光效。只有玩家仍手持启用状态的灵器并打开箱子、陷阱箱、木桶、Lootr 页面或已注册战利品容器时，当前打开页面的内容才会事务性转入个人存储；无法容纳的物品保留在原容器中。

### 扳手模式

使用公开扳手语义兼容 ImmortalStorage、通用机械、AE2、RS、机械动力等机器。对属于玩家的 ImmortalStorage 功能方块 Shift 右键可完整拆下并获得掉落物。

### 挖掘模式

拥有下界合金挖掘等级。普通左键挖掘会正常使用铁砧附魔并消耗耐久；六阶以后右键方块可消耗一个仙元执行灵器自带的精准采集，该右键能力不受灵器附魔影响。

### 建筑模式

客户端会显示与服务端实际放置计划一致的轮廓预览，材料可从背包和个人存储共同取用。默认单次上限为 64，可通过配置调整。按住“灵器特殊操作 / 灵剑召来”绑定键并右键，可以无掉落删除计划中的一层；该键默认是反引号键，可在控制设置中修改。每次放置或删除一层消耗一点耐久。

灵器不能在附魔台随机附魔，但可以通过铁砧接受当前下界合金镐允许的附魔，包括其他模组动态加入的附魔。

## 灵剑

灵剑的 Tooltip 与实际攻击共享同一套伤害模型。

- 空手按住“灵器特殊操作 / 灵剑召来”键右键，可从内置仙炉召来正在淬火的灵剑；手持灵剑执行同一操作可将其送回空闲通道。
- 召走灵剑后，其内置仙炉通道会保留并暂停当前淬火进度，自动填充不会占用该空位；送回同一把灵剑后继续原进度。

- 根据阶段获得额外伤害，命中时从背包与个人存储支付对应真元或仙元。
- 余额不足时仍能攻击，但只造成基础武器伤害。
- 不能在附魔台随机附魔，但可通过铁砧接受当前下界合金剑允许的附魔。
- 自带灵气修复，可消耗元气恢复耐久。
- 可在熔炉、高炉、放置仙炉和内置仙炉中反复淬火，每次增加一点淬火点且不给予经验。
- 普通灵剑每一点淬火使伤害提高 1%；一气归元剑为 0%，仙墟锻灵剑为 1.5%；每次命中后淬火点减半并向下取整。
- 当前淬火点与伤害加成会直接显示在 Tooltip 中。

仙墟锻灵剑右键传送目标后会立即清除目标速度，并施加 40 tick（2 秒）的绝对禁锢；期间忽略实体碰撞、挤压和外力，目标被固定在原位。`immortalstorage-common.toml` 与原生模组配置页提供“传送影响其他玩家”开关：默认开启以保持原行为；关闭后范围传送与禁锢只影响非玩家生物。

## 世界碎片开采器与聚宝盆

世界碎片开采器使用与信标相同的完整同种方块底座结构。完整钻石块、下界残骸或紫颂砖底座分别激活主世界、下界和末地模式，底座等级一至四控制工作速度。

默认情况下，开采器会读取当前世界实际生效的矿物生成规则，使数据包和其他模组修改的矿物分布参与权重计算。整合包也可以选择启用固定表覆盖世界读取。在仙窍内工作时，产物直接进入对应玩家存储；在其他维度则进入机器内部一个木桶容量的缓存，缓存满后停机。

聚宝盆与开采器逻辑解耦，双方不存在主动通信。聚宝盆只检查正下方是否存在已激活的世界碎片开采器，并根据观察到的工作模式抽取对应战利品表；它拥有独立缓存，同时可以遮挡信标光束。

## 材料、丹药、效果与村民

- 灵铁、粗灵铁、灵晶、对应矿石、深层矿石和储存块构成基础材料链。
- 灵核堆叠上限为 16，只作为中间合成材料，不再具有饰品功能。
- 所有丹药缩短食用时间、可以无视饱食度食用，并在 Tooltip 中写明效果。
- 破限丹在五阶使用时会将灵气进度补满，但不会自动完成飞升。
- 进阶虚弱与灵气饱和均为正式注册、可见且有独立图标的状态效果。
- 灵气修复为正式附魔，并拥有附魔书形态。
- 仙师孑遗村民职业以仙炉为工作方块并提供修行交易。
- 普通村民交易可以从当前个人存储补充精确要求的付款物品；不会自动用仙元替代真元。

## JEI 与 EMI

JEI 和 EMI 均为可选依赖。安装后支持：

- 在终端条目上使用 R/U 查询配方和用途；
- 材料识别、高亮与缺料提示；
- 终端搜索框与配方查看器搜索同步，并避免循环更新；
- 正确报告终端和侧边面板的界面避让区域；
- 从个人存储、玩家背包和真实合成格进行配方转移；
- 物品与流体条目的点击识别；
- 独立仙炉配方分类与转移处理。

不安装任何查看器可以正常游戏，同时安装两者也不会因可选类加载而崩溃。

## 存储与科技模组联动

普通自动化优先通过 NeoForge 官方物品/流体能力交互。所有可选兼容均放在独立边界中，未安装目标模组时不会静态引用其类型。

0.0.2 使用 ImmortalStorage 自己的共享资源 Key 与长整型权威账本表示 FE、通用机械化学品、Ars Nouveau Source、植物魔法 mana 和工业先锋灵魂。Iron's Spells 玩家魔力明确不接入。该核心接口不依赖 AE2；未安装 AE2 时，仙窍接口、终端和其他已安装模组仍可直接通过各自官方能力读取与写入这些资源。仙窍存储主目录把额外资源与物品、流体放在同一搜索、排序和连续滚动网格中显示。

安装 AE2 后，ImmortalStorage 的可选 AE2 适配层才会把同一共享 Key 包装成 AEKey，供仙窍交流磁盘和 AE2 网络访问。玩家不需要额外安装 Applied Flux、Applied Mekanistics、Ars Énergistique、Applied Botanics、Soulplied Energistics 或其他 AEKey 中介模组。AE2 适配层与无 AE2 路径共用同一份账本，不复制资源数量。

若玩家同时安装上述受支持的 AE2 中间层附属，ImmortalStorage 不注册竞争性的重复目录身份，而是优先映射其现有资源键到同一仙窍账本；缺少附属时才回退到 ImmortalStorage 自有键。两条路径共享去重、revision 和模拟/执行事务，禁止目录双计或互相转换复制。

当前工程包含 AE2、RS 交流存储介质与存储总线读取，以及通用机械能量/化学品、植物魔法魔力、Ars Nouveau Source、通量网络能量、工业先锋灵魂等兼容区域。具体可用功能取决于实际安装版本与服务器配置。

终端内的 Mekanism 化学品容器交互沿用流体/流体容器的容器驱动语义：空容器左键取出当前选中的化学品，已装容器右键把自身化学品存入仙窍；服务端按化学品注册 ID、菜单 revision 和访问阶段校验，并使用模拟、执行、回滚保证容器堆叠与存储账本不会半完成。

仙窍存储界面的玩家背包槽保持为直接绑定原版 `Inventory` 的标准槽，供 R 键整理类辅助模组识别；ImmortalStorage 不监听或占用 R 键。界面在仙窍存储与背包之间的空隙中提供三个紧凑的 8x8 小图标按钮：扳手整理、绿色上箭头全部存入、红色下箭头按当前筛选取出，并保留完整悬停说明。Building Gadgets 2 1.3.9 的复制粘贴小帮手在玩家主手或副手持有时，可把当前空窍/仙窍物品作为材料来源，模拟检查不扣物，正式施工才执行抽取。Create 蓝图大炮可把仙窍管理器当作标准 NeoForge 方块物品仓库读取，不需要 Create 私有 API。

Beyond Dimensions 安装与否都不会停用、替换或迁移 ImmortalStorage 自有空窍/仙窍存储；两套存储保持彼此独立，当前版本不安装外部权威后端路由。

植物魔法联动通过在仙窍接口上放置真实火花实现：接口向 Botania 注册官方 `SparkAttachable` 与 `ManaReceiver`，火花实体和强化负责网络流向，魔力直接进入或离开仙窍接口的共享魔力缓存。Ars Nouveau Source 同样通过官方的按位置 Source Provider 直接读写接口魔源缓存。这两类由方块本身直接完成、没有物理面参数的交互不受接口六面模式、主动推拉总开关或逐槽面掩码影响；即使六面全部关闭也保持可用。六面配置只约束物品、流体、FE、化学品等明确携带方向的能力。

本次重发的 Release 0.0.10 声明 Minecraft 1.21.1 / NeoForge `[21.1.235,21.2)` 的发行适配，构建基线为 NeoForge 21.1.235；21.1.236、21.1.248 源码门禁和 Sodium/Embeddium 客户端启动烟测均已完成。

## 游戏内配置

在主菜单或暂停菜单中打开 **模组 -> ImmortalStorage -> 配置**，即可使用 NeoForge 原生配置界面。所有现有分组、次级菜单、配置名称和 Tooltip 均已注册自然中文与英文翻译。

主要配置包括：

- 新玩家是否自带古玉；
- 古玉、丹药等战利品注入概率与数量；
- 正常流程最高可达阶数；
- 十阶是否启用无消耗无限仙元通道；
- 各阶段渡劫敌人注册 ID；
- 源方块认领、破坏与爆炸权限；
- 仙窍接口物品/流体缓存上限；
- 灵器建筑模式单次方块上限。
- 仙墟锻灵剑的传送与两秒绝对禁锢是否影响其他玩家。
- 安装通用机械/通量网络、植物魔法或新生魔艺后，配置页会分别显示仙元转化 FE、Mana、魔源的允许开关、比例和每刻上限；同一设置也写入 `immortalstorage-common.toml`，默认开启。

调整阶段、资源或兼容配置前，建议关闭服务器并备份世界。

## 快捷键与管理员指令

所有 ImmortalStorage 快捷键都可以在 Minecraft 控制设置中重新绑定。存储按键可以在大多数非文本界面上全局使用，其他模式按键保持在游戏场景内生效，避免抢占菜单输入。

需要权限等级 2 的管理员指令：

```text
/immortalstorage stage <0..10> [玩家]
/immortalstorage unload [玩家]
/immortalstorage reload [玩家]
/immortalstorage speed <固定档位>x [玩家]
```

不填写可选玩家参数时，默认作用于指令执行者。

## 性能与安全边界

- 存储目录使用稳定条目 ID、revision 校验、紧凑同步和仅视口渲染。
- 连续滚动不降低动画帧数，只卸载视野外行与不可见渲染内容。
- 高仙窍时间倍率下，单纯数量变化不会使终端物品无法点击。
- 长整型资源对只支持整形的 API 提供安全饱和值。
- 存储操作、合成补料、源兑换、接口传输和仙炉处理均由服务端权威事务执行。
- 可选模组兼容按安装状态隔离，专用服务端不会加载客户端查看器类。
- 源方块恢复 0.0.1 的原生方块物品模型；NeoForge `standalone` 只旁加载同一基础模型，自定义渲染器不再重复应用 GUI 变换或居中。产物角标与本体仍在同一次标准物品渲染中完成，并先于数量文字绘制。
- 正式 ImmortalStorage JAR 不包含 Numen、MCP、HTTP、Bearer Token 或外部调试端点；Numen 仅用于外部实机测试。

## 从源码构建

克隆仓库后，在 Windows PowerShell 中执行：

```powershell
cd project/neoforge-1.21.1-mdk
$env:JAVA_HOME = "C:\path\to\jdk-21"
.\gradlew.bat clean build --no-daemon --max-workers 1 --console=plain
```

发行文件生成在：

```text
project/neoforge-1.21.1-mdk/build/libs/immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.10.jar
```

完整验证命令：

```powershell
.\gradlew.bat test build verifyArsSourceAdapter verifyWithoutAe2Runtime verifyProductionJarBoundary verifyVersionComposition verifyVersionArtifact --no-daemon --max-workers 1 --console=plain
```

0.0.9 正式制品已通过 **717 项测试（196 suites / 0 failures/errors/skips）**、生产边界、版本组成与精确制品检查；统一持久身份、旧 UUID 一次性迁移、AE2/RS 磁盘、RS 无附属额外资源显示、RS 网格 Mapper、Refined Types/官方 RS Mekanism Integration 原生键退让、仙灵驱动器、替死傀儡、绑定机器、AE2 `Missing render handler` 崩溃条件及古玉手册双语完整性均有回归覆盖。此前 SHA-256 为 `BE3F8C2A5283166513CA178DE5A9186651C3780C9DCD358B416BA849FE4C1228` 的实机验证制品已部署到 30-JAR 全模组 PCL2 实例；Numen 在绑定旧交换磁盘上实机打开 RS `CraftingGridScreen`，确认化学品与仙藏一致仅显示注册表取样纯色，非纯色资源继续使用目录纹理，且 `No factory`、高级屏幕处理失败与外部资源纹理加载失败均为 0。包含完整手册的最终发布 JAR SHA-256 为 `79EECB2B262C0FCC27C0AA0CC7A67BE89A79458D03A8F8C426F98ED94B74B870`。详细记录见 `archive/2026-08-08-rs-native-fallback-rendering.md`。

0.0.10 重发制品已通过 **199 suites / 724 项测试 / 0 failures/errors/skips**、Ars Source API、无 AE2 运行时、生产边界、版本组成与精确版本产物检查。生成的 `immortalstorage-neoforge-mc1.21.1-nf21.1.235-0.0.10.jar` 为 5,163,055 字节，SHA-256 为 `EA09A8493367E4E05A4C04D520FCB6E74EBF6409DC103E5BE0A4AE2ACD6564B4`；同时完成 NeoForge 21.1.236/21.1.248 源码验证与 Sodium/Embeddium 优化栈客户端启动烟测。本次变更覆盖剑系淬火系数、最多两位小数的淬火百分比 Tooltip、双语古玉手册条目树契约、Mekanism 化学品容器交互、仙窍管理时间流速居中排版及文字重影修复。该制品作为 GitHub Release 0.0.10 的唯一 JAR 资产，并已与 PCL2 `1.21.1-NeoForge_21.1.235` 实例中的同名 JAR 完全一致。

## 仓库结构

```text
.
├── README.md                 中文默认介绍
├── README_en.md              English documentation
├── CHANGELOG.md              版本变更记录
├── LICENSE                   发行许可证
├── design/
│   ├── xianqiao-interface/       六面内凹仙窍接口美化设计稿（已接入 0.0.8 生产模型）
│   ├── advanced-xianqiao-interface/  高级仙窍接口设计稿（空心框架 + 边框材质，已接入 0.0.8）
│   ├── SourceVeinController.bbmodel  源方块管理器控制器设计稿（黑框笼 + 8 核旋转动效，已接入 0.0.8）
│   └── swords/                   灵剑/仙墟锻灵剑/一气归元剑 剑系美化设计稿（已接入 0.0.8 材质）
└── project/
    └── neoforge-1.21.1-mdk/
        ├── build.gradle
        ├── gradle.properties
        ├── gradle/
        └── src/
            ├── main/java/       模组实现
            ├── main/resources/  材质、模型、语言、配方、标签与数据
            └── test/java/       自动化行为与发行边界测试
```

参考模组、第三方提取源码、PCL2 测试实例、Numen 调试桥、日志、截图、归档和构建 JAR 均不会进入 Git 历史。编译后的正式版本只通过 GitHub Releases 分发。

## Bug 反馈

提交问题时，请附上：

- Minecraft、NeoForge、Java 与 ImmortalStorage 的准确版本；
- 问题发生于客户端、单人整合服务端还是专用服务端；
- 已安装联动模组及其准确版本；
- 最小复现步骤；
- 与问题相关的 `latest.log` 片段，请移除账号 Token 和其他隐私信息。

## 许可证

ImmortalStorage 使用仓库中的 All Rights Reserved 许可证。允许个人游玩与私人整合包使用；重新分发、商业使用、修改或发布衍生版本需要事先获得书面许可。

Minecraft 是 Mojang Studios 的商标。本项目为独立模组，与 Mojang Studios 或 Microsoft 无隶属或授权关系。

## 26.1.2 迁移验证状态（2026-08-12）

0.0.11 的 NeoForge 26.1.2.94 迁移版已完成固定档位 UI 生命周期适配：容器背景、控件、标签和槽位只由 26.1.2 提取器处理一次；工作方块使用 `TRY_WITH_EMPTY_HAND` 继续空手交互，因此手持普通物品时仍可打开机器 UI。方块物品通过新版官方 `useBlockDescriptionPrefix()` 复用双语 `block.*` 名称。仙窍时间流速会在玩家 post-tick 稳定阶段重新对账且不会重置相同倍率的小数 tick 累积。Patchouli 26.1-94 与 1.21.1 一样作为 required Jar-in-Jar 内置，古玉继续直接打开同一套双语手册。

## 0.0.11 双版本视觉修复状态（2026-08-13）

聚宝盆现按原版炼药锅的 13 个构件与 UV 分区等比缩放为底面居中的 `10×10×10` 三维模型，四张金色贴图不变；物品模型独立补齐标准方块 GUI、地面、固定与双手显示变换，避免原版炼药锅独立模型缺少 `block/block` 变换时出现的手持/物品栏尺寸错误。普通与高级仙窍接口在两代均声明为非遮挡方块，配合无环境遮蔽、无面裁剪模型，修复邻接完整方块时浮空接触面发黑。JEI 搜索仅在仙窍存储页保留并与内置搜索同步，其余仙藏容器通过 JEI GUI exclusion 隐藏搜索覆盖。

最终 1.21.1 制品通过 772 项测试，SHA-256 为 `E8AE6A3889480E9A92440C1D12322DFF6EE6B7DC7B059F01AF1BFC05963964B8`；26.1.2 制品通过 750 项测试，SHA-256 为 `2140A7C5EEE90A7065E285D1815DB80D4FF5C0157932861C98C5C7A522F11BD2`。两者均已替换对应 PCL2 实例，26.1.2 另同步到全局 `mods`。

## 0.0.11 双版本存储总线与碰撞修复（2026-08-13）

仙窍管理器现向 AE2 存储总线注册原生 `ME_STORAGE`，直接复用交换磁盘已经验证的长整型统一后端，因此物品、流体、FE 及其他已注册额外资源使用同一份仙窍账本；原有网格服务继续在同所有者交换磁盘与管理器总线并存时执行去重。RS 通过官方 `ExternalStorageProviderFactory` 增加管理器额外资源提供器，只发布 FE、魔力、魔源等额外资源，物品与流体仍由 RS 自带能力提供器读取，避免同一总线重复统计。聚宝盆的选择箱与实体碰撞箱同步为底部居中的 `x/z=3..13, y=0..10`，与现有 `10×10×10` 模型边界一致。

本轮 1.21.1 制品通过 773 项测试，SHA-256 为 `B3CEE5E93544BA91A774B3E37026E9C14108AB56A60A03AC33E155EA7C617455`；26.1.2 制品通过 751 项测试，SHA-256 为 `F3DC30C1ABEF8378D898E06B20199CFE67F8088DB79E3747E78495C11D70D890`。两代对应 PCL2 实例均已替换，26.1.2 同步替换全局 `mods`。
