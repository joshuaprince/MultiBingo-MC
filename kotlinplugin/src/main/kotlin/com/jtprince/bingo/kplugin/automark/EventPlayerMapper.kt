package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.player.BingoPlayer
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
    fun mapEvent(event: Event): BingoPlayer?

    val allPlayers: Collection<BingoPlayer>

    fun worldSet(player: BingoPlayer) : WorldSet
}
