package com.aermini.amongbows.utils;

import com.aermini.amongbows.api.BlockProvider;
import com.aermini.amongbows.api.CompatProvider;

public class BlockCompat {

    private static final BlockProvider PROVIDER = selectProvider();

    private static BlockProvider selectProvider() {
        try {
            Class<?> clazz = Class.forName(
                "com.aermini.amongbows.nms.v1_12_R1.BlockCompat1_12"
            );
            return (BlockProvider) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize BlockCompat", e);
        }

        try {
            Class<?> clazz = Class.forName(
                "com.aermini.amongbows.nms.v1_8_R3.BlockCompat1_8"
            );
            return (BlockProvider) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("No compatible BlockProvider found for this server version", e);
        }
    }

    public static int[] lookup(String name) {
        return PROVIDER.lookup(name);
    }

    public static org.bukkit.Material getMaterial(int id, int data) {
        return PROVIDER.getMaterialById(id, data);
    }

    public static org.bukkit.Material getMaterial(String name) {
        return PROVIDER.getMaterialByName(name);
    }

    public static String getProviderVersion() {
        return PROVIDER.getVersion();
    }

    public static CompatProvider compat() {
        return (CompatProvider) PROVIDER;
    }
}
