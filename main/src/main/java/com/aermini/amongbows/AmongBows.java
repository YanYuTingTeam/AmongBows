package com.aermini.amongbows;

import com.aermini.amongbows.action.ActionEngine;
import com.aermini.amongbows.action.ActionBarManager;
import com.aermini.amongbows.board.ScoreboardRenderer;
import com.aermini.amongbows.command.AmongBowsCommand;
import com.aermini.amongbows.config.ConfigManager;
import com.aermini.amongbows.config.ItemConfig;
import com.aermini.amongbows.config.LangManager;
import com.aermini.amongbows.game.GameManager;
import com.aermini.amongbows.listener.GuiListener;
import com.aermini.amongbows.listener.PlayerListener;
import com.aermini.amongbows.utils.BlockCompat;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class AmongBows extends JavaPlugin {

    private ConfigManager configs;
    private LangManager lang;
    private ItemConfig itemCfg;
    private ActionBarManager bars;
    private ActionEngine actions;
    private ScoreboardRenderer board;
    private GameManager manager;

    @Override
    public void onEnable() {
        getLogger().info("插件已启用");
        getLogger().info("BlockCompat version: " + BlockCompat.getProviderVersion());

        configs = new ConfigManager(this);
        configs.load();
        lang = new LangManager(this, "lang/zh_cn.yml");
        itemCfg = new ItemConfig(this);
        bars = new ActionBarManager(this);

        actions = new ActionEngine(this, bars);
        actions.load(YamlConfiguration.loadConfiguration(new File(getDataFolder(), "action.yml")));
        board = new ScoreboardRenderer(YamlConfiguration.loadConfiguration(new File(getDataFolder(), "scoreboard.yml")), lang);

        manager = new GameManager(this, lang, itemCfg, bars, actions, board);
        manager.init();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        PluginCommand command = getCommand("amongbows");
        if (command != null) {
            command.setExecutor(new AmongBowsCommand(this));
        }
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.shutdown();
        }
        getLogger().info("插件已禁用");
    }

    public ConfigManager configs() {
        return configs;
    }

    public LangManager lang() {
        return lang;
    }

    public ItemConfig itemCfg() {
        return itemCfg;
    }

    public ActionBarManager bars() {
        return bars;
    }

    public ActionEngine actions() {
        return actions;
    }

    public ScoreboardRenderer board() {
        return board;
    }

    public GameManager manager() {
        return manager;
    }
}
