package com.jtprince.bingo.plugin.automarking;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.jetbrains.annotations.Contract;

/**
 * Container for all Bukkit Listeners in a single BingoGame.
 *
 * Each BingGame has one AutoMarkListener, stored through the game's AutoMarking instance,
 * that listens to ALL possible Bukkit Events, checks if the raised event is relevant to this
 * BingoGame, and passes it to the AutoMarking instance to see if that Event should trigger any
 * squares.
 */
public class AutoMarkListener implements Listener {
    final AutoMarking autoMarking;

    public AutoMarkListener(AutoMarking aa) {
        this.autoMarking = aa;
        this.autoMarking.game.plugin.getServer().getPluginManager().registerEvents(this,
            this.autoMarking.game.plugin);
    }

    /**
     * Return whether to ignore event associated with this player, because they are not part of
     * a game.
     */
    @Contract("null -> true")
    private boolean ignore(LivingEntity player) {
        if (!(player instanceof Player)) {
            return true;
        }

        Player p = (Player) player;
        boolean ret = !autoMarking.game.getPlayers().contains(p);
        if (ret) {
            this.autoMarking.game.plugin.getLogger().fine(
                "ActivationListener ignored player " + player.getName());
        }
        return ret;
    }

    /**
     * Return whether to ignore event in this world, because it is not part of a game.
     */
    private boolean ignore(World world) {
        if (world == null) {
            return true;
        }

        Player p = this.autoMarking.game.getPlayerInWorld(world);

        if (p == null) {
            this.autoMarking.game.plugin.getLogger().fine(
                "ActivationListener ignored world " + world.getName());
        }
        return (p == null);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        Player p = (Player) event.getPlayer();
        this.autoMarking.impulseInventory(p);
        this.autoMarking.impulseEvent(event, p);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (ignore(event.getEntity())) {
            return;
        }

        Player p = (Player) event.getEntity();
        // 1 tick later so the item is in the player's inventory
        this.autoMarking.game.plugin.getServer().getScheduler().scheduleSyncDelayedTask(
            this.autoMarking.game.plugin, () -> this.autoMarking.impulseInventory(p), 1);
        this.autoMarking.impulseEvent(event, p);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerSleep(PlayerBedLeaveEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        this.autoMarking.impulseEvent(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onStructureGrow(StructureGrowEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        this.autoMarking.impulseEvent(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPortalCreate(PortalCreateEvent event) {
        if (ignore(event.getWorld())) {
            return;
        }

        this.autoMarking.impulseEvent(event, event.getWorld());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        this.autoMarking.impulseEvent(event, event.getPlayer());
    }

    // No ignoreCancelled because Bukkit cancels air interact events for some reason...
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        this.autoMarking.impulseEvent(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDirectDamageEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player damager = (Player) event.getDamager();
        if (ignore(damager)) {
            return;
        }

        this.autoMarking.impulseEvent(event, damager);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBreakBlock(BlockBreakEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        this.autoMarking.impulseEvent(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlaceBlock(BlockPlaceEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        this.autoMarking.impulseEvent(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        this.autoMarking.impulseEvent(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (ignore(event.getEntity())) {
            return;
        }

        this.autoMarking.impulseEvent(event, event.getEntity());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCraft(CraftItemEvent event) {
        if (ignore(event.getWhoClicked())) {
            return;
        }

        Player p = (Player) event.getWhoClicked();

        this.autoMarking.impulseEvent(event, p);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onFurnaceStartBurning(FurnaceBurnEvent event) {
        if (ignore(event.getBlock().getWorld())) {
            return;
        }

        this.autoMarking.impulseEvent(event, event.getBlock().getWorld());
    }
}
