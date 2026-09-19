package com.aermini.amongbows.command;

import com.aermini.amongbows.AmongBows;
import com.aermini.amongbows.game.Game;
import com.aermini.amongbows.game.GamePlayer;
import com.aermini.amongbows.game.Team;
import com.aermini.amongbows.game.TeamColor;
import com.aermini.amongbows.module.GuessModule;
import com.aermini.amongbows.utils.msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class AmongBowsCommand implements CommandExecutor {

    private final AmongBows plugin;

    public AmongBowsCommand(AmongBows plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Game game = plugin.manager().game();
        String sub = args.length > 0 ? args[0].toLowerCase() : "help";
        switch (sub) {
            case "join": {
                if (!(sender instanceof Player)) return true;
                Player p = (Player) sender;
                String mapId = args.length > 1 ? args[1] : null;
                if (mapId != null && plugin.configs().map(mapId) == null) {
                    p.sendMessage(game.prefix() + msg.t("&c未找到地图: " + mapId));
                    return true;
                }
                plugin.manager().join(p, mapId);
                return true;
            }
            case "leave": {
                if (!(sender instanceof Player)) return true;
                plugin.manager().leave((Player) sender);
                return true;
            }
            case "guess": {
                if (!(sender instanceof Player)) return true;
                plugin.manager().game().guess().open((Player) sender);
                return true;
            }
            case "start": {
                if (!sender.hasPermission("amongbows.admin")) {
                    sender.sendMessage(game.prefix() + msg.t("&c没有权限"));
                    return true;
                }
                if (game.phase() != Game.Phase.WAITING) {
                    sender.sendMessage(game.prefix() + msg.t("&c当前阶段无法开始"));
                    return true;
                }
                if (game.allPlayers().size() < 2) {
                    sender.sendMessage(game.prefix() + msg.t("&c至少需要2名玩家"));
                    return true;
                }
                game.maybeStartCountdown();
                if (game.phase() == Game.Phase.COUNTDOWN) {
                    game.broadcast(game.prefix() + msg.t("&a管理员强制开始了游戏"));
                }
                return true;
            }
            case "stop": {
                if (!sender.hasPermission("amongbows.admin")) {
                    sender.sendMessage(game.prefix() + msg.t("&c没有权限"));
                    return true;
                }
                game.finish(null);
                return true;
            }
            case "list": {
                sender.sendMessage(game.prefix() + msg.t("&7阶段: &f" + game.phase().name() + " &7玩家: &f" + game.allPlayers().size()));
                for (TeamColor color : TeamColor.values()) {
                    Team team = game.teams().get(color);
                    if (team == null) continue;
                    StringBuilder sb = new StringBuilder();
                    for (GamePlayer gp : team.members) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(gp.name).append("(").append(gp.state.name().toLowerCase()).append(")");
                    }
                    sender.sendMessage(game.prefix() + " " + plugin.lang().teamColor(color) + plugin.lang().teamName(color)
                        + msg.t(" &7技能点&f" + team.points + " &7: &f" + sb));
                }
                return true;
            }
            case "help":
            default: {
                sender.sendMessage(msg.t("&6&l弓箭之间 &7/amongbows, /ab"));
                sender.sendMessage(msg.t("&e/ab join [地图] &7- 加入游戏"));
                sender.sendMessage(msg.t("&e/ab leave &7- 退出游戏"));
                sender.sendMessage(msg.t("&e/ab guess &7- 打开猜测UI"));
                sender.sendMessage(msg.t("&e/ab list &7- 查看对局信息"));
                sender.sendMessage(msg.t("&e/ab start &7- 强制开始 &o(管理员)"));
                sender.sendMessage(msg.t("&e/ab stop &7- 强制结束 &o(管理员)"));
                return true;
            }
        }
    }
}
