package com.jtprince.bingo.bukkit.player

import net.kyori.adventure.text.TextComponent

sealed class BingoPlayer {
    /**
     * The name that will be displayed for this Bingo Player in communications external to the
     * Minecraft server.
     */
    abstract val name: String

    /**
     * The name that will be displayed for this Bingo Player in chat messages.
     */
    abstract val formattedName: TextComponent
}
