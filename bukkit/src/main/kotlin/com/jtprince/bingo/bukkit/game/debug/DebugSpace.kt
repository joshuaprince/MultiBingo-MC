package com.jtprince.bingo.bukkit.game.debug

import com.jtprince.bingo.bukkit.automark.AutomatedSpace
import com.jtprince.bingo.bukkit.automark.PlayerTriggerProgress
import com.jtprince.bingo.bukkit.automark.trigger.AutoMarkTriggerFactory
import com.jtprince.bingo.bukkit.game.SetVariables
import com.jtprince.bingo.bukkit.player.LocalBingoPlayer

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
