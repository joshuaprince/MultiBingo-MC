package com.jtprince.bingo.kplugin.player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.*

/**
 * A team of 1 or more players that will share a win condition and world.
 */
class BingoPlayerTeam(private val teamName: Component,
                      teammates: Collection<OfflinePlayer>) : BingoPlayer() {
    private val playerUuids: Collection<UUID> = teammates.map(OfflinePlayer::getUniqueId)

    override val name: String
        get() = PlainComponentSerializer.plain().serialize(teamName)

    override val formattedName: Component
        get() = teamName

    override val bukkitPlayers: Collection<Player>
        get() = playerUuids.mapNotNull(Bukkit::getPlayer).filter(OfflinePlayer::isOnline)

    override val offlinePlayers: Collection<OfflinePlayer>
        get() = playerUuids.map(Bukkit::getOfflinePlayer)
}
