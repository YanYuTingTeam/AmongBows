package com.aermini.amongbows.module;

import com.aermini.amongbows.game.Game;
import com.aermini.amongbows.utils.BlockCompat;
import com.aermini.amongbows.utils.GameItems;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedHashMap;
import java.util.Map;

public class DropModule {

    private static final String[] OIDS = {
        GameItems.ARROWPACK, GameItems.SKILLPACK, GameItems.HEAL, GameItems.GAPPLE,
        GameItems.CLUEBOOK, GameItems.SCROLL, GameItems.AMULET
    };
    private static final int[] WEIGHTS = {20, 20, 15, 15, 10, 10, 10};
    private static final int WEIGHT_SUM = 100;

    private final Game game;

    public DropModule(Game game) {
        this.game = game;
    }

    public void dropWave(long now) {
        int count = 3 + game.random().nextInt(3);
        Map<String, Object> positions = new LinkedHashMap<>();
        Map<String, Object> items = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            Location loc = game.randomSafeSpot();
            if (loc == null) continue;
            String oid = pick();
            ItemStack stack = create(oid);
            if (stack == null) continue;
            Item entity = loc.getWorld().dropItem(loc.add(0, 0.3, 0), stack);
            boolean rare = isRare(oid);
            if (rare) {
                applyGlow(entity);
            }
            positions.put(String.valueOf(i + 1), loc.getX() + "," + loc.getY() + "," + loc.getZ());
            Map<String, Object> itemArgs = new LinkedHashMap<>(game.itemArgs(oid));
            itemArgs.put("amount", 1);
            items.put(String.valueOf(i + 1), itemArgs);
        }
        Map<String, Object> args = game.baseArgs();
        args.put("position", positions);
        args.put("quantity", positions.size());
        args.put("items", items);
        long at = System.currentTimeMillis();
        game.actions().firePlanned("game-refresh_item", () -> args, game, null, null, at);
    }

    private String pick() {
        int roll = game.random().nextInt(WEIGHT_SUM);
        int acc = 0;
        for (int i = 0; i < OIDS.length; i++) {
            acc += WEIGHTS[i];
            if (roll < acc) return OIDS[i];
        }
        return OIDS[0];
    }

    private ItemStack create(String oid) {
        switch (oid) {
            case GameItems.HEAL: return GameItems.heal(1);
            case GameItems.GAPPLE: return GameItems.gapple();
            case GameItems.CLUEBOOK: return GameItems.cluebook();
            case GameItems.SCROLL: return GameItems.scroll();
            case GameItems.AMULET: return GameItems.amulet();
            case GameItems.SKILLPACK: return GameItems.skillpack();
            case GameItems.ARROWPACK: return GameItems.arrowpack();
            default: return null;
        }
    }

    private boolean isRare(String oid) {
        return oid.equals(GameItems.CLUEBOOK) || oid.equals(GameItems.SCROLL) || oid.equals(GameItems.AMULET);
    }

    private void applyGlow(Item entity) {
        boolean ok = BlockCompat.compat().setGlowing(entity, true);
        if (!ok) {
            new BukkitRunnable() {
                int left = 30;

                @Override
                public void run() {
                    if (!entity.isValid() || left-- <= 0) {
                        cancel();
                        return;
                    }
                    Location loc = entity.getLocation();
                    loc.getWorld().playEffect(loc, Effect.HAPPY_VILLAGER, 0);
                }
            }.runTaskTimer(game.plugin(), 0L, 10L);
        } else {
            game.plugin().getServer().getScheduler().runTaskLater(game.plugin(), () -> {
                if (entity.isValid()) {
                    BlockCompat.compat().setGlowing(entity, false);
                }
            }, 300L);
        }
    }
}
