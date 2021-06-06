package com.jtprince.bingo.bukkit.automark

import com.jtprince.bingo.bukkit.BingoPlugin
import com.jtprince.bingo.bukkit.player.LocalBingoPlayer
import org.bukkit.Bukkit

internal class TimedGoalReverter(private val ticks: Int, val consumer: AutoMarkConsumer) {
    private var taskIdMap = mutableMapOf<Pair<LocalBingoPlayer, Int>, Int>()

    fun revertLater(player: LocalBingoPlayer, space: AutomatedSpace) {
        taskIdMap[player to space.spaceId]?.let {
            /* If we're already planning to revert this player/space combo, cancel that task */
            Bukkit.getScheduler().cancelTask(it)
        }

        taskIdMap[player to space.spaceId] = Bukkit.getScheduler().scheduleSyncDelayedTask(BingoPlugin, {
            consumer.receiveAutoMark(player, space, false)
        }, ticks.toLong())
    }

    fun destroy() {
        taskIdMap.values.forEach { Bukkit.getScheduler().cancelTask(it) }
    }
}
