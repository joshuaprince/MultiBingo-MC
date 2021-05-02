package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.BingoPlugin

/**
 * Defines a Trigger that is called at a regular interval to check if a player has completed a
 * goal.
 */
class OccasionalTrigger internal constructor(
    val space: AutomatedSpace,
    private val playerMapper: EventPlayerMapper,
    private val callback: Callback,
    private val triggerDefinition: OccasionalTriggerDefinition,
) : AutoMarkTrigger() {

    private val taskId = BingoPlugin.server.scheduler.scheduleSyncRepeatingTask(
            BingoPlugin, this::invoke, triggerDefinition.ticks.toLong(), triggerDefinition.ticks.toLong())

    override fun destroy() {
        BingoPlugin.server.scheduler.cancelTask(taskId)
    }

    private fun invoke() {
        playerMapper.allPlayers.forEach {
            if (triggerDefinition.function(OccasionalTriggerDefinition.Parameters(it, this))) {
                callback.trigger(it, space, true)
            }
        }
    }
}
