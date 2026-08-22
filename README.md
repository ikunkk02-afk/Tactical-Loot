# Tactical Pickup

Tactical Pickup 是一个面向 Minecraft 1.21.1 Fabric 的早期开发模组，使用 Java 21 和 Mojang Official Mappings。

## 环境与依赖

- Minecraft 1.21.1
- Fabric Loader 0.19.3
- Java 21
- Fabric API 0.116.15+1.21.1
- Cloth Config API 15.0.140
- Mod Menu 11.0.4（可选；未安装时不影响模组运行）

Fabric API 和 Cloth Config API 是运行必需依赖。过滤管理使用轻量自定义客户端界面；Cloth Config 保留给后续一般设置。Mod Menu 只提供可选入口，不是必装前置。

## 当前功能

- 阻止玩家通过碰撞自动吸取地面物品，不影响物品物理、寿命、漏斗或其他运输方式
- 按 Vanilla 的 Item + Data Components 语义聚合附近相同掉落物；Stack Count 不参与分组身份
- HUD 按 Loot Group 显示真实物品名称、图标和附近总数量，数量可以超过单个物品栈上限
- 选中附魔物品时显示最多 5 条 Vanilla 本地化附魔详情，并支持附魔书的 Stored Enchantments
- 附近有物品时按 F 进入拾取模式，并屏蔽本次原版副手交换
- 拾取模式中使用滚轮按 Group 循环选择，不改变快捷栏槽位
- 每个新 Group 默认拾取全部；Shift + 滚轮按 1 调整数量，Ctrl + Shift + 滚轮按 16 快速调整
- 按 F 拾取选中数量；客户端只发送代表 ItemEntity ID 和数量上限，ALL 使用明确的 0 协议值
- 服务端根据真实代表 Stack 重新扫描并验证 Group，不信任客户端缓存、请求数量或成员列表
- 每个成员分别验证距离、owner 和 pickup delay，并复用安全库存事务逐个拾取
- 指定数量可以跨多个 ItemEntity；背包容量不足时只扣除实际插入数量，继续保证物品守恒
- 按 ESC 退出拾取模式；再次按 ESC 恢复原版暂停行为
- 受到实际伤害、死亡、离开范围、切换维度或断开连接时退出并清理状态
- 所有物品默认是 Normal；拾取模式中按 X 可按 Normal → Low Priority → Hidden → Normal 循环当前物品的过滤状态
- Low Priority 仍显示、可选择、可调整数量并正常拾取，但始终排在所有 Normal Group 后，并在 HUD 中使用较弱提示
- Hidden 完全不进入 Tactical Pickup HUD、滚轮选择和 F 上下文；若附近只有 Hidden 物品，F 保持原版主副手交换
- 过滤身份是 Item Registry ID，而不是 LootGroupKey；普通、附魔和不同耐久度的同种物品共享过滤状态
- 按 O 打开过滤管理；空搜索管理已有规则，输入名称或 Item ID 可查找已安装物品并直接降低优先级或屏蔽，也可恢复 Hidden 项和确认后全部恢复 Normal
- 过滤规则持久化在 `config/tactical_pickup_filters.json`，跨世界和服务器沿用；移除模组后其 Item ID 规则仍会保留
- Hidden 只影响 Tactical Pickup 的客户端显示与选择，不删除 ItemEntity，也不干涉水流、漏斗、五分钟寿命或其他模组系统
- 安装 Mod Menu 后可从 `Mods → Tactical Pickup → Config` 打开同一过滤管理界面；不安装时 O 入口及全部功能仍可用

## 构建

```powershell
gradlew.bat clean build
```

开发客户端和专用服务端可分别使用：

```powershell
gradlew.bat runClient
gradlew.bat runServer --args="nogui"
```

## 开发状态

**当前仍处于早期开发阶段。** 当前版本尚不包含大型战利品界面或 Forge/NeoForge 支持。

## License

MIT
