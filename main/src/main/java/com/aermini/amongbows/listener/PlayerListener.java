package com.aermini.amongbows.listener;

import com.aermini.amongbows.AmongBows;
import com.aermini.amongbows.game.Game;
import com.aermini.amongbows.game.GamePlayer;
import com.aermini.amongbows.game.InjuryType;
import com.aermini.amongbows.game.MarkType;
import com.aermini.amongbows.game.PlayerState;
import com.aermini.amongbows.game.TeamColor;
import com.aermini.amongbows.utils.GameItems;
import com.aermini.amongbows.utils.msg;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    private final AmongBows plugin;

    public PlayerListener(AmongBows plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.manager().restore(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.manager().onQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        Game game = plugin.manager().game();
        GamePlayer gp = game.get(victim);
        if (gp == null) return;
        event.setCancelled(true);
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent byEntity = (EntityDamageByEntityEvent) event;
            if (byEntity.getDamager() instanceof Arrow) {
                game.bow().onArrowHit(byEntity);
                return;
            }
            if (byEntity.getDamager() instanceof Player) {
                Player attacker = (Player) byEntity.getDamager();
                ItemStack hand = attacker.getItemInHand();
                if (GameItems.HEAL.equals(GameItems.oidOf(hand))) {
                    game.heal().healOther(attacker, victim);
                }
            }
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) return;
        double scale = 2 * plugin.configs().heartsPerHp();
        double gameHp = event.getDamage() / scale;
        if (gameHp < 0.5) return;
        InjuryType type = event.getCause() == EntityDamageEvent.DamageCause.FALL ? InjuryType.FALL : InjuryType.ENV;
        game.damage(gp, gameHp, type, null, null, null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        Game game = plugin.manager().game();
        GamePlayer gp = game.get(p);
        if (gp == null) return;
        ItemStack item = event.getItem();
        if (item == null) return;

        if (item.getType() == org.bukkit.Material.BOW && GameItems.BOW.equals(GameItems.oidOf(item))) {
            if (game.bow().handleInteract(event)) {
                event.setCancelled(true);
            }
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        String oid = GameItems.oidOf(item);
        if (oid == null) return;
        event.setCancelled(true);
        TeamColor jersey = GameItems.jerseyColor(oid);
        if (jersey != null) {
            game.jerseys().switchTo(p, jersey);
        } else if (oid.equals(GameItems.GUESS)) {
            game.guess().open(p);
        } else if (oid.equals(GameItems.CLUEBOOK)) {
            game.clue().use(p);
        } else if (oid.equals(GameItems.SCROLL)) {
            if (gp.state != PlayerState.LIVE) return;
            GameItems.consumeOne(p, GameItems.SCROLL);
            Location loc = game.randomSafeSpot();
            p.teleport(loc);
            p.sendMessage(game.prefix() + msg.t("&b传送卷轴已生效, 你被传送到了安全落脚点"));
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player p = event.getPlayer();
        Game game = plugin.manager().game();
        if (game.get(p) == null) return;
        if (!(event.getRightClicked() instanceof Player)) return;
        ItemStack hand = p.getItemInHand();
        if (GameItems.HEAL.equals(GameItems.oidOf(hand))) {
            event.setCancelled(true);
            game.heal().healOther(p, (Player) event.getRightClicked());
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        Game game = plugin.manager().game();
        GamePlayer gp = game.get(p);
        if (gp == null) return;
        String oid = GameItems.oidOf(event.getItemDrop().getItemStack());
        if (oid == null) return;
        if (oid.equals(GameItems.HEAL)) {
            game.heal().dropHeal(event);
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        Player p = event.getPlayer();
        Game game = plugin.manager().game();
        GamePlayer gp = game.get(p);
        if (gp == null) return;
        ItemStack stack = event.getItem().getItemStack();
        String oid = GameItems.oidOf(stack);
        if (oid == null) return;
        event.setCancelled(true);
        switch (oid) {
            case GameItems.SKILLPACK: {
                event.getItem().remove();
                gp.personalPoints = Math.min(10, gp.personalPoints + 1);
                if (gp.team != null) gp.team.points++;
                p.sendMessage(game.prefix() + msg.t("&d技能点包: &f个人技能点+1, 队伍技能点+1"));
                gp.mark(MarkType.GETITEM, 3000L);
                game.firePickup(gp, oid);
                return;
            }
            case GameItems.ARROWPACK: {
                event.getItem().remove();
                GameItems.addArrows(p, 3);
                p.sendMessage(game.prefix() + msg.t("&f箭矢包: &a+3支箭"));
                gp.mark(MarkType.GETITEM, 3000L);
                game.firePickup(gp, oid);
                return;
            }
            case GameItems.HEAL: {
                event.getItem().remove();
                game.heal().addCharge(p);
                p.sendMessage(game.prefix() + msg.t("&c治疗药水: &a治疗机会+1"));
                gp.mark(MarkType.GETITEM, 3000L);
                game.firePickup(gp, oid);
                return;
            }
            default: {
                int limit = GameItems.limitOf(oid);
                if (GameItems.countOf(p, oid) >= limit) {
                    game.bars().push(p, msg.t("&c手持该道具已达上限"), 40, null);
                    return;
                }
                int amount = stack.getAmount();
                event.getItem().remove();
                ItemStack give = stack.clone();
                give.setAmount(amount);
                p.getInventory().addItem(give);
                p.updateInventory();
                gp.mark(MarkType.GETITEM, 3000L);
                game.firePickup(gp, oid);
            }
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player p = event.getPlayer();
        Game game = plugin.manager().game();
        GamePlayer gp = game.get(p);
        if (gp == null) return;
        String oid = GameItems.oidOf(event.getItem());
        if (oid == null) return;
        event.setCancelled(true);
        if (oid.equals(GameItems.GAPPLE)) {
            GameItems.consumeOne(p, GameItems.GAPPLE);
            game.applyApple(gp);
            p.sendMessage(game.prefix() + msg.t("&6金苹果: &a永久+2HP &7(上限+5)"));
        }
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        GamePlayer gp = plugin.manager().game().get((Player) event.getEntity());
        if (gp == null) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        GamePlayer gp = plugin.manager().game().get(event.getPlayer());
        if (gp != null) {
            gp.drawing = false;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        GamePlayer gp = plugin.manager().game().get((Player) event.getPlayer());
        if (gp != null) {
            gp.guiOpen = false;
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        plugin.manager().game().bow().onLaunch(event);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        plugin.manager().game().bow().onProjectileHit(event);
    }
}
