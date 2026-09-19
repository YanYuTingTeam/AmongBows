package com.aermini.amongbows.config;

import com.aermini.amongbows.utils.BlockCompat;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ItemConfig {

    private String bowName = "&f弓 &7&n(左键选择技能)";
    private Material bowMaterial = Material.BOW;
    private List<String> bowLore = new ArrayList<>();

    public ItemConfig(JavaPlugin plugin) {
        File f = new File(plugin.getDataFolder(), "item.yml");
        if (!f.exists()) {
            plugin.saveResource("item.yml", false);
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection bow = yml.getConfigurationSection("bow.intro");
        if (bow != null) {
            bowName = bow.getString("name", bowName);
            String materialName = bow.getString("material", "BOW");
            Material resolved = resolve(materialName);
            if (resolved != null) {
                bowMaterial = resolved;
            }
            bowLore = bow.getStringList("lore");
        }
    }

    private Material resolve(String name) {
        if (name == null) return null;
        int[] idData = BlockCompat.lookup(name.trim());
        if (idData == null) return null;
        Material mat = BlockCompat.getMaterial(idData[0], idData[1]);
        if (mat == null) {
            mat = BlockCompat.getMaterial(name.trim());
        }
        return mat;
    }

    public String bowName() {
        return bowName;
    }

    public Material bowMaterial() {
        return bowMaterial;
    }

    public List<String> bowLore() {
        return bowLore;
    }
}
