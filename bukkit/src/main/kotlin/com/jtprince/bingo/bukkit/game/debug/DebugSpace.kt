package com.jtprince.bingo.bukkit.game.debug

import com.jtprince.bingo.bukkit.automark.trigger.BukkitAutoMarkTriggerFactory
import com.jtprince.bingo.core.SetVariables
import com.jtprince.bingo.core.automark.AutomatedSpace
import com.jtprince.bingo.core.automark.PlayerTriggerProgress
import com.jtprince.bingo.core.player.LocalBingoPlayer

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

    private val triggerFactory = BukkitAutoMarkTriggerFactory(game)
    private val triggers = triggerFactory.create(this, game)

    override fun destroy() {
        for (trigger in triggers) {
            trigger.destroy()
        }
    }
}
