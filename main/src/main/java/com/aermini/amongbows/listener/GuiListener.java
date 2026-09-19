package com.aermini.amongbows.listener;

import com.aermini.amongbows.AmongBows;
import com.aermini.amongbows.game.Game;
import com.aermini.amongbows.game.GamePlayer;
import com.aermini.amongbows.module.GuessModule;
import com.aermini.amongbows.module.SkillModule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;

public class GuiListener implements Listener {

    private final AmongBows plugin;

    public GuiListener(AmongBows plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        Game game = plugin.manager().game();
        GamePlayer gp = game.get(p);
        if (gp == null) return;
        String title = event.getView().getTopInventory() != null ? event.getView().getTitle() : "";
        if (SkillModule.GUI_TITLE.equals(title)) {
            game.skills().onClick(event);
            return;
        }
        if (GuessModule.TITLE_TARGET.equals(title) || GuessModule.TITLE_TEAM.equals(title)) {
            game.guess().onClick(event);
            return;
        }
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Game game = plugin.manager().game();
        if (game.get((Player) event.getWhoClicked()) == null) return;
        for (int slot : event.getRawSlots()) {
            if (slot >= 5 && slot <= 8) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
