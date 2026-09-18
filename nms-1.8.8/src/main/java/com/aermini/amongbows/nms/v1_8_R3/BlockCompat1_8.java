package com.aermini.amongbows.nms.v1_8_R3;

import com.aermini.amongbows.api.BlockProvider;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class BlockCompat1_8 implements BlockProvider {

    private static final String VERSION = "v1_8_R3";

    private static final Map<String, Material> FALLBACK_NAME = buildFallback();

    private static Map<String, Material> buildFallback() {
        Map<String, Material> map = new HashMap<>();
        String[] colors = {"WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME",
                "PINK", "GRAY", "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"};
        for (String c : colors) {
            map.put(c + "_CONCRETE", Material.HARD_CLAY);
            map.put(c + "_GLAZED_TERRACOTTA", Material.HARD_CLAY);
        }
        map.put("CHISELED_QUARTZ_BLOCK", Material.QUARTZ_BLOCK);
        map.put("SMOOTH_QUARTZ", Material.QUARTZ_BLOCK);
        map.put("CHISELED_SANDSTONE", Material.SANDSTONE);
        map.put("SMOOTH_SANDSTONE", Material.SANDSTONE);
        map.put("CHISELED_RED_SANDSTONE", Material.RED_SANDSTONE);
        map.put("SMOOTH_RED_SANDSTONE", Material.RED_SANDSTONE);
        map.put("PRISMARINE_BRICKS", Material.PRISMARINE);
        map.put("DARK_PRISMARINE", Material.PRISMARINE);
        return map;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public Material getMaterialById(int id, int data) {
        net.minecraft.server.v1_8_R3.IBlockData blockData =
            net.minecraft.server.v1_8_R3.Block.getByCombinedId(id << 4 | (data & 0xF));
        if (blockData == null) {
            return Material.getMaterial(id);
        }
        return org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers.getMaterial(blockData.getBlock());
    }

    @Override
    public Material getMaterialByName(String name) {
        if (name == null) return null;
        name = name.toUpperCase();
        Material mat = Material.getMaterial(name);
        if (mat != null) return mat;
        return FALLBACK_NAME.get(name);
    }

    @Override
    public int[] lookup(String name) {
        if (name == null) return null;
        name = name.trim().toUpperCase();

        Material mat = getMaterialByName(name);
        if (mat != null) {
            return new int[]{mat.getId(), getMaterialData(mat)};
        }

        if (name.matches("\\d+")) {
            return new int[]{Integer.parseInt(name), 0};
        }
        return null;
    }

    private int getMaterialData(Material mat) {
        Class<?> dataClass = mat.getData();
        if (dataClass == null) return 0;
        try {
            Object instance = dataClass.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method getData = dataClass.getMethod("getData");
            return ((Byte) getData.invoke(instance)).intValue();
        } catch (Exception e) {
            return 0;
        }
    }
}
