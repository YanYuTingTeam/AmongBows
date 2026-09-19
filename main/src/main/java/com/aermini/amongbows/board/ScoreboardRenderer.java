package com.aermini.amongbows.board;

import com.aermini.amongbows.config.LangManager;
import com.aermini.amongbows.game.Game;
import com.aermini.amongbows.game.GamePlayer;
import com.aermini.amongbows.game.MarkType;
import com.aermini.amongbows.game.PlayerState;
import com.aermini.amongbows.game.Team;
import com.aermini.amongbows.game.TeamColor;
import com.aermini.amongbows.utils.GameItems;
import com.aermini.amongbows.utils.TextUtil;
import com.aermini.amongbows.utils.msg;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScoreboardRenderer {

    private List<String> waiting = new ArrayList<>();
    private String ctWaiting = "";
    private String ctCounting = "";
    private List<String> gaming = new ArrayList<>();
    private String live = "";
    private String relive = "";
    private String died = "";
    private String loss = "";
    private final Map<String, String> marks = new HashMap<>();
    private String fmtTeam = "{team_color}&l{team_simple}";
    private String fmtPlayer = "{team_color}{playername}";
    private String fmtTime = "mm:ss";
    private String fmtDate = "yyyy/MM/dd";
    private List<String> after = new ArrayList<>();

    private final Map<UUID, List<String>> lastLines = new HashMap<>();

    private final LangManager lang;

    public ScoreboardRenderer(YamlConfiguration yml, LangManager lang) {
        this.lang = lang;
        waiting = yml.getStringList("waiting");
        ctWaiting = yml.getString("counttime.waiting", "");
        ctCounting = yml.getString("counttime.counting", "");
        gaming = yml.getStringList("gaming");
        live = yml.getString("live", "");
        relive = yml.getString("relive", "");
        died = yml.getString("died", "");
        loss = yml.getString("loss", "");
        for (String key : new String[]{"usebow_draw", "usebow_fire", "hplost", "guessed", "getitem"}) {
            marks.put(key, yml.getString("marks." + key, ""));
        }
        fmtTeam = yml.getString("format.team", fmtTeam);
        fmtPlayer = yml.getString("format.player", fmtPlayer);
        fmtTime = yml.getString("format.time", fmtTime);
        fmtDate = yml.getString("format.date", fmtDate);
        after = yml.getStringList("after");
    }

    public void renderAll(Game game) {
        for (GamePlayer gp : new ArrayList<>(game.allPlayers())) {
            Player p = gp.player();
            if (p == null || !p.isOnline()) continue;
            List<String> lines = buildLines(game, gp);
            apply(p, lines);
        }
    }

    private List<String> buildLines(Game game, GamePlayer viewer) {
        switch (game.phase()) {
            case PREPARE:
            case BATTLE:
                return expand(gaming, game);
            case ENDED:
                return expand(after, game);
            default:
                List<String> lines = new ArrayList<>();
                Map<String, String> vars = new HashMap<>();
                vars.put("map", msg.t(game.map().name));
                vars.put("player", String.valueOf(game.allPlayers().size()));
                vars.put("player_max", String.valueOf(game.map().playersMax));
                vars.put("counttime", msg.t(counttimeText(game)));
                for (String line : waiting) {
                    lines.add(msg.t(TextUtil.replace(line, vars)));
                }
                return lines;
        }
    }

    public String counttimeText(Game game) {
        Map<String, String> vars = new HashMap<>();
        vars.put("player_need", String.valueOf(Math.max(0, game.map().playersMin - game.allPlayers().size())));
        vars.put("count", String.valueOf(Math.max(0, game.countLeft())));
        if (game.phase() == Game.Phase.COUNTDOWN) {
            return TextUtil.replace(ctCounting, vars);
        }
        return TextUtil.replace(ctWaiting, vars);
    }

    private List<String> expand(List<String> template, Game game) {
        Map<String, String> vars = new HashMap<>();
        vars.put("map", msg.t(game.map().name));
        long remain = game.remainSeconds();
        long elapsed = game.elapsedSeconds();
        vars.put("time_format", TextUtil.formatDuration(game.phase() == Game.Phase.ENDED ? elapsed : remain, fmtTime));
        vars.put("date_format", TextUtil.formatDate(fmtDate));
        for (TeamColor color : TeamColor.values()) {
            Team team = game.teams().get(color);
            vars.put(color.id + "_skill_points", String.valueOf(team == null ? 0 : team.points));
        }
        List<String> out = new ArrayList<>();
        for (String line : template) {
            if (line.contains("{playersinfo}")) {
                out.addAll(playersInfo(game));
                continue;
            }
            out.add(msg.t(TextUtil.replace(line, vars)));
        }
        return out;
    }

    private List<String> playersInfo(Game game) {
        List<String> out = new ArrayList<>();
        for (TeamColor color : TeamColor.values()) {
            Team team = game.teams().get(color);
            if (team == null) continue;
            for (GamePlayer gp : team.members) {
                out.add(stateLine(game, gp));
            }
        }
        return out;
    }

    private String stateLine(Game game, GamePlayer gp) {
        String template;
        Map<String, String> vars = new HashMap<>();
        switch (gp.state) {
            case RELIVE:
                template = relive;
                long remain = Math.max(0, gp.stateUntil - System.currentTimeMillis());
                int progress = (int) Math.round((1.0 - remain / 10000.0) * 100);
                vars.put("rel_progress", String.valueOf(Math.min(100, Math.max(0, progress))));
                break;
            case DIED:
                template = died;
                break;
            case LOSS:
                template = loss;
                break;
            default:
                template = live;
                break;
        }
        String teamColor = lang.teamColor(gp.team.color);
        String playerColor = game.isTruthPink(gp) ? "&d" : teamColor;
        vars.put("team_color", teamColor);
        vars.put("team_simple", lang.teamSimple(gp.team.color));
        vars.put("teamname", lang.teamName(gp.team.color));
        String teamFormat = TextUtil.replace(fmtTeam, vars);
        if (gp.state == PlayerState.LIVE) {
            StringBuilder sb = new StringBuilder();
            for (MarkType type : MarkType.values()) {
                boolean show;
                if (type == MarkType.DRAW) {
                    show = gp.drawing || gp.guiOpen;
                } else if (type == MarkType.GUESSED) {
                    show = gp.hasMark(MarkType.GUESSED);
                } else {
                    show = gp.hasMark(type);
                }
                if (show) {
                    sb.append(msg.t(marks.get(type.key)));
                }
            }
            vars.put("marks", sb.toString());
            vars.put("hp", String.valueOf((int) Math.max(0, gp.hp)));
        } else {
            vars.put("marks", "");
            vars.put("hp", "0");
        }
        Player p = gp.player();
        vars.put("hasArrow", String.valueOf(p == null ? 0 : GameItems.countArrows(p)));
        vars.put("team_color", playerColor);
        String playerFormat = TextUtil.replace(fmtPlayer, vars);
        vars.put("team_color", teamColor);
        String teamFormatStr = msg.t(teamFormat);
        String playerFormatStr = msg.t(playerFormat);
        String line = TextUtil.replace(template, vars);
        line = line.replace("{team_format}", teamFormatStr);
        line = line.replace("{player_format}", playerFormatStr);
        return line;
    }

    private void apply(Player p, List<String> lines) {
        List<String> last = lastLines.get(p.getUniqueId());
        if (last != null && last.equals(lines)) return;
        Scoreboard board = p.getScoreboard();
        if (board == null || board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            p.setScoreboard(board);
        }
        Objective obj = board.getObjective("ab_side");
        if (obj == null) {
            obj = board.registerNewObjective("ab_side", "dummy");
        }
        String title = " ";
        List<String> entries = new ArrayList<>();
        if (!lines.isEmpty()) {
            title = lines.get(0);
            entries = lines.subList(1, lines.size());
        }
        obj.setDisplayName(title.length() > 32 ? title.substring(0, 32) : title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        ChatColor[] decor = ChatColor.values();
        Map<String, Integer> seen = new HashMap<>();
        List<String> rendered = new ArrayList<>();
        for (String entry : entries) {
            Integer count = seen.get(entry);
            if (count == null) {
                seen.put(entry, 1);
                rendered.add(entry);
            } else {
                seen.put(entry, count + 1);
                rendered.add(entry + decor[(count - 1) % decor.length].toString() + ChatColor.RESET);
            }
        }
        if (last != null) {
            for (int i = 1; i < last.size(); i++) {
                String old = last.get(i);
                if (!rendered.contains(old)) {
                    board.resetScores(old);
                }
            }
        }
        for (int i = 0; i < rendered.size(); i++) {
            obj.getScore(rendered.get(i)).setScore(rendered.size() - i);
        }
        lastLines.put(p.getUniqueId(), new ArrayList<>(lines));
    }
}
