package com.jtprince.bingo.bukkit.player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.*

/**
 * A bingo player playing alone.
 */
class LocalBingoPlayerSingle(player: OfflinePlayer) : LocalBingoPlayer() {
    private val playerUUID: UUID = player.uniqueId

    override val name: String
        get() = Bukkit.getOfflinePlayer(playerUUID).name ?: "Unknown Player"

    override val formattedName: TextComponent
        get() = Component.text(name)

    override val bukkitPlayers: Collection<Player>
        get() = setOfNotNull(Bukkit.getPlayer(playerUUID))

    override val offlinePlayers: Collection<OfflinePlayer>
        get() = setOfNotNull(Bukkit.getOfflinePlayer(playerUUID))
}
