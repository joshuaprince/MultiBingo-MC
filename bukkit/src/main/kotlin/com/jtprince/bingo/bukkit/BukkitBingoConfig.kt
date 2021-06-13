package com.jtprince.bingo.bukkit

import com.jtprince.bingo.core.BingoConfig
import org.bukkit.configuration.file.FileConfiguration
import org.koin.core.component.KoinComponent

/**
 * Implementation of Bingo configuration provider that uses Bukkit's plugin config YAML.
 */
class BukkitBingoConfig(
    private val pluginConfig: FileConfiguration
) : BingoConfig, KoinComponent {
    override val debug: Boolean
        get() = pluginConfig.getBoolean("debug", false)

    override val webUrl: String by lazy {
        pluginConfig.getString("web_url") ?: throw NoSuchFieldException("No web_url is configured!")
    }

    val saveWorlds: Boolean
        get() = pluginConfig.getBoolean("save_worlds", true)
}
