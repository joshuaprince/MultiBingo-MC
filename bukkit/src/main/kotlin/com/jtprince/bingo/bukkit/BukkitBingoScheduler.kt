package com.jtprince.bingo.bukkit

import com.jtprince.bingo.core.scheduler.Scheduler
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

/**
 * Wrapper for the [org.bukkit.scheduler.BukkitScheduler] scheduler for asynchronously dispatching
 * tasks.
 */
class BukkitBingoScheduler(private val plugin: Plugin): Scheduler {
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
