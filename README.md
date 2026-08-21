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

## 第一阶段功能

- 阻止玩家通过碰撞自动吸取地面物品，不影响物品物理、寿命、漏斗或其他运输方式
- 在玩家附近显示独立 ItemEntity 战利品 HUD
- 附近有物品时按 F 进入拾取模式，并屏蔽本次原版副手交换
- 拾取模式中使用滚轮循环选择物品，不改变快捷栏槽位
- 按 F 向服务端请求拾取当前选择的真实 ItemEntity
- 服务端重新验证实体、距离、拾取延迟和背包空间，并复用原版插入逻辑
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

**当前仍处于早期开发阶段。** 第一阶段不包含同类物品聚合、指定数量拾取、过滤器、大型战利品界面或 Forge/NeoForge 支持。

## License

MIT
