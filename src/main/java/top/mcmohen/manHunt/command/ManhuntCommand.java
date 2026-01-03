package top.mcmohen.manHunt.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import top.mcmohen.manHunt.Console;
import top.mcmohen.manHunt.ManHunt;
import top.mcmohen.manHunt.rule.RuleKey;

import java.util.List;

public class ManhuntCommand implements CommandExecutor, TabCompleter {
    private Console console;

    public ManhuntCommand(Console console) {
        this.console = console;
    }

    private List<String> helpMessage = List.of(
            ChatColor.GREEN + "ManHunt version: " + (String) ManHunt.getInstance().getDescription().getVersion(),
            ChatColor.GOLD + "/manhunt help " + ChatColor.WHITE + "- 显示帮助信息",
            ChatColor.GOLD + "/manhunt join (hunter/speedrunner/audience) " + ChatColor.WHITE + "- 加入指定队伍",
            ChatColor.GOLD + "/manhunt rule <ruleKey> " + ChatColor.WHITE + "- 查看或设置游戏规则",
            ChatColor.GOLD + "/manhunt leave " + ChatColor.WHITE + "- 离开当前队伍，加入观众队伍",
            ChatColor.GOLD + "/manhunt start " + ChatColor.WHITE + "- 在准备阶段开始游戏",
            ChatColor.GOLD + "/manhunt give <item> " + ChatColor.WHITE + "- 获取一个物品，目前只能获取compass（即猎人指南针，仅限游戏已开始且身份为猎人）",
            ChatColor.GOLD + "/manhunt stop " + ChatColor.WHITE + "- 发起提前结束游戏的投票，只能由游戏玩家执行，即未被淘汰的参与者"

            // TODO: 添加更多帮助信息
    );

    private List<String> ruleHelpMessage = List.of(
            ChatColor.GOLD + "/manhunt rule " + ChatColor.WHITE + "- 显示规则帮助信息",
            ChatColor.GOLD + "/manhunt rule <ruleKey> " + ChatColor.WHITE + "- 查看指定规则的当前值",
            ChatColor.GOLD + "/manhunt rule <ruleKey> <value> " + ChatColor.WHITE + "- 设置指定规则的新值"
    );

    private List<String> rules = List.of(
            "hunter_ready_cd",
            "hunter_respawn_cd",
            "friendly_fire"
    );

    private List<String> items = List.of(
            "compass"
    );

