package com.jtprince.bukkit.worldset

import org.bukkit.Bukkit
import org.bukkit.World

/**
 * A WorldSet is a container for 3 portal-linked Worlds (an Overworld, Nether, and End) that can be
 * indexed by Environment.
 */
class WorldSet internal constructor(
    val code: String,
    val manager: WorldSetManager?,
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

    companion object {
        /**
         * Get the WorldSet describing the default Overworld, Nether, and End generated at Bukkit
         * startup.
         *
         * Since the connectivity between these worlds is handled automatically, these worlds are
         * never tracked by any WorldSetManager.
         */
        val defaultWorldSet: WorldSet
            get() {
                @Suppress("UNCHECKED_CAST")  /* No filterValuesNotNull with type safety */
                val map = WorldSetManager.ENVIRONMENTS.keys.associateWith {
                        env -> Bukkit.getWorlds().find { w -> w.environment == env }
                }.filterValues { it != null } as Map<World.Environment, World>
                return WorldSet("", null, map)
            }
    }
}
