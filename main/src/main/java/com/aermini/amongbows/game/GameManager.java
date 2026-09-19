package com.aermini.amongbows.game;

import com.aermini.amongbows.AmongBows;
import com.aermini.amongbows.action.ActionEngine;
import com.aermini.amongbows.action.ActionBarManager;
import com.aermini.amongbows.board.ScoreboardRenderer;
import com.aermini.amongbows.config.ItemConfig;
import com.aermini.amongbows.config.LangManager;
import com.aermini.amongbows.map.SafeSpots;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {

    private final AmongBows plugin;
    private final LangManager lang;
    private final ItemConfig itemCfg;
    private final ActionBarManager bars;
    private final ActionEngine actions;
    private final ScoreboardRenderer board;

    private Game game;
    private final SafeSpots spots = new SafeSpots();
    private BukkitTask tickTask;
    private BukkitTask voidTask;
    private BukkitTask barTask;

    private final Map<UUID, ItemStack[]> invSnapshot = new HashMap<>();
    private final Map<UUID, ItemStack[]> armorSnapshot = new HashMap<>();
    private final Map<UUID, Location> locSnapshot = new HashMap<>();
    private final Map<UUID, GameMode> modeSnapshot = new HashMap<>();

    public GameManager(AmongBows plugin, LangManager lang, ItemConfig itemCfg,
                       ActionBarManager bars, ActionEngine actions, ScoreboardRenderer board) {
        this.plugin = plugin;
        this.lang = lang;
        this.itemCfg = itemCfg;
        this.bars = bars;
        this.actions = actions;
        this.board = board;
    }

    public void init() {
        game = new Game(plugin, plugin.configs().firstMap(), actions, bars, board, spots, lang, itemCfg);
        game.setOnCleanup(this::restoreAll);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, game::tick, 20L, 20L);
        voidTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            game.voidCheck();
            game.tickRelive();
        }, 10L, 10L);
        barTask = Bukkit.getScheduler().runTaskTimer(plugin, bars::tick, 10L, 10L);
    }

    private void restoreAll() {
        for (UUID uuid : new java.util.ArrayList<>(invSnapshot.keySet())) {
            restore(uuid);
        }
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
        if (voidTask != null) voidTask.cancel();
        if (barTask != null) barTask.cancel();
        if (game != null) {
            game.cleanup();
        }
    }

    public Game game() {
        return game;
    }

    public boolean join(Player p, String mapId) {
        if (game.get(p) != null) {
            p.sendMessage(game.prefix() + "你已经在游戏中了");
            return false;
        }
        if (game.phase() != Game.Phase.WAITING) {
            p.sendMessage(game.prefix() + "当前对局正在进行或正在结算, 请稍后再试");
            return false;
        }
        if (game.allPlayers().size() >= game.map().playersMax) {
            p.sendMessage(game.prefix() + "房间已满");
            return false;
        }
        if (!game.join(p)) {
            p.sendMessage(game.prefix() + "房间已满");
            return false;
        }
        snapshot(p);
        Player pl = p;
        pl.getInventory().clear();
        pl.getInventory().setArmorContents(null);
        pl.teleport(game.spawnLoc());
        game.maybeStartCountdown();
        game.board().renderAll(game);
        return true;
    }

    public void leave(Player p) {
        GamePlayer gp = game.get(p);
        if (gp == null) return;
        if (game.phase() == Game.Phase.PREPARE || game.phase() == Game.Phase.BATTLE) {
            if (gp.state != PlayerState.LOSS) {
                gp.state = PlayerState.LOSS;
                game.checkWin();
            }
        } else if (game.phase() == Game.Phase.COUNTDOWN) {
            game.removePlayer(gp);
            if (game.allPlayers().size() < game.map().playersMin) {
                game.cancelCountdown();
            }
        } else {
            game.removePlayer(gp);
        }
        restore(p.getUniqueId());
        game.board().renderAll(game);
    }

    public void onQuit(Player p) {
        GamePlayer gp = game.get(p);
        if (gp == null) return;
        if (game.phase() == Game.Phase.PREPARE || game.phase() == Game.Phase.BATTLE) {
            if (gp.state != PlayerState.LOSS) {
                gp.state = PlayerState.LOSS;
                game.checkWin();
            }
            game.removePlayer(gp);
        } else if (game.phase() == Game.Phase.COUNTDOWN) {
            game.removePlayer(gp);
            if (game.allPlayers().size() < game.map().playersMin) {
                game.cancelCountdown();
            }
        } else {
            game.removePlayer(gp);
        }
        game.board().renderAll(game);
    }

    private void snapshot(Player p) {
        UUID uuid = p.getUniqueId();
        invSnapshot.put(uuid, p.getInventory().getContents());
        armorSnapshot.put(uuid, p.getInventory().getArmorContents());
        locSnapshot.put(uuid, p.getLocation().clone());
        modeSnapshot.put(uuid, p.getGameMode());
    }

    public void restore(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        ItemStack[] inv = invSnapshot.remove(uuid);
        ItemStack[] armor = armorSnapshot.remove(uuid);
        Location loc = locSnapshot.remove(uuid);
        GameMode mode = modeSnapshot.remove(uuid);
        if (p == null || inv == null) return;
        p.getInventory().setContents(inv);
        if (armor != null) {
            p.getInventory().setArmorContents(armor);
        }
        if (mode != null) {
            p.setGameMode(mode);
        }
        if (loc != null) {
            p.teleport(loc);
        }
        p.updateInventory();
    }
}
