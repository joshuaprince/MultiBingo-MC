package com.jtprince.bingo.bukkit.platform

import com.jtprince.bingo.core.platform.Config
import org.bukkit.configuration.file.FileConfiguration

/**
 * Implementation of Bingo configuration provider that uses Bukkit's plugin config YAML.
 */
class BukkitBingoConfig(
    private val pluginConfig: FileConfiguration
) : Config {
    override val debug: Boolean
        get() = pluginConfig.getBoolean("debug", false)

    override val webUrl: String by lazy {
        pluginConfig.getString("web_url") ?: throw NoSuchFieldException("No web_url is configured!")
    }

    val saveWorlds: Boolean
        get() = pluginConfig.getBoolean("save_worlds", true)
}
