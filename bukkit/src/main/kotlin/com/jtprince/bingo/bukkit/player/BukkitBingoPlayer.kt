package com.jtprince.bingo.bukkit.player

import com.jtprince.bingo.bukkit.automark.BingoInventory
import com.jtprince.bingo.core.player.LocalBingoPlayer
import net.kyori.adventure.audience.Audience
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

abstract class BukkitBingoPlayer : LocalBingoPlayer() {
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
    val inventory = object: BingoInventory {
        override val items: Collection<ItemStack>
            get() = bukkitPlayers.map { it.inventory + it.itemOnCursor }.flatten().filterNotNull()
        override val playerInventories: Collection<PlayerInventory>
            get() = bukkitPlayers.map { it.inventory }
    }

    override fun audiences(): Iterable<Audience> = bukkitPlayers
}
