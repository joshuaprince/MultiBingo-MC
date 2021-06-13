package com.jtprince.bingo.bukkit.platform

import com.jtprince.bingo.bukkit.BingoPlugin
import com.jtprince.bingo.bukkit.automark.definitions.BukkitDslTriggers
import com.jtprince.bingo.core.automark.TriggerDefinition
import com.jtprince.bingo.core.platform.BingoPlatform
import com.jtprince.bukkit.eventregistry.BukkitEventRegistry
import com.jtprince.bukkit.worldset.WorldSetManager
import org.bukkit.Bukkit

class BukkitBingoPlatform(plugin: BingoPlugin) : BingoPlatform {
    override val config: BukkitBingoConfig = BukkitBingoConfig(plugin.config)
    override val scheduler: BukkitBingoScheduler = BukkitBingoScheduler(plugin)

    val eventRegistry = BukkitEventRegistry(plugin)
    val triggerDefinitionRegistry = TriggerDefinition.Registry()
    val worldSetManager = WorldSetManager(plugin, "bingo", baseWorld = { Bukkit.getWorlds()[0] })

    fun onEnable() {
        worldSetManager.onEnable()

        triggerDefinitionRegistry.registerAll(BukkitDslTriggers)
        triggerDefinitionRegistry.registerItemTriggers()
    }

    fun onDisable() {
        eventRegistry.unregisterAll()
        worldSetManager.destroy(config.saveWorlds)
    }
}
