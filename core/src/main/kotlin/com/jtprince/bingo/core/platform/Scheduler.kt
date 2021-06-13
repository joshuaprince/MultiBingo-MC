package com.jtprince.bingo.core.platform

/**
 * An interface for scheduling asynchronous tasks and potentially canceling them.
 *
 * This is made generic so that different mod platforms can implement it differently; for example,
 * Bukkit has BukkitScheduler.
 */
interface Scheduler {
    interface Task {
        fun cancel()
    }

    /**
     * Schedule a task to be executed off the main Minecraft thread, allowing it to block and
     * perform heavy operations such as web requests.
     *
     * @param ticks If nonzero, this task should be delayed by this many Minecraft ticks.
     */
    fun scheduleAsync(ticks: Long = 0, callback: () -> Unit): Task
}
