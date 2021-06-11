package com.jtprince.bingo.bukkit

import com.jtprince.bingo.core.webclient.model.WebModelPluginParity
import org.bukkit.GameRule
import org.bukkit.World
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.Serializable
import java.util.logging.Logger

class PluginParity(
    private val gameCode: String,
    private val overworld: World,
    private val sendEcho: (mySettings: Settings) -> Unit
) : KoinComponent {
    class Settings(val map: Map<String, Serializable>) : Map<String, Serializable> by map

    private val plugin: BingoPlugin by inject()
    private val logger: Logger by inject()

    private val printedMismatchClientIds = mutableSetOf<String>()

    fun getMySettings(): Settings {
        val unimportantGameRules = listOf(
            GameRule.ANNOUNCE_ADVANCEMENTS,
            GameRule.COMMAND_BLOCK_OUTPUT,
            GameRule.DISABLE_ELYTRA_MOVEMENT_CHECK,
            GameRule.LOG_ADMIN_COMMANDS,
            GameRule.MAX_COMMAND_CHAIN_LENGTH,
            GameRule.SEND_COMMAND_FEEDBACK,
            GameRule.SHOW_DEATH_MESSAGES,
            GameRule.SPAWN_RADIUS,
            GameRule.SPECTATORS_GENERATE_CHUNKS,
        )

        val gameRuleSettings = mutableMapOf<String, Serializable>()
        for (rule in GameRule.values()) {
            if (rule in unimportantGameRules) continue
            val value = overworld.getGameRuleValue(rule) ?: overworld.getGameRuleDefault(rule) ?: continue
            gameRuleSettings["Game Rule ${rule.name}"] = value as Serializable
        }

        val entityTrackingSettings = mutableMapOf<String, Serializable>()
        val trackingSection = plugin.server.spigot().spigotConfig.getConfigurationSection(
            "world-settings.default.entity-tracking-range")
        trackingSection?.getValues(false)?.forEach {
            entityTrackingSettings["Entity Tracking Range ${it.key}"] = it.value as Serializable
        }

        val otherSettings = mapOf(
            "Difficulty" to overworld.difficulty.name,
            "Generate Structures" to overworld.canGenerateStructures(),
            "MultiBingo plugin version" to plugin.description.version,
            "View Distance" to plugin.server.viewDistance,
        )

        return Settings(gameRuleSettings + entityTrackingSettings + otherSettings)
    }

    fun receiveSettings(message: WebModelPluginParity) {
        if (message.sender in printedMismatchClientIds) return

        val mySettings = getMySettings()
        var mismatchFound = false

        for (senderSetting in message.settings) {
            val mySetting = mySettings[senderSetting.key] ?: continue
            if (mySetting != senderSetting.value) {
                if (!mismatchFound) {
                    logger.warning("Configuration mismatch with another Bingo Plugin in game $gameCode:")
                    logger.warning("  Server contains players: ${getPlayersByClientId(message.sender)}")
                    mismatchFound = true
                }

                logger.warning("  * ${senderSetting.key}: Mine $mySetting, theirs ${senderSetting.value}")
            }
        }

        if (mismatchFound && !message.isEcho) {
            sendEcho(mySettings)
            printedMismatchClientIds += message.sender
        }
    }

    private fun getPlayersByClientId(clientId: String): String {
        val playerNames = clientId.split(":").getOrNull(1)?.split(",")
            ?: return "(Unknown)"

        if (playerNames.isEmpty()) {
            return "(None)"
        }

        return playerNames.joinToString(", ")
    }
}
