package com.aermini.amongbows.map;

import com.aermini.amongbows.game.TeamColor;
import com.aermini.amongbows.utils.BlockCompat;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapGenerator {

    private static class Pillar {
        int x;
        int z;
        int weight;
        int height;
        TeamColor color;
        boolean round;
    }

    public static void generate(World world, GameMap map, Random random) {
        if (!map.generateEnabled) return;
        int minX = (int) Math.floor(Math.min(map.pos1[0], map.pos2[0]));
        int maxX = (int) Math.ceil(Math.max(map.pos1[0], map.pos2[0]));
        int minZ = (int) Math.floor(Math.min(map.pos1[2], map.pos2[2]));
        int maxZ = (int) Math.ceil(Math.max(map.pos1[2], map.pos2[2]));
        if (maxX <= minX || maxZ <= minZ) return;

        int baseY = map.underY - 8;
        int hMin = map.underY + 6;
        int hMax = Math.min(hMin + map.maxDiffH, world.getMaxHeight() - 2);

        int[] main = BlockCompat.lookup(map.mainBlock);
        if (main == null) return;

        List<Pillar> pillars = new ArrayList<>();
        int step = Math.max(2, map.weightMax + map.spacingMin);
        for (int x = minX + 2; x <= maxX - 2; x += step) {
            for (int z = minZ + 2; z <= maxZ - 2; z += step) {
                if (random.nextInt(100) >= 75) {
                    continue;
                }
                Pillar p = new Pillar();
                p.x = x + random.nextInt(Math.max(1, map.spacingMax - map.spacingMin + 1));
                p.z = z + random.nextInt(Math.max(1, map.spacingMax - map.spacingMin + 1));
                if (p.x > maxX - 2 || p.z > maxZ - 2) continue;
                p.weight = map.weightMin + random.nextInt(Math.max(1, map.weightMax - map.weightMin + 1));
                p.height = hMin + random.nextInt(Math.max(1, hMax - hMin + 1));
                p.round = map.round;
                TeamColor[] colors = TeamColor.values();
                p.color = colors[random.nextInt(colors.length)];
                if (tooClose(p, pillars, map.spacingMin)) continue;
                pillars.add(p);
            }
        }

        for (Pillar p : pillars) {
            int radius = Math.max(0, (p.weight - 1) / 2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (p.round && dx * dx + dz * dz > radius * radius) continue;
                    for (int y = baseY; y <= p.height; y++) {
                        setBlock(world, p.x + dx, y, p.z + dz, main[0], p.color.woolData);
                    }
                }
            }
            if (map.safeBlocks) {
                placeSafeBlocks(world, p, map, random);
            }
        }

        if (map.helperBlocks && pillars.size() > 1) {
            int helpers = Math.max(1, pillars.size() / 2);
            for (int i = 0; i < helpers; i++) {
                Pillar a = pillars.get(random.nextInt(pillars.size()));
                Pillar b = pillars.get(random.nextInt(pillars.size()));
                if (a == b) continue;
                int mx = (a.x + b.x) / 2;
                int mz = (a.z + b.z) / 2;
                int lo = Math.min(a.height, b.height);
                int hi = Math.max(a.height, b.height);
                int hy = lo + 1 + random.nextInt(Math.max(1, hi - lo + 2));
                setBlock(world, mx, hy, mz, main[0], a.color.woolData);
            }
        }
    }

    private static boolean tooClose(Pillar p, List<Pillar> pillars, int spacingMin) {
        for (Pillar o : pillars) {
            int gapX = Math.abs(p.x - o.x) - (p.weight + o.weight) / 2;
            int gapZ = Math.abs(p.z - o.z) - (p.weight + o.weight) / 2;
            if (Math.max(gapX, gapZ) < spacingMin) return true;
        }
        return false;
    }

    private static void placeSafeBlocks(World world, Pillar p, GameMap map, Random random) {
        int radius = Math.max(0, (p.weight - 1) / 2);
        for (String[] def : map.safeBlockDefs) {
            String mode = def[0].toLowerCase();
            int[] block = BlockCompat.lookup(def[1]);
            if (block == null) continue;
            int data = 0;
            if (def[2].equals("-1")) {
                data = p.color.woolData;
            } else {
                try {
                    data = Integer.parseInt(def[2]);
                } catch (NumberFormatException ignored) {
                }
            }
            int count = 2 + random.nextInt(3);
            for (int i = 0; i < count; i++) {
                int y = map.underY + 2 + random.nextInt(Math.max(1, p.height - map.underY - 2));
                int side = random.nextInt(4);
                int dx = 0;
                int dz = 0;
                if (side == 0) dx = radius + 1;
                else if (side == 1) dx = -(radius + 1);
                else if (side == 2) dz = radius + 1;
                else dz = -(radius + 1);
                if (mode.equals("on")) {
                    Block block2 = world.getBlockAt(p.x + dx, y, p.z + dz);
                    block2.setTypeIdAndData(block[0], ladderData(side), false);
                } else {
                    setBlock(world, p.x + dx, y, p.z + dz, block[0], data);
                }
            }
            setBlock(world, p.x, p.height + 1, p.z, block[0], data);
        }
    }

    private static byte ladderData(int side) {
        switch (side) {
            case 0: return 5;
            case 1: return 4;
            case 2: return 3;
            default: return 2;
        }
    }

    private static void setBlock(World world, int x, int y, int z, int id, int data) {
        Block block = world.getBlockAt(x, y, z);
        block.setTypeIdAndData(id, (byte) (data & 0xF), false);
    }
}
