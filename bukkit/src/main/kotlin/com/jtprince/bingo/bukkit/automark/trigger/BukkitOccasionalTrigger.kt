package com.jtprince.bingo.bukkit.automark.trigger

import com.jtprince.bingo.bukkit.automark.EventPlayerMapper
import com.jtprince.bingo.bukkit.automark.TimedGoalReverter
import com.jtprince.bingo.bukkit.automark.definitions.OccasionalTriggerDefinition
import com.jtprince.bingo.core.automark.AutoMarkConsumer
import com.jtprince.bingo.core.automark.AutoMarkTrigger
import com.jtprince.bingo.core.automark.AutomatedSpace
import org.bukkit.plugin.Plugin

/**
 * Defines a Trigger that is called at a regular interval to check if a player has completed a
 * goal.
 */
class BukkitOccasionalTrigger internal constructor(
    val space: AutomatedSpace,
    private val plugin: Plugin,
    private val playerMapper: EventPlayerMapper,
    private val consumer: AutoMarkConsumer,
    private val triggerDefinition: OccasionalTriggerDefinition,
) : AutoMarkTrigger {

    private val taskId = plugin.server.scheduler.scheduleSyncRepeatingTask(
            plugin, this::invoke, triggerDefinition.ticks.toLong(), triggerDefinition.ticks.toLong())

    private val timedReverter: TimedGoalReverter? = when (triggerDefinition.revertAfterTicks) {
        null -> null
        else -> TimedGoalReverter(triggerDefinition.revertAfterTicks, consumer)
    }

    override fun destroy() {
        plugin.server.scheduler.cancelTask(taskId)
        timedReverter?.destroy()
    }

    private fun invoke() {
        for (player in playerMapper.localPlayers) {
            val worlds = playerMapper.worldSet(player)
            val satisfied = triggerDefinition.function(
                OccasionalTriggerDefinition.Parameters(player, worlds, this))

            /* Occasional triggers are never revertible. */
            if (satisfied) {
                consumer.receiveAutoMark(AutoMarkConsumer.Activation(player, space, true))
                timedReverter?.revertLater(player, space)
            }
        }
    }
}
