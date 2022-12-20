package com.jtprince.multibingo.fabric.event

import com.jtprince.multibingo.core.game.BingoGameFactory
import com.jtprince.multibingo.fabric.FabricBingoPlayer
import com.jtprince.multibingo.fabric.FabricMain
import net.kyori.adventure.platform.fabric.FabricClientAudiences

object LoginEventHandler {
    /**
     * Called when the client logs in to a server (or singleplayer world).
     * There is no Fabric event for this, so this is called from a Mixin.
     */
    fun onLogin() {
        val game = BingoGameFactory.currentGame ?: return

        val playerName = "FabricPlayer" // TODO
        FabricMain.bingoPlayer = FabricBingoPlayer(playerName, FabricClientAudiences.of().audience())
        game.administrator = FabricClientAudiences.of().audience()
        game.onPlayerEnterWorld()
    }
}
