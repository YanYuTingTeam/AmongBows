package com.aermini.amongbows.action;

import com.aermini.amongbows.utils.BlockCompat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ActionBarManager {

    private static class Entry {
        String text;
        int weight;
        long expiresAt;
        long addedAt;
    }

    private final JavaPlugin plugin;
    private final Map<UUID, List<Entry>> queues = new HashMap<>();

    public ActionBarManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void push(Player player, String text, int ticks, Integer weight) {
        if (player == null || text == null || text.isEmpty()) return;
        Entry entry = new Entry();
        entry.text = text;
        entry.weight = weight == null ? Integer.MAX_VALUE : weight;
        entry.expiresAt = System.currentTimeMillis() + ticks * 50L;
        entry.addedAt = System.nanoTime();
        List<Entry> list = queues.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        list.add(entry);
        showTop(player);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, List<Entry>> e : queues.entrySet()) {
            e.getValue().removeIf(entry -> entry.expiresAt <= now);
            if (e.getValue().isEmpty()) {
                continue;
            }
            Player player = Bukkit.getPlayer(e.getKey());
            if (player != null && player.isOnline()) {
                showTop(player);
            }
        }
    }

    public void clear(UUID uuid) {
        queues.remove(uuid);
    }

    private void showTop(Player player) {
        List<Entry> list = queues.get(player.getUniqueId());
        if (list == null || list.isEmpty()) return;
        Entry top = null;
        for (Entry entry : list) {
            if (top == null
                || entry.weight > top.weight
                || (entry.weight == top.weight && entry.addedAt > top.addedAt)) {
                top = entry;
            }
        }
        if (top != null) {
            BlockCompat.compat().sendActionBar(player, top.text);
        }
    }
}