    /**
     * 处理 /manhunt 命令
     */
    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        handleCommand(sender, args, true);
        return true;
    }

    /**
     * 处理 /manhunt 命令的自动补全
     */
    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return handleCommand(sender, args, false);
    }

    /**
     * 执行或补全命令
     * @param sender 命令发送者
     * @param args 命令参数
     * @param flag 是否为执行(true表示执行，false表示补全)
     * @return
     */
    private List<String> handleCommand(org.bukkit.command.CommandSender sender, String[] args, boolean flag) {
        // 没有参数：执行时显示帮助，补全时返回子命令列表
        if (args == null || args.length == 0) {
            if (flag) {
                sendHelp(sender);
                return null;
            } else {
                return List.of("help", "join", "rule", "leave", "start", "give", "stop");
            }
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "help":
                return onHelp(sender, flag);
            case "join":
                return onJoin(sender, args, flag);
            case "rule":
                return onRule(sender, args, flag);
            case "leave":
                return onLeave(sender, flag);
            case "start":
                return onStart(sender, flag);
            case "give":
                return onGive(sender, args, flag);
            case "stop":
                return onStop(sender, flag);
            default:
                if (flag) {
                    sender.sendMessage("§e未知的子命令。使用 /manhunt help 获取帮助。");
                    return null;
                } else {
                    // 补全：当在第一个参数时，根据已输入前缀过滤子命令
                    if (args.length == 1) {
                        List<String> candidates = new java.util.ArrayList<>();
                        for (String s : List.of("help", "join", "rule", "leave", "start", "give", "stop")) {
                            if (s.startsWith(sub)) candidates.add(s);
                        }
                        return candidates;
                    }
                    return null;
                }
        }

    }

    /**
     * 发送帮助信息
     * @param sender
     */
    private void sendHelp(CommandSender sender) {
        helpMessage.forEach(message -> sender.sendMessage(message));
    }

    /**
     * 发送关于Rule子命令的帮助信息
     */
    private void senderHelpRule(CommandSender sender) { ruleHelpMessage.forEach(message -> sender.sendMessage(message)); }

    /**
     * Help子命令
     */
    private List<String> onHelp(CommandSender sender, boolean flag) {
        if (flag)
            sendHelp(sender);
        return null;
    }

    /**
     * Join子命令
     */
    private List<String> onJoin(CommandSender sender, String[] args, boolean flag) {
        if (console.getStage() != Console.GameStage.PREPARING && console.getBeginningCountdownTask() != null) {
            if (flag) {
                sender.sendMessage("§c只能在准备阶段加入队伍，现在无法加入！");
            }
            return null;
        }
        // args[0] = "join" 这是必然的，接下来判断 args[1]
        if (args.length == 1) {
            if (flag) {
                sender.sendMessage("§e请指定要加入的队伍：hunter, speedrunner, audience");
            }
            return null;
        }

        String teamName = args[1].toLowerCase();
        if (flag) {
            if (!(sender instanceof Player)){
                sender.sendMessage("命令执行者不是玩家，无法加入队伍！");
                return null;
            }
        }
        if (flag){
            switch (teamName) {
                case "hunter":
                    console.joinHunter((Player) sender);
                    return null;
                case "speedrunner":
                    console.joinSpeedrunner((Player) sender);
                    return null;
                case "audience":
                    console.joinAudience((Player) sender);
                    return null;
                default:
                    sender.sendMessage("§e未知的队伍名称！请使用：hunter, speedrunner, audience");
                    return null;
            }
        } else {
            if (args.length == 2) {
                return List.of("hunter", "speedrunner", "audience");
            } else {
                return null;
            }
        }

    }

    /**
     * Rule子命令, 查看或修改一项游戏设置
     */
    private List<String> onRule(CommandSender sender, String[] args, boolean flag) {
        // 此时args[0] = “rule”，进入rule子命令
        if (args.length == 1) {
            // "/mh rule"
            if (flag) {
                // 发送关于Rule的帮助信息
                senderHelpRule(sender);
            }
            return null;
        }

        // 此时args.length != 1, 说明用户输入了规则键
        String ruleKey = args[1];
        switch (ruleKey) {
            case "hunter_ready_cd":
                return getOrSetRule(sender, args, flag, RuleKey.Hunter_Ready_CD);
            case "hunter_respawn_cd":
                return getOrSetRule(sender, args, flag, RuleKey.Hunter_Respawn_CD);
            case "friendly_fire":
                return getOrSetRule(sender, args, flag, RuleKey.Friendly_Fire);
            default:
                if (flag) {
                    // "/mh rule fr"类似这样的
                    sender.sendMessage("§e未知的规则项：" + ruleKey);
                    return null;
                } else {
                    // 补全
                    if (args.length == 2) {
                        // "/mh rule fr"类似这样的
                        List<String> candidates = new java.util.ArrayList<>();
                        for (String s : rules) {
                            if (s.startsWith(ruleKey)) candidates.add(s);
                        }
                        return candidates;
                    }
                    return null;
                }
        }
    }

    /**
     * Leave子命令 - 离开某一队伍
     */
    private List<String> onLeave(CommandSender sender, boolean flag) {
        if (flag) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("命令执行者不是玩家，无法离开队伍！");
                return null;
            } else {
                console.joinAudience((Player) sender);
                return null;
            }
        }
        return null;
    }

    /**
     * Start子命令 - 在准备阶段开始游戏
     */
    private List<String> onStart(CommandSender sender, boolean flag) {
        if (flag) {
            String result = console.tryStart();
            if (result.isEmpty()) {
                // 执行成功，准备开始游戏
                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.sendMessage("§a游戏即将开始，请做好准备！");
                });
            } else {
                // 执行失败，给所有玩家发送消息
                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.sendMessage("start 失败，原因：" + ChatColor.RED + result);
                });
            }
        }
        return null;
    }

    /**
     * Give子命令 - 给予玩家特殊物品
     */
    private List<String> onGive(CommandSender sender, String[] args, boolean flag) {
        // args[0] == "give"
        if (args.length == 1) {
            if (flag) {
                sender.sendMessage("缺少参数");
            }
            return null;
        }

        // 具有第二个参数
        String item = args[1];
        if (flag) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "该命令只能由玩家执行！");
                return null;
            }

            switch (item) {
                case "compass":
                    console.giveCompassToHunter((Player) sender);
                    return null;
                default:
                    sender.sendMessage(ChatColor.RED + "你想获取的物品不存在或不合法，目前支持的物品: ");
                    for (String legal_item : items) {
                        sender.sendMessage(ChatColor.WHITE + " - " + ChatColor.GOLD + legal_item);
                    }
                    return null;
            }

        } else {
            if (args.length == 2) {
                // "/mh give comp"类似这样的
                List<String> candidates = new java.util.ArrayList<>();
                for (String s : items) {
                    if (s.startsWith(item)) candidates.add(s);
                }
                return candidates;
            }
            return null;
        }
    }

    /**
     * Stop子命令 - 投票终止游戏
     */
    private List<String> onStop(CommandSender sender, boolean flag) {
        if (flag) {
            if (sender instanceof Player) {
                console.voteForStop((Player) sender);
            } else {
                sender.sendMessage(ChatColor.RED + "只有游戏中的玩家才能投票喵~");
            }
        }
        return null;
    }

    /**
     * 查询或修改一项规则值
     */
    private List<String> getOrSetRule(CommandSender sender, String[] args, boolean flag, RuleKey<?> ruleKey) {
        if (args.length == 2){
            // 参数类似："/mh rule hunter_ready_cd"
            // 获取规则的值
            if (flag) {
                sendRuleInfo(sender, ruleKey);
            }
            return null;

        } else if (args.length == 3) {
            //参数类似："/mh rule hunter_ready_cd 20"
            // 修改规则的值
            if (flag) {
                // 给规则赋值
                String newValueStr = args[2];
                if (console.getGameRules().setGameRuleValueSafe(ruleKey, newValueStr)) {
                    // 修改成功，给所有玩家广播
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.sendMessage("修改规则项: " + ChatColor.GOLD + ruleKey.getName() +
                                ChatColor.RESET + " 为 " + ChatColor.GOLD + newValueStr);
                    });
                    // 刷新规则面板
                    console.refreshRuleBoard();
                    return null;
                } else {
                    sender.sendMessage("§e无效的规则值: " + newValueStr);
                    return null;
                }

            } else {
                // 补全推荐值
                return ruleKey.getRecommendedValues();
            }
            
        } else {
            if (flag) {
                sender.sendMessage("§e参数过多！正确用法请使用 /manhunt rule 获取帮助。");
            }
            return null;
        }
    }

    /**
     * 获取指定规则的详情
     */
    private void sendRuleInfo(CommandSender sender, RuleKey<?> ruleKey) {
        sender.sendMessage("游戏规则: " + ChatColor.GOLD + ruleKey.getName());
        sender.sendMessage("描述: " + ChatColor.GOLD + ruleKey.getInfo());
        sender.sendMessage("值类型: " + ChatColor.GOLD + ruleKey.getTypeInfo());
        sender.sendMessage("数值: " + ChatColor.GOLD + console.getGameRules().getRuleValue(ruleKey));
    }


}
