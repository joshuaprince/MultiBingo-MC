package com.jtprince.bingo.bukkit.platform

import com.jtprince.bingo.core.platform.Scheduler
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import org.koin.core.component.KoinComponent

/**
 * Wrapper for the [org.bukkit.scheduler.BukkitScheduler] scheduler for asynchronously dispatching
 * tasks.
 */
class BukkitBingoScheduler(private val plugin: Plugin) : Scheduler, KoinComponent {
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
