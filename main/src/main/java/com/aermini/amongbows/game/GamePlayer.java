package com.aermini.amongbows.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GamePlayer {

    public final UUID uuid;
    public String name;
    public Team team;
    public PlayerState state = PlayerState.WAITING;

    public double hp;
    public final int maxHp;
    public int appleBonus;
    public double lastHealth;

    public int personalPoints = 3;
    public int revivedCount;
    public int kills;

    public final Map<SkillType, Long> skillCd = new HashMap<>();
    public long bowCdUntil;
    public long jerseyCdUntil;
    public long guessCdUntil;
    public long invincibleUntil;
    public long stateUntil;

    public TeamColor jersey;
    public SkillType armed;
    public boolean oneShot;
    public boolean drawing;
    public boolean guiOpen;

    public final Map<MarkType, Long> marks = new HashMap<>();

    public Inventory snapshot;

    public GamePlayer(UUID uuid, String name, int maxHp) {
        this.uuid = uuid;
        this.name = name;
        this.maxHp = maxHp;
        this.hp = 10;
    }

    public Player player() {
        return Bukkit.getPlayer(uuid);
    }

    public boolean online() {
        Player p = player();
        return p != null && p.isOnline();
    }

    public int effectiveMaxHp() {
        return maxHp + appleBonus;
    }

    public boolean hasMark(MarkType type) {
        Long until = marks.get(type);
        return until != null && until > System.currentTimeMillis();
    }

    public void mark(MarkType type, long durationMs) {
        marks.put(type, durationMs < 0 ? Long.MAX_VALUE : System.currentTimeMillis() + durationMs);
    }

    public void clearMark(MarkType type) {
        marks.remove(type);
    }

    public boolean onSkillCd(SkillType type) {
        Long until = skillCd.get(type);
        return until != null && until > System.currentTimeMillis();
    }

    public long skillCdRemain(SkillType type) {
        Long until = skillCd.get(type);
        if (until == null) return 0;
        return Math.max(0, until - System.currentTimeMillis());
    }
}
