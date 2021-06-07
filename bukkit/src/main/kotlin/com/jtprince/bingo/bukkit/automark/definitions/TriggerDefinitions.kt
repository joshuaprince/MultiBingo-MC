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

package com.jtprince.bingo.bukkit.automark.definitions

import com.jtprince.bingo.bukkit.BingoPlugin
import com.jtprince.bingo.bukkit.automark.ActivationHelpers
import com.jtprince.bingo.bukkit.automark.ActivationHelpers.FISH_ENTITIES
import com.jtprince.bingo.bukkit.automark.ActivationHelpers.containsQuantity
import com.jtprince.bingo.bukkit.automark.ActivationHelpers.get4x4Art
import com.jtprince.bingo.bukkit.automark.ActivationHelpers.inVillage
import com.jtprince.bingo.bukkit.automark.ActivationHelpers.isCompletedMap
import com.jtprince.bingo.bukkit.automark.ActivationHelpers.throughNight
import com.jtprince.bingo.core.automark.MissingVariableException
import com.jtprince.util.KotlinUtils.decrement
import com.jtprince.util.KotlinUtils.increment
import io.papermc.paper.event.player.PlayerTradeEvent
import org.bukkit.*
import org.bukkit.block.Banner
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Levelled
import org.bukkit.entity.*
import org.bukkit.event.block.*
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
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BannerMeta
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.potion.PotionEffectType
import org.spigotmc.event.entity.EntityMountEvent
import java.util.*

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

    eventTrigger<BlockRedstoneEvent>("jm_arrow_button") {
        // Shoot a Button with an Arrow
        if (!event.block.type.key.key.contains("_button")) return@eventTrigger false

        /* A list of Locations where a player pressed a button themselves. Make sure we don't
         * award the trigger for any of these locations. */
        val playerPressed = playerState.extra { mutableSetOf<Location>() }

        if (event.newCurrent == 0) {
            playerPressed -= event.block.location
            return@eventTrigger false
        }

        event.block.location !in playerPressed
                && event.block.location.getNearbyEntitiesByType(Arrow::class.java, 1.0).isNotEmpty()
    }
    eventTrigger<PlayerInteractEvent>("jm_arrow_button") {
        // Shoot a Button with an Arrow
        /* Prevent the player from just clicking a button with an arrow nearby */
        val block = event.clickedBlock ?: return@eventTrigger false
        if (block.type.key.key.contains("_button")) {
            playerState.extra { mutableSetOf<Location>() } += block.location
        }
        false
    }

    occasionalTrigger("jm_bounce_slime", ticks = 4) {
        // Bounce on a Slime Block
        player.bukkitPlayers.any {
            it.location.block.getRelative(BlockFace.DOWN).type == Material.SLIME_BLOCK
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
            Tag.CARPETS.isTagged(it.type)
        }
    }

    eventTrigger<PlayerInteractEvent>("jm_cauldron_water") {
        // Cauldron with water (put water in a cauldron)
        event.item?.type == Material.WATER_BUCKET
                && event.clickedBlock?.type == Material.CAULDRON
                && event.action == Action.RIGHT_CLICK_BLOCK
    }

    eventTrigger<PlayerInteractEvent>("jm_clean_banner") {
        // Clean a Pattern off a Banner
        val item = event.item ?: return@eventTrigger false
        val block = event.clickedBlock ?: return@eventTrigger false

        item.type.key.key.contains("_banner")
                && block.type == Material.CAULDRON
                && ((item.itemMeta as? BannerMeta)?.numberOfPatterns() ?: 0) > 0
                && ((block.blockData as? Levelled)?.level ?: 0) > 0
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

    specialItemTrigger("jm_different_shields", true, vars("var")) {
        // $var Different Pattern / Color Shields
        val shields = inventory.items.asSequence()
            .filter { it.type == Material.SHIELD }.map(ItemStack::getItemMeta)
            .filterIsInstance<BlockStateMeta>().map { it.blockState }
            .filterIsInstance<Banner>().map { it.baseColor to it.patterns }
            .filterNot { it.second.isEmpty() }
            .toSet()

        shields.size >= vars["var"] ?: throw MissingVariableException("var")
    }

    eventTrigger<BlockBreakEvent>("jm_dig_bedrock") {
        // Dig straight down to Bedrock from Sea level (1x1 hole)
        /* `diggers` are all Bukkit player UUIDs who are currently digging straight down. */
        val diggers = playerState.extra { mutableMapOf<UUID, Location>() }
        if (event.block.y > 60) {
            /* Any block broken over sea level makes a player a digger. */
            diggers[event.player.uniqueId] = event.block.location
        }

        false
    }
    occasionalTrigger("jm_dig_bedrock", ticks = 10) {
        // Dig straight down to Bedrock from Sea level (1x1 hole)
        /* If the player moves out of their 1x1 column, they are no longer a digger. */
        val diggers = playerState.extra { mutableMapOf<UUID, Location>() }
        diggers.any { (playerUuid, loc) ->
            val player = Bukkit.getPlayer(playerUuid) ?: return@any false
            if (loc.blockX != player.location.blockX || loc.blockZ != player.location.blockZ) {
                diggers -= playerUuid
                return@any false
            }

            if (player.location.block.getRelative(BlockFace.DOWN).type == Material.BEDROCK) {
                diggers -= playerUuid
                true
            } else false
        }
    }

    eventTrigger<EntityTransformEvent>("jm_drown_zombie") {
        // Drown a Zombie
        event.entityType == EntityType.ZOMBIE
                && event.transformReason == EntityTransformEvent.TransformReason.DROWNED
                && event.entity.scoreboardTags.contains("hasTargetedPlayer")
    }
    eventTrigger<EntityTargetEvent>("jm_drown_zombie") {
        // Drown a Zombie
        /* Only Zombies that have seen the player count - to prevent drowning far away */
        if (event.entityType == EntityType.ZOMBIE && event.target?.type == EntityType.PLAYER) {
            event.entity.addScoreboardTag("hasTargetedPlayer")
        }
        false
    }

    val goalPotEffectMap = mapOf(
        "jtp_effect_slowness" to PotionEffectType.SLOW,
        "jtp_effect_poison" to PotionEffectType.POISON,
        "jtp_effect_weakness" to PotionEffectType.WEAKNESS,
        "jtp_effect_mfatigue" to PotionEffectType.SLOW_DIGGING,
        "jtp_effect_fire_res" to PotionEffectType.FIRE_RESISTANCE,
        "jtp_effect_absorption" to PotionEffectType.ABSORPTION
    )
    eventTrigger<EntityPotionEffectEvent>(goalPotEffectMap.keys.toTypedArray()) {
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

    occasionalTrigger("jm_finish_jump_world", ticks = 5) {
        // Finish by jumping from top to bottom of the world
        for (player in player.bukkitPlayers) {
            if (player.location.y >= 256) {
                player.setMetadata("lastTickAtHeightLimit", FixedMetadataValue(BingoPlugin, Bukkit.getCurrentTick()))
            }
        }
        false
    }
    eventTrigger<PlayerDeathEvent>("jm_finish_jump_world", revertAfterTicks = 60) {
        // Finish by jumping from top to bottom of the world
        val lastTickAtHeightLimit = event.entity.getMetadata("lastTickAtHeightLimit")
            .find { it.owningPlugin == BingoPlugin }?.asInt() ?: return@eventTrigger false
        event.entity.lastDamageCause?.cause == EntityDamageEvent.DamageCause.FALL
                && event.entity.location.block.getRelative(BlockFace.DOWN).type == Material.BEDROCK
                && (Bukkit.getCurrentTick() - lastTickAtHeightLimit) < 200
    }

    occasionalTrigger("jm_finish_spawnpoint", ticks = 20, revertAfterTicks = 40) {
        // Finish where you spawned using a Compass
        player.bukkitPlayers.any {
            val spawnLocIgnoreY = it.world.spawnLocation
            spawnLocIgnoreY.y = it.location.y
            it.inventory.contains(Material.COMPASS)
                    && it.world.environment == World.Environment.NORMAL
                    && it.location.distanceSquared(spawnLocIgnoreY) < 5
        }
    }

    occasionalTrigger("jm_finish_top_world", ticks = 20, revertAfterTicks = 40) {
        // Finish on top of the world
        player.bukkitPlayers.any {
            it.location.y >= 256
        }
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
                Tag.ICE.isTagged(event.block.getRelative(BlockFace.UP).type)
            }
            in Tag.ICE.values -> {
                event.block.getRelative(BlockFace.DOWN).type == Material.MAGMA_BLOCK
            }
            else -> false
        }
    }

    eventTrigger<EntityDeathEvent>("jm_kill_animals_fire", vars("var")) {
        // Kill $var Animals with only fire
        if (event.entity is Animals
            && event.entity.lastDamageCause?.cause in ActivationHelpers.FIRE_DAMAGE_CAUSES
            && !event.entity.scoreboardTags.contains("playerHurt")
            && event.entity.location.getNearbyPlayers(30.0).isNotEmpty()
        ) {
            playerState.advance("var")
        } else false
    }
    eventTrigger<EntityDamageByEntityEvent>("jm_kill_animals_fire", vars("var")) {
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

    eventTrigger<EntityDeathEvent>("jm_kill_mob_gravel") {
        // Kill a hostile mob with Gravel/Sand
        event.entity is Monster
                && event.entity.lastDamageCause?.cause == EntityDamageEvent.DamageCause.SUFFOCATION
                && event.entity.location.getNearbyPlayers(20.0).isNotEmpty()
    }

    eventTrigger<PlayerDeathEvent>("jm_kill_self_arrow") {
        // Kill yourself with your own arrow
        ((event.entity.lastDamageCause as? EntityDamageByEntityEvent)?.damager as? Arrow)?.shooter == event.entity
    }

    eventTrigger<ProjectileHitEvent>("jm_kill_self_pearl") {
        // Kill yourself with an Ender Pearl
        val proj = event.entity
        val shooter = event.entity.shooter
        if (proj is EnderPearl && shooter is Player) {
            /* There is no inherent way in Bukkit to detect if the player died to an Ender pearl,
             *  those deaths are just shown as fall deaths. To hack around this, every time a Bukkit
             *  player teleports with an Ender pearl, set that player's "last pearl tick" to the
             *  current tick. If a player died to an ender pearl, then their last pearl tick will
             *  be equal to the current tick. */
            val lastPearlTick = playerState.extra { mutableMapOf<Player, Int>() }
            lastPearlTick[shooter] = event.entity.server.currentTick
        }
        false
    }
    eventTrigger<PlayerDeathEvent>("jm_kill_self_pearl") {
        // Kill yourself with an Ender Pearl
        /* See above ProjectileHitEvent eventTrigger */
        val lastPearlTick = playerState.extra { mutableMapOf<Player, Int>() }
        event.entity.lastDamageCause?.cause == EntityDamageEvent.DamageCause.FALL
                && lastPearlTick[event.entity]?.let { event.entity.server.currentTick == it } == true
    }

    eventTrigger<EntityDeathEvent>("jm_kill_skeleton_own_arrow") {
        // Kill a Skeleton with its own Arrow
        event.entityType == EntityType.SKELETON
                && ((event.entity.lastDamageCause as? EntityDamageByEntityEvent)?.damager as? Arrow)
                    ?.shooter == event.entity
    }

    eventTrigger<BlockPlaceEvent>("jm_lava_glass_cube") {
        // Build a glass cube and fill the inner with lava
        /* No need to check lava place event - impossible to place with an enclosed cube */
        if (event.block.type != Material.GLASS) return@eventTrigger false

        /* Find lava nearby */
        val lavaBlocks = mutableSetOf<Block>()
        for (dx in -1..1) {
            for (dy in -1..1) {
                for (dz in -1..1) {
                    val blk = event.block.getRelative(dx, dy, dz)
                    if (blk.type == Material.LAVA) {
                        lavaBlocks += blk
                    }
                }
            }
        }

        /* Check if each lava is fully enclosed by glass */
        lavaBlocks.any { lava ->
            for (dx in -1..1) {
                for (dy in -1..1) {
                    for (dz in -1..1) {
                        if (dx == 0 && dy == 0 && dz == 0) continue
                        val blk = lava.getRelative(dx, dy, dz)
                        if (blk.location == event.block.location) continue
                        if (blk.type != Material.GLASS) {
                            return@any false
                        }
                    }
                }
            }
            true
        }
    }

    eventTrigger<PlayerLeashEntityEvent>("jm_lead_dolphin") {
        // Leash a Dolphin to a Fence
        event.entity.type == EntityType.DOLPHIN && event.leashHolder.type == EntityType.LEASH_HITCH
    }

    eventTrigger<PlayerLeashEntityEvent>("jm_lead_hang") {
        // Hang a mob with a lead
        if (event.leashHolder.type == EntityType.LEASH_HITCH) {
            val hitchedEntities = playerState.extra { mutableSetOf<UUID>() }
            hitchedEntities += event.entity.uniqueId
        }
        false
    }
    eventTrigger<EntityUnleashEvent>("jm_lead_hang") {
        // Hang a mob with a lead
        val hitchedEntities = playerState.extra { mutableSetOf<UUID>() }
        hitchedEntities -= event.entity.uniqueId
        false
    }
    occasionalTrigger("jm_lead_hang", ticks = 10) {
        // Hang a mob with a lead
        val hitchedEntities = playerState.extra { mutableSetOf<UUID>() }
        for (uuid in hitchedEntities) {
            val entity = Bukkit.getEntity(uuid) ?: continue
            /* The mob must remain off the ground for 3 consecutive iterations of the
             *  occasionalTrigger to be considered hanged. */
            if (!entity.isOnGround) {
                val hangTime = entity.getMetadata("bingoHangTime")
                    .find { it.owningPlugin == BingoPlugin }?.asInt() ?: 0
                entity.setMetadata("bingoHangTime", FixedMetadataValue(BingoPlugin, hangTime + 1))
                if (hangTime > 2) {
                    return@occasionalTrigger true
                }
            } else {
                entity.setMetadata("bingoHangTime", FixedMetadataValue(BingoPlugin, 0))
            }
        }
        false
    }

    eventTrigger<PlayerLeashEntityEvent>("jm_lead_rabbit") {
        // Use a lead on a rabbit
        event.entity.type == EntityType.RABBIT
    }

    eventTrigger<PlayerLevelChangeEvent>("jm_level", vars("var")) {
        // Level <x>
        val required = vars["var"] ?: throw MissingVariableException("var")
        event.newLevel >= required
    }

    eventTrigger<PlayerInteractEvent>("jm_map_marker") {
        // Add a Marker to a Map (by right clicking a banner with the map)
        event.item?.type == Material.FILLED_MAP
                && event.clickedBlock?.type?.let { Tag.BANNERS.isTagged(it) } == true
                && event.action == Action.RIGHT_CLICK_BLOCK
    }

    eventTrigger<CreatureSpawnEvent>("jm_nether_fish") {
        // Get a fish into the nether
        // Going through a portal still fires this event, so no need for other EventTriggers.
        event.location.world.environment == World.Environment.NETHER
                && event.entityType in FISH_ENTITIES
    }

    eventTrigger<PortalCreateEvent>("jm_nether_portal_size", vars("var1", "var2")) {
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

    occasionalTrigger("jm_pigman_water", ticks = 10) {
        worlds.world(World.Environment.NORMAL).getEntitiesByClass(PigZombie::class.java).any {
            it.location.block.type == Material.WATER
        }
    }

    eventTrigger<BlockPlaceEvent>("jm_pimp_tower") {
        // Place an Iron, Gold and Diamond block on top of each other
        val blocks = setOf(Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.IRON_BLOCK)
        if (event.block.type !in blocks) return@eventTrigger false

        val counts = blocks.associateWith { 0 }.toMutableMap()
        for (i in -2..2) {
            val mat = event.block.getRelative(0, i, 0).type
            if (mat in blocks) counts.increment(mat)
        }

        counts.all { (_, v) -> v > 0 }
    }

    eventTrigger<BlockRedstoneEvent>("jm_power_redstone_lamp") {
        // Power a Redstone Lamp
        event.block.type == Material.REDSTONE_LAMP
    }

    eventTrigger<PlayerInteractEvent>("jm_sleep_nether") {
        // Nether bed
        val block = event.clickedBlock
        block != null
                && block.world.environment == World.Environment.NETHER
                && Tag.BEDS.isTagged(block.type)
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
