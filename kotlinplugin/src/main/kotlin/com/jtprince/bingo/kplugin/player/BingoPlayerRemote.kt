package com.jtprince.bingo.kplugin.player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

/**
 * Represents a Bingo Player that the web server knows about, but that this plugin does not know
 * about. No Bukkit player will ever exist for this player, and it should only used for referencing
 * a remote player (such as printing messages when a square is marked).
 */
class BingoPlayerRemote(override val name: String) : BingoPlayer() {
    override val formattedName: TextComponent
        get() = Component.text(name)

    override val bukkitPlayers: Collection<Player>
        get() = emptySet()
    override val offlinePlayers: Collection<OfflinePlayer>
        get() = emptySet()
}
