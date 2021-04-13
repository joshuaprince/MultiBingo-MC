package com.jtprince.bingo.kplugin

import com.jtprince.bingo.kplugin.automark.AutoMarkBukkitListener
import com.jtprince.bingo.kplugin.webclient.WebHttpClient
import dev.jorel.commandapi.CommandAPI
import org.bukkit.plugin.java.JavaPlugin

val BingoPlugin: BingoPluginClass
    get() = pluginInstance
private lateinit var pluginInstance: BingoPluginClass

class BingoPluginClass : JavaPlugin() {
    init {
        pluginInstance = this
    }

    override fun onEnable() {
        CommandAPI.onEnable(this)
        Commands.registerCommands()
        saveDefaultConfig()

        WebHttpClient.pingBackend()

        server.pluginManager.registerEvents(AutoMarkBukkitListener, this)
        server.pluginManager.registerEvents(WorldManager.WorldManagerListener, this)
    }

    override fun onLoad() {
        val debug =  BingoConfig.debug

        if (debug) {
            logger.info("Debug mode is enabled.")
        }

        CommandAPI.onLoad(true)
    }
}
