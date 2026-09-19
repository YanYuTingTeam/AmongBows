package com.aermini.amongbows.module;

import com.aermini.amongbows.game.Game;
import com.aermini.amongbows.game.GamePlayer;
import com.aermini.amongbows.game.PlayerState;
import com.aermini.amongbows.game.TeamColor;
import com.aermini.amongbows.utils.GameItems;
import com.aermini.amongbows.utils.msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class JerseyModule {

    private final Game game;

    public JerseyModule(Game game) {
        this.game = game;
    }

    public boolean switchTo(Player p, TeamColor color) {
        GamePlayer gp = game.get(p);
        if (gp == null || gp.state != PlayerState.LIVE) return false;
        long now = System.currentTimeMillis();
        if (now < gp.jerseyCdUntil) {
            long remain = (gp.jerseyCdUntil - now + 999) / 1000;
            game.bars().push(p, msg.t("&c队服切换冷却中, 还需 &e" + remain + " &c秒"), 40, null);
            return true;
        }
        if (!GameItems.consumeOne(p, "jersey_" + color.id)) return false;
        TeamColor previous = gp.jersey;
        equipArmor(gp, color);
        gp.jersey = color;
        gp.jerseyCdUntil = now + 5000L;
        if (previous != null && previous != color) {
            p.getInventory().addItem(GameItems.jersey(previous));
            p.updateInventory();
        }
        p.sendMessage(game.prefix() + msg.t("&f你换上了 ") + game.lang().teamColor(color) + game.lang().teamName(color) + msg.t("队服"));
        return true;
    }

    public void forceWear(GamePlayer gp, TeamColor color) {
        if (gp.jersey == color) return;
        Player p = gp.player();
        if (p != null) {
            GameItems.consumeOne(p, "jersey_" + color.id);
        }
        equipArmor(gp, color);
        gp.jersey = color;
    }

    private void equipArmor(GamePlayer gp, TeamColor color) {
        Player p = gp.player();
        if (p == null) return;
        PlayerInventory inv = p.getInventory();
        inv.setHelmet(jerseyPiece(color, Material.LEATHER_HELMET));
        inv.setChestplate(jerseyPiece(color, Material.LEATHER_CHESTPLATE));
        inv.setLeggings(jerseyPiece(color, Material.LEATHER_LEGGINGS));
        inv.setBoots(jerseyPiece(color, Material.LEATHER_BOOTS));
        p.updateInventory();
    }

    private ItemStack jerseyPiece(TeamColor color, org.bukkit.Material material) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color.leather);
        meta.setDisplayName(GameItems.jerseyName(color));
        meta.setLore(java.util.Collections.singletonList(msg.t("&7队服颜色不代表真实队伍")));
        item.setItemMeta(meta);
        return item;
    }
}
