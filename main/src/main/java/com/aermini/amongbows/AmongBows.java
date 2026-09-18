package com.aermini.amongbows;

import com.aermini.amongbows.utils.BlockCompat;
import org.bukkit.plugin.java.JavaPlugin;

public class AmongBows extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("插件已启用");
        getLogger().info("BlockCompat version: " + BlockCompat.getProviderVersion());
    }

    @Override
    public void onDisable() {
        getLogger().info("插件已禁用");
    }
}
