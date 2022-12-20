package com.jtprince.multibingo.core.player

import net.kyori.adventure.text.TextComponent

sealed interface BingoPlayer {
    /**
     * The name that will be displayed for this Bingo Player in communications external to the
     * Minecraft server.
     */
    val name: String

    /**
     * The name that will be displayed for this Bingo Player in chat messages.
     */
    val formattedName: TextComponent
}
