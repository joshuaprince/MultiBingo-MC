/**
 * Register automated triggers here. Keep them sorted by goal ID. Trigger types that can be added
 * to this file:
 *   - Event Triggers: Listen for a Bukkit event and decide whether it fulfills the goal.
 *   - Occasional Triggers: Get a periodic callback for each player.
 *   - Special Item Triggers: Scan an inventory for items that can't be represented with regular
 *                            item triggers.
 *
 * Regular Item Triggers should be registered in item_triggers.yml.
 */

package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.automark.ActivationHelpers.FISH_ENTITIES
import com.jtprince.bingo.kplugin.automark.ActivationHelpers.ICE_BLOCKS
import com.jtprince.bingo.kplugin.automark.ActivationHelpers.containsQuantity
import com.jtprince.bingo.kplugin.automark.ActivationHelpers.get4x4Art
import com.jtprince.bingo.kplugin.automark.ActivationHelpers.inVillage
import com.jtprince.bingo.kplugin.automark.ActivationHelpers.isCompletedMap
import com.jtprince.bingo.kplugin.automark.ActivationHelpers.throughNight
import com.jtprince.util.KotlinUtils.decrement
import com.jtprince.util.KotlinUtils.increment
import io.papermc.paper.event.player.PlayerTradeEvent
import org.bukkit.Art
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.entity.*
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.*
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.FurnaceBurnEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.*
import org.bukkit.event.world.PortalCreateEvent
import org.bukkit.event.world.StructureGrowEvent
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.potion.PotionEffectType
import org.spigotmc.event.entity.EntityMountEvent

