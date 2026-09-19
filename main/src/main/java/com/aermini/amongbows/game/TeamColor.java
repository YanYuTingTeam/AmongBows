package com.aermini.amongbows.game;

import org.bukkit.Color;

public enum TeamColor {
    RED("red", 14, Color.fromRGB(0xE0342C)),
    BLUE("blue", 11, Color.fromRGB(0x3465D8)),
    GREEN("green", 13, Color.fromRGB(0x4EA830)),
    YELLOW("yellow", 4, Color.fromRGB(0xE8D21C));

    public final String id;
    public final int woolData;
    public final Color leather;

    TeamColor(String id, int woolData, Color leather) {
        this.id = id;
        this.woolData = woolData;
        this.leather = leather;
    }

    public static TeamColor byId(String id) {
        for (TeamColor c : values()) {
            if (c.id.equalsIgnoreCase(id)) return c;
        }
        return null;
    }
}
