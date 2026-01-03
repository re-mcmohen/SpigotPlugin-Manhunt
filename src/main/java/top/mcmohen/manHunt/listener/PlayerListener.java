package top.mcmohen.manHunt.listener;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import top.mcmohen.manHunt.Console;

public class PlayerListener implements Listener {
    private Console console;

    public PlayerListener(Console console){ this.console = console; }

    /**
     * 玩家加入游戏时的监听器
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        if (console.getStage() == Console.GameStage.PREPARING) {
            player.setGameMode(GameMode.ADVENTURE);
            console.joinAudience(player);  // 自动加入观众队伍
            // TODO: 其他玩家加入时的设置

        }
    }

    /**
     * 玩家离开游戏时的监听器
     */
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event){
        Player player = event.getPlayer();
        if (console.getStage() == Console.GameStage.PREPARING) {
            if (console.getBeginningCountdownTask() != null && (console.isHunter(player) || console.isSpeedrunner(player))) {
                console.interruptCountdownToStart();  // 如果是猎人或速通者，取消倒计时
            }

            console.joinAudience(player);  // 自动加入观众队伍（在退出时）
        }
    }

    /**
     * 监听玩家传送门事件，改变维度时记录坐标
     */
    @EventHandler
    public void onPlayerChangeWorld(PlayerPortalEvent event) {
        if (console.getStage() != Console.GameStage.PROCESSING || !console.isSpeedrunner(event.getPlayer())) return;

        World fromWorld = event.getFrom().getWorld();
        if (fromWorld != null)
            console.recordLocAtPortal(event.getPlayer(), event.getFrom());
    }

    /**
     * 监听玩家丢弃物品事件，以切换指南针目标
     */
    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        if (!console.isHunter(event.getPlayer())) return;

        ItemStack itemStack = event.getItemDrop().getItemStack();
        if (!console.isHunterCompass(itemStack)) return;

        // 更新追踪
        console.trackNextPlayer(event.getPlayer());
        // 取消这次丢弃事件
        event.setCancelled(true);
    }

    /**
     * 监听猎人移动事件 - 以在准备阶段(其实游戏已经开始 - PROCESSING)取消猎人移动事件
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
         if (console.getStage() != Console.GameStage.PROCESSING) return;

         Player player = event.getPlayer();
         if (console.isHunter(player) && player.getGameMode() == GameMode.SPECTATOR)
             event.setCancelled(true);
    }

    /**
     * 监听猎人传送事件 - 以在准备阶段(其实游戏已经开始 - PROCESSING)取消猎人传送事件
     */
    @EventHandler
    public void onPlayerTp(PlayerTeleportEvent event) {
        if (console.getStage() != Console.GameStage.PROCESSING) return;

        Player player = event.getPlayer();
        if (console.isHunter(player) && player.getGameMode() == GameMode.SPECTATOR && event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN)
            event.setCancelled(true);
    }

    /**
     * 监听玩家死亡事件 - 直接传递给handlePlayerDeath方法进行处理
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        console.handlePlayerDeath(event.getEntity().getPlayer());
    }

    /**
     * 监听玩家重生事件 - 给予猎人一个新的指南针
     */
    @EventHandler
    public void onPlayerSpawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // 身份判断和阶段判断均在下面这个方法内部
        console.giveCompassToHunter(player);
    }

    /**
     * 监听末影龙死亡事件
     */
    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon && console.getStage() == Console.GameStage.PROCESSING) {
            console.end("速通者");
        }
    }

}
