package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.board.SetVariables
import com.jtprince.bingo.kplugin.player.BingoPlayer

interface AutomatedSpace {
    val goalId: String
    val text: String
    val spaceId: Int
    val variables: SetVariables
    val playerProgress: MutableMap<BingoPlayer, PlayerTriggerProgress>

    fun destroy()
}
