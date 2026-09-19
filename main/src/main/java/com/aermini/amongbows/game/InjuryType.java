package com.aermini.amongbows.game;

public enum InjuryType {
    ARROW(true),
    SKILL(true),
    FRIENDLY(true),
    DOT(true),
    SELF(false),
    VOID(false),
    FALL(false),
    ENV(false);

    public final boolean injury;

    InjuryType(boolean injury) {
        this.injury = injury;
    }
}
