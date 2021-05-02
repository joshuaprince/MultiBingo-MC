package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.BingoPlugin
import com.jtprince.bingo.kplugin.board.SetVariables

/**
 * Defines a Trigger that is called at a regular interval to check if a player has completed a
 * goal.
 */
class OccasionalTrigger internal constructor(
    val goalId: String,
    val spaceId: Int,
    val variables: SetVariables,
    private val playerMapper: EventPlayerMapper,
    val callback: AutoMarkCallback,
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
                callback(it, spaceId, true)
            }
        }
    }
}
