package com.jtprince.bukkit.worldset

import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPortalEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerRespawnEvent

/**
 * Bukkit listener class for all WorldSets that achieves portal linkage between worlds and
 * WorldSet-specific respawns.
 */
internal class WorldSetManagerListener internal constructor(
    private val manager: WorldSetManager
) : Listener {
    @EventHandler
    fun onPortal(event: PlayerPortalEvent) {
        val worldSet = manager.worldSetOf(event.from.world) ?: run {
            /* The player is not portaling from a tracked world; do nothing */
            return
        }

        val from: World.Environment = event.from.world.environment
        var to: World.Environment = event.to.world.environment

        if (from == World.Environment.NETHER && to == World.Environment.NETHER) {
            /* Special handling for nether -> overworld:
             *
             * When going from bingo_overworld to bingo_nether, the coordinates we are
             * handed are pre-divided by 8. From is bingo_overworld, but To is the_nether.
             * Therefore we just have to update the To world, and we are done.
             *
             * But when we go from bingo_nether to bingo_overworld, the coordinates are not
             * scaled at all! The From dimension is bingo_nether, but the To dimension is
             * incorrectly still the_nether instead of the_overworld.
             *
             * It seems like the game's internal checks for which dimension to send a portal
             * to looks like: "Is To in the_nether ? If so, send to the_overworld. Else,
             * send to the_nether." That means that for any bingo_* world, we will be sent
             * to the_nether.
             *
             * My theory: coming from *any* nether to the_nether, the environment is not
             * changing, so the coordinates calculated and handed to us are not scaled.
             *
             * This was fixed as SPIGOT-6347, so only needs to be run if that patch is not
             * present. We test for that by checking if the `to` environment is also Nether.
             */
            event.to.x *= 8
            event.to.z *= 8
            event.searchRadius = 128
            to = World.Environment.NORMAL
        }

        val targetWorld = worldSet.world(to)
        val toLoc = event.to
        toLoc.world = targetWorld
        event.to = toLoc
    }

    @EventHandler
    fun onEntityPortal(event: EntityPortalEvent) {
        val ws: WorldSet = manager.worldSetOf(event.from.world) ?: return

        val from: World.Environment = event.from.world.environment
        var to: World.Environment = event.to?.world?.environment ?: return

        if (from == World.Environment.NETHER && to == World.Environment.NETHER) {
            /* Special handling for nether -> overworld: see onPortal */
            event.to!!.x *= 8
            event.to!!.z *= 8
            event.searchRadius = 128
            to = World.Environment.NORMAL
        }

        val targetWorld = ws.world(to)
        val toLoc = event.to
        toLoc!!.world = targetWorld
        event.to = toLoc
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        /* Ignore deaths that happened outside of a WorldSet */
        val playerDiedInWorldSet = manager.worldSetOf(event.player.world) ?: return

        if ((event.isAnchorSpawn || event.isBedSpawn)
            && manager.worldSetOf(event.respawnLocation.world) == playerDiedInWorldSet) {
            /* Don't interfere with bed/anchor spawns that are already directed to this WorldSet */
            return
        }

        /* Force respawn at this WorldSet's Overworld's spawn point */
        event.respawnLocation = playerDiedInWorldSet.world(World.Environment.NORMAL).spawnLocation
    }
}
