package com.jtprince.bingo.bukkit.automark

import com.jtprince.bingo.bukkit.player.BukkitBingoPlayer

/**
 * Defines an actor that can receive automated space markings from the automark module.
 */
fun interface AutoMarkConsumer {
    /**
     * Called when a space related to this consumer should be marked a certain way. Not filtered,
     * meaning that this might be called several times with the same inputs.
     */
    fun receiveAutoMark(player: BukkitBingoPlayer, space: AutomatedSpace, fulfilled: Boolean)
}
