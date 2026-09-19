package com.aermini.amongbows.map;

import com.aermini.amongbows.game.Game;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SafeSpots {

    private final List<Location> spots = new ArrayList<>();

    public void scan(World world, GameMap map) {
        spots.clear();
        double x1 = Math.min(map.pos1[0], map.pos2[0]);
        double z1 = Math.min(map.pos1[2], map.pos2[2]);
        double x2 = Math.max(map.pos1[0], map.pos2[0]);
        double z2 = Math.max(map.pos1[2], map.pos2[2]);
        int minX = (int) Math.floor(x1);
        int maxX = (int) Math.ceil(x2);
        int minZ = (int) Math.floor(z1);
        int maxZ = (int) Math.ceil(z2);
        int yTop = map.underY + 48;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = map.underY + 1; y <= yTop; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType().isSolid()
                        && !world.getBlockAt(x, y + 1, z).getType().isSolid()
                        && !world.getBlockAt(x, y + 2, z).getType().isSolid()
                        && world.getBlockAt(x, y + 1, z).isEmpty()
                        && world.getBlockAt(x, y + 2, z).isEmpty()) {
                        spots.add(new Location(world, x + 0.5, y + 1, z + 0.5));
                        break;
                    }
                }
            }
        }
    }

    public boolean isEmpty() {
        return spots.isEmpty();
    }

    public Location nearest(Location from) {
        if (spots.isEmpty()) return null;
        Location best = null;
        double bestDist = Double.MAX_VALUE;
        for (Location loc : spots) {
            if (!loc.getWorld().equals(from.getWorld())) continue;
            double dist = loc.distanceSquared(from);
            if (dist < bestDist) {
                bestDist = dist;
                best = loc;
            }
        }
        return best;
    }

    public Location randomPreferred(Game game, Random random) {
        if (spots.isEmpty()) return null;
        List<Location> candidates = new ArrayList<>();
        int bestScore = Integer.MAX_VALUE;
        for (Location loc : spots) {
            int score = 0;
            for (Player p : loc.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(loc) < 64) {
                    score += 10;
                }
            }
            for (Item item : loc.getWorld().getEntitiesByClass(Item.class)) {
                if (item.getLocation().distanceSquared(loc) < 64) {
                    score += 1;
                }
            }
            if (score < bestScore) {
                bestScore = score;
                candidates.clear();
                candidates.add(loc);
            } else if (score == bestScore) {
                candidates.add(loc);
            }
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    public Location random(Random random) {
        if (spots.isEmpty()) return null;
        return spots.get(random.nextInt(spots.size()));
    }
}
