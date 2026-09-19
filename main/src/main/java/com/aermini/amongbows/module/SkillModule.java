package com.aermini.amongbows.module;

import com.aermini.amongbows.game.Game;
import com.aermini.amongbows.game.GamePlayer;
import com.aermini.amongbows.game.PlayerState;
import com.aermini.amongbows.game.SkillType;
import com.aermini.amongbows.utils.msg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SkillModule {

    public static final String GUI_TITLE = msg.t("&6弓箭之间 · 选择技能");

    private static final int SLOT_TELEPORT = 2;
    private static final int SLOT_ONESHOT = 4;
    private static final int SLOT_REVIVE = 6;

    private final Game game;

    public SkillModule(Game game) {
        this.game = game;
    }

    public void openGUI(Player p) {
        GamePlayer gp = game.get(p);
        if (gp == null || gp.state != PlayerState.LIVE) return;
        Inventory inv = Bukkit.createInventory(null, 9, GUI_TITLE);
        inv.setItem(SLOT_TELEPORT, skillItem(SkillType.TELEPORT, gp));
        inv.setItem(SLOT_ONESHOT, skillItem(SkillType.ONESHOT, gp));
        inv.setItem(SLOT_REVIVE, skillItem(SkillType.REVIVE, gp));
        gp.guiOpen = true;
        p.openInventory(inv);
    }

    private ItemStack skillItem(SkillType type, GamePlayer gp) {
        Material material;
        String desc;
        switch (type) {
            case TELEPORT:
                material = Material.ENDER_PEARL;
                desc = "传送到箭矢的落地位置";
                break;
            case ONESHOT:
                material = Material.BLAZE_ROD;
                desc = "下次箭矢命中额外造成5HP瞬间伤害";
                break;
            default:
                material = Material.GHAST_TEAR;
                desc = "复活一名已死亡的玩家";
                break;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(msg.t("&6技能: " + type.display));
        List<String> lore = new ArrayList<>();
        lore.add(msg.t("&7消耗: &e个人" + type.cost + "点 &f或 &e队伍" + type.cost + "点"));
        lore.add(msg.t("&7冷却: &e" + type.cooldown + "秒"));
        lore.add(msg.t("&7效果: &f" + desc));
        lore.add(msg.t(""));
        lore.add(msg.t("&f个人技能点: &e" + gp.personalPoints + "&7/10"));
        lore.add(msg.t("&f队伍技能点: &e" + (gp.team != null ? gp.team.points : 0)));
        lore.add(msg.t("&a左键&7使用个人技能点 &a右键&7使用队伍技能点"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        GamePlayer gp = game.get(p);
        if (gp == null || gp.state != PlayerState.LIVE) return;
        int slot = event.getRawSlot();
        SkillType type;
        if (slot == SLOT_TELEPORT) type = SkillType.TELEPORT;
        else if (slot == SLOT_ONESHOT) type = SkillType.ONESHOT;
        else if (slot == SLOT_REVIVE) type = SkillType.REVIVE;
        else return;

        ClickType click = event.getClick();
        boolean useTeam;
        if (click == ClickType.RIGHT) {
            useTeam = true;
        } else if (click == ClickType.LEFT) {
            useTeam = false;
        } else {
            return;
        }

        if (gp.onSkillCd(type)) {
            long remain = (gp.skillCdRemain(type) + 999) / 1000;
            game.bars().push(p, msg.t("&c技能冷却中, 还需 &e" + remain + " &c秒"), 40, null);
            return;
        }

        boolean paid;
        if (useTeam) {
            if (gp.team == null) return;
            if (gp.team.points < type.cost) {
                game.bars().push(p, msg.t("&c队伍技能点不足 (" + gp.team.points + "/" + type.cost + ")"), 40, null);
                return;
            }
            gp.team.points -= type.cost;
            paid = true;
        } else {
            if (gp.personalPoints < type.cost) {
                game.bars().push(p, msg.t("&c个人技能点不足 (" + gp.personalPoints + "/" + type.cost + ")"), 40, null);
                return;
            }
            gp.personalPoints -= type.cost;
            paid = true;
        }
        if (!paid) return;

        if (type.cooldown > 0) {
            gp.skillCd.put(type, System.currentTimeMillis() + type.cooldown * 1000L);
        }
        if (type == SkillType.ONESHOT) {
            gp.oneShot = true;
            p.sendMessage(game.prefix() + msg.t("&a已充能一击必杀, 下次箭矢命中将额外造成5HP伤害"));
        } else {
            gp.armed = type;
            p.sendMessage(game.prefix() + msg.t("&a技能 &e" + type.display + " &a已就绪, 瞄准方向射出技能箭"));
        }
        p.closeInventory();
        gp.guiOpen = false;
    }

    public void executeSkillArrow(SkillType type, Player shooter, Location landing) {
        GamePlayer gp = game.get(shooter);
        if (gp == null) return;
        if (type == SkillType.TELEPORT) {
            Location target = landing.clone();
            Block block = target.getBlock();
            int tries = 0;
            while (block.getType().isSolid() && tries < 6) {
                target = target.add(0, 1, 0);
                block = target.getBlock();
                tries++;
            }
            target.setYaw(shooter.getLocation().getYaw());
            target.setPitch(shooter.getLocation().getPitch());
            shooter.teleport(target);
            shooter.sendMessage(game.prefix() + msg.t("&a传送完成"));
            return;
        }
        if (type == SkillType.REVIVE) {
            GamePlayer best = null;
            double bestDist = Double.MAX_VALUE;
            for (GamePlayer other : game.allPlayers()) {
                if (other.state != PlayerState.DIED) continue;
                Player op = other.player();
                if (op == null) continue;
                double dist = op.getLocation().distanceSquared(landing);
                if (dist <= 25 && dist < bestDist) {
                    bestDist = dist;
                    best = other;
                }
            }
            if (best == null) {
                shooter.sendMessage(game.prefix() + msg.t("&c未命中: 落点附近没有已死亡的玩家, 技能点不退还"));
                return;
            }
            if (game.reviveBySkill(best, gp)) {
                shooter.sendMessage(game.prefix() + msg.t("&a成功复活 &e" + best.name));
            }
        }
    }
}
