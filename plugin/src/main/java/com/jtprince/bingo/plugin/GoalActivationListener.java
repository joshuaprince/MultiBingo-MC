package com.jtprince.bingo.plugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;

import java.util.Arrays;
import java.util.Objects;

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
        if (world == null) {
            return true;
        }

        Player p = this.autoActivation.game.getPlayerInWorld(world);

        if (p == null) {
            this.autoActivation.game.plugin.getLogger().fine(
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
        this.autoActivation.impulseInventory(p);

        ItemStack[] armor = p.getInventory().getArmorContents();
        if (armor[2] != null) {
            // chestplate slot
            this.autoActivation.impulseGoalNegative(p, "jm_never_lates77348");
        }
        if (Arrays.stream(armor).anyMatch(Objects::nonNull)) {
            this.autoActivation.impulseGoalNegative(p, "jm_never_rmour42273");
            // never armor or shields
            this.autoActivation.impulseGoalNegative(p, "jm_never_ields14785");
        }
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            // Handled by onInteract
            return;
        }

        // Nether bed
        if (block.getWorld().getEnvironment() == World.Environment.NETHER
            && block.getType().getKey().toString().contains("_bed")
            && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            this.autoActivation.impulseGoal(event.getPlayer(), "jm_try__nether11982");
        }
    }

    // No ignoreCancelled because Bukkit cancels air interact events for some reason...
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        if (event.getItem() != null) {
            // Never use a fishing rod
            if (event.getItem().getType() == Material.FISHING_ROD
                && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                this.autoActivation.impulseGoalNegative(event.getPlayer(), "jm_never_g_rod73476");
            }

            // Never use a shield
            if (event.getItem().getType() == Material.SHIELD
                && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                this.autoActivation.impulseGoalNegative(event.getPlayer(), "jm_never_hield82710");
                this.autoActivation.impulseGoalNegative(event.getPlayer(), "jm_never_ields14785");
            }

            // Never use (place) boats
            // Boat placement calls RIGHT_CLICK_BLOCK on the water it is placed on
            if (event.getItem().getType().getKey().toString().contains("_boat")
                && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                this.autoActivation.impulseGoalNegative(event.getPlayer(), "jm_never__boat85417");
            }
        }
    }
}
