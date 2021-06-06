package com.jtprince.bingo.core.player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent

/**
 * Represents a Bingo Player that the web server knows about, but that this plugin does not know
 * about. No Bukkit player will ever exist for this player, and it should only used for referencing
 * a remote player (such as printing messages when a space is marked).
 */
class RemoteBingoPlayer(override val name: String) : BingoPlayer {
    override val formattedName: TextComponent
        get() = Component.text(name)
}
