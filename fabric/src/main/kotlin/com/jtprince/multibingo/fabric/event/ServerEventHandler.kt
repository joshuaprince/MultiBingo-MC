package com.jtprince.multibingo.fabric.event

import com.jtprince.multibingo.core.game.BingoGameFactory
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.MinecraftServer

object ServerEventHandler {
    fun register() {
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping)
    }

    private fun onServerStopping(server: MinecraftServer) {
        BingoGameFactory.closeCurrentGame()
    }
}
