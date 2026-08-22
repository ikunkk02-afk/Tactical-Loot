# Tactical Pickup

Tactical Pickup 是一个面向 Minecraft 1.21.1 Fabric 的早期开发模组，使用 Java 21 和 Mojang Official Mappings。

## 环境与依赖

- Minecraft 1.21.1
- Fabric Loader 0.19.3
- Java 21
- Fabric API 0.116.15+1.21.1
- Cloth Config API 15.0.140
- Mod Menu 11.0.4（可选；未安装时不影响模组运行）

Fabric API 和 Cloth Config API 是运行必需依赖。当前阶段已经接入 Cloth Config，但尚未提供完整配置界面。

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

**当前仍处于早期开发阶段。** 当前版本尚不包含过滤器、大型战利品界面或 Forge/NeoForge 支持。

## License

MIT
