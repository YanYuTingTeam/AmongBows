package com.aermini.amongbows.config;

import com.aermini.amongbows.map.GameMap;
import com.aermini.amongbows.utils.BlockCompat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigManager {

    private final JavaPlugin plugin;
    private YamlConfiguration config;
    private double heartsPerHp = 1;
    private int defaultMaxHp = 15;
    private final Map<String, GameMap> maps = new LinkedHashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        saveResource("config.yml");
        saveResource("action.yml");
        saveResource("item.yml");
        saveResource("scoreboard.yml");
        saveResource("lang/zh_cn.yml");
        saveResource("game/default.yml");

        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        heartsPerHp = Math.max(0.1, config.getDouble("hp.hearts-per-hp", 1));
        defaultMaxHp = config.getInt("hp.max-hp", 15);

        maps.clear();
        File dir = new File(plugin.getDataFolder(), "game");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                String id = f.getName().substring(0, f.getName().length() - 4);
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
                maps.put(id, GameMap.load(id, yml));
            }
        }
        if (maps.isEmpty()) {
            throw new IllegalStateException("No game map config found in game/");
        }
    }

    private void saveResource(String path) {
        File f = new File(plugin.getDataFolder(), path);
        if (!f.exists()) {
            plugin.saveResource(path, false);
        }
    }

    public double heartsPerHp() {
        return heartsPerHp;
    }

    public int defaultMaxHp() {
        return defaultMaxHp;
    }

    public Map<String, GameMap> maps() {
        return maps;
    }

    public GameMap map(String id) {
        return maps.get(id);
    }

    public GameMap firstMap() {
        return maps.values().iterator().next();
    }

    public static int[] resolveBlock(String name) {
        return BlockCompat.lookup(name);
    }

    public static ConfigurationSection section(YamlConfiguration yml, String path) {
        return yml.getConfigurationSection(path);
    }
}
