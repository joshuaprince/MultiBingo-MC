package com.jtprince.bingo.bukkit

import com.jtprince.bingo.bukkit.automark.definitions.BukkitDslTriggers
import com.jtprince.bingo.bukkit.game.BingoGame
import com.jtprince.bingo.core.BingoCore
import com.jtprince.bingo.core.automark.TriggerDefinition
import com.jtprince.bukkit.eventregistry.BukkitEventRegistry
import com.jtprince.bukkit.worldset.WorldSetManager
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIConfig
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.context.startKoin
import org.koin.dsl.module

class BingoPlugin : JavaPlugin() {
    private companion object {
        private lateinit var pluginInstance: BingoPlugin
    }

    init {
        pluginInstance = this
    }

    /* DI Module */
    private val bingoPluginModule = module {
        single { pluginInstance }
        single { pluginInstance.logger }
    }

    val scheduler = BukkitBingoScheduler()
    lateinit var bingoConfig: BukkitBingoConfig
    lateinit var bingoCore: BingoCore
    lateinit var eventRegistry: BukkitEventRegistry
    lateinit var triggerDefinitionRegistry: TriggerDefinition.Registry
    lateinit var worldSetManager: WorldSetManager

    override fun onEnable() {
        CommandAPI.onEnable(this)
        Commands.registerCommands()
        saveDefaultConfig()

        bingoCore = BingoCore(bingoConfig, scheduler)
        worldSetManager = WorldSetManager(this, "bingo", baseWorld = { Bukkit.getWorlds()[0] })
        eventRegistry = BukkitEventRegistry(this)

        triggerDefinitionRegistry = TriggerDefinition.Registry()
        triggerDefinitionRegistry.registerAll(BukkitDslTriggers)
        triggerDefinitionRegistry.registerItemTriggers()
    }

    override fun onLoad() {
        startKoin {
            modules(bingoPluginModule)
        }

        bingoConfig = BukkitBingoConfig(config)
        val debug =  bingoConfig.debug

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
        worldSetManager.destroy(bingoConfig.saveWorlds)
    }
}
