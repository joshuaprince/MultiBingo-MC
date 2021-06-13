package com.jtprince.bingo.bukkit.game

import com.jtprince.bingo.bukkit.BingoPlugin
import com.jtprince.bingo.bukkit.BukkitMessages.bingoTellError
import com.jtprince.bingo.bukkit.game.debug.DebugGame
import com.jtprince.bingo.bukkit.game.web.WebBackedGame
import com.jtprince.bingo.bukkit.game.web.WebBackedGameProto
import com.jtprince.bingo.bukkit.player.BukkitBingoPlayerFactory
import com.jtprince.bingo.core.SetVariables
import com.jtprince.bingo.core.automark.AutoMarkConsumer
import com.jtprince.bingo.core.automark.AutomatedSpace
import com.jtprince.bingo.core.player.LocalBingoPlayer
import com.jtprince.bingo.core.webclient.model.WebGameSettings
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BingoGame(
    val creator: CommandSender,
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
        protected set

    private fun destroyCurrentGame(sender: CommandSender?) {
        state = State.DESTROYING
        signalDestroy(sender)
    }

    abstract fun signalStart(sender: CommandSender?)
    abstract fun signalEnd(sender: CommandSender?)
    protected abstract fun signalDestroy(sender: CommandSender?)

    abstract override fun receiveAutoMark(player: LocalBingoPlayer, space: AutomatedSpace,
                                          fulfilled: Boolean)

    companion object Manager : KoinComponent {
        private val plugin: BingoPlugin by inject()

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

        fun prepareNewWebGame(creator: CommandSender, settings: WebGameSettings) {
            destroyCurrentGame(creator)
            val newGame = WebBackedGameProto(creator, settings)
            currentGame = newGame

            plugin.core.httpClient.generateBoard(settings) { gameCode ->
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
                    val bingoPlayers = BukkitBingoPlayerFactory.createPlayers()
                    currentGame = WebBackedGame(creator, gameCode, bingoPlayers)
                }
            }
        }

        fun prepareDebugGame(creator: Player, goalId: String, variables: SetVariables) {
            destroyCurrentGame(creator)
            val newGame = DebugGame(creator, BukkitBingoPlayerFactory.createPlayers(), goalId, variables)
            currentGame = newGame
        }
    }
}
