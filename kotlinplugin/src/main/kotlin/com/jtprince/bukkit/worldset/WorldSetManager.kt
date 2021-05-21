package com.jtprince.bukkit.worldset

import com.jtprince.bingo.kplugin.BingoPlugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.plugin.Plugin

/**
 * Bukkit utility module that provides multiworld support, where instead of allowing individual
 * worlds to be created, allows for the creation of "World Sets" - combinations of linked Overworld,
 * Nether, and End worlds.
 *
 * @param plugin Plugin instance used to create Event handlers.
 * @param prefix String that will be appended to the names of all Worlds created by this manager.
 * @param baseWorld Function to get the World that will be used to derive basic settings for
 *                  generated Worlds, such as difficulty and whether to generate structures.
 */
class WorldSetManager(
    internal val plugin: Plugin,
    private val prefix: String,
    private val baseWorld: () -> World,  /* Lambda so WorldSetManager can be created before worlds load */
) {
    companion object {
        internal val ENVIRONMENTS = mapOf(
            World.Environment.NORMAL to "overworld",
            World.Environment.NETHER to "nether",
            World.Environment.THE_END to "end"
        )
    }

    private val worldSetNameMap = HashMap<String, WorldSet>()
    private val worldSetWorldMap = HashMap<World, WorldSet>()

    private val listener: WorldSetManagerListener = WorldSetManagerListener(this)

    init {
        plugin.server.pluginManager.registerEvents(listener, plugin)
    }

    /**
     * Unregister WorldSet listeners and unload all associated worlds.
     *
     * @see unload
     */
    fun destroy(
        saveWorlds: Boolean,
        onTeleportPlayerOut: ((player: Player) -> Unit)? = null
    ) {
        HandlerList.unregisterAll(listener)
        for (worldSet in worldSetNameMap.values) {
            worldSet.unload(saveWorlds, onTeleportPlayerOut)
        }
    }

    /**
     * Create an Overworld, Nether, and End, identified uniquely by `worldCode`, that link to each
     * other by portals.
     *
     * @param worldSetCode A unique identifier for this WorldSet that will be used in its file name.
     * @param seed Seed to use for this WorldSet.
     */
    fun createWorldSet(worldSetCode: String, seed: Long): WorldSet {
        val envWorldMap = mutableMapOf<World.Environment, World>()

        if (worldSetCode in worldSetNameMap.keys) {
            throw RuntimeException(
                "Tried to create a WorldSet with code $worldSetCode that already exists.")
        }

        ENVIRONMENTS.onEachIndexed { index, (env, dim) ->
            val creator = WorldCreator.name("world_${prefix}_${worldSetCode}_${dim}")

            val template: World = BingoPlugin.server.worlds[index]
            creator.copy(template)
            creator.seed(seed)

            val world = creator.createWorld()
            world!!.difficulty = template.difficulty
            envWorldMap[env] = world
        }

        val worldSet = WorldSet(worldSetCode, this, envWorldMap.toMap())
        for (world in envWorldMap.values) {
            /* Add reverse lookup table for World -> WorldSet */
            worldSetWorldMap[world] = worldSet
        }
        worldSetNameMap[worldSetCode] = worldSet
        return worldSet
    }

    /**
     * Create an Overworld, Nether, and End, identified uniquely by `worldCode`, that link to each
     * other by portals.
     *
     * @param worldSetCode A unique identifier for this WorldSet that will be used in its file name.
     * @param seed Seed to use for this WorldSet.
     */
    fun createWorldSet(worldSetCode: String, seed: String): WorldSet =
        createWorldSet(worldSetCode, seed.hashCode().toLong())

    /**
     * Unload all Worlds within a WorldSet. Any players within those Worlds will be teleported to
     * the spawn point of this WorldSetManager's [baseWorld].
     *
     * @param worldSet The WorldSet to unload.
     * @param saveWorlds If true, worlds will be saved to disk before being unloaded. If false,
     *                   worlds may still be saved, but it is not guaranteed.
     * @param onTeleportPlayerOut A callback that will be executed for each player that is within
     *                            this WorldSet when they are teleported out.
     */
    fun unload(
        worldSet: WorldSet,
        saveWorlds: Boolean,
        onTeleportPlayerOut: ((player: Player) -> Unit)? = null
    ) {
        val teleportOutTo: Location = baseWorld().spawnLocation

        plugin.logger.info("Unloading WorldSet ${worldSet.code}")

        for (env in ENVIRONMENTS.keys) {
            val world = worldSet.world(env)

            /* Move all players in this world to the spawn world */
            for (player in world.players) {
                player.teleport(teleportOutTo)
                onTeleportPlayerOut?.invoke(player)
            }

            Bukkit.getServer().unloadWorld(world, saveWorlds)
            worldSetWorldMap.remove(world)
        }

        worldSetNameMap.remove(worldSet.code)
    }

    /**
     * Find the WorldSet that contains a certain World, or `null` if the World is not a part of a
     * WorldSet.
     */
    fun worldSetOf(world: World): WorldSet? = worldSetWorldMap[world]
}
