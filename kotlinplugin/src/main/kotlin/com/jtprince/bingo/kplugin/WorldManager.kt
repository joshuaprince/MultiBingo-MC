package com.jtprince.bingo.kplugin

import com.jtprince.bingo.kplugin.Messages.bingoTell
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPortalEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerRespawnEvent

object WorldManager {
    val ENVIRONMENTS = mapOf(
        World.Environment.NORMAL to "overworld",
        World.Environment.NETHER to "nether",
        World.Environment.THE_END to "end"
    )

    internal val worldSetNameMap = HashMap<String, WorldSet>()
    internal val worldSetWorldMap = HashMap<World, WorldSet>()

    val spawnWorld: World
        get() = Bukkit.getWorlds()[0]

    /**
     * Create an Overworld, Nether, and End, identified by `worldCode`, that link to each other by
     * portals.
     */
    fun createWorlds(worldCode: String, seed: String): WorldSet {
        val envWorldMap = HashMap<World.Environment, World>()

        ENVIRONMENTS.onEachIndexed { index, (env, dim) ->
            val wc = WorldCreator.name("world_bingo_${worldCode}_${dim}")

            val template: World = BingoPlugin.server.worlds[index]
            wc.copy(template)
            wc.seed(seed.hashCode().toLong())

            val world = wc.createWorld()
            world!!.difficulty = template.difficulty
            envWorldMap[env] = world
        }

        val ws = WorldSet(worldCode, envWorldMap)
        for (world in envWorldMap.values) {
            worldSetWorldMap[world] = ws
        }
        worldSetNameMap[worldCode] = ws
        return ws
    }

    /**
     * A WorldSet is a container for 3 Worlds that can be indexed by Environment.
     */
    class WorldSet internal constructor(private val worldSetCode: String,
                                        private val map: Map<World.Environment, World>) {
        internal fun world(env: World.Environment) = map[env]
        val worlds: Collection<World> = map.values

        fun unloadWorlds() {
            BingoPlugin.logger.info("Unloading WorldSet $worldSetCode")

            for (env in ENVIRONMENTS.keys) {
                val world = world(env) ?: continue

                // Move all players in this world to the spawn world
                world.players.forEach {
                    it.teleport(spawnWorld.spawnLocation)
                    it.bingoTell("The game is over, returning you to the spawn world.")
                }

                Bukkit.getServer().unloadWorld(world, BingoConfig.saveWorlds)
                worldSetWorldMap.remove(world)
            }

            worldSetNameMap.remove(worldSetCode)
        }
    }

    object WorldManagerListener : Listener {
        @EventHandler
        fun onPortal(event: PlayerPortalEvent) {
            val ws: WorldSet = worldSetWorldMap[event.from.world] ?: return

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

            val targetWorld = ws.world(to)
            if (targetWorld != null) {
                val toLoc = event.to
                toLoc.world = targetWorld
                event.to = toLoc
            }
        }

        @EventHandler
        fun onEntityPortal(event: EntityPortalEvent) {
            val ws: WorldSet = worldSetWorldMap[event.from.world] ?: return

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
            if (targetWorld != null) {
                val toLoc = event.to
                toLoc!!.world = targetWorld
                event.to = toLoc
            }
        }

        @EventHandler
        fun onPlayerRespawn(event: PlayerRespawnEvent) {
            if (event.isAnchorSpawn || event.isBedSpawn) {
                if (event.respawnLocation.world != spawnWorld) {
                    return
                }
            }

            val ws: WorldSet = worldSetWorldMap[event.player.world] ?: return
            event.respawnLocation = ws.world(World.Environment.NORMAL)!!.spawnLocation
        }
    }
}
