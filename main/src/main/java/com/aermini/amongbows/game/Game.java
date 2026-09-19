package com.aermini.amongbows.game;

import com.aermini.amongbows.AmongBows;
import com.aermini.amongbows.action.ActionEngine;
import com.aermini.amongbows.action.ActionBarManager;
import com.aermini.amongbows.board.ScoreboardRenderer;
import com.aermini.amongbows.config.ItemConfig;
import com.aermini.amongbows.config.LangManager;
import com.aermini.amongbows.map.GameMap;
import com.aermini.amongbows.map.MapGenerator;
import com.aermini.amongbows.map.SafeSpots;
import com.aermini.amongbows.module.BowModule;
import com.aermini.amongbows.module.ClueModule;
import com.aermini.amongbows.module.DropModule;
import com.aermini.amongbows.module.GuessModule;
import com.aermini.amongbows.module.HealModule;
import com.aermini.amongbows.module.JerseyModule;
import com.aermini.amongbows.module.RandomEventModule;
import com.aermini.amongbows.module.SkillModule;
import com.aermini.amongbows.utils.GameItems;
import com.aermini.amongbows.utils.TextUtil;
import com.aermini.amongbows.utils.msg;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class Game {

    public enum Phase {WAITING, COUNTDOWN, PREPARE, BATTLE, ENDED}

    private final AmongBows plugin;
    private final GameMap map;
    private final ActionEngine actions;
    private final ActionBarManager bars;
    private final ScoreboardRenderer board;
    private final SafeSpots spots;
    private final LangManager lang;
    private final ItemConfig itemCfg;
    private final Random random = new Random();

    private final BowModule bow;
    private final SkillModule skills;
    private final GuessModule guess;
    private final HealModule heal;
    private final JerseyModule jerseys;
    private final ClueModule clue;
    private final DropModule drops;
    private final RandomEventModule events;

    private Phase phase = Phase.WAITING;
    private final List<GamePlayer> players = new ArrayList<>();
    private final Map<TeamColor, Team> teams = new EnumMap<>(TeamColor.class);
    private int countLeft;
    private int prepareLeft;
    private long gameStartAt;
    private long endAt;
    private long nextDropAt;
    private long nextEventAt;
    private Runnable onCleanup;

    public Game(AmongBows plugin, GameMap map, ActionEngine actions, ActionBarManager bars,
                ScoreboardRenderer board, SafeSpots spots, LangManager lang, ItemConfig itemCfg) {
        this.plugin = plugin;
        this.map = map;
        this.actions = actions;
        this.bars = bars;
        this.board = board;
        this.spots = spots;
        this.lang = lang;
        this.itemCfg = itemCfg;
        this.bow = new BowModule(this);
        this.skills = new SkillModule(this);
        this.guess = new GuessModule(this);
        this.heal = new HealModule(this);
        this.jerseys = new JerseyModule(this);
        this.clue = new ClueModule(this);
        this.drops = new DropModule(this);
        this.events = new RandomEventModule(this);
    }

    public AmongBows plugin() {
        return plugin;
    }

    public GameMap map() {
        return map;
    }

    public ActionEngine actions() {
        return actions;
    }

    public ActionBarManager bars() {
        return bars;
    }

    public ScoreboardRenderer board() {
        return board;
    }

    public SafeSpots spots() {
        return spots;
    }

    public LangManager lang() {
        return lang;
    }

    public ItemConfig itemCfg() {
        return itemCfg;
    }

    public Random random() {
        return random;
    }

    public BowModule bow() {
        return bow;
    }

    public SkillModule skills() {
        return skills;
    }

    public GuessModule guess() {
        return guess;
    }

    public HealModule heal() {
        return heal;
    }

    public JerseyModule jerseys() {
        return jerseys;
    }

    public ClueModule clue() {
        return clue;
    }

    public DropModule drops() {
        return drops;
    }

    public RandomEventModule events() {
        return events;
    }

    public Phase phase() {
        return phase;
    }

    public int countLeft() {
        return countLeft;
    }

    public Map<TeamColor, Team> teams() {
        return teams;
    }

    public List<GamePlayer> allPlayers() {
        return players;
    }

    public GamePlayer get(Player p) {
        if (p == null) return null;
        for (GamePlayer gp : players) {
            if (gp.uuid.equals(p.getUniqueId())) return gp;
        }
        return null;
    }

    public GamePlayer get(UUID uuid) {
        for (GamePlayer gp : players) {
            if (gp.uuid.equals(uuid)) return gp;
        }
        return null;
    }

    public void removePlayer(GamePlayer gp) {
        players.remove(gp);
        if (gp.team != null) {
            gp.team.members.remove(gp);
        }
    }

    public long elapsedSeconds() {
        return gameStartAt <= 0 ? 0 : (System.currentTimeMillis() - gameStartAt) / 1000L;
    }

    public long remainSeconds() {
        if (endAt <= 0) return map.time;
        return Math.max(0, (endAt - System.currentTimeMillis()) / 1000L);
    }

    public World mapWorld() {
        return Bukkit.getWorld(map.world);
    }

    public Location spawnLoc() {
        World world = mapWorld();
        if (world == null) world = Bukkit.getWorlds().get(0);
        return new Location(world, map.spawn[0], map.spawn[1], map.spawn[2]);
    }

    public Location randomSafeSpot() {
        Location loc = spots.randomPreferred(this, random);
        if (loc == null) {
            loc = spots.random(random);
        }
        return loc != null ? loc : spawnLoc();
    }

    public Location nearestSafe(Location from) {
        Location loc = spots.nearest(from);
        return loc != null ? loc : spawnLoc();
    }

    public String prefix() {
        return msg.t("&b[&3&l弓箭之间&b] ");
    }

    public void broadcast(String text) {
        for (GamePlayer gp : players) {
            Player p = gp.player();
            if (p != null) {
                p.sendMessage(text);
            }
        }
    }

    public boolean isTruthPink(GamePlayer gp) {
        return gp.team != null && gp.jersey != gp.team.color && events.isTruthActive();
    }

    public boolean join(Player p) {
        if (get(p) != null) return false;
        if (players.size() >= map.playersMax) return false;
        GamePlayer gp = new GamePlayer(p.getUniqueId(), p.getName(), plugin.configs().defaultMaxHp());
        players.add(gp);
        return true;
    }

    public void maybeStartCountdown() {
        if (phase == Phase.WAITING && players.size() >= map.playersMin) {
            phase = Phase.COUNTDOWN;
            countLeft = 30;
        }
    }

    public void cancelCountdown() {
        if (phase != Phase.COUNTDOWN) return;
        phase = Phase.WAITING;
        broadcast(prefix() + msg.t("&c人数不足, 倒计时已取消"));
    }

    public void tick() {
        switch (phase) {
            case COUNTDOWN: {
                if (players.size() < map.playersMin) {
                    cancelCountdown();
                    break;
                }
                countLeft--;
                Map<String, Object> args = baseArgs();
                args.put("count", countLeft);
                actions.fire("game-before-countdown", args, this, null, null, spawnLoc());
                if (countLeft <= 0) {
                    startGame();
                }
                break;
            }
            case PREPARE: {
                prepareLeft--;
                if (prepareLeft <= 0) {
                    startBattle();
                }
                break;
            }
            case BATTLE: {
                long now = System.currentTimeMillis();
                events.tick(now);
                if (now >= nextDropAt) {
                    drops.dropWave(now);
                    nextDropAt = now + 45000L;
                }
                if (now >= nextEventAt) {
                    events.triggerRandom();
                    nextEventAt = now + 90000L;
                }
                if (now >= endAt) {
                    finishByTimeout();
                    break;
                }
                checkWin();
                break;
            }
            default:
                break;
        }
        board.renderAll(this);
    }

    public void voidCheck() {
        if (phase != Phase.BATTLE && phase != Phase.PREPARE) return;
        World world = mapWorld();
        if (world == null) return;
        for (GamePlayer gp : new ArrayList<>(players)) {
            if (gp.state != PlayerState.LIVE) continue;
            Player p = gp.player();
            if (p == null || !p.getWorld().equals(world)) continue;
            if (p.getLocation().getY() < map.underY) {
                damage(gp, 5, InjuryType.VOID, null, null, null);
                if (gp.state == PlayerState.LIVE) {
                    enterRelive(gp);
                }
            }
        }
    }

    private void startGame() {
        int n = players.size();
        int[] plan = TeamPlanner.pick(n, random);
        if (plan == null) {
            phase = Phase.WAITING;
            return;
        }
        teams.clear();
        List<TeamColor> colors = TeamPlanner.colors(plan.length, random);
        List<GamePlayer> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled, random);
        int idx = 0;
        for (int i = 0; i < plan.length; i++) {
            Team team = new Team(colors.get(i), plan[i]);
            for (int k = 0; k < plan[i]; k++) {
                GamePlayer gp = shuffled.get(idx++);
                gp.team = team;
                team.members.add(gp);
            }
            teams.put(colors.get(i), team);
        }

        World world = mapWorld();
        if (world != null) {
            MapGenerator.generate(world, map, random);
        }
        spots.scan(world, map);

        for (int i = 0; i < players.size(); i++) {
            GamePlayer gp = players.get(i);
            gp.state = PlayerState.LIVE;
            gp.hp = 10;
            gp.personalPoints = 3;
            gp.appleBonus = 0;
            gp.lastHealth = 0;
            gp.revivedCount = 0;
            gp.kills = 0;
            gp.marks.clear();
            gp.skillCd.clear();
            gp.armed = null;
            gp.oneShot = false;
            gp.drawing = false;
            gp.guiOpen = false;
            gp.jersey = null;
            gp.bowCdUntil = 0;
            gp.jerseyCdUntil = 0;
            gp.guessCdUntil = 0;
            gp.invincibleUntil = 0;
            gp.stateUntil = 0;
            Player p = gp.player();
            if (p == null) continue;
            p.setGameMode(GameMode.SURVIVAL);
            p.setFoodLevel(20);
            for (org.bukkit.potion.PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            double angle = Math.PI * 2 * i / Math.max(1, players.size());
            Location loc = spawnLoc().add(Math.cos(angle) * 3, 1, Math.sin(angle) * 3);
            p.teleport(loc);
            giveInitialItems(gp, n);
            TeamColor randColor = TeamColor.values()[random.nextInt(TeamColor.values().length)];
            jerseys.forceWear(gp, randColor);
            syncHealth(gp);
            p.sendMessage(prefix() + msg.t("&f你的真实队伍是: " + lang.teamName(gp.team.color)));
        }
        if (world != null) {
            world.setGameRuleValue("naturalRegeneration", "false");
        }
        gameStartAt = System.currentTimeMillis();
        endAt = gameStartAt + map.time * 1000L;
        prepareLeft = 15;
        phase = Phase.PREPARE;

        Map<String, Object> args = baseArgs();
        actions.firePlanned("game-gamestart_inst", () -> args, this, null, spawnLoc(), System.currentTimeMillis());
    }

    private void giveInitialItems(GamePlayer gp, int playerCount) {
        Player p = gp.player();
        if (p == null) return;
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        int arrows = 5 + playerCount / 2;
        p.getInventory().setItem(0, GameItems.bow(itemCfg.bowName(), itemCfg.bowLore()));
        GameItems.addArrows(p, arrows);
        p.getInventory().setItem(2, GameItems.heal(2));
        p.getInventory().setItem(3, GameItems.guess());
        int slot = 4;
        TeamColor[] colors = TeamColor.values();
        TeamColor worn = colors[random.nextInt(colors.length)];
        for (TeamColor color : colors) {
            if (color == worn) continue;
            if (slot < 9) {
                p.getInventory().setItem(slot, GameItems.jersey(color));
                slot++;
            }
        }
        p.updateInventory();
        jerseys.forceWear(gp, worn);
    }

    private void startBattle() {
        phase = Phase.BATTLE;
        long now = System.currentTimeMillis();
        nextDropAt = now + 30000L;
        nextEventAt = now + 90000L;
        for (GamePlayer gp : players) {
            jerseys.forceWear(gp, gp.team.color);
            gp.team.lastInjuryAt = now;
        }
    }

    public void checkWin() {
        if (phase != Phase.BATTLE) return;
        long now = System.currentTimeMillis();
        for (Team team : teams.values()) {
            int alive = team.aliveCount();
            if (alive == 0) continue;
            int othersAlive = 0;
            boolean othersOut = true;
            for (Team other : teams.values()) {
                if (other == team) continue;
                othersAlive += other.aliveCount();
                if (!other.allOutOfGame()) othersOut = false;
            }
            boolean condA = alive >= othersAlive + 1 && now - team.lastInjuryAt >= 15000L;
            if (condA || othersOut) {
                finish(team);
                return;
            }
        }
    }

    private void finishByTimeout() {
        Team best = null;
        boolean tie = false;
        for (Team t : teams.values()) {
            if (best == null) {
                best = t;
                continue;
            }
            int cmp = Integer.compare(t.aliveCount(), best.aliveCount());
            if (cmp == 0) {
                cmp = Integer.compare(t.totalKills(), best.totalKills());
            }
            if (cmp > 0) {
                best = t;
                tie = false;
            } else if (cmp == 0) {
                tie = true;
            }
        }
        finish(tie ? null : best);
    }

    public void finish(Team winner) {
        if (phase == Phase.ENDED || phase == Phase.WAITING) return;
        phase = Phase.ENDED;
        World world = mapWorld();
        if (world != null) {
            world.setGameRuleValue("naturalRegeneration", "true");
        }
        Map<String, Object> args = baseArgs();
        if (winner != null) {
            Map<String, Object> winnerArgs = teamArgs(winner);
            int i = 1;
            for (GamePlayer gp : winner.members) {
                winnerArgs.put(String.valueOf(i), playerArgs(gp));
                i++;
            }
            args.put("winner", winnerArgs);
        }
        GamePlayer mvp = null;
        for (GamePlayer gp : players) {
            if (mvp == null || gp.kills > mvp.kills) mvp = gp;
        }
        if (mvp != null && mvp.kills > 0) {
            args.put("mvp", playerArgs(mvp));
        }
        actions.fire("game-gameover_inst", args, this, null, null, null);

        java.util.List<String> lines = new ArrayList<>();
        lines.add(prefix() + msg.t("&6&l==== 对局结束 ===="));
        if (winner != null) {
            lines.add(prefix() + msg.t("&f获胜队伍: ") + lang.teamColor(winner.color) + lang.teamName(winner.color));
        } else {
            lines.add(prefix() + msg.t("&f本场对局平局"));
        }
        for (Team team : teams.values()) {
            lines.add(prefix() + msg.t("&f") + lang.teamColor(team.color) + lang.teamName(team.color)
                + msg.t(" &7存活 &f" + team.aliveCount()
                    + " &7死亡 &f" + team.diedCount()
                    + " &7淘汰 &f" + team.lossCount()
                    + " &7击杀 &f" + team.totalKills()
                    + " &7技能点 &f" + team.points));
        }
        if (mvp != null && mvp.kills > 0) {
            lines.add(prefix() + msg.t("&6全场最佳: &e" + mvp.name + " &7(" + mvp.kills + "击杀)"));
        }
        for (String line : lines) {
            broadcast(line);
        }
        Bukkit.getScheduler().runTaskLater(plugin, this::cleanup, 200L);
    }

    public void cleanup() {
        for (GamePlayer gp : new ArrayList<>(players)) {
            Player p = gp.player();
            if (p != null) {
                p.setGameMode(GameMode.SURVIVAL);
                for (org.bukkit.potion.PotionEffect effect : p.getActivePotionEffects()) {
                    p.removePotionEffect(effect.getType());
                }
                p.setMaxHealth(20.0);
                p.setHealth(20.0);
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
            bars.clear(gp.uuid);
            gp.state = PlayerState.WAITING;
            gp.team = null;
        }
        players.clear();
        teams.clear();
        phase = Phase.WAITING;
        gameStartAt = 0;
        endAt = 0;
        events.reset();
        if (onCleanup != null) {
            onCleanup.run();
        }
        board.renderAll(this);
    }

    public void setOnCleanup(Runnable onCleanup) {
        this.onCleanup = onCleanup;
    }

    public void damage(GamePlayer gp, double amount, InjuryType type, GamePlayer attacker, String itemOid, String skillName) {
        if (amount <= 0) return;
        if (phase != Phase.BATTLE && !(phase == Phase.PREPARE && type == InjuryType.VOID)) return;
        Player p = gp.player();
        if (p == null || gp.state != PlayerState.LIVE) return;
        long now = System.currentTimeMillis();
        if (now < gp.invincibleUntil) return;
        if (gp.hp - amount <= 0 && takeAmulet(gp)) return;
        if (events.isWeakArrow()) {
            amount = Math.floor(amount / 2);
        }
        if (amount <= 0) amount = 0;
        double overflow = Math.max(0, amount - gp.hp);
        gp.hp = Math.max(0, gp.hp - amount);
        if (type.injury && gp.team != null) {
            gp.team.lastInjuryAt = now;
        }
        gp.mark(MarkType.HPLOST, 3000L);
        syncHealth(gp);
        if (gp.hp <= 0) {
            gp.lastHealth = -overflow;
            die(gp, attacker, itemOid, skillName);
        }
    }

    private boolean takeAmulet(GamePlayer gp) {
        Player p = gp.player();
        if (p == null) return false;
        if (!GameItems.consumeOne(p, GameItems.AMULET)) return false;
        gp.invincibleUntil = System.currentTimeMillis() + 2000L;
        bars.push(p, msg.t("&a护身符抵挡了致命伤害! 获得2秒无敌"), 60, null);
        return true;
    }

    public void costHealth(GamePlayer gp, double amount) {
        gp.hp = Math.max(0, gp.hp - amount);
        gp.mark(MarkType.HPLOST, 3000L);
        syncHealth(gp);
        if (gp.hp <= 0) {
            die(gp, null, null, null);
        }
    }

    public void healHp(GamePlayer gp, double amount) {
        gp.hp = Math.min(gp.effectiveMaxHp(), gp.hp + amount);
        syncHealth(gp);
    }

    public void applyApple(GamePlayer gp) {
        gp.appleBonus = Math.min(5, gp.appleBonus + 2);
        healHp(gp, 2);
    }

    public void syncHealth(GamePlayer gp) {
        Player p = gp.player();
        if (p == null) return;
        double scale = 2 * plugin.configs().heartsPerHp();
        double maxHealth = gp.effectiveMaxHp() * scale;
        p.setMaxHealth(maxHealth);
        p.setHealth(Math.max(1.0, Math.min(gp.hp * scale, maxHealth)));
    }

    private void die(GamePlayer gp, GamePlayer attacker, String itemOid, String skillName) {
        gp.state = gp.revivedCount > 0 ? PlayerState.LOSS : PlayerState.DIED;
        gp.armed = null;
        gp.drawing = false;
        Player p = gp.player();
        if (p != null) {
            p.setGameMode(GameMode.SPECTATOR);
        }
        if (attacker != null && attacker != gp && gp.team != null && attacker.team != null && attacker.team != gp.team) {
            attacker.kills++;
            attacker.personalPoints = Math.min(10, attacker.personalPoints + 2);
            attacker.team.points++;
            Player kp = attacker.player();
            if (kp != null) {
                bars.push(kp, msg.t("&6击杀 +2个人技能点 &7(" + attacker.personalPoints + "/10)"), 60, null);
            }
        }
        Map<String, Object> args = baseArgs();
        args.put("player", playerArgs(gp));
        if (attacker != null && attacker != gp) {
            args.put("killer", playerArgs(attacker));
        }
        if (itemOid != null) {
            args.put("item", itemArgs(itemOid));
        }
        if (skillName != null) {
            Map<String, Object> skill = new LinkedHashMap<>();
            skill.put("name", skillName);
            skill.put("id", skillName);
            args.put("skill", skill);
        }
        actions.fire("player-death", args, this, p, attacker != null ? attacker.player() : null, p != null ? p.getLocation() : null);
    }

    private void enterRelive(GamePlayer gp) {
        gp.state = PlayerState.RELIVE;
        gp.stateUntil = System.currentTimeMillis() + 10000L;
        Player p = gp.player();
        if (p != null) {
            p.setGameMode(GameMode.SPECTATOR);
        }
    }

    public void tickRelive() {
        long now = System.currentTimeMillis();
        for (GamePlayer gp : new ArrayList<>(players)) {
            if (gp.state != PlayerState.RELIVE) continue;
            if (now < gp.stateUntil) continue;
            Player p = gp.player();
            gp.state = PlayerState.LIVE;
            gp.hp = Math.min(gp.hp, 6);
            gp.invincibleUntil = now + 1000L;
            if (p != null) {
                p.setGameMode(GameMode.SURVIVAL);
                Location loc = nearestSafe(p.getLocation());
                loc.setYaw(p.getLocation().getYaw());
                loc.setPitch(p.getLocation().getPitch());
                p.teleport(loc);
            }
            syncHealth(gp);
        }
    }

    public boolean reviveBySkill(GamePlayer target, GamePlayer reviver) {
        if (target.state != PlayerState.DIED) return false;
        target.state = PlayerState.LIVE;
        target.revivedCount++;
        double owed = Math.max(0, -target.lastHealth);
        target.hp = Math.max(1, 6 - owed);
        Player tp = target.player();
        if (tp != null) {
            tp.setGameMode(GameMode.SURVIVAL);
            tp.teleport(randomSafeSpot());
        }
        syncHealth(target);
        Map<String, Object> args = baseArgs();
        args.put("player", playerArgs(target));
        args.put("target", playerArgs(reviver));
        actions.fire("player-relive", args, this, tp, reviver != null ? reviver.player() : null,
            tp != null ? tp.getLocation() : null);
        return true;
    }

    public void adjustHealthParam(GamePlayer gp, String param) {
        if (param == null || param.trim().isEmpty()) return;
        param = param.trim();
        try {
            if (param.startsWith("+")) {
                healHp(gp, Double.parseDouble(param.substring(1)));
            } else if (param.startsWith("-")) {
                double remain = gp.hp - Double.parseDouble(param.substring(1));
                if (remain >= 0) {
                    gp.hp = remain;
                    syncHealth(gp);
                } else {
                    gp.lastHealth += remain;
                    gp.hp = 0;
                    syncHealth(gp);
                    die(gp, null, null, null);
                }
            } else {
                double value = Double.parseDouble(param);
                gp.hp = Math.max(0, Math.min(value, gp.effectiveMaxHp()));
                syncHealth(gp);
                if (gp.hp <= 0) {
                    die(gp, null, null, null);
                }
            }
        } catch (NumberFormatException ignored) {
        }
    }

    public void firePlayerHit(GamePlayer attacker, GamePlayer victim) {
        Map<String, Object> args = baseArgs();
        args.put("player", playerArgs(attacker));
        args.put("target", playerArgs(victim));
        Player vp = victim.player();
        actions.fire("game-player-hit", args, this, attacker.player(), vp, vp != null ? vp.getLocation() : null);
    }

    public void firePickup(GamePlayer picker, String oid) {
        Map<String, Object> args = baseArgs();
        args.put("player", playerArgs(picker));
        args.put("item", itemArgs(oid));
        Player p = picker.player();
        actions.fire("player-pickup", args, this, p, null, p != null ? p.getLocation() : null);
    }

    public Map<String, Object> baseArgs() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("game", map.id);
        args.put("map", map.name);
        args.put("players", players.size());
        args.put("maxplayers", map.playersMax);
        args.put("minplayers", map.playersMin);
        int spec = 0;
        int lossN = 0;
        int diedN = 0;
        for (GamePlayer gp : players) {
            if (gp.state.spectator()) spec++;
            if (gp.state == PlayerState.LOSS) lossN++;
            if (gp.state == PlayerState.DIED) diedN++;
        }
        args.put("specplayers", spec);
        args.put("lossplayers", lossN);
        args.put("diedplayers", diedN);
        Map<String, Object> time = new LinkedHashMap<>();
        time.put("s", String.valueOf(elapsedSeconds()));
        time.put("mm:ss", TextUtil.formatDuration(elapsedSeconds(), "mm:ss"));
        args.put("time", time);
        Map<String, Object> dtime = new LinkedHashMap<>();
        dtime.put("s", String.valueOf(remainSeconds()));
        dtime.put("mm:ss", TextUtil.formatDuration(remainSeconds(), "mm:ss"));
        args.put("dtime", dtime);
        Map<String, Object> teamArgs = new LinkedHashMap<>();
        for (Team team : teams.values()) {
            teamArgs.put(team.color.id, teamArgs(team));
        }
        args.put("team", teamArgs);
        return args;
    }

    public Map<String, Object> teamArgs(Team team) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", lang.teamName(team.color));
        map.put("id", team.color.id);
        map.put("formatname", lang.teamColor(team.color) + lang.teamName(team.color));
        map.put("color", lang.teamColor(team.color));
        map.put("simplename", lang.teamSimple(team.color));
        map.put("players", team.initialSize);
        map.put("died", team.diedCount());
        map.put("loss", team.lossCount());
        map.put("live", team.aliveCount());
        return map;
    }

    public Map<String, Object> playerArgs(GamePlayer gp) {
        Map<String, Object> map = new LinkedHashMap<>();
        String color = gp.team != null ? lang.teamColor(gp.team.color) : "&7";
        String teamName = gp.team != null ? lang.teamName(gp.team.color) : "";
        String simple = gp.team != null ? lang.teamSimple(gp.team.color) : "";
        map.put("name", gp.name);
        map.put("formatname", msg.t(color + gp.name));
        map.put("team", gp.team != null ? gp.team.color.id : "");
        map.put("teamid", gp.team != null ? gp.team.color.id : "");
        map.put("formatteam", msg.t(color + teamName));
        map.put("uuid", gp.uuid.toString());
        map.put("simpleteam", simple);
        map.put("Gteam", gp.team != null ? gp.team.color.id : "");
        map.put("Gteamid", gp.team != null ? gp.team.color.id : "");
        map.put("Gsimpleteam", simple);
        map.put("Gformatteam", msg.t(color + teamName));
        map.put("Gformatname", msg.t(color + gp.name));
        return map;
    }

    public Map<String, Object> itemArgs(String oid) {
        Map<String, Object> map = new LinkedHashMap<>();
        ItemStack sample;
        switch (oid) {
            case GameItems.HEAL: sample = GameItems.heal(1); break;
            case GameItems.GAPPLE: sample = GameItems.gapple(); break;
            case GameItems.CLUEBOOK: sample = GameItems.cluebook(); break;
            case GameItems.SCROLL: sample = GameItems.scroll(); break;
            case GameItems.AMULET: sample = GameItems.amulet(); break;
            case GameItems.SKILLPACK: sample = GameItems.skillpack(); break;
            case GameItems.ARROWPACK: sample = GameItems.arrowpack(); break;
            case GameItems.GUESS: sample = GameItems.guess(); break;
            case GameItems.BOW:
            default:
                sample = GameItems.bow(itemCfg.bowName(), itemCfg.bowLore());
                break;
        }
        map.put("name", sample.hasItemMeta() && sample.getItemMeta().hasDisplayName()
            ? sample.getItemMeta().getDisplayName() : sample.getType().name());
        map.put("id", sample.getType().getId());
        map.put("lore", sample.hasItemMeta() && sample.getItemMeta().hasLore()
            ? String.join(" ", sample.getItemMeta().getLore()) : "");
        map.put("amount", 1);
        map.put("oid", GameItems.oidOf(sample) != null ? GameItems.oidOf(sample) : oid);
        return map;
    }
}
