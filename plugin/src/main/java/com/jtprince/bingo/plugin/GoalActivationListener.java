package com.jtprince.bingo.plugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;

public class GoalActivationListener implements Listener {
    protected final AutoActivation autoActivation;

    public GoalActivationListener(AutoActivation aa) {
        this.autoActivation = aa;
        this.autoActivation.game.plugin.getServer().getPluginManager().registerEvents(this,
            this.autoActivation.game.plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player p = (Player) event.getPlayer();
            this.autoActivation.impulseInventory(p);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();
            // 1 tick later so the item is in the player's inventory
            this.autoActivation.game.plugin.getServer().getScheduler().scheduleSyncDelayedTask(
                this.autoActivation.game.plugin, () -> this.autoActivation.impulseInventory(p), 1);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerSleep(PlayerBedLeaveEvent event) {
        if (event.getPlayer().getWorld().getTime() > 1000) {
            // player did not sleep through the night
            return;
        }

        this.autoActivation.impulseGoal(event.getPlayer(), "jm_sleep_a_bed24483");
    }
}
