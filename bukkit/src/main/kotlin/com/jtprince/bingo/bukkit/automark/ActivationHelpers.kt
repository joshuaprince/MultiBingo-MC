package com.jtprince.bingo.bukkit.automark

import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Painting
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerBedLeaveEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapRenderer
import org.bukkit.map.MapView

object ActivationHelpers {
    val SKELETON_DROPPED_BOW = Component.text("Dropped by a Skeleton")

    val MEATS = setOf( // Invalidate Vegetarian
        Material.CHICKEN, Material.COOKED_CHICKEN, Material.COD, Material.COOKED_COD,
        Material.BEEF, Material.COOKED_BEEF, Material.MUTTON, Material.COOKED_MUTTON,
        Material.RABBIT, Material.COOKED_RABBIT, Material.SALMON, Material.COOKED_SALMON,
        Material.PORKCHOP, Material.COOKED_PORKCHOP, Material.TROPICAL_FISH, Material.PUFFERFISH,
        Material.RABBIT_STEW, Material.ROTTEN_FLESH, Material.SPIDER_EYE
    )

    val NON_MEATS = setOf( // Invalidate Carnivore
        Material.APPLE, Material.BAKED_POTATO, Material.BEETROOT, Material.BEETROOT_SOUP,
        Material.BREAD, Material.CARROT, Material.CHORUS_FRUIT, Material.COOKIE,
        Material.DRIED_KELP, Material.ENCHANTED_GOLDEN_APPLE, Material.GOLDEN_APPLE,
        Material.GOLDEN_CARROT, Material.MELON_SLICE, Material.MUSHROOM_STEW,
        Material.POISONOUS_POTATO, Material.POTATO, Material.PUMPKIN_PIE, Material.SUSPICIOUS_STEW,
        Material.SWEET_BERRIES
    )

    /* Consumables that do not invalidate either of the above: Potions, Honey, Milk, Cake (block) */

    val TORCHES = setOf(
        Material.TORCH, Material.WALL_TORCH, Material.SOUL_TORCH, Material.SOUL_WALL_TORCH,
        Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH
    )

    val TREES = setOf(
        TreeType.ACACIA, TreeType.BIG_TREE, TreeType.BIRCH, TreeType.COCOA_TREE,
        TreeType.DARK_OAK, TreeType.JUNGLE, TreeType.JUNGLE_BUSH, TreeType.MEGA_REDWOOD,
        TreeType.REDWOOD, TreeType.SMALL_JUNGLE, TreeType.SWAMP, TreeType.TALL_BIRCH,
        TreeType.TALL_REDWOOD, TreeType.TREE
    )

    val MUSHROOMS = setOf(
        TreeType.BROWN_MUSHROOM, TreeType.RED_MUSHROOM
    )

    val FISH_ENTITIES = setOf(
        EntityType.COD, EntityType.SALMON, EntityType.PUFFERFISH, EntityType.TROPICAL_FISH
    )

    val FISHING_TREASURES = setOf(
        Material.BOW, Material.ENCHANTED_BOOK, Material.NAME_TAG, Material.NAUTILUS_SHELL,
        Material.SADDLE /* Fishing Rod item is treasure only if enchanted */
    )

    val FISHING_JUNK = setOf(
        Material.LILY_PAD, Material.BOWL, Material.LEATHER, Material.LEATHER_BOOTS,
        Material.ROTTEN_FLESH, Material.STICK, Material.STRING, Material.POTION,  /* Water Bottle */
        Material.BONE, Material.INK_SAC, Material.TRIPWIRE_HOOK /* Fishing Rod item is junk only if unenchanted */
    )

    val LEATHER_ARMOR = setOf(
        Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS,
        Material.LEATHER_BOOTS
    )

    val FIRE_DAMAGE_CAUSES = setOf(
        EntityDamageEvent.DamageCause.FIRE, EntityDamageEvent.DamageCause.FIRE_TICK,
        EntityDamageEvent.DamageCause.HOT_FLOOR
    )

    fun Location.inVillage(): Boolean {
        val nearestVillage = world.locateNearestStructure(
            this, StructureType.VILLAGE, 8, false
        ) ?: return false

        // locateNearestStructure returns Y=0. Only calculate horizontal distance
        nearestVillage.y = y
        return distance(nearestVillage) < 100
    }

    /**
     * Returns whether an ItemStack is a fully explored Map, which is defined as 99.5% or more of
     * the pixels on the map being filled.
     *
     * NOTE: This function may incorrectly return false the first time it is called on a new
     * MapView. The map must be rendered for the player so the pixels can be counted before it
     * is accurate.
     *
     * @return True if the item is a "completed" map. False if it is not a map, the map is not
     * completed, or if the map has not been rendered yet.
     */
    fun ItemStack.isCompletedMap(): Boolean {
        if (type != Material.FILLED_MAP) return false
        val meta = itemMeta as? MapMeta ?: return false
        val view = meta.mapView ?: return false
        if (view.renderers.stream().noneMatch { r: MapRenderer? -> r is MapCompletionRenderer }) {
            view.addRenderer(MapCompletionRenderer())
        }
        val renderer = view.renderers.stream()
            .filter { r: MapRenderer? -> r is MapCompletionRenderer }.findFirst().orElseThrow() as MapCompletionRenderer
        return renderer.completedPercent > 0.995
    }

    /**
     * Determine whether an Inventory contains x or more total items.
     */
    fun Inventory.containsQuantity(quantity: Int): Boolean {
        var q = 0
        for (i in contents.orEmpty()) {
            if (i != null) {
                q += i.amount
            }
        }
        return q >= quantity
    }

    /**
     * Determine if a player slept through the night.
     */
    fun PlayerBedLeaveEvent.throughNight() : Boolean {
        return player.world.time < 1000
    }

    /**
     * If this entity is a 4x4 or above Painting, return the Art on it. Otherwise, returns null.
     */
    fun Entity.get4x4Art(): Art? {
        return when {
            this is Painting && art.blockHeight >= 4 && art.blockWidth >= 4 -> art
            else -> null
        }
    }

    private class MapCompletionRenderer : MapRenderer() {
        var completedPercent = 0.0
        override fun render(map: MapView, canvas: MapCanvas, player: Player) {
            var totalPixels = 0
            var mappedPixels = 0
            for (x in 0..127) {
                for (y in 0..127) {
                    totalPixels++
                    if (canvas.getBasePixel(x, y).toInt() != 0) {
                        mappedPixels++
                    }
                }
            }
            completedPercent = mappedPixels.toDouble() / totalPixels
        }
    }
}
