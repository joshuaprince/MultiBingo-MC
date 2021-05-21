package com.jtprince.bingo.kplugin

import com.jtprince.bingo.kplugin.game.BingoGame
import com.jtprince.bingo.kplugin.webclient.WebHttpClient
import com.jtprince.bukkit.eventregistry.BukkitEventRegistry
import com.jtprince.bukkit.worldset.WorldSetManager
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIConfig
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

val BingoPlugin: BingoPluginClass
    get() = pluginInstance
private lateinit var pluginInstance: BingoPluginClass

class BingoPluginClass : JavaPlugin() {
    init {
        pluginInstance = this
    }

    lateinit var eventRegistry: BukkitEventRegistry
    lateinit var worldSetManager: WorldSetManager

    override fun onEnable() {
        CommandAPI.onEnable(this)
        Commands.registerCommands()
        saveDefaultConfig()

        WebHttpClient.pingBackend()

        worldSetManager = WorldSetManager(this, "bingo", baseWorld = { Bukkit.getWorlds()[0] })
        eventRegistry = BukkitEventRegistry(this)
    }

    override fun onLoad() {
        val debug =  BingoConfig.debug

        if (debug) {
            logger.info("Debug mode is enabled.")
        }

        val cfg = CommandAPIConfig()
        cfg.verboseOutput = debug
        CommandAPI.onLoad(cfg)
    }

    override fun onDisable() {
        BingoGame.destroyCurrentGame(Bukkit.getConsoleSender())
        eventRegistry.unregisterAll()
        worldSetManager.destroy(BingoConfig.saveWorlds)
    }
}
