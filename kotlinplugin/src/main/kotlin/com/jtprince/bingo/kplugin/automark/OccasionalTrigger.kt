package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.BingoPlugin
import com.jtprince.bingo.kplugin.board.SetVariables
import com.jtprince.bingo.kplugin.game.PlayerManager

/**
 * Defines a Trigger that is called at a regular interval to check if a player has completed a
 * goal.
 */
class OccasionalTrigger internal constructor(
    goalId: String,
    spaceId: Int,
    variables: SetVariables,
    playerManager: PlayerManager,
    callback: AutoMarkCallback,
    private val triggerDefinition: OccasionalTriggerDefinition,
) : AutoMarkTrigger(goalId, spaceId, variables, playerManager, callback) {

    override val revertible = false

    private val taskId = BingoPlugin.server.scheduler.scheduleSyncRepeatingTask(
            BingoPlugin, this::invoke, triggerDefinition.ticks.toLong(), triggerDefinition.ticks.toLong())

    override fun destroy() {
        BingoPlugin.server.scheduler.cancelTask(taskId)
    }

    private fun invoke() {
        playerManager.localPlayers.forEach {
            if (triggerDefinition.function(OccasionalTriggerDefinition.Parameters(it, this))) {
                callback(it, spaceId, true)
            }
        }
    }
}
