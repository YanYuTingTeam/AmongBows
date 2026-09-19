package com.aermini.amongbows.module;

import com.aermini.amongbows.game.Game;
import com.aermini.amongbows.game.GamePlayer;
import com.aermini.amongbows.game.InjuryType;
import com.aermini.amongbows.game.MarkType;
import com.aermini.amongbows.game.PlayerState;
import com.aermini.amongbows.game.TeamColor;
import com.aermini.amongbows.utils.GameItems;
import com.aermini.amongbows.utils.msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuessModule {

    public static final String TITLE_TARGET = msg.t("&b猜测 · 选择目标玩家");
    public static final String TITLE_TEAM = msg.t("&b猜测 · 选择队伍");

    private final Game game;
    private final Map<UUID, UUID> selecting = new HashMap<>();

    public GuessModule(Game game) {
        this.game = game;
    }

    public void open(Player p) {
        GamePlayer gp = game.get(p);
        if (gp == null || gp.state != PlayerState.LIVE) return;
        if (System.currentTimeMillis() < gp.guessCdUntil) {
            long remain = (gp.guessCdUntil - System.currentTimeMillis() + 999) / 1000;
            game.bars().push(p, msg.t("&c猜测冷却中, 还需 &e" + remain + " &c秒"), 40, null);
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_TARGET);
        int slot = 0;
        for (GamePlayer other : game.allPlayers()) {
            if (other.uuid.equals(gp.uuid)) continue;
            Player op = other.player();
            if (op == null) continue;
            ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setDisplayName(msg.t((other.team != null ? game.lang().teamColor(other.team.color) : "&7") + other.name));
            meta.setOwner(op.getName());
            head.setItemMeta(meta);
            inv.setItem(slot, head);
            slot++;
            if (slot >= inv.getSize()) break;
        }
        p.openInventory(inv);
    }

    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        GamePlayer gp = game.get(p);
        if (gp == null) return;
        String title = event.getView().getTitle();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        if (title.equals(TITLE_TARGET)) {
            if (clicked.getType() != Material.SKULL_ITEM) return;
            String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                ? clicked.getItemMeta().getDisplayName() : null;
            if (name == null) return;
            GamePlayer target = findByName(gp, msg.strip(name));
            if (target == null) return;
            selecting.put(gp.uuid, target.uuid);
            openTeamPage(p, gp);
            return;
        }
        if (title.equals(TITLE_TEAM)) {
            if (clicked.getType() != Material.WOOL) return;
            UUID targetUuid = selecting.remove(gp.uuid);
            if (targetUuid == null) return;
            GamePlayer target = game.get(targetUuid);
            if (target == null) return;
            TeamColor chosen = matchWool(clicked);
            if (chosen == null) return;
            p.closeInventory();
            resolve(gp, target, chosen);
        }
    }

    private void openTeamPage(Player p, GamePlayer gp) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_TEAM);
        int slot = 10;
        for (TeamColor color : TeamColor.values()) {
            if (gp.team != null && gp.team.color == color) continue;
            ItemStack wool = new ItemStack(Material.WOOL, 1, (short) color.woolData);
            ItemMeta meta = wool.getItemMeta();
            meta.setDisplayName(msg.t(game.lang().teamColor(color) + game.lang().teamName(color)));
            wool.setItemMeta(meta);
            inv.setItem(slot, wool);
            slot += 2;
        }
        p.openInventory(inv);
    }

    private GamePlayer findByName(GamePlayer self, String name) {
        for (GamePlayer other : game.allPlayers()) {
            if (other.uuid.equals(self.uuid)) continue;
            if (other.name.equals(name)) return other;
        }
        return null;
    }

    private TeamColor matchWool(ItemStack item) {
        int data = item.getData() != null ? item.getData().getData() : 0;
        for (TeamColor color : TeamColor.values()) {
            if (color.woolData == data) return color;
        }
        return null;
    }

    private void resolve(GamePlayer guesser, GamePlayer target, TeamColor chosen) {
        Player p = guesser.player();
        if (p == null) return;
        if (target.team != null && target.team.color == chosen) {
            GameItems.addArrows(p, 1);
            target.mark(MarkType.GUESSED, -1L);
            guesser.guessCdUntil = System.currentTimeMillis() + 5000L;
            p.sendMessage(game.prefix() + msg.t("&a猜测正确! &f" + target.name + " &a的真实队伍是 ")
                + game.lang().teamColor(chosen) + game.lang().teamName(chosen) + msg.t(" &7(+1支箭)"));
            Player tp = target.player();
            if (tp != null) {
                tp.sendMessage(game.prefix() + msg.t("&c你的真实队伍被猜中了!"));
            }
        } else {
            guesser.guessCdUntil = Math.max(System.currentTimeMillis(), guesser.guessCdUntil) + 20000L;
            game.damage(guesser, 2, InjuryType.SELF, null, null, null);
            p.sendMessage(game.prefix() + msg.t("&c猜测错误! 自身扣除2HP, 冷却增加20秒"));
        }
    }
}
