package com.jtprince.bingo.bukkit

import com.jtprince.bingo.core.scheduler.Scheduler
import org.bukkit.scheduler.BukkitTask
import org.koin.java.KoinJavaComponent.inject

/**
 * Wrapper for the [org.bukkit.scheduler.BukkitScheduler] scheduler for asynchronously dispatching
 * tasks.
 */
class BukkitBingoScheduler : Scheduler {
    val plugin by inject<BingoPlugin>(BingoPlugin::class.java)

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
