package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.player.BingoPlayer

typealias AutoMarkCallback = (BingoPlayer, spaceId: Int, fulfilled: Boolean) -> Unit

abstract class AutoMarkTrigger {
    companion object {
        val allAutomatedGoals
            get() = ItemTriggerYaml.defaultYaml.allAutomatedGoals + dslRegistry.allAutomatedGoals
    }

    abstract fun destroy()

    internal val playerStates: MutableMap<BingoPlayer, PlayerTriggerProgress> by lazy { mutableMapOf() }
}
