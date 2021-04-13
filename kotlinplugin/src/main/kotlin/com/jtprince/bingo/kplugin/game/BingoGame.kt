package com.jtprince.bingo.kplugin.game

import com.jtprince.bingo.kplugin.Messages.bingoTell
import com.jtprince.bingo.kplugin.board.SetVariables
import com.jtprince.bingo.kplugin.board.Space
import com.jtprince.bingo.kplugin.player.BingoPlayer
import com.jtprince.bingo.kplugin.player.BingoPlayerFactory
import com.jtprince.bingo.kplugin.webclient.WebHttpClient
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

abstract class BingoGame(
    val creator: CommandSender,
    val gameCode: String,
    players: Collection<BingoPlayer>
) {
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
    val playerManager = PlayerManager(players)
    val spaces = HashMap<Int, Space>()

    private fun destroyCurrentGame(sender: CommandSender?) {
        state = State.DESTROYING
        playerManager.destroy()
        for (space in spaces.values) {
            space.destroy()
        }
        signalDestroy(sender)
    }

    abstract fun signalStart(sender: CommandSender?)
    abstract fun signalEnd(sender: CommandSender?)
    protected abstract fun signalDestroy(sender: CommandSender?)

    /**
     * Called when a space in [spaces] should be marked a certain way. Not filtered, meaning
     * that this might be called several times with the same inputs.
     */
    protected abstract fun receiveAutomark(bingoPlayer: BingoPlayer, spaceId: Int,
                                           satisfied: Boolean)

    companion object Manager {
        var currentGame: BingoGame? = null
            private set

        fun destroyCurrentGame(sender: CommandSender) {
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
                    creator.bingoTell("Board generation failed.")
                    protoGame.state = State.FAILED
                    return@generateBoard
                }

                if (protoGame.state == State.BOARD_GENERATING) {
                    // Only move to the WebBackedGame if nothing else changed in the meantime.
                    currentGame = WebBackedGame(
                        creator, gameCode, BingoPlayerFactory.createPlayers())
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
