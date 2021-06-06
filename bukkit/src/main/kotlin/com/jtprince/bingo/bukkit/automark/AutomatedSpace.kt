package com.jtprince.bingo.bukkit.automark

import com.jtprince.bingo.bukkit.game.SetVariables
import com.jtprince.bingo.bukkit.player.LocalBingoPlayer

interface AutomatedSpace {
    val goalId: String
    val text: String
    val spaceId: Int
    val variables: SetVariables
    val playerProgress: MutableMap<LocalBingoPlayer, PlayerTriggerProgress>

    fun destroy()
}
