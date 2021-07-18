package com.jtprince.bingo.core.game

import com.jtprince.bingo.core.automark.AutoMarkConsumer
import net.kyori.adventure.audience.Audience

interface BingoGame : AutoMarkConsumer {
    val name: String
    val creator: Audience

    fun destroy(sender: Audience?)
}
