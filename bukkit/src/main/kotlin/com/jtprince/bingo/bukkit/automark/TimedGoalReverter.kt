package com.jtprince.bingo.bukkit.automark

import com.jtprince.bingo.bukkit.BingoPlugin
import com.jtprince.bingo.bukkit.player.BukkitBingoPlayer
import com.jtprince.bingo.core.automark.AutoMarkConsumer
import com.jtprince.bingo.core.automark.AutomatedSpace
import org.bukkit.Bukkit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class TimedGoalReverter(
    private val ticks: Int,
    private val consumer: AutoMarkConsumer
) : KoinComponent {
    private val plugin: BingoPlugin by inject()
    private var taskIdMap = mutableMapOf<Pair<BukkitBingoPlayer, Int>, Int>()

    fun revertLater(player: BukkitBingoPlayer, space: AutomatedSpace) {
        taskIdMap[player to space.spaceId]?.let {
            /* If we're already planning to revert this player/space combo, cancel that task */
            Bukkit.getScheduler().cancelTask(it)
        }

        taskIdMap[player to space.spaceId] = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
            consumer.receiveAutoMark(AutoMarkConsumer.Activation(player, space, false))
        }, ticks.toLong())
    }

    fun destroy() {
        taskIdMap.values.forEach { Bukkit.getScheduler().cancelTask(it) }
    }
}
