package com.jtprince.bingo.kplugin.automark.trigger

import com.jtprince.bingo.kplugin.automark.AutomatedSpace
import com.jtprince.bingo.kplugin.automark.definitions.ItemTriggerYaml
import com.jtprince.bingo.kplugin.automark.definitions.dslRegistry
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

    abstract fun destroy()
}