val dslRegistry = TriggerDslRegistry {
    eventTrigger<EntityMountEvent>("jm_2_creepers_boat") {
        // 2 Creepers in the same Boat
        @Suppress("ReplaceSizeCheckWithIsNotEmpty")
        event.mount is Boat
                && event.entity is Creeper
                // Only check for 1 because this event is adding the second Creeper
                && event.mount.passengers.filterIsInstance<Creeper>().size >= 1
    }

    eventTrigger<HangingPlaceEvent>("jm_4x4_paintings") {
        // Hang up 3 Different 4x4 Paintings
        val state = playerState.extra<MutableMap<Art, Int>> { mutableMapOf() }

        val art = event.entity.get4x4Art() ?: return@eventTrigger false

        val newCountThisArt = state.increment(art)
        playerState.advance(towards = 3, amount = (if (newCountThisArt == 1) 1 else 0))
    }
    eventTrigger<HangingBreakEvent>("jm_4x4_paintings") {
        // Hang up 3 Different 4x4 Paintings
        /* This event reduces the player's progress when a painting is broken, to prevent the player
         * from using the same painting item 3 times. */
        val state = playerState.extra<MutableMap<Art, Int>> { mutableMapOf() }

        val art = event.entity.get4x4Art() ?: return@eventTrigger false

        val currentCount = state[art] ?: 0
        if (currentCount > 0) {
            val newCountThisArt = state.decrement(art)
            if (newCountThisArt == 0) {
                playerState.advance(towards = 999, amount = -1)
            }
        }

        false
    }

    specialItemTrigger("jm_armor_different_types", revertible = false) {
        // Wear 4 Different Armor types at the same time
        inventory.playerInventories.any { playerInv ->
            val types = setOf("leather", "golden", "iron", "chainmail", "diamond", "netherite")
            val counts = types.associateWith { 0 }.toMutableMap()

            @Suppress("UselessCallOnCollection") // Bukkit annotation on `armorContents` is incorrect
            for (armor in playerInv.armorContents.filterNotNull()) {
                var type: String? = null
                for (t in types) {
                    if (armor.type.key.key.contains(t)) {
                        type = t
                    }
                }
                type?.also { counts.increment(it) }
            }

            counts.filterValues { it > 0 }.size >= 4
        }
    }

    specialItemTrigger("jm_armor_leather_colors", revertible = false) {
        // Wear 4 different color Leather Armor at the same time
        inventory.playerInventories.any { playerInv ->
            val colorsFound = HashSet<Color>()

            @Suppress("UselessCallOnCollection") // Bukkit annotation on `armorContents` is incorrect
            val leatherArmor = playerInv.armorContents.filterNotNull().filter {
                ActivationHelpers.LEATHER_ARMOR.contains(it.type)
            }

            leatherArmor.forEach { armor ->
                (armor.itemMeta as? LeatherArmorMeta)?.color?.also { colorsFound += it }
            }
            colorsFound.size >= 4
        }
    }

    eventTrigger<CreatureSpawnEvent>("jm_build_golem_iron") {
        // Create an Iron Golem
        event.spawnReason == CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM
    }

    eventTrigger<CreatureSpawnEvent>("jm_build_golem_snow") {
        // Create a Snow Golem
        event.spawnReason == CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN
    }

    eventTrigger<PlayerItemConsumeEvent>("jm_carnivore") {
        // Only eat meat (i.e. trigger if NOT meat)
        event.item.type in ActivationHelpers.NON_MEATS
    }

    eventTrigger<InventoryCloseEvent>("jm_carpet_llama") {
        // Put a Carpet on a Llama
        event.inventory.holder is Llama
                && event.inventory.contents.filterNotNull().any {
            ActivationHelpers.CARPETS.contains(it.type)
        }
    }

    eventTrigger<PlayerInteractEvent>("jm_cauldron_water") {
        // Cauldron with water (put water in a cauldron)
        event.item?.type == Material.WATER_BUCKET
                && event.clickedBlock?.type == Material.CAULDRON
                && event.action == Action.RIGHT_CLICK_BLOCK
    }

    occasionalTrigger("jm_complete_map", ticks = 20) {
        // Complete a map (Any zoom)
        player.inventory.items.any { i -> i.isCompletedMap() }
    }

    eventTrigger<PlayerDeathEvent>("jm_death_msg_escape") {
        // Get a '... while trying to escape ...' Death message
        // TODO: Figure out how to get a death message string from Adventure.
        @Suppress("DEPRECATION")
        event.deathMessage?.contains("trying to escape") ?: false
    }

    eventTrigger<PlayerItemBreakEvent>("jm_deplete_sword_iron") {
        // Deplete an Iron Sword
        event.brokenItem.type == Material.IRON_SWORD
    }

    eventTrigger<BlockBreakEvent>("jm_destroy_spawner") {
        // Destroy a monster spawner
        event.block.type == Material.SPAWNER
    }

    val goalPotEffectMap = mapOf(
        "jtp_effect_slowness" to PotionEffectType.SLOW,
        "jtp_effect_poison" to PotionEffectType.POISON,
        "jtp_effect_weakness" to PotionEffectType.WEAKNESS,
        "jtp_effect_mfatigue" to PotionEffectType.SLOW_DIGGING,
        "jtp_effect_fire_res" to PotionEffectType.FIRE_RESISTANCE,
        "jtp_effect_absorption" to PotionEffectType.ABSORPTION
    )
    eventTrigger<EntityPotionEffectEvent>(*goalPotEffectMap.keys.toTypedArray()) {
        // Be afflicted by <Potion Effect>
        // Instant effects (harming, instant health) must listen to EntityDamageByEntityEvent
        event.modifiedType == goalPotEffectMap[goalId]
    }

    eventTrigger<EnchantItemEvent>("jm_enchant_any") {
        // Enchant an item
        true
    }

    specialItemTrigger("jm_enchanted_gold_sword", revertible = true) { inventory.items.any {
        // Enchanted Golden Sword
        it.type == Material.GOLDEN_SWORD && it.enchantments.isNotEmpty()
    }}

    eventTrigger<InventoryCloseEvent>("jm_fill_hopper") {
        // Fill a Hopper with 320 items
        event.inventory.type == InventoryType.HOPPER
                && event.inventory.containsQuantity(320)
    }

    eventTrigger<BlockIgniteEvent>("jm_fire_village") {
        // Set fire to a Villager's House
        // This is a hard one to make specific...
        event.cause == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL
                && event.block.location.inVillage()
    }

    eventTrigger<PlayerFishEvent>("jm_fish_junk") {
        // Fish a Junk Item
        val caughtItem = event.caught as? Item ?: return@eventTrigger false
        val material = caughtItem.itemStack.type

        if (material == Material.FISHING_ROD) {
            caughtItem.itemStack.enchantments.isEmpty()
        } else {
            material in ActivationHelpers.FISHING_JUNK
        }
    }

    eventTrigger<PlayerFishEvent>("jm_fish_treasure") {
        // Fish a Treasure Item
        val caughtItem = event.caught as? Item ?: return@eventTrigger false
        val material = caughtItem.itemStack.type

        if (material == Material.FISHING_ROD) {
            caughtItem.itemStack.enchantments.isNotEmpty()
        } else {
            material in ActivationHelpers.FISHING_TREASURES
        }
    }

    eventTrigger<PlayerInteractEvent>("jm_flower_pot_cactus") {
        // Place a cactus in a flower pot
        event.item?.type == Material.CACTUS
                && event.clickedBlock?.type == Material.FLOWER_POT
                && event.action == Action.RIGHT_CLICK_BLOCK
    }

    eventTrigger<EntityDeathEvent>("jm_get_skeleton_bow") {
        // Get a Skeleton's Bow
        // This callback ONLY adds item lore to the dropped bow. See EntityPickupItemEvent
        //  variant for the part that actually activates the goal.
        if (event.entity.type != EntityType.SKELETON) return@eventTrigger false

        for (item in event.drops) {
            if (item.type == Material.BOW) {
                var currentLore = item.lore()
                if (currentLore == null) {
                    currentLore = ArrayList()
                }
                currentLore.add(ActivationHelpers.SKELETON_DROPPED_BOW)
                item.lore(currentLore)
            }
        }
        false
    }
    eventTrigger<EntityPickupItemEvent>("jm_get_skeleton_bow") {
        // Get a Skeleton's Bow
        // This callback relies on the EntityDeathEvent variant, which adds a custom lore to any
        //  bows dropped by a Skeleton.
        event.item.itemStack.lore()?.contains(ActivationHelpers.SKELETON_DROPPED_BOW) ?: false
    }

    eventTrigger<StructureGrowEvent>("jm_grow_mushroom") {
        // Grow a huge mushroom
        event.species in ActivationHelpers.MUSHROOMS
    }

    eventTrigger<BlockPlaceEvent>("jm_ice_magma") {
        // Ice Block on top of a Magma Block
        when (event.block.type) {
            Material.MAGMA_BLOCK -> {
                event.block.getRelative(BlockFace.UP).type in ICE_BLOCKS
            }
            in ICE_BLOCKS -> {
                event.block.getRelative(BlockFace.DOWN).type == Material.MAGMA_BLOCK
            }
            else -> false
        }
    }

    eventTrigger<EntityDeathEvent>("jm_kill_animals_fire") {
        // Kill $var Animals with only fire
        if (event.entity is Animals
            && event.entity.lastDamageCause?.cause in ActivationHelpers.FIRE_DAMAGE_CAUSES
            && !event.entity.scoreboardTags.contains("playerHurt")
            && event.entity.location.getNearbyPlayers(30.0).isNotEmpty()
        ) {
            playerState.advance("var")
        } else false
    }
    eventTrigger<EntityDamageByEntityEvent>("jm_kill_animals_fire") {
        // Kill $var Animals with only fire
        // Exclude animals that were hit by the player directly
        if (event.entity is Animals && event.damager.type == EntityType.PLAYER) {
            event.entity.addScoreboardTag("playerHurt")
        }

        false
    }

    eventTrigger<EntityDeathEvent>("jm_kill_creeper_fire") {
        // Kill a Creeper with only fire
        event.entity.type == EntityType.CREEPER
            && event.entity.lastDamageCause?.cause in ActivationHelpers.FIRE_DAMAGE_CAUSES
            && !event.entity.scoreboardTags.contains("playerHurt")
            && event.entity.location.getNearbyPlayers(50.0).isNotEmpty()
    }
    eventTrigger<EntityDamageByEntityEvent>("jm_kill_creeper_fire") {
        // Kill a Creeper with only fire
        // Exclude creepers that were hit by the player directly
        if (event.entity.type == EntityType.CREEPER && event.damager.type == EntityType.PLAYER) {
            event.entity.addScoreboardTag("playerHurt")
        }

        false
    }

    eventTrigger<EntityDeathEvent>("jm_kill_golem_iron") {
        // Kill an Iron Golem
        event.entityType == EntityType.IRON_GOLEM && event.entity.killer != null
    }

    eventTrigger<EntityDeathEvent>("jm_kill_mob_anvil") {
        // Kill a hostile mob with an Anvil
        if (event.entity !is Monster) return@eventTrigger false
        val lastDamageCause = event.entity.lastDamageCause
        if (lastDamageCause !is EntityDamageByEntityEvent) return@eventTrigger false
        val damager = lastDamageCause.damager
        if (damager !is FallingBlock) return@eventTrigger false
        damager.blockData.material.key.toString().contains("anvil")
    }

    eventTrigger<PlayerInteractEntityEvent>("jm_lead_rabbit") {
        // Use a lead on a rabbit
        val hand = event.player.inventory.getItem(event.hand)
        event.rightClicked.type == EntityType.RABBIT
                && hand != null && hand.type == Material.LEAD
    }

    eventTrigger<PlayerLevelChangeEvent>("jm_level") {
        // Level <x>
        val required = vars["var"] ?: throw MissingVariableException("var")
        event.newLevel >= required
    }

    eventTrigger<PlayerInteractEvent>("jm_map_marker") {
        // Add a Marker to a Map (by right clicking a banner with the map)
        event.item?.type == Material.FILLED_MAP
                && event.clickedBlock?.type?.key.toString().contains("_banner")
                && event.action == Action.RIGHT_CLICK_BLOCK
    }

    eventTrigger<CreatureSpawnEvent>("jm_nether_fish") {
        // Get a fish into the nether
        // Going through a portal still fires this event, so no need for other EventTriggers.
        event.location.world.environment == World.Environment.NETHER
                && event.entityType in FISH_ENTITIES
    }

    eventTrigger<PortalCreateEvent>("jm_nether_portal_size") {
        // Activate a $var1x$var2 Nether Portal (not counting corners)
        val width = vars["var1"] ?: throw MissingVariableException("var1")
        val height = vars["var2"] ?: throw MissingVariableException("var2")
        val portalBlocks = event.blocks.filter { it.type == Material.NETHER_PORTAL }.size
        /* Just counting the total blocks is a little bit of a cheat vs actually measuring the
         * width and height, but it's way simpler. */
        portalBlocks >= (width * height)
    }

    eventTrigger<PortalCreateEvent>("jm_nether_portal_village") {
        // Portal in village
        event.entity is Player && event.blocks[0].location.inVillage()
    }

    specialItemTrigger("jm_never_armor_any", revertible = false) {
        // Never use armor
        @Suppress("UselessCallOnCollection") // Bukkit annotation on `armorContents` is incorrect
        inventory.playerInventories.any { playerInv -> playerInv.armorContents.filterNotNull().any() }
    }

    eventTrigger<BlockBreakEvent>("jm_never_axe") {
        // Never use an axe
        event.player.inventory.itemInMainHand.type.key.asString().contains("_axe")
    }
    eventTrigger<EntityDamageByEntityEvent>("jm_never_axe") {
        // Never use a sword
        val damager = event.damager
        damager is Player
                && damager.inventory.itemInMainHand.type.key.asString().contains("_axe")
    }

    eventTrigger<EntityMountEvent>("jm_never_boat") {
        // Never use (enter) boats
        event.entity is Player && event.mount is Boat
    }

    eventTrigger<PlayerInteractEvent>("jm_never_buckets") {
        // Never use buckets
        event.item?.type?.key.toString().contains("bucket")
                && event.action in setOf(Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR)
    }

    specialItemTrigger("jm_never_chestplates", revertible = false) {
        // Never wear chestplates
        @Suppress("SENSELESS_COMPARISON") // Bukkit annotation on `armorContents` is incorrect
        inventory.playerInventories.any { playerInv -> playerInv.armorContents[2] != null }
    }

    eventTrigger<CraftItemEvent>("jm_never_coal") {
        // Never use coal
        @Suppress("UselessCallOnCollection") // Bukkit annotation on `matrix` is incorrect
        event.inventory.matrix.filterNotNull().any { it.type == Material.COAL }
    }
    eventTrigger<FurnaceBurnEvent>("jm_never_coal") {
        // Never use coal
        event.fuel.type in setOf(Material.COAL, Material.COAL_BLOCK)
    }

    eventTrigger<PlayerDeathEvent>("jm_never_die") {
        // Never die
        true
    }

    eventTrigger<PlayerInteractEvent>("jm_never_fish") {
        // Never use a fishing rod
        event.item?.type == Material.FISHING_ROD
                && event.action in setOf(Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR)
    }

    eventTrigger<PlayerInteractEvent>("jm_never_shield") {
        // Never use a shield
        event.item?.type == Material.SHIELD
                && event.action in setOf(Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR)
    }

    eventTrigger<PlayerBedLeaveEvent>("jm_never_sleep") {
        // Never Sleep (through a night)
        event.throughNight()
    }

    eventTrigger<BlockBreakEvent>("jm_never_sword") {
        // Never use a sword
        event.player.inventory.itemInMainHand.type.key.asString().contains("_sword")
    }
    eventTrigger<EntityDamageByEntityEvent>("jm_never_sword") {
        // Never use a sword
        val damager = event.damager
        damager is Player
                && damager.inventory.itemInMainHand.type.key.asString().contains("_sword")
    }

    eventTrigger<PlayerInteractEvent>("jm_sleep_nether") {
        // Nether bed
        val block = event.clickedBlock
        block != null
                && block.world.environment == World.Environment.NETHER
                && block.type.key.toString().contains("_bed")
                && event.action == Action.RIGHT_CLICK_BLOCK
    }

    eventTrigger<PlayerBedLeaveEvent>("jm_sleep_village") {
        // Sleep in a village
        event.throughNight() && event.player.location.inVillage()
    }

    eventTrigger<EntityTameEvent>("jtp_tame_cat") {
        // Tame a cat
        event.entityType == EntityType.CAT
    }

    eventTrigger<EntityTameEvent>("jm_tame_horse") {
        // Tame a horse
        event.entityType == EntityType.HORSE
    }

    eventTrigger<EntityTameEvent>("jm_tame_wolf") {
        // Tame a wolf
        event.entityType == EntityType.WOLF
    }

    eventTrigger<EntityExplodeEvent>("jm_tnt_minecart_detonate") {
        // Detonate a TNT minecart
        event.entityType == EntityType.MINECART_TNT
    }

    eventTrigger<PlayerTradeEvent>("jm_trade_any") {
        // Trade a villager
        event.villager is Villager
    }

    eventTrigger<StructureGrowEvent>("jm_tree_nether") {
        // Grow a tree in the nether
        event.world.environment == World.Environment.NETHER
                && event.species in ActivationHelpers.TREES
    }

    eventTrigger<PlayerItemConsumeEvent>("jm_vegetarian") {
        // Never eat meat (i.e. trigger if meat)
        event.item.type in ActivationHelpers.MEATS
    }

    eventTrigger<EntityDamageByEntityEvent>("jtp_effect_harming") {
        // Be afflicted by Harming
        val damager = event.damager
        damager is ThrownPotion && damager.effects.any { eff -> eff.type == PotionEffectType.HARM }
    }
}
