# Tactical Loot（战术拾取）

This branch contains the NeoForge 1.21.1 version of Tactical Loot.

Tactical Loot 重新设计了 Minecraft 地面掉落物的拾取与管理流程。附近存在掉落物时，世界 HUD 会显示物品图标、名称、数量与操作提示；大型战术拾取界面则可在附近战利品和玩家背包之间进行精确管理。所有真实拾取、指定槽位放置和丢弃操作均由服务端验证并执行。

## 功能

- 检测并聚合玩家 5 格范围内的地面掉落物，HUD 最多显示 6 组
- 使用滚轮选择战利品，按 1 或 16 调整拾取数量
- 深色 Vanilla+ 战术拾取界面，左侧背包、右侧附近战利品
- 按本地化名称或物品 ID 搜索，并支持物品优先级与隐藏过滤
- `Shift + 鼠标右键` 快速拾取整组物品
- 将地面战利品拖入指定背包槽位，支持同组件物品合并和部分堆叠
- 将背包物品拖到战利品区域，由服务端生成地面物品
- 基于 elapsed time 的 Fade、Slide、Scale、Hover、Press、Selection 与传输反馈动画
- 独立调整 HUD 和战术拾取界面的位置、大小，支持 Reset Current / Reset All
- 中文与英文界面

## 操作方式

| 操作 | 默认输入 |
| --- | --- |
| 进入拾取模式 / 确认拾取 | `F`（复用原版副手键） |
| 切换战利品 | 拾取模式中滚轮 |
| 调整拾取数量 | `Shift + 滚轮` |
| 按 16 调整拾取数量 | `Ctrl + Shift + 滚轮` |
| 打开战术拾取界面 | `H` |
| 快速拾取整组战利品 | `Shift + 鼠标右键` |
| 打开设置 | `U` |
| 打开过滤管理 | `O` |
| 切换当前物品过滤状态 | `X` |

所有自定义按键均可在控制设置的 Tactical Loot 分类中重新绑定。也可从 NeoForge Mods 页面点击 Tactical Loot 的配置按钮打开现有设置界面。

## 安装要求

- Minecraft 1.21.1
- NeoForge 21.1.248 或兼容的 21.1.x 版本
- Java 21

将 `TacticalLoot-1.0.1-neoforge-1.21.1.jar` 放入游戏实例的 `mods` 目录。多人游戏中，客户端和服务端都必须安装版本一致的 Tactical Loot。

客户端 JSON 配置保存在 NeoForge 配置目录中的 `tactical-loot-client.json` 与 `tactical_pickup_filters.json`。配置缺失、损坏或越界时会安全回退到修正后的默认值。

## 构建与开发运行

```powershell
.\gradlew.bat clean build
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runGameTestServer
```

正式安装文件输出到 `build/libs/TacticalLoot-1.0.1-neoforge-1.21.1.jar`。项目使用 NeoForge ModDevGradle 2.0.144、Mojmap 与 Parchment 2024.11.17 mappings。

## 注意事项

- 当前分支仅对应 Minecraft 1.21.1 NeoForge；Mod ID 保持为 `tactical_pickup`。
- 模组会阻止玩家通过碰撞自动吸取掉落物；水流、漏斗和物品寿命仍由 Minecraft 处理。
- 被隐藏或降低优先级的物品不会从世界中删除。
- 问题反馈：[GitHub Issues](https://github.com/ikunkk02-afk/Tactical-Loot/issues)
