package com.jtprince.multibingo.fabric

import com.jtprince.multibingo.core.BingoCore
import com.jtprince.multibingo.core.platform.BingoPlatform
import com.jtprince.multibingo.fabric.automark.FabricAutoMarkTriggerFactory
import com.jtprince.multibingo.fabric.automark.FabricEventListener
import com.jtprince.multibingo.fabric.event.ServerEventHandler
import com.jtprince.multibingo.fabric.gui.TitleScreenEventHandler
import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Suppress("UNUSED")
object FabricMain: ModInitializer, BingoPlatform {
    override val logger: Logger = LoggerFactory.getLogger("MultiBingo")

    override fun onInitialize() {
        println("MultiBingo has been initialized.")
        TitleScreenEventHandler.register()
        ServerEventHandler.register()
        core.enable()
        FabricBingoCommands.register()

        FabricEventListener.register()
    }

    override val config = FabricBingoConfig
    override val autoMarkFactory = FabricAutoMarkTriggerFactory
    val core = BingoCore(this)

    var bingoPlayer: FabricBingoPlayer? = null
}
