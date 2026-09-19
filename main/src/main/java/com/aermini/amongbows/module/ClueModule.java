package com.aermini.amongbows.module;

import com.aermini.amongbows.game.Game;
import com.aermini.amongbows.game.GamePlayer;
import com.aermini.amongbows.game.PlayerState;
import com.aermini.amongbows.game.Team;
import com.aermini.amongbows.utils.GameItems;
import com.aermini.amongbows.utils.msg;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClueModule {

    private final Game game;

    public ClueModule(Game game) {
        this.game = game;
    }

    public boolean use(Player p) {
        GamePlayer gp = game.get(p);
        if (gp == null || gp.state != PlayerState.LIVE) return false;
        if (!GameItems.consumeOne(p, GameItems.CLUEBOOK)) return false;
        int count = 1 + game.random().nextInt(2);
        p.sendMessage(game.prefix() + msg.t("&e你翻开了线索书, 获得了 &f" + count + " &e条线索"));
        for (int i = 0; i < count; i++) {
            String clue = generate(gp);
            if (clue != null) {
                p.sendMessage(game.prefix() + msg.t("&e线索: &f" + clue));
            }
        }
        return true;
    }

    private String generate(GamePlayer gp) {
        Player p = gp.player();
        if (p == null) return null;
        List<GamePlayer> others = new ArrayList<>();
        for (GamePlayer other : game.allPlayers()) {
            if (other != gp) others.add(other);
        }
        int roll = game.random().nextInt(86);
        if (roll < 35) {
            GamePlayer target = pick(others);
            if (target == null) return null;
            Player tp = target.player();
            if (tp == null) return null;
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack item : tp.getInventory().getContents()) {
                if (item != null && item.getAmount() > 0) items.add(item);
            }
            if (items.isEmpty()) return null;
            ItemStack item = items.get(game.random().nextInt(items.size()));
            String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName() : item.getType().name();
            return "玩家 " + target.name + " 持有道具 " + name + " (" + item.getAmount() + ")";
        }
        if (roll < 65) {
            GamePlayer target = pick(others);
            if (target == null || target.jersey == null) return null;
            return "玩家 " + target.name + " 当前队服" + (target.jersey == target.team.color ? "是" : "不是") + "其真实队服";
        }
        if (roll < 80) {
            int count = 0;
            for (GamePlayer other : game.allPlayers()) {
                if (other == gp || other.jersey == null) continue;
                Player op = other.player();
                if (op == null || !op.getWorld().equals(p.getWorld())) continue;
                if (op.getLocation().distanceSquared(p.getLocation()) <= 100
                    && other.jersey != other.team.color) count++;
            }
            return "你周围10格内有 " + count + " 名玩家穿着错误的队服";
        }
        if (roll < 85) {
            int count = 0;
            for (GamePlayer other : game.allPlayers()) {
                if (other == gp) continue;
                Player op = other.player();
                if (op == null || !op.getWorld().equals(p.getWorld())) continue;
                if (op.getLocation().distanceSquared(p.getLocation()) <= 100
                    && other.team != null && other.team != gp.team) count++;
            }
            return "你周围10格内有 " + count + " 名不同队伍的玩家";
        }
        GamePlayer target = pick(others);
        if (target == null || target.team == null) return null;
        Team team = target.team;
        return "玩家 " + target.name + " 的真实队伍是 "
            + game.lang().teamColor(team.color) + game.lang().teamName(team.color);
    }

    private GamePlayer pick(List<GamePlayer> list) {
        if (list.isEmpty()) return null;
        return list.get(game.random().nextInt(list.size()));
    }
}
