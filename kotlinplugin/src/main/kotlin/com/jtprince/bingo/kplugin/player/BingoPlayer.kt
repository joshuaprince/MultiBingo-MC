package com.jtprince.bingo.kplugin.player

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.audience.ForwardingAudience
import net.kyori.adventure.text.Component
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

abstract class BingoPlayer : ForwardingAudience {
    /**
     * The name that should be used on the WebSocket, and will be displayed on the webpage.
     */
    abstract val name: String

    /**
     * The name that should be used on the WebSocket, but formatted nicely.
     */
    abstract val formattedName: Component

    /**
     * A list of [Player]s that are online playing as this BingoPlayer.
     * If no Bukkit Players playing as this BingoPlayer are online, returns an empty collection.
     */
    abstract val bukkitPlayers: Collection<Player>

    /**
     * A list of [OfflinePlayer]s that are playing as this BingoPlayer.
     */
    abstract val offlinePlayers: Collection<OfflinePlayer>

    /**
     * The list of [ItemStack]s made up of the collective of all online players' inventories.
     */
    val inventory: Collection<ItemStack>
        get() = bukkitPlayers.map { p -> p.inventory + p.itemOnCursor }.flatten().filterNotNull()

    /**
     * The Player's name with Spaces stripped out.
     */
    val slugName: String
        get() = name.replace(" ", "")

    /**
     * Allows BingoPlayer to be an Audience.
     */
    override fun audiences(): Iterable<Audience> = bukkitPlayers
}
