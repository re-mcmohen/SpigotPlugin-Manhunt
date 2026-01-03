package top.mcmohen.manHunt;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import top.mcmohen.manHunt.rule.GameRules;
import top.mcmohen.manHunt.rule.RuleKey;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Console {
    private final World overworld = Bukkit.getWorld("world");
    private final World Nether = Bukkit.getWorld("world_nether");

    private Team speedrunnerTeam;
    private Team hunterTeam;
    private Team audienceTeam;
    private GameStage stage = GameStage.PREPARING;

    // 维护三种玩家 UUID 集合，方便遍历等操作（用Team对象不太方便）
    private Set<UUID> speedrunnerSet = new HashSet<>();
    private Set<UUID> hunterSet = new HashSet<>();
    private Set<UUID> audienceSet = new HashSet<>();


    // TODO: 速通者的UUID列表还没有进行维护（没有创建）
    // 维护速通者列表（用于猎人指南针指向和切换目标）
    private List<UUID> speedRunnerList = new ArrayList<>();

    // 速通者离开主世界时的最后位置
    private Map<UUID, Location> playerLocInWorldMap = new HashMap<>();

    // 速通者离开下界时的最后位置
    private Map<UUID, Location> playerLocInNetherMap = new HashMap<>();

    // 记录每个猎人追踪的速通者(Integer是记录速通者在列表中的ID，这样方便实现切换目标功能)
    private Map<UUID, Integer> trackRunnerMap = new HashMap<>();

    // 记录投票玩家和其选择的选项
    private Map<UUID, Boolean> voteEndMap = new HashMap<>();
    // 记录赞成Stop的玩家数
    private Integer votingCount = 0;

    // 淘汰的速通者集合
    private Set<UUID> outPlayers = new HashSet<>();


    // 猎人指南针的标记
    private String compassflag = "compassflag";
    // 猎人指南针的物品
    private ItemStack hunterCompass;


    // 猎人重生事件的Task，处理猎人死亡后重生的事件
    private Map<UUID, BukkitTask> hunterRespawnTasks = new HashMap<>();
    // 游戏开始前的倒计时task
    private BukkitTask beginningCountdownTask = null;
    // 猎人开始行动前的倒计时task
    private BukkitTask hunterSpawnCD = null;
    // 猎人指南针的刷新任务
    private BukkitTask compassRefreshTask = null;
    // 结束游戏的投票任务
    private BukkitTask voteTask = null;

    // 是否确认要重赛（无视风险继续安装）
    private boolean confirmRestart = false;


    private Scoreboard scoreboard;

    private GameRules gameRules = new GameRules();

    public Console() {
        Bukkit.getLogger().info("ManHunt 插件已启用，当前主世界为: " + overworld.getName());
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        initScoreboard();
        overworld.getWorldBorder().setSize(32);
        overworld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        overworld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        overworld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        overworld.setGameRule(GameRule.KEEP_INVENTORY, true);
        overworld.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        overworld.setGameRule(GameRule.SPAWN_RADIUS, 0);
        overworld.setDifficulty(Difficulty.HARD);

        // 获取队伍（或注册）
        if (scoreboard.getTeam("speedrunner") == null) {
            speedrunnerTeam = scoreboard.registerNewTeam("speedrunner");
        } else {
            speedrunnerTeam = scoreboard.getTeam("speedrunner");
        }
        if (scoreboard.getTeam("hunter") == null) {
            hunterTeam = scoreboard.registerNewTeam("hunter");
        } else {
            hunterTeam = scoreboard.getTeam("hunter");
        }
        if (scoreboard.getTeam("audience") == null) {
            audienceTeam = scoreboard.registerNewTeam("audience");
        } else {
            audienceTeam = scoreboard.getTeam("audience");
        }

        speedrunnerTeam.setColor(ChatColor.BLUE);
        speedrunnerTeam.setPrefix(ChatColor.BLUE + "[速通者] ");
        speedrunnerTeam.getEntries().forEach(entry -> speedrunnerTeam.removeEntry(entry));

        hunterTeam.setColor(ChatColor.RED);
        hunterTeam.setPrefix(ChatColor.RED + "[猎人] ");
        hunterTeam.getEntries().forEach(entry -> hunterTeam.removeEntry(entry));

        audienceTeam.setColor(ChatColor.GRAY);
        audienceTeam.setPrefix(ChatColor.GRAY + "[观众] ");
        audienceTeam.getEntries().forEach(entry -> audienceTeam.removeEntry(entry));

        // 创建猎人指南针物品
        hunterCompass  = new ItemStack(Material.COMPASS);
        ItemMeta compassmeta = hunterCompass.getItemMeta();
        compassmeta.setDisplayName(ChatColor.GOLD + "猎人指南针");
        compassmeta.setLore(java.util.List.of(
                ChatColor.YELLOW + "指向最近的速通者",
                ChatColor.GRAY + "(按Q切换目标)",
                // 加入标记以便识别
                ChatColor.GRAY + compassflag
        ));
        compassmeta.addEnchant(Enchantment.VANISHING_CURSE, 1, false);
        hunterCompass.setItemMeta(compassmeta);

    }

    /**
     * 尝试开始游戏
     * return 错误信息，若成功则返回空字符串
     */
    public String tryStart() {
        // 是否有速通者
        boolean flag = false;
        for (String entry : speedrunnerTeam.getEntries()) {
            Player player = Bukkit.getPlayer(entry);
            if (player != null) {
                flag = true;
                break;
            }
        }
        if (!flag) {
            return "至少需要一名速通者才能开始游戏！";
        }

        // 是否在准备阶段/在倒计时阶段
        if (stage == GameStage.PROCESSING || beginningCountdownTask != null)
            return "只能在准备阶段且不在倒计时阶段开始游戏！";

        // 若在结束阶段，可以强制重赛
        if (stage == GameStage.OVER) {
            if (!confirmRestart) {
                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.sendMessage("现在是结束阶段，如果想重赛，需知：" + ChatColor.YELLOW + "本插件只能重新开始一次比赛，但并不能复原地形，且无法复活末影龙");
                    player.sendMessage("如果仍然想重赛，请再输入一次(60秒内)：" + ChatColor.GREEN + "/manhunt start");
                });
                confirmRestart = true;
                Bukkit.getScheduler().runTaskLater(ManHunt.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        // 60s后取消confirm
                        confirmRestart = false;
                    }
                }, 60 * 20L);
                return "请确认！";
            } else {
                // 无视风险继续开赛
                countdownToStart();
                confirmRestart = false;
                return "";
            }
        }

        // TODO: 将来可以加入更多的开始条件

        // 正式开启倒计时
        countdownToStart();
        return "";
    }

    /**
     * 游戏开始前的倒计时
     */
    public void countdownToStart() {
    beginningCountdownTask = Bukkit.getScheduler().runTaskTimer(ManHunt.getInstance(), new Runnable() {
                private int countdown = 6;

                @Override
                public void run() {
                    countdown--;
                    if (countdown > 0) {
                        // 给所有在线玩家发送倒计时信息
                        Bukkit.getOnlinePlayers().forEach(player -> {
                            player.sendTitle(
                                    "§c" + countdown,          // 主标题：红色倒计时数字（屏幕中央大号字体）
                                    "§e游戏即将开始！"         // 副标题：黄色提示文字（主标题下方小字）
                            );
                        });
                    } else {
                        beginningCountdownTask.cancel();
                        beginningCountdownTask = null;
                        start();
                    }
                }
            }, 0, 20);
    }

    /**
     * 游戏开始
     */
    public void start() {
        //TODO: 实现游戏开始逻辑
        Bukkit.getLogger().info("猎人游戏开始！");
        speedrunnerTeam.getEntries().forEach(entry -> {
            Player player = Bukkit.getPlayer(entry);
            if (player != null) {
                // 将速通者的UUID加入集合
                speedrunnerSet.add(player.getUniqueId());
            }
        });

        hunterTeam.getEntries().forEach(entry -> {
            Player player = Bukkit.getPlayer(entry);
            if (player != null) {
                // 将猎人的UUID加入集合
                hunterSet.add(player.getUniqueId());
            }
        });

        overworld.setGameRule(GameRule.DO_WEATHER_CYCLE, true);
        overworld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        overworld.setGameRule(GameRule.DO_MOB_SPAWNING, true);
        overworld.setGameRule(GameRule.KEEP_INVENTORY, false);
        overworld.setGameRule(GameRule.SPAWN_RADIUS, 10);
        overworld.setDifficulty(Difficulty.HARD);
        overworld.getWorldBorder().setSize(999999.0);

        Location spawnLocation = overworld.getSpawnLocation();

        // 重置所有玩家的状态
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.setGameMode(GameMode.SPECTATOR);
            player.setHealth(20.0);
            player.getInventory().clear();
            player.setSaturation(20.0f);
            player.setFoodLevel(20);
            player.setExp(0f);
            player.setLevel(0);
            player.teleport(spawnLocation);
        });

        // 设置速通者的状态
        speedrunnerSet.forEach(UUID -> {
            Player player = Bukkit.getPlayer(UUID);
            if (player != null) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        });

        // 初始化速通者列表
        speedRunnerList = speedrunnerSet.stream().toList();

        // 设置猎人状态
        hunterSet.forEach(UUID -> {
            Player player = Bukkit.getPlayer(UUID);
            if (player != null) {
                player.teleport(new Location(overworld, 0.0, -65.0, 0.0));
                // 均追踪第一个速通者（0号速通者）
                trackRunnerMap.put(UUID, 0);
            }
        });

        // 设置友伤的有无
        Boolean pvp = gameRules.getRuleValue(RuleKey.Friendly_Fire);
        speedrunnerTeam.setAllowFriendlyFire(pvp);
        hunterTeam.setAllowFriendlyFire(pvp);


        // compassTask为实现一次猎人指南针更新的Task
        BukkitRunnable compassTask = new BukkitRunnable(){
            @Override
            public void run() {
                hunterSet.forEach(UUID -> {
                    Player player = Bukkit.getPlayer(UUID);
                    if (player != null) {
                        // 即当前猎人正在追踪的速通者
                        Integer speedRunnerIndex = trackRunnerMap.get(UUID);
                        Player speedRunner = null;
                        if (speedRunnerIndex != null) {
                            speedRunnerIndex %= speedRunnerList.size();
                            speedRunner = Bukkit.getPlayer(speedRunnerList.get(speedRunnerIndex));
                        }
                        if (speedRunner != null && player != null) {
                            refreshCompassTrack(player, speedRunner);
                        }
                    }
                });
            }
        };


        // 猎人准备的倒计时Task
        hunterSpawnCD = Bukkit.getScheduler().runTaskLater(ManHunt.getInstance(), new Runnable() {
            @Override
            public void run() {
                // 释放猎人
                hunterSet.forEach(UUID -> {
                    Player player = Bukkit.getPlayer(UUID);
                    if (player != null) {
                        player.teleport(spawnLocation);
                        player.setGameMode(GameMode.SURVIVAL);
                        player.getInventory().addItem(hunterCompass);
                        player.sendMessage(ChatColor.GOLD + "你已到达出生点，猎人游戏正式开始！");
                    }
                });

                // 定义RefreshTask，让20ticks为周期执行compassTask，让指南针指向速通者
                compassRefreshTask = compassTask.runTaskTimer(ManHunt.getInstance(), 0, 20);


                // 通知速通者猎人已释放
                speedrunnerSet.forEach(UUID -> {
                    Player player = Bukkit.getPlayer(UUID);
                    if (player != null) {
                        player.sendMessage(ChatColor.RED + "猎人已准备完毕，猎人游戏正式开始！");
                    }
                });

                // 倒计时任务执行完毕，清空任务引用
                hunterSpawnCD = null;
            }
        }, gameRules.getRuleValue(RuleKey.Hunter_Ready_CD) * 20L);

        // 上面调度器的设置并不会阻塞下面的执行，故在start的一开始就会隐藏规则栏且设置游戏状态为PROCESSING
        // 游戏进入进行阶段，右侧游戏规则栏隐藏
        if (scoreboard.getObjective("rule-list") != null)
            scoreboard.getObjective("rule-list").setDisplaySlot(null);

        // 更新游戏阶段为PROCESSING
        setStage(GameStage.PROCESSING);
    }

    /**
     * 让指南针的目标切换为下一个 - 在PlayerListener中监听物品丢弃事件以触发
     */
    public void trackNextPlayer(Player hunter) {
        if (stage != GameStage.PROCESSING || !isHunter(hunter)) return;
        // 获取当前猎人追踪的速通者Index
        Integer trackIndex = trackRunnerMap.get(hunter.getUniqueId());
        if (trackIndex == null || speedRunnerList.isEmpty()) return;

        // j为下一个追踪的目标
        int j = trackIndex;
        while(true) {
            j++;
            j %= speedRunnerList.size();

            UUID uuid = speedRunnerList.get(j);
            Player speedrunner = Bukkit.getPlayer(uuid);

            // 极端情况判断，如果所有速通者均掉线，则break
            if (trackIndex.equals(j)) {
                // 可能是只有一位速通者，照样提醒
                if(!outPlayers.contains(uuid) && speedrunner != null)
                    hunter.sendMessage("只有一个速通者，追踪：" + ChatColor.GOLD + speedrunner.getName());
                // 此时才是全部掉线
                break;
            }

            if (speedrunner != null && !outPlayers.contains(uuid)) {
                trackRunnerMap.put(hunter.getUniqueId(), j);
                refreshCompassTrack(hunter, speedrunner); //立即刷新一次compass
                hunter.sendMessage("已切换目标，现在在追踪：" + ChatColor.GOLD + speedrunner.getName());
                break;
            }
        }
    }

    /**
     * 刷新猎人指南针的指向 (使hunter的指南针指向speedrunner)
     */
    private void refreshCompassTrack(Player hunter, Player speedrunner) {
        // 得到猎人背包中所有的指南针
        HashMap<Integer, ?> items = hunter.getInventory().all(Material.COMPASS);
        if (items.isEmpty()) {
            return;
        }
        // 遍历以找到带有标记的猎人指南针
        for (Map.Entry<Integer, ?> entry : items.entrySet()) {
            ItemStack item = hunter.getInventory().getItem(entry.getKey());
            if (item == null) continue;

            if (isHunterCompass(item)) {
                // 找到猎人指南针，设置其指向速通者
                CompassMeta meta = (CompassMeta) item.getItemMeta();
                meta.setLodestoneTracked(false); // 关闭磁石追踪更新（即固定指南针的指向）

                // 判断维度信息
                if (hunter.getWorld().getUID() == speedrunner.getWorld().getUID()) {
                    Location location = speedrunner.getLocation();
                    meta.setLodestone(location);
                } else if (hunter.getWorld().getUID() == overworld.getUID()) {
                    Location location = playerLocInWorldMap.get(speedrunner.getUniqueId());
                    meta.setLodestone(location);

                } else if (hunter.getWorld().getUID() == Nether.getUID()) {
                    Location location = playerLocInNetherMap.get(speedrunner.getUniqueId());
                    meta.setLodestone(location);
                } else {
                    meta.setLodestone(null);
                }
                item.setItemMeta(meta);
            }
        }
    }

    /**
     * 判断该物品堆是不是“猎人指南针”
     */
    public boolean isHunterCompass(ItemStack itemStack) {
        List<String> lore = Objects.requireNonNull(itemStack.getItemMeta()).getLore();
        if (lore == null) return false;
        if (lore.size() != 3) return false;
        String flag = lore.get(2);
        if (flag.equals(ChatColor.GRAY + compassflag)) return true;

        return false;
    }

    /**
     * 记录玩家进入传送门的位置 - 该方法由PlayerListener监听触发
     */
    public void recordLocAtPortal(Player player, Location location){
        World world = location.getWorld();
        if (world.getUID() == overworld.getUID()) {
            playerLocInWorldMap.put(player.getUniqueId(), location);
        } else if (world.getUID() == Nether.getUID()) {
            playerLocInNetherMap.put(player.getUniqueId(), location);
        }
    }

    /**
     * 处理玩家死亡的事件
     */
    public void handlePlayerDeath(Player player) {
        if (stage != GameStage.PROCESSING) return;
        UUID uuid = player.getUniqueId();
        
        if (isHunter(player)) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage("你已死亡，等待重生");
            player.sendMessage(ChatColor.WHITE + "按照设置，你的复活CD为: " + ChatColor.GOLD + gameRules.getRuleValue(RuleKey.Hunter_Respawn_CD) + "秒");

            // 创建延迟的猎人重生Task
            hunterRespawnTasks.put(
                    uuid,
                    Bukkit.getScheduler().runTaskLater(ManHunt.getInstance(), new Runnable() {
                @Override
                public void run() {
                    player.setGameMode(GameMode.SURVIVAL);
                    // 移除map中的该猎人重生任务
                    hunterRespawnTasks.remove(uuid);
                }
            }, gameRules.getRuleValue(RuleKey.Hunter_Respawn_CD) * 20L));

        } else if (isSpeedrunner(player)) {
            // TODO: 速通者死亡的实际实现
            // 将该速通者加入死亡名单
            outPlayers.add(player.getUniqueId());
            player.setGameMode(GameMode.SPECTATOR);

            if (outPlayers.size() == speedrunnerSet.size()) {
                // 所有速通者均死亡 - 触发猎人获胜（结束游戏）
                Bukkit.getScheduler().runTaskLater(ManHunt.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        end("猎人");
                    }
                }, 0);
            }
        }
    }

    /**
     * 投票以结束游戏
     */
    public void voteForStop(Player player) {
        if (stage != GameStage.PROCESSING) {
            player.sendMessage(ChatColor.RED + "只有在游戏中才能投票");
            return;
        }
        if (!isHunter(player) && !(isSpeedrunner(player))) {
            player.sendMessage(ChatColor.RED + "观众不许投票喵~");
            return;
        }

        if (voteTask == null) {
            // 投票发起
            voteTask = Bukkit.getScheduler().runTaskLater(ManHunt.getInstance(), new Runnable() {
                @Override
                public void run() {
                    // 投票结束后，复原这些数据
                    if (voteEndMap != null) {
                        voteEndMap.clear();
                        voteEndMap = null;
                        votingCount = 0;
                    }
                    Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                        onlinePlayer.sendMessage(ChatColor.GOLD + "投票时间结束，投票数量不足，游戏继续~");
                    });
                }
            }, 60 * 20L);
            // 统计参与投票的玩家
            speedrunnerSet.forEach(uuid -> {
                Player speedrunner = Bukkit.getPlayer(uuid);
                if (speedrunner == null) return;

                if (!outPlayers.contains(speedrunner)) {
                    // 若该速通者没有被淘汰 - 允许投票
                    voteEndMap.put(uuid, false);
                }
            });

            hunterSet.forEach(uuid -> {
                Player hunter = Bukkit.getPlayer(uuid);
                if (hunter == null) return;
                voteEndMap.put(uuid, false);
            });

            Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                onlinePlayer.sendMessage(ChatColor.GOLD + player.getName() + ChatColor.WHITE + "发起了终止游戏的投票喵~");
                onlinePlayer.sendMessage(ChatColor.WHITE + "如果赞成，请在60秒内执行 /manhunt stop，否则默认不赞成");
            });
        }

        if (!voteEndMap.containsKey(player.getUniqueId())) {
            // 当前玩家不在可投票的名单中
            player.sendMessage(ChatColor.RED + "你已被淘汰或者不为游戏人员，不可投票");
            return;
        }
        // 当前玩家可投票
        Boolean vote = voteEndMap.get(player.getUniqueId());
        if (vote) {
            player.sendMessage(ChatColor.RED + "你已经投过票了喵，请勿重复投票");
            return;
        }

        voteEndMap.put(player.getUniqueId(), true);
        votingCount++;
        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            onlinePlayer.sendMessage(ChatColor.GOLD + "投票情况: " + votingCount + "/" + voteEndMap.size());
        });
        if (votingCount != voteEndMap.size())
            return;

        // 此时说明经过当前玩家的投票，所有人均已同意stop
        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            onlinePlayer.sendMessage(ChatColor.GOLD + "------投票通过，结束游戏------");
        });
        voteTask.cancel();
        end(null);
    }

    /**
     * 游戏结束 - 善后工作
     */
    public void end(String winner) {
        if (stage != GameStage.PROCESSING) return;

        // 切换游戏状态为OVER
        stage = GameStage.OVER;
        // 取消各种任务
        if (hunterSpawnCD != null) {
            hunterSpawnCD.cancel();
            hunterSpawnCD = null;
        }

        if (compassRefreshTask != null) {
            compassRefreshTask.cancel();
            compassRefreshTask = null;
        }

        hunterRespawnTasks.forEach((uuid, task) -> {
            task.cancel();
        });
        hunterRespawnTasks.clear();

        // 设置友伤为True，以供玩家互相报仇 (不影响下一次开游戏时的友伤设置)
        speedrunnerTeam.setAllowFriendlyFire(true);
        hunterTeam.setAllowFriendlyFire(true);

        // 重新显示规则板
        refreshRuleBoard();

        // 发送结束信息
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendMessage(ChatColor.GREEN + "------游戏结束------");
            if (winner != null) {
                player.sendMessage(ChatColor.GREEN + "获胜者: " + ChatColor.GOLD + winner);
            } else {
                player.sendMessage(ChatColor.GOLD + "没有赢家");
            }
            // 将所有玩家设置为生存模式
            player.setGameMode(GameMode.SURVIVAL);
        });

    }

    /**
     * 给予猎人追踪指南针
     */
    public void giveCompassToHunter(Player player) {
        if (stage != GameStage.PROCESSING) {
            player.sendMessage(ChatColor.RED + "游戏未开始，不能获取猎人指南针");
            return;
        }

        if (!isHunter(player)) {
            player.sendMessage(ChatColor.RED + "你不是猎人，不能获取猎人指南针");
            return;
        }

        // 此时游戏开始且player是猎人 - 接下来判断其背包内是否已经存在猎人指南针

        HashMap<Integer, ?> items = player.getInventory().all(Material.COMPASS);
        boolean have = false; // 记录当前猎人是否持有猎人指南针
        for (Object obj : items.values()) {
            ItemStack compassItem = (ItemStack) obj;
            if (isHunterCompass(compassItem)) {
                have = true;
                break;
            }
        }
        if (!have) {
            // 当前猎人没有猎人指南针 - 给予一个猎人指南针
            player.getInventory().addItem(hunterCompass);
        } else {
            player.sendMessage(ChatColor.RED + "你已拥有一个猎人指南针，请检查你的背包");
        }

    }

    public void joinHunter(Player player) {
        if (stage == GameStage.PREPARING) {
            hunterTeam.addPlayer(player);
            player.sendMessage("你已加入[猎人]");
        }
    }

    public void joinSpeedrunner(Player player) {
        if (stage == GameStage.PREPARING) {
            speedrunnerTeam.addPlayer(player);
            player.sendMessage("你已加入[速通者]");
        }
    }

    public void joinAudience(Player player) {
        if (stage == GameStage.PREPARING) {
            audienceTeam.addPlayer(player);
            player.sendMessage("你已加入[观众]");
        }
    }

    /**
     * 属于猎人阵营
     */
    public boolean isHunter(Player player) {
        return hunterTeam.hasPlayer(player);
    }

    /**
     * 属于速通者阵营
     */
    public boolean isSpeedrunner(Player player) {
        return speedrunnerTeam.hasPlayer(player);
    }

    /**
     * 中断开始倒计时
     */
    public void interruptCountdownToStart(){
        if (beginningCountdownTask != null) {
            beginningCountdownTask.cancel();
            beginningCountdownTask = null;
            // 给所有在线玩家发送中断信息
            Bukkit.getOnlinePlayers().forEach(player -> {
                player.sendTitle(
                        "§c已取消",          // 主标题：红色“已取消”（屏幕中央大号字体）
                        "§e有玩家加入或离开，游戏开始已取消！"         // 副标题：黄色提示文字（主标题下方小字）
                );
            });
        }
    }

    /**
     * 属于观众阵营
     */
    public boolean isAudience(Player player) {
        return audienceTeam.hasPlayer(player);
    }

    /**
     * 初始化计分板和Team
     */
    private void initScoreboard() {
        scoreboard.getTeams().forEach(team -> {team.unregister();});
        if (scoreboard.getObjective("players") != null) {
            scoreboard.getObjective("players").unregister();
        }
        if (scoreboard.getObjective("rule-list") != null) {
            scoreboard.getObjective("rule-list").unregister();
        }

        // 上面确保这俩计分板对象被移除，下面重新创建
        Objective healthObjective = scoreboard.registerNewObjective("players", Criteria.HEALTH, "players");
        healthObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        healthObjective.setRenderType(RenderType.HEARTS);

        Objective ruleListObjective = scoreboard.registerNewObjective("rule-list", Criteria.DUMMY, ChatColor.DARK_AQUA + "规则列表");
        ruleListObjective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // 侧边栏规则列表的具体显示内容
        AtomicInteger order = new AtomicInteger(gameRules.getAllRules().size());  // 用于控制侧边栏显示顺序的计数器
        gameRules.getAllRules().forEach((ruleKey, value) -> {
            String name = ruleKey.getName();
            String info = ruleKey.getInfo() == null ? "" : ruleKey.getInfo();

            // 根据规则值类型格式化显示值
            String valueStr;
            if (ruleKey.getType() == Boolean.class) {
                valueStr = Boolean.TRUE.equals(value) ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭";
            } else {
                valueStr = value == null ? ChatColor.GRAY + "未设置" : ChatColor.GOLD + value.toString();
            }

            // 组合一整行的显示内容
            String line = ChatColor.YELLOW + info + ChatColor.WHITE + ": " + valueStr;

            // 避免超长
            if (line.length() > 40)
                line = line.substring(0, 37) + "...";

            // 写入侧边栏（score使用递减的顺序保证列表顺序）
            Score score = ruleListObjective.getScore(line);
            score.setScore(order.getAndDecrement());
        });

    }


    /**
     * 游戏阶段
     */
    public enum GameStage {
        PREPARING,
        PROCESSING,
        OVER
    }

    public GameStage getStage() {
        return stage;
    }

    public void setStage(GameStage stage) {
        this.stage = stage;
    }

    public BukkitTask getBeginningCountdownTask() {
        return beginningCountdownTask;
    }

    public GameRules getGameRules() {
        return gameRules;
    }


    /**
     * 刷新规则侧边栏中所有规则的显示内容（即从gameRules中重新读取所有规则和value）
     */
    public void refreshRuleBoard() {

        // 直接移除旧的 objective 并重新创建，以确保侧边栏被刷新。
        if (scoreboard.getObjective("rule-list") != null) {
            scoreboard.getObjective("rule-list").unregister();
        }

        Objective ruleListObjective = scoreboard.registerNewObjective("rule-list", Criteria.DUMMY, ChatColor.DARK_AQUA + "规则列表");
        ruleListObjective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // 重新写入所有规则（保持与 initScoreboard 相同的显示逻辑）
        AtomicInteger order = new AtomicInteger(gameRules.getAllRules().size());
        gameRules.getAllRules().forEach((key, value) -> {
            String info = key.getInfo() == null ? "" : key.getInfo();

            String valueStr;
            if (key.getType() == Boolean.class) {
                valueStr = Boolean.TRUE.equals(value) ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭";
            } else {
                valueStr = value == null ? ChatColor.GRAY + "未设置" : ChatColor.GOLD + value.toString();
            }

            String line = ChatColor.YELLOW + info + ChatColor.WHITE + ": " + valueStr;
            if (line.length() > 40)
                line = line.substring(0, 37) + "...";

            Score score = ruleListObjective.getScore(line);
            score.setScore(order.getAndDecrement());
        });
    }
}
