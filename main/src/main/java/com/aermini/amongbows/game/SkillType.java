package com.aermini.amongbows.game;

public enum SkillType {
    TELEPORT("传送", 2, 15),
    ONESHOT("一击必杀", 3, 0),
    REVIVE("复活", 5, 45);

    public final String display;
    public final int cost;
    public final int cooldown;

    SkillType(String display, int cost, int cooldown) {
        this.display = display;
        this.cost = cost;
        this.cooldown = cooldown;
    }
}
