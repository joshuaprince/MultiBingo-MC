package com.jtprince.multibingo.core.automark

import com.jtprince.multibingo.core.player.LocalBingoPlayer

interface AutomatedSpace {
    val goalId: String
    val text: String
    val spaceId: Int
    val variables: Map<String, Int>
    val playerProgress: MutableMap<LocalBingoPlayer, PlayerTriggerProgress>

    fun destroy()
}
