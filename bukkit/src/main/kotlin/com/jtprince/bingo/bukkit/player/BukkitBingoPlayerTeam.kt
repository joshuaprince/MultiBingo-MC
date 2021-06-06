package com.jtprince.bingo.bukkit.player

import com.jtprince.util.ChatUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.*

/**
 * A team of 1 or more players that will share a win condition and world.
 */
class BukkitBingoPlayerTeam(
    private val teamName: TextComponent,
    teammates: Collection<OfflinePlayer>
) : BukkitBingoPlayer() {
    private val playerUuids: Collection<UUID> = teammates.map(OfflinePlayer::getUniqueId)

    override val name: String
        get() = PlainComponentSerializer.plain().serialize(teamName)

    override val formattedName: TextComponent
        get() {
            val playersStr = ChatUtils.commaSeparated(offlinePlayers.map { p ->
                Component.text(p.name ?: "UnknownPlayer${p.uniqueId.toString().substring(0..5)}")
            })
            return teamName.hoverEvent(HoverEvent.showText(playersStr))
        }

    override val bukkitPlayers: Collection<Player>
        get() = playerUuids.mapNotNull(Bukkit::getPlayer).filter(OfflinePlayer::isOnline)

    override val offlinePlayers: Collection<OfflinePlayer>
        get() = playerUuids.map(Bukkit::getOfflinePlayer)
}
