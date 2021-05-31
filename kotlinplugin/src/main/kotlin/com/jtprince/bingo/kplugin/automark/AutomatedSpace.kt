package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.game.SetVariables
import com.jtprince.bingo.kplugin.player.LocalBingoPlayer

interface AutomatedSpace {
    val goalId: String
    val text: String
    val spaceId: Int
    val variables: SetVariables
    val playerProgress: MutableMap<LocalBingoPlayer, PlayerTriggerProgress>

    fun destroy()
}
