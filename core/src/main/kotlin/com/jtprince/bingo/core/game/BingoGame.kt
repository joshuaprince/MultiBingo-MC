package com.jtprince.bingo.core.game

import com.jtprince.bingo.core.automark.AutoMarkConsumer
import com.jtprince.bingo.core.automark.AutomatedSpace
import com.jtprince.bingo.core.player.LocalBingoPlayer
import net.kyori.adventure.audience.Audience

abstract class BingoGame(
    val creator: Audience,
    val gameCode: String,
) : AutoMarkConsumer {
    enum class State {
        BOARD_GENERATING,
        WAITING_FOR_WEBSOCKET,
        WORLDS_GENERATING,
        READY,
        COUNTING_DOWN,
        RUNNING,
        DONE,
        FAILED,
        DESTROYING
    }

    abstract var state: State
        // protected set TODO

    fun destroyCurrentGame(sender: Audience?) {
        state = State.DESTROYING
        signalDestroy(sender)
    }

    abstract fun signalStart(sender: Audience?)
    abstract fun signalEnd(sender: Audience?)
    protected abstract fun signalDestroy(sender: Audience?)

    abstract override fun receiveAutoMark(player: LocalBingoPlayer, space: AutomatedSpace,
                                          fulfilled: Boolean)
}
