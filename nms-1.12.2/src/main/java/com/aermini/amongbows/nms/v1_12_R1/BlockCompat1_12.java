package com.aermini.amongbows.nms.v1_12_R1;

import com.aermini.amongbows.api.BlockProvider;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class BlockCompat1_12 implements BlockProvider {

    private static final String VERSION = "v1_12_R1";

    private static final Map<String, Material> FALLBACK_NAME = buildFallback();

    private static Material resolveMaterial(String name) {
        Material mat = Material.getMaterial(name);
        if (mat != null) return mat;
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    private static Map<String, Material> buildFallback() {
        Map<String, Material> map = new HashMap<>();
        String[] colors = {"WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME",
                "PINK", "GRAY", "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"};
        for (String c : colors) {
            Material concrete = resolveMaterial(c + "_CONCRETE");
            if (concrete != null) {
                map.put(c + "_CONCRETE", concrete);
            }
            Material glazed = resolveMaterial(c + "_GLAZED_TERRACOTTA");
            if (glazed != null) {
                map.put(c + "_GLAZED_TERRACOTTA", glazed);
            }
        }
        map.put("CHISELED_QUARTZ_BLOCK", resolveMaterial("QUARTZ_BLOCK"));
        map.put("SMOOTH_QUARTZ", resolveMaterial("QUARTZ_BLOCK"));
        map.put("CHISELED_SANDSTONE", resolveMaterial("SANDSTONE"));
        map.put("SMOOTH_SANDSTONE", resolveMaterial("SANDSTONE"));
        map.put("CHISELED_RED_SANDSTONE", resolveMaterial("RED_SANDSTONE"));
        map.put("SMOOTH_RED_SANDSTONE", resolveMaterial("RED_SANDSTONE"));
        map.put("PRISMARINE_BRICKS", resolveMaterial("PRISMARINE"));
        map.put("DARK_PRISMARINE", resolveMaterial("PRISMARINE"));
        return map;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public Material getMaterialById(int id, int data) {
        net.minecraft.server.v1_12_R1.Block nmsBlock =
            net.minecraft.server.v1_12_R1.Block.getById(id);
        if (nmsBlock == null) return null;
        return org.bukkit.craftbukkit.v1_12_R1.util.CraftMagicNumbers.getMaterial(nmsBlock);
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
