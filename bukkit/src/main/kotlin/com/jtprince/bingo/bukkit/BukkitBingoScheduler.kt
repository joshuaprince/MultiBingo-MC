package com.jtprince.bingo.bukkit

import com.jtprince.bingo.core.scheduler.Scheduler
import org.bukkit.scheduler.BukkitTask
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Wrapper for the [org.bukkit.scheduler.BukkitScheduler] scheduler for asynchronously dispatching
 * tasks.
 */
class BukkitBingoScheduler : Scheduler, KoinComponent {
    val plugin: BingoPlugin by inject()

    private class BukkitBingoTask(
        private val bukkitTask: BukkitTask
    ): Scheduler.Task {
        override fun cancel() {
            bukkitTask.cancel()
        }
    }

    override fun scheduleAsync(ticks: Long, callback: () -> Unit): Scheduler.Task {
        return BukkitBingoTask(
            plugin.server.scheduler.runTaskLaterAsynchronously(plugin, { -> callback() }, ticks)
        )
    }
}
