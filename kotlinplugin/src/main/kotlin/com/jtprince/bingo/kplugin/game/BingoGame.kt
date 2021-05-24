package com.jtprince.bingo.kplugin.game

import com.jtprince.bingo.kplugin.Messages.bingoTellError
import com.jtprince.bingo.kplugin.automark.AutoMarkConsumer
import com.jtprince.bingo.kplugin.automark.AutomatedSpace
import com.jtprince.bingo.kplugin.game.debug.DebugGame
import com.jtprince.bingo.kplugin.game.web.WebBackedGame
import com.jtprince.bingo.kplugin.game.web.WebBackedGameProto
import com.jtprince.bingo.kplugin.player.BingoPlayer
import com.jtprince.bingo.kplugin.player.BingoPlayerFactory
import com.jtprince.bingo.kplugin.webclient.WebHttpClient
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

abstract class BingoGame(
    val creator: CommandSender,
    val gameCode: String,
    val players: Collection<BingoPlayer>
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
        protected set

    private fun destroyCurrentGame(sender: CommandSender?) {
        state = State.DESTROYING
        signalDestroy(sender)
    }

    abstract fun signalStart(sender: CommandSender?)
    abstract fun signalEnd(sender: CommandSender?)
    protected abstract fun signalDestroy(sender: CommandSender?)

    abstract override fun receiveAutoMark(player: BingoPlayer, space: AutomatedSpace,
                                          fulfilled: Boolean)

    companion object Manager {
        var currentGame: BingoGame? = null
            private set

        fun destroyCurrentGame(sender: CommandSender, explicit: Boolean = false) {
            if (explicit && currentGame == null) {
                sender.bingoTellError("No game to destroy!")
                return
            }
            currentGame?.destroyCurrentGame(sender)
            currentGame = null
        }

        fun prepareNewWebGame(creator: CommandSender,
                              settings: WebBackedGameProto.WebGameSettings) {
            destroyCurrentGame(creator)
            val newGame = WebBackedGameProto(creator, settings)
            currentGame = newGame

            WebHttpClient.generateBoard(settings) { gameCode ->
                val protoGame = currentGame ?: return@generateBoard

                if (gameCode == null) {
                    creator.bingoTellError(
                        "Board generation failed. Check the server log for errors."
                    )
                    protoGame.state = State.FAILED
                    return@generateBoard
                }

                if (protoGame.state == State.BOARD_GENERATING) {
                    // Only move to the WebBackedGame if nothing else changed in the meantime.
                    val bingoPlayers = BingoPlayerFactory.createPlayers()
                    currentGame = WebBackedGame(creator, gameCode, bingoPlayers)
                }
            }
        }

        fun prepareDebugGame(creator: Player, goalId: String, variables: SetVariables) {
            destroyCurrentGame(creator)
            val newGame = DebugGame(creator, BingoPlayerFactory.createPlayers(), goalId, variables)
            currentGame = newGame
        }
    }
}
