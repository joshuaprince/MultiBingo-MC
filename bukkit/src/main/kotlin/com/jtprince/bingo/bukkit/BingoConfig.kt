package com.jtprince.bingo.bukkit

import com.jtprince.bingo.bukkit.player.BukkitBingoPlayer
import org.apache.http.client.utils.URIBuilder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URI

object BingoConfig : KoinComponent {
    private val plugin: BingoPlugin by inject()

    val debug: Boolean
        get() = plugin.config.getBoolean("debug", false)

    private val webUrl: String by lazy {
        plugin.config.getString("web_url") ?: throw NoSuchFieldException("No web_url is configured!")
    }

    fun boardCreateUrl(): URI {
        val builder = URIBuilder(webUrl)
        builder.setPathSegments("rest", "generate_board")
        return builder.build()
    }

    fun gameUrl(gameCode: String, forPlayer: BukkitBingoPlayer): URI {
        val builder = URIBuilder(webUrl)
        builder.setPathSegments("game", gameCode)
        builder.setParameter("name", forPlayer.name)
        return builder.build()
    }

    fun webPingUrl(): URI {
        val builder = URIBuilder(webUrl)
        builder.setPathSegments("ping")
        return builder.build()
    }

    val saveWorlds: Boolean
        get() = plugin.config.getBoolean("save_worlds", true)

    fun websocketUrl(gameCode: String, clientId: String): URI {
        val builder = URIBuilder(webUrl)
        builder.setPathSegments("ws", "board-plugin", gameCode, clientId)
        builder.scheme = when(builder.scheme) {
            "http" -> "ws"
            "https" -> "wss"
            else -> throw IllegalArgumentException("Invalid web URL protocol ${builder.scheme}")
        }

        return builder.build()
    }
}
