package com.jtprince.bingo.plugin.automarking;

import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.entity.EntityType;

import java.util.Set;

/**
 * Aggregate definitions for goals. For example, a goal for "Never Eat Meat" needs to know exactly
 * what counts as a "meat".
 */
public class TriggerDefinition {
    static final Set<Material> MEATS = Set.of( // Invalidate Vegetarian
        Material.CHICKEN, Material.COOKED_CHICKEN, Material.COD, Material.COOKED_COD,
        Material.BEEF, Material.COOKED_BEEF, Material.MUTTON, Material.COOKED_MUTTON,
        Material.RABBIT, Material.COOKED_RABBIT, Material.SALMON, Material.COOKED_SALMON,
        Material.PORKCHOP, Material.COOKED_PORKCHOP, Material.TROPICAL_FISH, Material.PUFFERFISH,
        Material.RABBIT_STEW, Material.ROTTEN_FLESH, Material.SPIDER_EYE
    );

    static final Set<Material> NON_MEATS = Set.of(  // Invalidate Carnivore
        Material.APPLE, Material.BAKED_POTATO, Material.BEETROOT, Material.BEETROOT_SOUP,
        Material.BREAD, Material.CARROT, Material.CHORUS_FRUIT, Material.COOKIE,
        Material.DRIED_KELP, Material.ENCHANTED_GOLDEN_APPLE, Material.GOLDEN_APPLE,
        Material.GOLDEN_CARROT, Material.MELON_SLICE, Material.MUSHROOM_STEW,
        Material.POISONOUS_POTATO, Material.POTATO, Material.PUMPKIN_PIE, Material.SUSPICIOUS_STEW,
        Material.SWEET_BERRIES
    );

    /* Consumables that do not invalidate either of the above: Potions, Honey, Milk, Cake (block) */

    static final Set<Material> TORCHES = Set.of(
        Material.TORCH, Material.WALL_TORCH, Material.SOUL_TORCH, Material.SOUL_WALL_TORCH,
        Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH
    );

    static final Set<TreeType> TREES = Set.of(
        TreeType.ACACIA, TreeType.BIG_TREE, TreeType.BIRCH, TreeType.COCOA_TREE,
        TreeType.DARK_OAK, TreeType.JUNGLE, TreeType.JUNGLE_BUSH, TreeType.MEGA_REDWOOD,
        TreeType.REDWOOD, TreeType.SMALL_JUNGLE, TreeType.SWAMP, TreeType.TALL_BIRCH,
        TreeType.TALL_REDWOOD, TreeType.TREE
    );

    static final Set<TreeType> MUSHROOMS = Set.of(
        TreeType.BROWN_MUSHROOM, TreeType.RED_MUSHROOM
    );

    static final Set<EntityType> FISH_ENTITIES = Set.of(
        EntityType.COD, EntityType.SALMON, EntityType.PUFFERFISH, EntityType.TROPICAL_FISH
    );
}
