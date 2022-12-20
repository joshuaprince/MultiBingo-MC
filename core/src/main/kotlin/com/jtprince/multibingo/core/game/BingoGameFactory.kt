package com.jtprince.multibingo.core.game

import com.jtprince.multibingo.core.BingoCore

object BingoGameFactory {
    var currentGame: BingoGame? = null
        private set

    fun createDebugGame() {
        closeCurrentGame()
        currentGame = DebugGame()
    }

    fun closeCurrentGame() {
        val game = currentGame ?: return
        BingoCore.INSTANCE.platform.logger.info("Closing game $currentGame.")
        game.close()

        currentGame = null
    }
}
