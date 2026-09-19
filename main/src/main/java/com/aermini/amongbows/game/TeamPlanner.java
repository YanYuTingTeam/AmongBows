package com.aermini.amongbows.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TeamPlanner {

    private static final Map<Integer, List<int[]>> PLANS = new HashMap<>();

    static {
        PLANS.put(2, Arrays.asList(new int[]{1, 1}));
        PLANS.put(3, Arrays.asList(new int[]{1, 1, 1}));
        PLANS.put(4, Arrays.asList(new int[]{2, 2}, new int[]{1, 1, 1, 1}));
        PLANS.put(5, Arrays.asList(new int[]{2, 3}, new int[]{1, 2, 2}));
        PLANS.put(6, Arrays.asList(new int[]{3, 3}, new int[]{2, 2, 2}));
        PLANS.put(7, Arrays.asList(new int[]{3, 4}, new int[]{2, 2, 3}));
        PLANS.put(8, Arrays.asList(new int[]{4, 4}, new int[]{2, 3, 3}, new int[]{2, 2, 2, 2}));
        PLANS.put(9, Arrays.asList(new int[]{3, 3, 3}, new int[]{2, 2, 2, 3}));
        PLANS.put(10, Arrays.asList(new int[]{3, 3, 4}, new int[]{2, 2, 3, 3}));
        PLANS.put(11, Arrays.asList(new int[]{3, 4, 4}, new int[]{2, 3, 3, 3}));
        PLANS.put(12, Arrays.asList(new int[]{4, 4, 4}, new int[]{3, 3, 3, 3}));
        PLANS.put(13, Arrays.asList(new int[]{3, 3, 3, 4}));
        PLANS.put(14, Arrays.asList(new int[]{3, 3, 4, 4}));
        PLANS.put(15, Arrays.asList(new int[]{3, 4, 4, 4}));
        PLANS.put(16, Arrays.asList(new int[]{4, 4, 4, 4}));
    }

    private static final TeamColor[] COLORS = {TeamColor.RED, TeamColor.BLUE, TeamColor.GREEN, TeamColor.YELLOW};

    public static int[] pick(int players, Random random) {
        List<int[]> options = PLANS.get(players);
        if (options == null || options.isEmpty()) return null;
        return options.get(random.nextInt(options.size()));
    }

    public static List<TeamColor> colors(int teamCount, Random random) {
        List<TeamColor> pool = new ArrayList<>(Arrays.asList(COLORS));
        Collections.shuffle(pool, random);
        List<TeamColor> chosen = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            chosen.add(pool.get(i));
        }
        return chosen;
    }
}
