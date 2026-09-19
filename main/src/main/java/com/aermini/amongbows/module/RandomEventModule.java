package com.aermini.amongbows.module;

import com.aermini.amongbows.game.Game;
import com.aermini.amongbows.game.GamePlayer;
import com.aermini.amongbows.game.InjuryType;
import com.aermini.amongbows.game.PlayerState;
import com.aermini.amongbows.game.Team;
import com.aermini.amongbows.game.TeamColor;
import com.aermini.amongbows.utils.GameItems;
import com.aermini.amongbows.utils.msg;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RandomEventModule {

    private final Game game;
    private long corrosionUntil;
    private long nextCorrosionAt;
    private long weakUntil;
    private long truthStart = -1;
    private int truthUses;
    private final List<UUID> weakGranted = new ArrayList<>();

    public RandomEventModule(Game game) {
        this.game = game;
    }

    public void reset() {
        corrosionUntil = 0;
        nextCorrosionAt = 0;
        weakUntil = 0;
        truthStart = -1;
        truthUses = 0;
        weakGranted.clear();
    }

    public boolean isWeakArrow() {
        return weakUntil > System.currentTimeMillis();
    }

    public boolean isTruthActive() {
        long now = System.currentTimeMillis();
        return truthStart > 0 && now >= truthStart && now <= truthStart + 3000L;
    }

    public void tick(long now) {
        if (corrosionUntil > now && now >= nextCorrosionAt) {
            for (GamePlayer gp : game.allPlayers()) {
                if (gp.state == PlayerState.LIVE) {
                    game.damage(gp, 1, InjuryType.DOT, null, null, null);
                }
            }
            nextCorrosionAt = now + 5000L;
        }
        if (weakUntil > 0 && now >= weakUntil) {
            for (UUID uuid : weakGranted) {
                GamePlayer gp = game.get(uuid);
                if (gp == null) continue;
                Player p = gp.player();
                if (p != null) {
                    GameItems.removeArrows(p, 3);
                }
            }
            weakGranted.clear();
            weakUntil = 0;
        }
    }

    public void triggerRandom() {
        int pick = 1 + game.random().nextInt(8);
        if (pick == 5 && truthUses >= 2) {
            pick = 1 + game.random().nextInt(4);
        }
        switch (pick) {
            case 1: crisis(); break;
            case 2: lifeSurge(); break;
            case 3: corrosion(); break;
            case 4: charge(); break;
            case 5: truthMoment(); break;
            case 6: airdrop(); break;
            case 7: bodyEnhance(); break;
            default: weakArrow(); break;
        }
    }

    private void crisis() {
        List<GamePlayer> alive = alivePlayers();
        int count = 1 + game.random().nextInt(3);
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < count && !alive.isEmpty(); i++) {
            GamePlayer gp = alive.remove(game.random().nextInt(alive.size()));
            TeamColor wrong;
            do {
                wrong = TeamColor.values()[game.random().nextInt(TeamColor.values().length)];
            } while (wrong == gp.team.color);
            game.jerseys().forceWear(gp, wrong);
            names.append(gp.name).append(" ");
        }
        if (names.length() > 0) {
            game.broadcast(game.prefix() + msg.t("&6[随机事件] 服装危机: &f以下玩家被强制更换队服: &e" + names.toString().trim()));
        }
    }

    private void lifeSurge() {
        for (GamePlayer gp : game.allPlayers()) {
            if (gp.state == PlayerState.LIVE) {
                game.healHp(gp, 3);
            }
        }
        game.broadcast(game.prefix() + msg.t("&6[随机事件] 生命涌动: &f所有存活玩家恢复3HP"));
    }

    private void corrosion() {
        long now = System.currentTimeMillis();
        corrosionUntil = now + 30000L;
        nextCorrosionAt = now + 5000L;
        game.broadcast(game.prefix() + msg.t("&6[随机事件] 腐蚀之雨: &c30秒内每5秒所有存活玩家受到1点伤害"));
    }

    private void charge() {
        for (GamePlayer gp : game.allPlayers()) {
            gp.skillCd.clear();
            gp.personalPoints = Math.min(10, gp.personalPoints + 1);
        }
        for (Team team : game.teams().values()) {
            team.points += 2;
        }
        game.broadcast(game.prefix() + msg.t("&6[随机事件] 充能: &f个人技能CD刷新, 个人技能点+1, 各队伍技能点+2"));
    }

    private void truthMoment() {
        truthStart = System.currentTimeMillis() + 2000L;
        truthUses++;
        game.broadcast(game.prefix() + msg.t("&6[随机事件] 真相时刻: &f2秒后, 穿着错误队服的玩家名字将在计分板上变粉3秒"));
    }

    private void airdrop() {
        Location loc = game.randomSafeSpot();
        if (loc == null) return;
        Block block = loc.getBlock();
        if (block.getType() != Material.AIR) {
            block = loc.clone().add(0, 1, 0).getBlock();
        }
        if (block.getType() != Material.AIR) return;
        block.setType(Material.CHEST);
        Chest chest = (Chest) block.getState();
        int count = 1 + game.random().nextInt(2);
        for (int i = 0; i < count; i++) {
            String oid = randomItem();
            ItemStack stack = createItem(oid);
            if (stack != null) {
                chest.getInventory().addItem(stack);
            }
        }
        chest.update();
        game.broadcast(game.prefix() + msg.t("&6[随机事件] 空投: &f一个道具箱降落在场地中!"));
    }

    private void bodyEnhance() {
        for (GamePlayer gp : game.allPlayers()) {
            Player p = gp.player();
            if (p == null || gp.state != PlayerState.LIVE) continue;
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 1), true);
            PotionEffectType jump = PotionEffectType.getByName("JUMP");
            if (jump == null) jump = PotionEffectType.getByName("JUMP_BOOST");
            if (jump != null) {
                p.addPotionEffect(new PotionEffect(jump, 600, 1), true);
            }
        }
        game.broadcast(game.prefix() + msg.t("&6[随机事件] 体质增强: &f所有玩家获得速度II、跳跃II效果, 持续30秒"));
    }

    private void weakArrow() {
        long now = System.currentTimeMillis();
        weakUntil = now + 15000L;
        weakGranted.clear();
        for (GamePlayer gp : game.allPlayers()) {
            Player p = gp.player();
            if (p == null) continue;
            GameItems.addArrows(p, 3);
            weakGranted.add(gp.uuid);
        }
        game.broadcast(game.prefix() + msg.t("&6[随机事件] 弱箭: &c15秒内所有伤害减半, 所有玩家获得3支临时箭矢"));
    }

    private List<GamePlayer> alivePlayers() {
        List<GamePlayer> list = new ArrayList<>();
        for (GamePlayer gp : game.allPlayers()) {
            if (gp.state == PlayerState.LIVE) list.add(gp);
        }
        return list;
    }

    private String randomItem() {
        String[] oids = {GameItems.ARROWPACK, GameItems.SKILLPACK, GameItems.HEAL, GameItems.GAPPLE,
            GameItems.CLUEBOOK, GameItems.SCROLL, GameItems.AMULET};
        return oids[game.random().nextInt(oids.length)];
    }

    private ItemStack createItem(String oid) {
        switch (oid) {
            case GameItems.HEAL: return GameItems.heal(1);
            case GameItems.GAPPLE: return GameItems.gapple();
            case GameItems.CLUEBOOK: return GameItems.cluebook();
            case GameItems.SCROLL: return GameItems.scroll();
            case GameItems.AMULET: return GameItems.amulet();
            case GameItems.SKILLPACK: return GameItems.skillpack();
            case GameItems.ARROWPACK: return GameItems.arrowpack();
            default: return null;
        }
    }
}
