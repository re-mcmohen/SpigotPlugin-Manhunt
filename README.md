ManHunt
=======

A lightweight Manhunt (hunter vs speedrunner) minigame plugin for Spigot.

Plugin
------
- Name: ManHunt
- Version: 1.0-SNAPSHOT
- Main class: top.mcmohen.manHunt.ManHunt

Description
-----------
ManHunt provides a small Manhunt-style minigame where players can join as hunters, speedrunners or audience. Hunters can receive a tracking compass and players can adjust a few game rules.

Notice: This plugin is written in Chinese, so some messages and commands may contain Chinese text (sure your can translate it and build your own version xD)

Supported versions
------------------
- Spigot server: 1.19.4+

Installation
------------
1. Build the plugin (produces ManHunt-1.0-SNAPSHOT.jar in the `target/` folder).
2. Copy the jar to your server's `plugins/` folder and restart the server.

Or
1. Download the latest release jar file.
2. Copy the jar to your server's `plugins/` folder and restart the server.

Commands
--------
All commands are rooted at `/manhunt` (alias `/mh`). There is also a `/test` command for quick rule testing.

- /manhunt help
  - Show help messages and a quick list of available subcommands.
  - Example: `/manhunt help`

- /manhunt join <team>
  - Join a team. Valid teams: `hunter`, `speedrunner`, `audience`.
  - Example: `/manhunt join hunter`
  - Note: Only players can execute this command.

- /manhunt leave
  - Leave your current team and join the audience.
  - Example: `/manhunt leave`

- /manhunt start
  - Attempt to start the game (runs checks and announces start or failure).
  - Example: `/manhunt start`

- /manhunt give <item>
  - Give a special item to the player (currently supported: `compass`).
  - Example: `/manhunt give compass` (only usable by players)

- /manhunt stop
  - Vote to stop the running game (only players can vote).
  - Example: `/manhunt stop`

- /manhunt rule
  - Show rule-related help.
  - Example: `/manhunt rule`

- /manhunt rule <ruleKey>
  - Show information and current value for a rule.
  - Available rule keys:
    - `hunter_ready_cd` (hunter ready countdown in seconds)
    - `hunter_respawn_cd` (hunter respawn cooldown)
    - `friendly_fire` (enable/disable friendly fire)
  - Example: `/manhunt rule hunter_ready_cd`

- /manhunt rule <ruleKey> <value>
  - Set a new value for the specified rule (validated by the plugin).
  - Example: `/manhunt rule hunter_ready_cd 10`

- /test
  - A simple test command that changes a rule and refreshes the rule board. Meant for debugging.
  - Example: `/test`

Notes and limitations
---------------------
- Some commands are only available to in-game players (not the console).
- Some actions are restricted by the current game stage (for example, joining is blocked when the game is already preparing/started).

More / Acknowledgements
-----------------------
Thanks and inspiration: 【[Minecraft开发] 猎人游戏 Paper 插件】
https://www.bilibili.com/video/BV1fG9TYxEwq/?p=8&share_source=copy_web&vd_source=a3deb786fffe446e9822d1a1ea99e345
