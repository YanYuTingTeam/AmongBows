package com.aermini.amongbows.api;

/**
 * 跨版本客户端表现兼容层（标题/动作栏/实体发光）.
 */
public interface CompatProvider {

    boolean sendTitle(org.bukkit.entity.Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    boolean sendActionBar(org.bukkit.entity.Player player, String text);

    boolean setGlowing(org.bukkit.entity.Entity entity, boolean glowing);
}
