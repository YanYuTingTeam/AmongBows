package com.aermini.amongbows.module;

import com.aermini.amongbows.game.Game;
import com.aermini.amongbows.game.GamePlayer;
import com.aermini.amongbows.game.InjuryType;
import com.aermini.amongbows.game.MarkType;
import com.aermini.amongbows.game.PlayerState;
import com.aermini.amongbows.game.SkillType;
import com.aermini.amongbows.utils.GameItems;
import com.aermini.amongbows.utils.msg;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BowModule {

    private final Game game;
    private final Map<UUID, SkillType> skillArrows = new HashMap<>();

    public BowModule(Game game) {
        this.game = game;
    }

    public boolean handleInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack hand = p.getItemInHand();
        if (hand == null || hand.getType() != Material.BOW) return false;
        if (!GameItems.BOW.equals(GameItems.oidOf(hand))) return false;
        GamePlayer gp = game.get(p);
        if (gp == null || gp.state != PlayerState.LIVE) return false;

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            game.skills().openGUI(p);
            return true;
        }
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (System.currentTimeMillis() < gp.bowCdUntil) {
                gp.drawing = false;
                game.bars().push(p, msg.t("&c弓冷却中..."), 40, null);
                return true;
            }
            gp.drawing = true;
            return false;
        }
        return false;
    }

    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        Arrow arrow = (Arrow) event.getEntity();
        ProjectileSource shooter = arrow.getShooter();
        if (!(shooter instanceof Player)) return;
        GamePlayer gp = game.get((Player) shooter);
        if (gp == null) return;
        if (System.currentTimeMillis() < gp.bowCdUntil) {
            event.setCancelled(true);
            return;
        }
        gp.bowCdUntil = System.currentTimeMillis() + 2000L;
        gp.drawing = false;
        gp.mark(MarkType.FIRE, 2000L);
        SkillType armed = gp.armed;
        if (armed != null) {
            gp.armed = null;
            if (armed == SkillType.TELEPORT || armed == SkillType.REVIVE) {
                skillArrows.put(arrow.getUniqueId(), armed);
            } else if (armed == SkillType.ONESHOT) {
                gp.oneShot = true;
            }
        }
    }

    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        Arrow arrow = (Arrow) event.getEntity();
        SkillType skill = skillArrows.remove(arrow.getUniqueId());
        ProjectileSource shooter = arrow.getShooter();
        boolean fromParticipant = shooter instanceof Player && game.get((Player) shooter) != null;
        if (!fromParticipant) return;
        arrow.remove();
        if (skill != null && shooter instanceof Player) {
            game.skills().executeSkillArrow(skill, (Player) shooter, arrow.getLocation());
        }
    }

    public boolean onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return false;
        if (!(event.getDamager() instanceof Arrow)) return false;
        Player victim = (Player) event.getEntity();
        Arrow arrow = (Arrow) event.getDamager();
        event.setCancelled(true);
        ProjectileSource shooter = arrow.getShooter();
        if (!(shooter instanceof Player)) return true;
        GamePlayer attacker = game.get((Player) shooter);
        GamePlayer victimGp = game.get(victim);
        if (attacker == null || victimGp == null) return true;
        if (attacker.state != PlayerState.LIVE || victimGp.state != PlayerState.LIVE) return true;

        boolean oneShot = attacker.oneShot;
        attacker.oneShot = false;
        if (attacker == victimGp) {
            game.damage(victimGp, 5, InjuryType.SELF, attacker, GameItems.BOW, null);
        } else if (attacker.team != null && attacker.team == victimGp.team) {
            game.damage(victimGp, oneShot ? 5 : 3, InjuryType.FRIENDLY, attacker, GameItems.BOW, oneShot ? "一击必杀" : null);
            game.damage(attacker, 5, InjuryType.FRIENDLY, attacker, GameItems.BOW, null);
        } else {
            game.damage(victimGp, 5 + (oneShot ? 5 : 0), InjuryType.ARROW, attacker, GameItems.BOW, oneShot ? "一击必杀" : null);
        }
        game.firePlayerHit(attacker, victimGp);
        return true;
    }
}
