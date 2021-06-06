package com.jtprince.bingo.bukkit.automark

import com.jtprince.bingo.bukkit.player.BukkitBingoPlayer
import com.jtprince.bukkit.worldset.WorldSet
import org.bukkit.event.Event

/**
 * Resolves Bukkit Events to the BingoPlayer that should be rewarded if that event causes a space to
 * be marked.
 */
interface EventPlayerMapper {
    /**
     * Determine which BingoPlayer an Event is associated with, for determining who to potentially
     * automark for. If no Bingo Player was involved with this event, returns null.
     */
    fun mapEvent(event: Event): BukkitBingoPlayer?

    val localPlayers: Collection<BukkitBingoPlayer>

    fun worldSet(player: BukkitBingoPlayer) : WorldSet
}
