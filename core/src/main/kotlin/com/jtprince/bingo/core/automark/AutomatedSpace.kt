package com.jtprince.bingo.core.automark

import com.jtprince.bingo.core.SetVariables
import com.jtprince.bingo.core.player.LocalBingoPlayer

interface AutomatedSpace {
    val goalId: String
    val text: String
    val spaceId: Int
    val variables: SetVariables
    val playerProgress: MutableMap<LocalBingoPlayer, PlayerTriggerProgress>

    fun destroy()
}
