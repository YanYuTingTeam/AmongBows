package com.aermini.amongbows.map;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class GameMap {

    public final String id;
    public String name;
    public List<String> authors;

    public int time;
    public String world;
    public double[] pos1;
    public double[] pos2;
    public int underY;
    public double[] spawn;
    public int playersMin;
    public int playersMax;

    public boolean generateEnabled;
    public int maxDiffH;
    public boolean helperBlocks;
    public boolean safeBlocks;
    public int spacingMin;
    public int spacingMax;
    public int weightMin;
    public int weightMax;
    public boolean round;
    public String mainBlock;
    public List<String[]> safeBlockDefs = new ArrayList<>();

    public GameMap(String id) {
        this.id = id;
    }

    public static GameMap load(String id, ConfigurationSection yml) {
        GameMap map = new GameMap(id);
        ConfigurationSection intro = yml.getConfigurationSection("intro");
        if (intro != null) {
            map.name = intro.getString("name", id);
            map.authors = intro.getStringList("author");
        }
        if (map.name == null) map.name = id;
        map.time = yml.getInt("time", 1800);
        map.world = yml.getString("region", "world");
        map.pos1 = parseXYZ(yml.getString("loc.pos1"), 0, 0, 0);
        map.pos2 = parseXYZ(yml.getString("loc.pos2"), 0, 0, 0);
        map.underY = yml.getInt("loc.underY", 40);
        map.spawn = parseXYZ(yml.getString("loc.spawn"), 0, 64, 0);
        map.playersMin = yml.getInt("players.min", 2);
        map.playersMax = yml.getInt("players.max", 16);
        ConfigurationSection gen = yml.getConfigurationSection("generate");
        if (gen != null) {
            map.generateEnabled = gen.getBoolean("enabled", false);
            map.maxDiffH = gen.getInt("max_diffH", 12);
            map.helperBlocks = gen.getBoolean("helper_blocks", true);
            map.safeBlocks = gen.getBoolean("safe_blocks", true);
            map.spacingMin = gen.getInt("spacing-min", 4);
            map.spacingMax = gen.getInt("spacing-max", 10);
            map.weightMin = gen.getInt("weight-min", 1);
            map.weightMax = gen.getInt("weight-max", 5);
            map.round = gen.getBoolean("round", false);
            map.mainBlock = gen.getString("blocks.main", "CONCRETE");
            for (String def : gen.getStringList("blocks.safe_blocks")) {
                String[] parts = def.split("\\|");
                String[] entry = new String[3];
                entry[0] = parts.length > 0 ? parts[0].trim() : "by";
                entry[1] = parts.length > 1 ? parts[1].trim() : "STONE";
                entry[2] = parts.length > 2 ? parts[2].trim() : "0";
                map.safeBlockDefs.add(entry);
            }
        }
        return map;
    }

    private static double[] parseXYZ(String s, double dx, double dy, double dz) {
        double[] out = {dx, dy, dz};
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
}
