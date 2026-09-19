package com.aermini.amongbows.utils;

import org.bukkit.ChatColor;

public class msg {

    /**
     *  
     *
     */
    public static String t(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String strip(String s) {
        return ChatColor.stripColor(t(s));
    }
}
