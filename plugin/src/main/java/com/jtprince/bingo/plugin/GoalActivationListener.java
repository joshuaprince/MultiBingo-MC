package com.jtprince.bingo.plugin;

import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.jetbrains.annotations.Contract;

import java.util.Arrays;

public class GoalActivationListener implements Listener {
    protected final AutoActivation autoActivation;

    public GoalActivationListener(AutoActivation aa) {
        this.autoActivation = aa;
        this.autoActivation.game.plugin.getServer().getPluginManager().registerEvents(this,
            this.autoActivation.game.plugin);
    }

    /**
     * Return whether to ignore event associated with this player, because they are not part of a game.
     */
    @Contract("null -> true")
    protected boolean ignore(LivingEntity player) {
        if (!(player instanceof Player)) {
            return true;
        }

        Player p = (Player) player;
        boolean ret = !autoActivation.game.playerWorldSetMap.containsKey(p);
        if (ret) {
            this.autoActivation.game.plugin.getLogger().fine(
                "ActivationListener ignored player " + player.getName());
        }
        return ret;
    }

    /**
     * Return whether to ignore event in this world, because it is not part of a game.
     */
    protected boolean ignore(World world) {
        for (WorldManager.WorldSet ws : autoActivation.game.playerWorldSetMap.values()) {
            for (World w : ws.map.values()) {
                if (w.equals(world)) {
                    return false;
                }
            }
        }

        this.autoActivation.game.plugin.getLogger().fine(
            "ActivationListener ignored world " + world.getName());
        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        Player p = (Player) event.getPlayer();
        this.autoActivation.impulseInventory(p);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (ignore(event.getEntity())) {
            return;
        }

        Player p = (Player) event.getEntity();
        // 1 tick later so the item is in the player's inventory
        this.autoActivation.game.plugin.getServer().getScheduler().scheduleSyncDelayedTask(
            this.autoActivation.game.plugin, () -> this.autoActivation.impulseInventory(p), 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerSleep(PlayerBedLeaveEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        if (event.getPlayer().getWorld().getTime() > 1000) {
            // player did not sleep through the night
            return;
        }

        // Just sleep in a bed
        this.autoActivation.impulseGoal(event.getPlayer(), "jm_sleep_a_bed24483");
        this.autoActivation.impulseGoalNegative(event.getPlayer(), "jm_never_sleep35022");

        Location playerLoc = event.getPlayer().getLocation();

        // Sleep in a village
        if (ActivationHelpers.inVillage(playerLoc)) {
            this.autoActivation.impulseGoal(event.getPlayer(), "jm_sleep_llage18859");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onStructureGrow(StructureGrowEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        TreeType[] trees = {
            TreeType.ACACIA, TreeType.BIG_TREE, TreeType.BIRCH, TreeType.COCOA_TREE,
            TreeType.DARK_OAK, TreeType.JUNGLE, TreeType.JUNGLE_BUSH, TreeType.MEGA_REDWOOD,
            TreeType.REDWOOD, TreeType.SMALL_JUNGLE, TreeType.SWAMP, TreeType.TALL_BIRCH,
            TreeType.TALL_REDWOOD, TreeType.TREE
        };

        TreeType[] mushrooms = {TreeType.BROWN_MUSHROOM, TreeType.RED_MUSHROOM};

        // Grow a tree in the nether
        if (event.getPlayer().getWorld().getEnvironment() == World.Environment.NETHER &&
                Arrays.stream(trees).anyMatch(t -> t == event.getSpecies())) {
            this.autoActivation.impulseGoal(event.getPlayer(), "jm_grow__ether38694");
        }

        // Grow a huge mushroom
        if (Arrays.stream(mushrooms).anyMatch(t -> t == event.getSpecies())) {
            this.autoActivation.impulseGoal(event.getPlayer(), "jm_grow__hroom76894");
        }

        // Grow a full jungle tree
        if (event.getSpecies() == TreeType.JUNGLE) {
            this.autoActivation.impulseGoal(event.getPlayer(), "jm_grow___tree94140");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPortalCreate(PortalCreateEvent event) {
        if (ignore(event.getWorld())) {
            return;
        }

        // Portal in village
        if (event.getEntity() instanceof Player &&
                ActivationHelpers.inVillage(event.getBlocks().get(0).getLocation())) {
            this.autoActivation.impulseGoal((Player) event.getEntity(), "jm_activ_llage72436");
        }
    }
}
