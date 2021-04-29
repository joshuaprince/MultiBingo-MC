package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.player.BingoPlayer

typealias AutoMarkCallback = (BingoPlayer, spaceId: Int, fulfilled: Boolean) -> Unit

interface AutoMarkTrigger {
    companion object {
        val allAutomatedGoals
            get() = ItemTriggerYaml.defaultYaml.allAutomatedGoals + dslRegistry.allAutomatedGoals
    }

    fun destroy()
}
