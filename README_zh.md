ManHunt 插件
=============

一个轻量级的 Manhunt（猎人 vs 速通者）小游戏插件，适用于 Spigot 服务端。

插件信息
--------
- 名称: ManHunt
- 版本: 1.0-SNAPSHOT
- 主类: top.mcmohen.manHunt.ManHunt

插件介绍
--------
ManHunt 提供一个简易的 Manhunt 风格小游戏，玩家可以加入猎人（hunter）、速通者（speedrunner）或观众（audience）队伍。猎人可以获得指向速通者的指南针，插件也提供若干游戏规则的查看与修改功能。

适用版本
--------
- Spigot 服务端：1.19.4+

安装说明
--------
1. 构建插件（会在 `target/` 目录生成 ManHunt-1.0-SNAPSHOT.jar）。
2. 将生成的 jar 复制到服务器的 `plugins/` 目录并重启服务器。

或者
1. 下载最新的发布版本 jar 文件。
2. 将 jar 文件复制到服务器的 `plugins/` 目录并重启服务器。

指令用法
--------
所有主指令为 `/manhunt`（别名 `/mh`）。另外还有一个 `/test` 用于调试。

- /manhunt help
  - 显示帮助信息与可用子指令列表。
  - 示例：`/manhunt help`

- /manhunt join <team>
  - 加入指定队伍。可选队伍：`hunter`、`speedrunner`、`audience`。
  - 示例：`/manhunt join hunter`
  - 注意：此命令只可由玩家执行（不可由控制台执行）。

- /manhunt leave
  - 离开当前队伍并加入观众队伍。
  - 示例：`/manhunt leave`

- /manhunt start
  - 尝试开始游戏（进行必要检查并广播开始或失败信息）。
  - 示例：`/manhunt start`

- /manhunt give <item>
  - 给玩家特殊物品（当前支持：`compass`）。
  - 示例：`/manhunt give compass`（仅玩家可用）

- /manhunt stop
  - 投票结束当前游戏（仅游戏内玩家可以投票）。
  - 示例：`/manhunt stop`

- /manhunt rule
  - 显示规则相关帮助。
  - 示例：`/manhunt rule`

- /manhunt rule <ruleKey>
  - 显示指定规则的信息及当前值。
  - 可用规则键：
    - `hunter_ready_cd`（猎人准备倒计时，单位为秒）
    - `hunter_respawn_cd`（猎人重生冷却）
    - `friendly_fire`（是否允许友伤）
  - 示例：`/manhunt rule hunter_ready_cd`

- /manhunt rule <ruleKey> <value>
  - 为指定规则设置新值（插件会进行验证）。
  - 示例：`/manhunt rule hunter_ready_cd 10`

- /test
  - 一个用于测试的简单命令，会更改规则并刷新规则面板，用于调试。
  - 示例：`/test`

备注与限制
-----------
- 某些命令只能由游戏内玩家执行（控制台无法执行）。
- 部分操作会受当前游戏阶段限制（例如：当游戏正在准备/已开始时无法加入队伍）。

鸣谢
----
鸣谢与灵感来源：【[Minecraft开发] 猎人游戏 Paper 插件】
https://www.bilibili.com/video/BV1fG9TYxEwq/?p=8&share_source=copy_web&vd_source=a3deb786fffe446e9822d1a1ea99e345


