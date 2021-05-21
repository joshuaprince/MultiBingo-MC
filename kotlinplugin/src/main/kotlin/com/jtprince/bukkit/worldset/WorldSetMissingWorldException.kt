package com.jtprince.bukkit.worldset

/**
 * Thrown when attempting to access a World within a WorldSet, but that World cannot be found.
 */
class WorldSetMissingWorldException internal constructor(msg: String) : Exception(msg)
