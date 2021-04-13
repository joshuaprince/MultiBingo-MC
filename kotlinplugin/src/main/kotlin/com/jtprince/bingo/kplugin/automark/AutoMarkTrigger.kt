package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.board.SetVariables
import com.jtprince.bingo.kplugin.game.PlayerManager
import com.jtprince.bingo.kplugin.player.BingoPlayer

typealias AutoMarkCallback = (BingoPlayer, spaceId: Int, fulfilled: Boolean) -> Unit

abstract class AutoMarkTrigger(
    val goalId: String,
    val spaceId: Int,
    val variables: SetVariables,
    val playerManager: PlayerManager,
    val callback: AutoMarkCallback,
) {
    companion object {
        val allAutomatedGoals
            get() = ItemTriggerYaml.defaultYaml.allAutomatedGoals + dslRegistry.allAutomatedGoals
    }

    /**
     * If (and only if) revertible is true, the AutoMarkCallback may be called with
     * fulfilled == false. In this case, the callback should put the goal into a "reverted" state.
     */
    abstract val revertible: Boolean

    abstract fun destroy()
}
