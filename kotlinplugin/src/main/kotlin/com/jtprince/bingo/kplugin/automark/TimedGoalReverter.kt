package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.BingoPlugin
import com.jtprince.bingo.kplugin.automark.trigger.AutoMarkTrigger
import com.jtprince.bingo.kplugin.player.BingoPlayer
import org.bukkit.Bukkit

internal class TimedGoalReverter(private val ticks: Int, val callback: AutoMarkTrigger.Callback) {
    private var taskIdMap = mutableMapOf<Pair<BingoPlayer, Int>, Int>()

    fun revertLater(player: BingoPlayer, space: AutomatedSpace) {
        taskIdMap[player to space.spaceId]?.let {
            /* If we're already planning to revert this player/space combo, cancel that task */
            Bukkit.getScheduler().cancelTask(it)
        }

        taskIdMap[player to space.spaceId] = Bukkit.getScheduler().scheduleSyncDelayedTask(BingoPlugin, {
            callback.trigger(player, space, false)
        }, ticks.toLong())
    }

    fun destroy() {
        taskIdMap.values.forEach { Bukkit.getScheduler().cancelTask(it) }
    }
}
