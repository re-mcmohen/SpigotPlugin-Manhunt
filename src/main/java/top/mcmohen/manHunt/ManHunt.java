package top.mcmohen.manHunt;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import top.mcmohen.manHunt.command.ManhuntCommand;
import top.mcmohen.manHunt.command.TestCommand;
import top.mcmohen.manHunt.listener.PlayerListener;

public final class ManHunt extends JavaPlugin {
    private static ManHunt instance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        var console = new Console();

        // 注册事件
        this.getServer().getPluginManager().registerEvents(new PlayerListener(console), this);

        // 注册命令
        this.getCommand("manhunt").setExecutor(new ManhuntCommand(console));
        this.getCommand("manhunt").setTabCompleter(new ManhuntCommand(console));
        this.getCommand("test").setExecutor(new TestCommand(console));

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static ManHunt getInstance() {
        return instance;
    }
}
