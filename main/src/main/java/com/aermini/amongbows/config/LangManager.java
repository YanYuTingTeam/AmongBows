package com.aermini.amongbows.config;

import com.aermini.amongbows.game.TeamColor;
import com.aermini.amongbows.utils.msg;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class LangManager {

    private final YamlConfiguration yml;

    public LangManager(JavaPlugin plugin, String file) {
        plugin.saveResource(file, false);
        yml = YamlConfiguration.loadConfiguration(new java.io.File(plugin.getDataFolder(), file));
    }

    public String raw(String key) {
        return yml.getString("content." + key, "");
    }

    public String teamColor(TeamColor color) {
        return msg.t(raw("team-color-" + color.id));
    }

    public String teamSimple(TeamColor color) {
        return msg.t(raw("team-simple-" + color.id));
    }

    public String teamName(TeamColor color) {
        return msg.t(raw("team-" + color.id));
    }
}
