package com.jtprince.bingo.plugin;

import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Objects;

public class GoalListener {
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BingoListener {
        String[] extraGoals() default {};
    }

    /* Definitions */

    static final Material[] meats = {
        Material.CHICKEN, Material.COOKED_CHICKEN, Material.COD, Material.COOKED_COD,
        Material.BEEF, Material.COOKED_BEEF, Material.MUTTON, Material.COOKED_MUTTON,
        Material.RABBIT, Material.COOKED_RABBIT, Material.SALMON, Material.COOKED_SALMON,
        Material.PORKCHOP, Material.COOKED_PORKCHOP, Material.TROPICAL_FISH, Material.PUFFERFISH,
        Material.RABBIT_STEW, Material.ROTTEN_FLESH
    };

    static final Material[] torches = {
        Material.TORCH, Material.WALL_TORCH, Material.SOUL_TORCH, Material.SOUL_WALL_TORCH,
        Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH
    };

    static final TreeType[] trees = {
        TreeType.ACACIA, TreeType.BIG_TREE, TreeType.BIRCH, TreeType.COCOA_TREE,
        TreeType.DARK_OAK, TreeType.JUNGLE, TreeType.JUNGLE_BUSH, TreeType.MEGA_REDWOOD,
        TreeType.REDWOOD, TreeType.SMALL_JUNGLE, TreeType.SWAMP, TreeType.TALL_BIRCH,
        TreeType.TALL_REDWOOD, TreeType.TREE
    };

    static final TreeType[] mushrooms = {TreeType.BROWN_MUSHROOM, TreeType.RED_MUSHROOM};

    /* Listeners */

    @BingoListener
    public static boolean jm_never_sword96977(BlockBreakEvent event) {
        // Never use a sword
        // See also EntityDamageByEntityEvent variant
        return event.getPlayer().getInventory().getItemInMainHand()
            .getType().getKey().toString().contains("_sword");
    }

    @BingoListener
    public static boolean jm_never_n_axe38071(BlockBreakEvent event) {
        // Never use an axe
        // See also EntityDamageByEntityEvent variant
        return event.getPlayer().getInventory().getItemInMainHand()
            .getType().getKey().toString().contains("_axe");
    }

    @BingoListener
    public static boolean jm_never_rches51018(BlockPlaceEvent event) {
        // Never place torches
        return Arrays.stream(torches).anyMatch(m -> event.getBlock().getType() == m);
    }

    @BingoListener
    public static boolean jm_never_ticks40530(CraftItemEvent event) {
        // Never craft sticks
        return event.getRecipe().getResult().getType() == Material.STICK;
    }

    @BingoListener
    public static boolean jm_never__coal44187(CraftItemEvent event) {
        // Never use coal
        // See also FurnaceBurnEvent variant
        if (event.getRecipe() instanceof ShapedRecipe) {
            ShapedRecipe r = (ShapedRecipe) event.getRecipe();

            return r.getIngredientMap().values().stream().anyMatch(i ->
                i != null && i.getType() == Material.COAL);
        }
        else if (event.getRecipe() instanceof ShapelessRecipe) {
            ShapelessRecipe r = (ShapelessRecipe) event.getRecipe();

            return r.getIngredientList().stream().anyMatch(i ->
                i != null && i.getType() == Material.COAL);
        }
        else return false;
    }

    @BingoListener
    public static boolean jm_never_sword96977(EntityDamageByEntityEvent event) {
        // Never use a sword
        // See also BlockBreakEvent variant
        return ((Player) event.getDamager()).getInventory().getItemInMainHand()
            .getType().getKey().toString().contains("_sword");
    }

    @BingoListener
    public static boolean jm_never_n_axe38071(EntityDamageByEntityEvent event) {
        // Never use an axe
        // See also BlockBreakEvent variant
        return ((Player) event.getDamager()).getInventory().getItemInMainHand()
            .getType().getKey().toString().contains("_axe");
    }

    @BingoListener
    public static boolean jm_never__coal44187(FurnaceBurnEvent event) {
        // Never use coal
        // See also CraftItemEvent variant
        return event.getFuel().getType() == Material.COAL
            || event.getFuel().getType() == Material.COAL_BLOCK;
    }

