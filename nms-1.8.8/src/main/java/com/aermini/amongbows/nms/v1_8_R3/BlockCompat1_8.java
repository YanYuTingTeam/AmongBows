package com.aermini.amongbows.nms.v1_8_R3;

import com.aermini.amongbows.api.BlockProvider;
import com.aermini.amongbows.api.CompatProvider;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class BlockCompat1_8 implements BlockProvider, CompatProvider {

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
        map.put("CONCRETE", Material.HARD_CLAY);
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

    @Override
    public boolean sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            net.minecraft.server.v1_8_R3.PacketPlayOutTitle times = new net.minecraft.server.v1_8_R3.PacketPlayOutTitle(
                net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction.TIMES, null, fadeIn, stay, fadeOut);
            send(player, times);
            if (title != null && !title.isEmpty()) {
                send(player, new net.minecraft.server.v1_8_R3.PacketPlayOutTitle(
                    net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction.TITLE,
                    net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + escape(title) + "\"}"), 0, 0, 0));
            }
            if (subtitle != null && !subtitle.isEmpty()) {
                send(player, new net.minecraft.server.v1_8_R3.PacketPlayOutTitle(
                    net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction.SUBTITLE,
                    net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + escape(subtitle) + "\"}"), 0, 0, 0));
            }
            return true;
        } catch (Exception e) {
            player.sendTitle(title == null ? "" : title, subtitle == null ? "" : subtitle);
            return true;
        }
    }

    @Override
    public boolean sendActionBar(Player player, String text) {
        try {
            net.minecraft.server.v1_8_R3.IChatBaseComponent comp =
                net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + escape(text) + "\"}");
            send(player, new net.minecraft.server.v1_8_R3.PacketPlayOutChat(comp, (byte) 2));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean setGlowing(org.bukkit.entity.Entity entity, boolean glowing) {
        return false;
    }

    private void send(Player player, net.minecraft.server.v1_8_R3.Packet<?> packet) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    private String escape(String text) {
        StringBuilder sb = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
