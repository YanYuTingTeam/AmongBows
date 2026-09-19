package com.aermini.amongbows.game;

public enum MarkType {
    DRAW("usebow_draw"),
    FIRE("usebow_fire"),
    HPLOST("hplost"),
    GUESSED("guessed"),
    GETITEM("getitem");

    public final String key;

    MarkType(String key) {
        this.key = key;
    }
}
