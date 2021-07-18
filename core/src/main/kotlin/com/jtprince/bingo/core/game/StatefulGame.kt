package com.jtprince.bingo.core.game

import net.kyori.adventure.audience.Audience

interface StatefulGame {
    fun signalStart(sender: Audience?)
    fun signalEnd(sender: Audience?)

    fun canSpectate(): Boolean
}
