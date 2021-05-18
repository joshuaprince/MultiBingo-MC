package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.player.BingoPlayer

abstract class AutoMarkTrigger {
    /**
     * Callback that is executed whenever a BingoPlayer performs an action that activates this
     * trigger.
     *
     * For some triggers, the player can undo their progress, which will result in this being called
     * with `fulfilled = false`.
     */
    fun interface Callback {
        fun trigger(player: BingoPlayer, space: AutomatedSpace, fulfilled: Boolean)
    }

    companion object {
        val allAutomatedGoals by lazy {
            ItemTriggerYaml.defaultYaml.allAutomatedGoals + dslRegistry.allAutomatedGoals
        }
    }

    abstract fun destroy()
}
