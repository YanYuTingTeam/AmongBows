package com.aermini.amongbows.game;

public enum PlayerState {
    WAITING,
    LIVE,
    RELIVE,
    DIED,
    LOSS;

    public boolean spectator() {
        return this == RELIVE || this == DIED || this == LOSS;
    }
}
