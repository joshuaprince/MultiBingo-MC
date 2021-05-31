package com.jtprince.bingo.kplugin.game.debug

import com.jtprince.bingo.kplugin.automark.AutomatedSpace
import com.jtprince.bingo.kplugin.automark.PlayerTriggerProgress
import com.jtprince.bingo.kplugin.automark.trigger.AutoMarkTriggerFactory
import com.jtprince.bingo.kplugin.game.SetVariables
import com.jtprince.bingo.kplugin.player.LocalBingoPlayer

class DebugSpace(
    val game: DebugGame,
    override val goalId: String,
    override val variables: SetVariables,
) : AutomatedSpace {
    companion object {
        private var lastUsedId = 0
    }

    override val text = goalId + (if (variables.isEmpty()) "" else variables.toString())
    override val spaceId = lastUsedId++
    override val playerProgress: MutableMap<LocalBingoPlayer, PlayerTriggerProgress> by lazy { mutableMapOf() }

    val triggers = AutoMarkTriggerFactory().create(this, game, game)

    override fun destroy() {
        for (trigger in triggers) {
            trigger.destroy()
        }
    }
}
