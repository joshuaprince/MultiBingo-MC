package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.board.SetVariables
import com.jtprince.bingo.kplugin.game.PlayerManager
import com.jtprince.bingo.kplugin.player.BingoPlayer

class SpecialItemTrigger internal constructor(
    goalId: String,
    spaceId: Int,
    variables: SetVariables,
    playerManager: PlayerManager,
    callback: AutoMarkCallback,
    private val triggerDefinition: SpecialItemTriggerDefinition,
) : ItemTrigger(goalId, spaceId, variables, playerManager, callback, null) {

    override val revertible = triggerDefinition.revertible

    override fun satisfiedBy(player: BingoPlayer): Boolean {
        return triggerDefinition.function(
            SpecialItemTriggerDefinition.Parameters(player, player.inventory, this))
    }
}