    @BingoListener(extraGoals = {"jm_never_ields14785"})
    public static boolean jm_never_rmour42273(InventoryCloseEvent event) {
        // Never use armor
        // Never use armor or shields
        Player p = (Player) event.getPlayer();
        return Arrays.stream(p.getInventory().getArmorContents()).anyMatch(Objects::nonNull);
    }

    @BingoListener
    public static boolean jm_never_lates77348(InventoryCloseEvent event) {
        // Never wear chestplates
        Player p = (Player) event.getPlayer();
        return p.getInventory().getArmorContents()[2] != null;
    }

    @BingoListener(extraGoals = {"jm_never_sleep35022"})
    public static boolean jm_sleep_a_bed24483(PlayerBedLeaveEvent event) {
        // Just sleep
        return event.getPlayer().getWorld().getTime() < 1000;
    }

    @BingoListener
    public static boolean jm_sleep_llage18859(PlayerBedLeaveEvent event) {
        // Sleep in a village
        return jm_sleep_a_bed24483(event)
            && ActivationHelpers.inVillage(event.getPlayer().getLocation());
    }

    @BingoListener
    public static boolean jm_never_die_37813(PlayerDeathEvent event) {
        // Never die
        return true;
    }

    @BingoListener
    public static boolean jm_try__nether11982(PlayerInteractEvent event) {
        // Nether bed
        return event.getClickedBlock() != null
            && event.getClickedBlock().getWorld().getEnvironment() == World.Environment.NETHER
            && event.getClickedBlock().getType().getKey().toString().contains("_bed")
            && event.getAction() == Action.RIGHT_CLICK_BLOCK;
    }

    @BingoListener
    public static boolean jm_never_g_rod73476(PlayerInteractEvent event) {
        // Never use a fishing rod
        return event.getItem() != null
            && event.getItem().getType() == Material.FISHING_ROD
            && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK);
    }

    @BingoListener(extraGoals = {"jm_never_ields14785"})
    public static boolean jm_never_hield82710(PlayerInteractEvent event) {
        // Never use a shield
        return event.getItem() != null
            && event.getItem().getType() == Material.SHIELD
            && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK);
    }

    @BingoListener
    public static boolean jm_never__boat85417(PlayerInteractEvent event) {
        // Never use (place) boats
        // Boat placement calls RIGHT_CLICK_BLOCK on the water it is placed on
        // TODO also check Interact Entity event for when player gets in boat
        return event.getItem() != null
            && event.getItem().getType().getKey().toString().contains("_boat")
            && event.getAction() == Action.RIGHT_CLICK_BLOCK;
    }

    @BingoListener
    public static boolean jm_never_ckets96909(PlayerInteractEvent event) {
        // Never use buckets
        return event.getItem() != null
            && event.getItem().getType().getKey().toString().contains("bucket")
            && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK);
    }

    @BingoListener
    public static boolean jm_carnivore_30882(PlayerItemConsumeEvent event) {
        // Only eat meat (i.e. trigger if NOT meat)
        return Arrays.stream(meats).noneMatch(f -> event.getItem().getType() == f);
    }

    @BingoListener
    public static boolean jm_vegetarian_67077(PlayerItemConsumeEvent event) {
        // Never eat meat (i.e. trigger if meat)
        return Arrays.stream(meats).anyMatch(f -> event.getItem().getType() == f);
    }

    @BingoListener
    public static boolean jm_grow__ether38694(StructureGrowEvent event) {
        // Grow a tree in the nether
        return event.getWorld().getEnvironment() == World.Environment.NETHER
            && Arrays.stream(trees).anyMatch(t -> t == event.getSpecies());
    }

    @BingoListener
    public static boolean jm_grow__hroom76894(StructureGrowEvent event) {
        // Grow a huge mushroom
        return Arrays.stream(mushrooms).anyMatch(t -> t == event.getSpecies());
    }

    @BingoListener
    public static boolean jm_grow___tree94140(StructureGrowEvent event) {
        // Grow a full jungle tree
        return event.getSpecies() == TreeType.JUNGLE;
    }

    @BingoListener
    public static boolean jm_activ_llage72436(PortalCreateEvent event) {
        // Portal in village
        return event.getEntity() instanceof Player
            && ActivationHelpers.inVillage(event.getBlocks().get(0).getLocation());
    }
}
