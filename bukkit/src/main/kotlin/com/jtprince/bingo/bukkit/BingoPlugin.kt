package com.jtprince.bingo.bukkit

import com.jtprince.bingo.bukkit.game.BingoGame
import com.jtprince.bingo.bukkit.platform.BukkitBingoPlatform
import com.jtprince.bingo.core.BingoCore
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

    val platform = BukkitBingoPlatform(this)
    val core = BingoCore(platform)

    override fun onEnable() {
        CommandAPI.onEnable(this)
        Commands.registerCommands()
        saveDefaultConfig()

        platform.onEnable()
        core.onEnable()
    }

    override fun onLoad() {
        startKoin {
            modules(bingoPluginModule)
        }

        val debug = platform.config.debug
        if (debug) {
            logger.info("Debug mode is enabled.")
        }

        val cfg = CommandAPIConfig()
        cfg.verboseOutput = debug
        CommandAPI.onLoad(cfg)
    }

    override fun onDisable() {
        BingoGame.destroyCurrentGame(Bukkit.getConsoleSender())
        platform.onDisable()
    }
}
