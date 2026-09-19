package com.aermini.amongbows.module;

import com.aermini.amongbows.game.Game;
import com.aermini.amongbows.game.GamePlayer;
import com.aermini.amongbows.game.PlayerState;
import com.aermini.amongbows.utils.GameItems;
import com.aermini.amongbows.utils.msg;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class HealModule {

    private final Game game;

    public HealModule(Game game) {
        this.game = game;
    }

    public boolean healOther(Player healer, Player victim) {
        GamePlayer healerGp = game.get(healer);
        GamePlayer victimGp = game.get(victim);
        if (healerGp == null || victimGp == null) return false;
        if (healerGp.state != PlayerState.LIVE || victimGp.state != PlayerState.LIVE) return false;
        if (healer == victim) {
            healSelf(healerGp);
            return true;
        }
        if (healerGp.hp <= 2) {
            game.bars().push(healer, msg.t("&c生命值不足, 无法为他人治疗"), 40, null);
            return true;
        }
        if (!GameItems.consumeOne(healer, GameItems.HEAL)) return false;
        game.costHealth(healerGp, 2);
        game.healHp(victimGp, 5);
        healer.sendMessage(game.prefix() + msg.t("&f你为 &e" + victim.getName()
            + " &f治疗了 &a+5HP&f, 自身扣除 &c-2HP"));
        victim.sendMessage(game.prefix() + msg.t("&a你被 &e" + healer.getName() + " &a治疗了 &f+5HP"));
        return true;
    }

    public boolean dropHeal(PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        GamePlayer gp = game.get(p);
        if (gp == null || gp.state != PlayerState.LIVE) return false;
        event.getItemDrop().remove();
        game.healHp(gp, 3);
        p.sendMessage(game.prefix() + msg.t("&f丢弃治疗药水, 自身恢复 &a+3HP"));
        return true;
    }

    private void healSelf(GamePlayer gp) {
        Player p = gp.player();
        if (p == null) return;
        if (!GameItems.consumeOne(p, GameItems.HEAL)) return;
        game.healHp(gp, 3);
        p.sendMessage(game.prefix() + msg.t("&f点击自己, 自身恢复 &a+3HP"));
    }

    public void addCharge(Player p) {
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;
            if (GameItems.HEAL.equals(GameItems.oidOf(item))) {
                item.setAmount(Math.min(64, item.getAmount() + 1));
                p.updateInventory();
                return;
            }
        }
        p.getInventory().addItem(GameItems.heal(1));
        p.updateInventory();
    }
}
