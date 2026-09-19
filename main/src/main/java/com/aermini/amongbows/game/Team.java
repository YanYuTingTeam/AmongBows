package com.aermini.amongbows.game;

import com.aermini.amongbows.config.LangManager;

import java.util.ArrayList;
import java.util.List;

public class Team {

    public final TeamColor color;
    public final List<GamePlayer> members = new ArrayList<>();
    public final int initialSize;
    public int points;
    public long lastInjuryAt;

    public Team(TeamColor color, int initialSize) {
        this.color = color;
        this.initialSize = initialSize;
        this.points = 5;
    }

    public String displayName(LangManager lang) {
        return lang.teamName(color);
    }

    public int aliveCount() {
        int n = 0;
        for (GamePlayer gp : members) {
            if (gp.state == PlayerState.LIVE) n++;
        }
        return n;
    }

    public int diedCount() {
        int n = 0;
        for (GamePlayer gp : members) {
            if (gp.state == PlayerState.DIED) n++;
        }
        return n;
    }

    public int lossCount() {
        int n = 0;
        for (GamePlayer gp : members) {
            if (gp.state == PlayerState.LOSS) n++;
        }
        return n;
    }

    public int totalKills() {
        int n = 0;
        for (GamePlayer gp : members) {
            n += gp.kills;
        }
        return n;
    }

    public boolean allOutOfGame() {
        for (GamePlayer gp : members) {
            if (gp.state != PlayerState.DIED && gp.state != PlayerState.LOSS) return false;
        }
        return true;
    }
}
