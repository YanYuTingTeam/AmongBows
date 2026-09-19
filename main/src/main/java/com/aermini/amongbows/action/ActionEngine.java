package com.aermini.amongbows.action;

import com.aermini.amongbows.AmongBows;
import com.aermini.amongbows.game.Game;
import com.aermini.amongbows.game.GamePlayer;
import com.aermini.amongbows.game.PlayerState;
import com.aermini.amongbows.game.TeamColor;
import com.aermini.amongbows.utils.BlockCompat;
import com.aermini.amongbows.utils.ExprEval;
import com.aermini.amongbows.utils.GameItems;
import com.aermini.amongbows.utils.MiniJson;
import com.aermini.amongbows.utils.msg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ActionEngine {

    private static class EventDef {
        String base;
        String opera;
        int before;
        String condition;
        List<String> actions = new ArrayList<>();
    }

    private final AmongBows plugin;
    private final ActionBarManager bars;
    private final Map<String, EventDef> defs = new LinkedHashMap<>();

    public ActionEngine(AmongBows plugin, ActionBarManager bars) {
        this.plugin = plugin;
        this.bars = bars;
    }

    public void load(YamlConfiguration yml) {
        defs.clear();
        for (String key : yml.getKeys(false)) {
            Object val = yml.get(key);
            if (!(val instanceof Map)) continue;
            int open = key.indexOf('[');
            EventDef def = new EventDef();
            if (open >= 0 && key.endsWith("]")) {
                def.base = key.substring(0, open);
                def.opera = key.substring(open + 1, key.length() - 1);
            } else {
                def.base = key;
                def.opera = null;
            }
            def.before = yml.getInt(key + ".before", 0);
            def.condition = yml.getString(key + ".condition");
            def.actions = yml.getStringList(key + ".actions");
            defs.put(key, def);
        }
    }

    public void fire(String base, Map<String, Object> args, Game game, Player trigger, Player target, Location pos) {
        for (EventDef def : new ArrayList<>(defs.values())) {
            if (!def.base.equals(base)) continue;
            if (!matchOpera(def, args)) continue;
            executeDef(def, () -> args, game, trigger, target, pos, 0);
        }
    }

    public void firePlanned(String base, Supplier<Map<String, Object>> args, Game game, Player trigger, Location pos, long atMillis) {
        for (EventDef def : new ArrayList<>(defs.values())) {
            if (!def.base.equals(base)) continue;
            long runAt = Math.max(System.currentTimeMillis(), atMillis - def.before);
            executeDef(def, args, game, trigger, null, pos, runAt - System.currentTimeMillis());
        }
    }

    private boolean matchOpera(EventDef def, Map<String, Object> args) {
        if (def.opera == null || def.opera.trim().isEmpty()) return true;
        Object count = args.get("count");
        if (count instanceof Number) {
            return ExprEval.eval(String.valueOf(((Number) count).intValue()) + def.opera);
        }
        if (args.containsKey("item")) {
            String opera = def.opera.trim();
            return opera.isEmpty() || opera.equals(String.valueOf(args.get("item")));
        }
        return false;
    }

    private void executeDef(EventDef def, Supplier<Map<String, Object>> argsSup, Game game, Player trigger, Player target, Location pos, long delayMs) {
        Map<String, Object> args = argsSup.get();
        boolean cond = def.condition == null || ExprEval.eval(subst(def.condition, args));
        Bukkit.getScheduler().runTaskLater(plugin, new Chain(def.actions, cond, args, game, trigger, target, pos), delayMs / 50L);
    }

    private class Chain implements Runnable {
        private final List<String> actions;
        private final boolean condBool;
        private final Map<String, Object> args;
        private final Game game;
        private final Player trigger;
        private final Player target;
        private final Location pos;
        private int idx;
        private long delayMs;

        Chain(List<String> actions, boolean condBool, Map<String, Object> args, Game game, Player trigger, Player target, Location pos) {
            this.actions = actions;
            this.condBool = condBool;
            this.args = args;
            this.game = game;
            this.trigger = trigger;
            this.target = target;
            this.pos = pos;
        }

        @Override
        public void run() {
            while (idx < actions.size()) {
                String raw = actions.get(idx).trim();
                idx++;
                if (raw.isEmpty()) continue;

                boolean run = condBool;
                while (raw.startsWith("<")) {
                    String condStr = extractCond(raw);
                    raw = raw.substring(condEnd(raw)).trim();
                    if (condStr.equalsIgnoreCase("true")) {
                        run &= condBool;
                    } else if (condStr.equalsIgnoreCase("false")) {
                        run &= !condBool;
                    } else {
                        run &= ExprEval.eval(subst(condStr, args));
                    }
                }
                if (raw.isEmpty()) continue;
                if (!run) continue;

                if (raw.toUpperCase().startsWith("[WAIT]")) {
                    String rest = raw.substring(6).trim().replace("\"", "").trim();
                    try {
                        delayMs += Long.parseLong(rest);
                    } catch (NumberFormatException ignored) {
                    }
                    Bukkit.getScheduler().runTaskLater(plugin, this, delayMs / 50L);
                    return;
                }

                int open = raw.indexOf(']');
                if (!raw.startsWith("[") || open < 0) continue;
                String prefix = raw.substring(1, open).trim().toUpperCase();
                String payload = raw.substring(open + 1).trim();
                executeAction(prefix, payload, args, game, trigger, target, pos);
            }
        }
    }

    private static int condEnd(String line) {
        int i = 1;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '\\' && i + 1 < line.length()) {
                i += 2;
                continue;
            }
            if (c == '>') return i + 1;
            i++;
        }
        return line.length();
    }

    private static String extractCond(String line) {
        return line.substring(1, Math.max(1, condEnd(line) - 1));
    }

    private void executeAction(String prefix, String payload, Map<String, Object> args, Game game, Player trigger, Player target, Location pos) {
        Map<String, String> json = payload.startsWith("{") ? MiniJson.parse(payload) : new LinkedHashMap<>();
        if (json.isEmpty() && !payload.startsWith("{") && !payload.isEmpty()) {
            json.put("content", payload);
        }
        String targetSel = json.get("target");
        List<Player> targets = resolveTargets(targetSel, game, trigger, target);

        switch (prefix) {
            case "TITLE": {
                String title = msg.t(subst(json.get("title"), args));
                String subtitle = msg.t(subst(json.get("subtitle"), args));
                int[] times = parseTimes(subst(json.get("time"), args));
                for (Player p : targets) {
                    BlockCompat.compat().sendTitle(p, title, subtitle, times[0], times[1], times[2]);
                }
                break;
            }
            case "ACTIONBAR": {
                String content = msg.t(subst(json.get("content"), args));
                int ticks = parseInt(subst(json.get("time"), args), 100);
                Integer weight = json.containsKey("weight") ? parseInt(subst(json.get("weight"), args), Integer.MAX_VALUE) : null;
                for (Player p : targets) {
                    bars.push(p, content, ticks, weight);
                }
                break;
            }
            case "MSG":
            case "MESSAGE": {
                String content = msg.t(subst(json.get("content"), args));
                for (Player p : targets) {
                    p.sendMessage(content);
                }
                break;
            }
            case "SOUND": {
                Sound sound = resolveSound(subst(json.get("sound"), args));
                if (sound == null) break;
                float[] vp = parseVolumePitch(subst(json.get("param"), args));
                Location loc = resolvePosition(subst(json.get("position"), args), game, null, trigger, target, pos);
                for (Player p : targets) {
                    p.playSound(loc != null ? loc : p.getLocation(), sound, vp[0], vp[1]);
                }
                break;
            }
            case "EFFECT": {
                PotionEffectType type = resolveEffect(subst(json.get("effect"), args));
                if (type == null) break;
                int level = Math.max(0, parseInt(subst(json.get("level"), args), 0));
                int seconds = json.containsKey("time") ? parseInt(subst(json.get("time"), args), 30) : parseInt(subst(json.get("duration"), args), 30);
                for (Player p : targets) {
                    p.addPotionEffect(new PotionEffect(type, seconds * 20, level), true);
                }
                break;
            }
            case "TELEPORT": {
                String position = json.get("position");
                for (Player p : targets) {
                    Location loc = resolvePosition(subst(position, args), game, p, trigger, target, pos);
                    if (loc != null) {
                        p.teleport(loc);
                    }
                }
                break;
            }
            case "HEALTH": {
                String health = json.get("health");
                if (health == null) break;
                for (Player p : targets) {
                    GamePlayer gp = game.get(p);
                    if (gp != null) {
                        game.adjustHealthParam(gp, subst(health, args));
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    private List<Player> resolveTargets(String sel, Game game, Player trigger, Player target) {
        List<GamePlayer> gps = filter(sel, new ArrayList<>(game.allPlayers()), game, trigger, target);
        List<Player> out = new ArrayList<>();
        for (GamePlayer gp : gps) {
            Player p = gp.player();
            if (p != null && p.isOnline()) out.add(p);
        }
        return out;
    }

    private List<GamePlayer> filter(String sel, List<GamePlayer> set, Game game, Player trigger, Player target) {
        if (sel == null || sel.trim().isEmpty()) return set;
        sel = sel.trim();
        int i = 0;
        while (i < sel.length() && (Character.isLetterOrDigit(sel.charAt(i)) || sel.charAt(i) == '_' || sel.charAt(i) == '-')) i++;
        String name = sel.substring(0, i);
        List<String> groups = new ArrayList<>();
        while (i < sel.length() && sel.charAt(i) == '[') {
            int end = readGroup(sel, i);
            groups.add(unescape(sel.substring(i + 1, end - 1)));
            i = end;
        }
        String inner = null;
        if (i < sel.length() && sel.charAt(i) == '{') {
            int end = readGroup(sel, i);
            inner = sel.substring(i + 1, end - 1);
            i = end;
        }
        List<GamePlayer> out = applySelector(name, groups, set, game, trigger, target);
        if (inner != null) {
            out = filter(inner, out, game, trigger, target);
        }
        return out;
    }

    private int readGroup(String s, int start) {
        char open = s.charAt(start);
        char close = open == '[' ? ']' : '}';
        int depth = 0;
        int i = start;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && open == '[') {
                i += 2;
                continue;
            }
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) return i + 1;
            }
            i++;
        }
        return s.length();
    }

    private String unescape(String s) {
        return s.replace("\\<", "<").replace("\\>", ">");
    }

    private List<GamePlayer> applySelector(String name, List<String> groups, List<GamePlayer> set, Game game, Player trigger, Player target) {
        String key = name.toLowerCase();
        List<GamePlayer> out = new ArrayList<>();
        switch (key) {
            case "all":
                return set;
            case "red":
            case "blue":
            case "green":
            case "yellow": {
                TeamColor color = TeamColor.byId(key);
                for (GamePlayer gp : set) {
                    if (gp.team != null && gp.team.color == color) out.add(gp);
                }
                return out;
            }
            case "trigger": {
                for (GamePlayer gp : set) {
                    if (trigger != null && trigger.getUniqueId().equals(gp.uuid)) out.add(gp);
                }
                return out;
            }
            case "target": {
                for (GamePlayer gp : set) {
                    if (target != null && target.getUniqueId().equals(gp.uuid)) out.add(gp);
                }
                return out;
            }
            case "alive": {
                for (GamePlayer gp : set) {
                    if (gp.state == PlayerState.LIVE) out.add(gp);
                }
                return out;
            }
            case "relive": {
                for (GamePlayer gp : set) {
                    if (gp.state == PlayerState.RELIVE) out.add(gp);
                }
                return out;
            }
            case "died": {
                for (GamePlayer gp : set) {
                    if (gp.state == PlayerState.DIED) out.add(gp);
                }
                return out;
            }
            case "loss": {
                for (GamePlayer gp : set) {
                    if (gp.state == PlayerState.LOSS) out.add(gp);
                }
                return out;
            }
            case "hasitem": {
                String oid = groups.size() > 0 ? groups.get(0).trim() : "";
                String cond = groups.size() > 1 ? groups.get(1).trim() : "";
                for (GamePlayer gp : set) {
                    Player p = gp.player();
                    if (p == null) continue;
                    int count = 0;
                    for (ItemStack item : p.getInventory().getContents()) {
                        String o = GameItems.oidOf(item);
                        if (o != null && (oid.isEmpty() || o.equalsIgnoreCase(oid))) count += item.getAmount();
                    }
                    if (cond.isEmpty() ? count > 0 : ExprEval.eval(count + " " + cond)) out.add(gp);
                }
                return out;
            }
            case "skillpoint": {
                String cond = groups.size() > 0 ? groups.get(0).trim() : "";
                for (GamePlayer gp : set) {
                    if (ExprEval.eval(gp.personalPoints + " " + cond)) out.add(gp);
                }
                return out;
            }
            case "hasarrow": {
                String cond = groups.size() > 0 ? groups.get(0).trim() : "";
                for (GamePlayer gp : set) {
                    Player p = gp.player();
                    if (p == null) continue;
                    if (ExprEval.eval(GameItems.countArrows(p) + " " + cond)) out.add(gp);
                }
                return out;
            }
            case "name": {
                String want = groups.size() > 0 ? groups.get(0).trim() : "";
                for (GamePlayer gp : set) {
                    if (gp.name.equals(want)) out.add(gp);
                }
                return out;
            }
            case "uuid": {
                String want = groups.size() > 0 ? groups.get(0).trim() : "";
                for (GamePlayer gp : set) {
                    if (gp.uuid.toString().equalsIgnoreCase(want)) out.add(gp);
                }
                return out;
            }
            case "world": {
                String want = groups.size() > 0 ? groups.get(0).trim() : "";
                for (GamePlayer gp : set) {
                    Player p = gp.player();
                    if (p != null && p.getWorld().getName().equals(want)) out.add(gp);
                }
                return out;
            }
            default:
                return set;
        }
    }

    private Location resolvePosition(String posStr, Game game, Player forPlayer, Player trigger, Player target, Location ctxPos) {
        if (posStr == null || posStr.trim().isEmpty()) {
            if (forPlayer != null) return forPlayer.getLocation();
            if (ctxPos != null) return ctxPos;
            return trigger != null ? trigger.getLocation() : null;
        }
        posStr = posStr.trim();
        if (posStr.startsWith("_")) {
            List<GamePlayer> posTargets = filter(posStr.substring(1), new ArrayList<>(game.allPlayers()), game, trigger, target);
            GamePlayer self = forPlayer != null ? game.get(forPlayer) : null;
            if (self != null && posTargets.contains(self) && self.player() != null) {
                return self.player().getLocation();
            }
            for (GamePlayer gp : posTargets) {
                if (gp.player() != null) return gp.player().getLocation();
            }
            return forPlayer != null ? forPlayer.getLocation() : ctxPos;
        }
        World world = game.mapWorld();
        double x;
        double y;
        double z;
        if (posStr.startsWith(".")) {
            String rest = posStr.substring(1).trim();
            if (rest.startsWith("[")) {
                int end = rest.indexOf(']');
                if (end > 0) {
                    World named = Bukkit.getWorld(rest.substring(1, end));
                    if (named != null) world = named;
                    rest = rest.substring(end + 1);
                }
            }
            double[] v = parseXYZ(rest);
            x = v[0];
            y = v[1];
            z = v[2];
        } else if (posStr.toLowerCase().startsWith("randomsafe")) {
            return game.randomSafeSpot();
        } else {
            double[] v = parseXYZ(posStr);
            x = v[0];
            y = v[1];
            z = v[2];
        }
        return new Location(world, x, y, z);
    }

    private double[] parseXYZ(String s) {
        double[] out = {0, 0, 0};
        if (s == null) return out;
        String[] parts = s.split(",");
        try {
            if (parts.length > 0) out[0] = Double.parseDouble(parts[0].trim());
            if (parts.length > 1) out[1] = Double.parseDouble(parts[1].trim());
            if (parts.length > 2) out[2] = Double.parseDouble(parts[2].trim());
        } catch (NumberFormatException ignored) {
        }
        return out;
    }

    public static String subst(String line, Map<String, Object> args) {
        if (line == null) return "";
        while (true) {
            int idx = line.lastIndexOf("[$");
            if (idx < 0) break;
            int close = line.indexOf("$]", idx);
            if (close < 0) break;
            String inner = line.substring(idx + 2, close);
            String value = resolvePath(subst(inner, args), args);
            line = line.substring(0, idx) + value + line.substring(close + 2);
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '$') {
                int j = line.indexOf('$', i + 1);
                if (j > i) {
                    out.append(resolvePath(line.substring(i + 1, j), args));
                    i = j + 1;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        String result = out.toString();
        for (Map.Entry<String, Object> e : args.entrySet()) {
            if (!(e.getValue() instanceof Map)) {
                result = result.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
            }
        }
        return result;
    }

    private static String resolvePath(String path, Map<String, Object> args) {
        if (path.startsWith("args.")) {
            path = path.substring(5);
        }
        Object cur = args;
        for (String key : path.split("\\.")) {
            if (cur instanceof Map) {
                cur = ((Map<?, ?>) cur).get(key);
            } else {
                return "";
            }
        }
        return cur == null ? "" : String.valueOf(cur);
    }

    private int[] parseTimes(String s) {
        int[] out = {10, 40, 10};
        if (s == null) return out;
        String[] parts = s.split("/");
        try {
            if (parts.length > 0) out[0] = Integer.parseInt(parts[0].trim());
            if (parts.length > 1) out[1] = Integer.parseInt(parts[1].trim());
            if (parts.length > 2) out[2] = Integer.parseInt(parts[2].trim());
        } catch (NumberFormatException ignored) {
        }
        return out;
    }

    private int parseInt(String s, int def) {
        if (s == null) return def;
        try {
            return (int) Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private float[] parseVolumePitch(String s) {
        float[] out = {1f, 1f};
        if (s == null) return out;
        String[] parts = s.split("/");
        try {
            if (parts.length > 0) out[0] = Float.parseFloat(parts[0].trim());
            if (parts.length > 1) out[1] = Float.parseFloat(parts[1].trim());
        } catch (NumberFormatException ignored) {
        }
        return out;
    }

    private static final Map<String, String[]> SOUND_CANDIDATES = new LinkedHashMap<>();

    static {
        SOUND_CANDIDATES.put("random.click", new String[]{"CLICK", "UI_BUTTON_CLICK"});
        SOUND_CANDIDATES.put("note.pling", new String[]{"NOTE_PLING", "BLOCK_NOTE_PLING"});
        SOUND_CANDIDATES.put("mob.elderguardian.curse", new String[]{"ELDER_GUARDIAN_CURSE", "ENTITY_ELDER_GUARDIAN_CURSE"});
        SOUND_CANDIDATES.put("mob.enderdragon.growl", new String[]{"ENDERDRAGON_GROWL", "ENTITY_ENDERDRAGON_GROWL"});
        SOUND_CANDIDATES.put("random.totem", new String[]{"TOTEM_USE", "ITEM_TOTEM_USE"});
    }

    private Sound resolveSound(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        id = id.trim();
        List<String> tries = new ArrayList<>();
        String[] candidates = SOUND_CANDIDATES.get(id);
        if (candidates != null) {
            tries.addAll(Arrays.asList(candidates));
        }
        String upper = id.replace('.', '_').toUpperCase();
        tries.add(upper);
        int dot = id.lastIndexOf('.');
        if (dot >= 0 && dot < id.length() - 1) {
            String last = id.substring(dot + 1).toUpperCase();
            tries.add(last);
            tries.add("ENTITY_" + last);
            tries.add("BLOCK_" + last);
            tries.add("ITEM_" + last);
        }
        for (String t : tries) {
            try {
                return Sound.valueOf(t);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    private PotionEffectType resolveEffect(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        PotionEffectType type = PotionEffectType.getByName(id.trim().toUpperCase());
        if (type != null) return type;
        String upper = id.trim().toUpperCase();
        if (upper.equals("JUMP")) type = PotionEffectType.getByName("JUMP_BOOST");
        if (type == null && upper.equals("SLOW")) type = PotionEffectType.getByName("SLOWNESS");
        return type;
    }
}
