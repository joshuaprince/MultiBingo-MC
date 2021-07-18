package com.jtprince.bingo.core.automark

import com.jtprince.bingo.core.player.LocalBingoPlayer

/**
 * Defines an actor that can receive automated space markings from the automark module.
 */
fun interface AutoMarkConsumer {
    /**
     * Container for information about an automated marking event for a player.
     */
    class Activation (
        val player: LocalBingoPlayer,
        val space: AutomatedSpace,
        val fulfilled: Boolean,
    )

    /**
     * Called when a space related to this consumer should be marked a certain way. Not filtered,
     * meaning that this might be called several times with the same inputs.
     */
    fun receiveAutoMark(activation: Activation)
}
