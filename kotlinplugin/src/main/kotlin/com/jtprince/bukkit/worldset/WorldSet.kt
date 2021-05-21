package com.jtprince.bukkit.worldset

import org.bukkit.World
import org.bukkit.entity.Player

/**
 * A WorldSet is a container for 3 portal-linked Worlds (an Overworld, Nether, and End) that can be
 * indexed by Environment.
 *
 * WorldSets are only created by the WorldSet module. Worlds that are generated externally, such as
 * the initial Overworld/Nether/End created on a standard Bukkit server, will NOT be grouped into a
 * WorldSet.
 */
class WorldSet internal constructor(
    val code: String,
    private val manager: WorldSetManager,
    private val map: Map<World.Environment, World>
) {
    /**
     * Get a World within this WorldSet.
     */
    fun world(env: World.Environment) = map[env]
        ?: throw WorldSetMissingWorldException("No environment ${env.name} found in WorldSet $code")

    /**
     * All Worlds within this WorldSet.
     */
    val worlds: Collection<World> = map.values

    /**
     * Unload all Worlds within a WorldSet. Behaves identically to the function in WorldSetManager
     * with `this` as the first argument.
     *
     * @see WorldSetManager.unload
     */
    fun unload(saveWorlds: Boolean, onTeleportPlayerOut: ((player: Player) -> Unit)? = null) {
        manager.unload(this, saveWorlds, onTeleportPlayerOut)
    }
}
