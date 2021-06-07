package com.jtprince.bingo.bukkit.automark

import com.jtprince.bingo.bukkit.player.BukkitBingoPlayer
import com.jtprince.bingo.core.SetVariables

interface AutomatedSpace {
    val goalId: String
    val text: String
    val spaceId: Int
    val variables: SetVariables
    val playerProgress: MutableMap<BukkitBingoPlayer, PlayerTriggerProgress>

    fun destroy()
}
