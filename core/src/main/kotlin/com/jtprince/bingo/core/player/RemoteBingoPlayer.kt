package com.jtprince.bingo.core.player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent

/**
 * Represents a Bingo Player that the backend server has a board for, but that this Minecraft
 * instance does not know about.
 *
 * No Minecraft player will ever exist for this player on this instance. This class can only used
 * for referencing a remote player (such as printing messages when a space is marked by someone
 * externally).
 */
class RemoteBingoPlayer(override val name: String) : BingoPlayer {
    override val formattedName: TextComponent
        get() = Component.text(name)
}
