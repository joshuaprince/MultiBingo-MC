package com.jtprince.bingo.core.player

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.audience.ForwardingAudience

/**
 * Represents a Bingo Player that is playing on this Minecraft instance (as opposed to a
 * RemoteBingoPlayer, who does not have a presence on this instance).
 */
abstract class LocalBingoPlayer : BingoPlayer, ForwardingAudience {
    /**
     * Allows BingoPlayer to be an Audience.
     */
    abstract override fun audiences(): Iterable<Audience>
}
