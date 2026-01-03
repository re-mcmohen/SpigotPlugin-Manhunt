package top.mcmohen.manHunt.command;

import org.bukkit.command.CommandExecutor;
import top.mcmohen.manHunt.Console;
import top.mcmohen.manHunt.rule.GameRules;
import top.mcmohen.manHunt.rule.RuleKey;

public class TestCommand implements CommandExecutor {
    private Console console;
    public TestCommand(Console console) {
        this.console = console;
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        sender.sendMessage("测试开始");
        GameRules gameRules = console.getGameRules();
        boolean success = gameRules.setGameRuleValueSafe(RuleKey.Hunter_Ready_CD, "10");
        sender.sendMessage("设置猎人准备倒计时为10秒: " + (success ? "成功" : "失败"));
        Integer hunterReadyCd = gameRules.getRuleValue(RuleKey.Hunter_Ready_CD);
        sender.sendMessage("当前猎人准备倒计时: " + hunterReadyCd + "秒");
        console.refreshRuleBoard();
        sender.sendMessage("测试结束");

        return true;
    }

}
