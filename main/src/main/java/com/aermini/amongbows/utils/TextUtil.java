package com.aermini.amongbows.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class TextUtil {

    public static String formatDuration(long seconds, String pattern) {
        seconds = Math.max(0, seconds);
        if (pattern == null) pattern = "mm:ss";
        switch (pattern.toLowerCase()) {
            case "s":
                return String.valueOf(seconds);
            case "m":
                return String.valueOf(seconds / 60);
            case "mm:ss":
                return String.format("%02d:%02d", seconds / 60, seconds % 60);
            case "hh:mm:ss":
                return String.format("%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
            default:
                try {
                    return new SimpleDateFormat(pattern).format(new Date(seconds * 1000L));
                } catch (Exception e) {
                    return String.valueOf(seconds);
                }
        }
    }

    public static String formatDate(String pattern) {
        if (pattern == null || pattern.isEmpty()) pattern = "yyyy/MM/dd";
        try {
            return new SimpleDateFormat(pattern).format(new Date());
        } catch (Exception e) {
            return pattern;
        }
    }

    public static String replace(String s, Map<String, String> vars) {
        if (s == null) return "";
        for (Map.Entry<String, String> e : vars.entrySet()) {
            s = s.replace("{" + e.getKey() + "}", e.getValue());
        }
        return s;
    }
}
