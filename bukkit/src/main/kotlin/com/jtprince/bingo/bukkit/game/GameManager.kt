package com.jtprince.bingo.bukkit.game

import com.jtprince.bingo.bukkit.BingoPlugin
import com.jtprince.bingo.bukkit.BukkitMessages.bingoTellError
import com.jtprince.bingo.bukkit.game.debug.DebugGame
import com.jtprince.bingo.bukkit.game.web.WebBackedGame
import com.jtprince.bingo.bukkit.game.web.WebBackedGameProto
import com.jtprince.bingo.bukkit.player.BukkitBingoPlayerFactory
import com.jtprince.bingo.core.SetVariables
import com.jtprince.bingo.core.game.BingoGame
import com.jtprince.bingo.core.webclient.model.WebGameSettings
import net.kyori.adventure.audience.Audience
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object GameManager : KoinComponent {
    private val plugin: BingoPlugin by inject()

    var currentGame: BingoGame? = null
        private set

    fun destroyCurrentGame(sender: Audience, explicit: Boolean = false) {
        if (explicit && currentGame == null) {
            sender.bingoTellError("No game to destroy!")
            return
        }
        currentGame?.destroy(sender)
        currentGame = null
    }

    fun prepareNewWebGame(creator: Audience, settings: WebGameSettings) {
        destroyCurrentGame(creator)
        val newGame = WebBackedGameProto(creator, settings)
        currentGame = newGame

        plugin.core.httpClient.generateBoard(settings) { gameCode ->
            val protoGame = currentGame ?: return@generateBoard

            if (gameCode == null) {
                creator.bingoTellError(
                    "Board generation failed. Check the server log for errors."
                )
                // protoGame.state = BingoGame.State.FAILED TODO
                return@generateBoard
            }

            // if (protoGame.state == BingoGame.State.BOARD_GENERATING) { TODO
                // Only move to the WebBackedGame if nothing else changed in the meantime.
                val bingoPlayers = BukkitBingoPlayerFactory.createPlayers()
                currentGame = WebBackedGame(creator, gameCode, bingoPlayers)
            // }
        }
    }

    fun prepareDebugGame(creator: Audience, goalId: String, variables: SetVariables) {
        destroyCurrentGame(creator)
        val newGame = DebugGame(creator, BukkitBingoPlayerFactory.createPlayers(), goalId, variables)
        currentGame = newGame
    }
}
