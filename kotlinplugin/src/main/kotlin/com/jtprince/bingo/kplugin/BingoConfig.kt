package com.jtprince.bingo.kplugin

import com.jtprince.bingo.kplugin.player.BingoPlayer
import io.ktor.http.*

object BingoConfig {
    val debug: Boolean
        get() = BingoPlugin.config.getBoolean("debug", false)

    private val webUrl: String by lazy {
        BingoPlugin.config.getString("web_url") ?: throw NoSuchFieldException("No web_url is configured!")
    }

    fun boardCreateUrl(): Url {
        val builder = URLBuilder(webUrl)
        builder.path("rest", "generate_board")
        return builder.build()
    }

    fun gameUrl(gameCode: String, forPlayer: BingoPlayer): Url {
        val builder = URLBuilder(webUrl)
        builder.path("game", gameCode)
        builder.parameters["name"] = forPlayer.name
        return builder.build()
    }

    fun webPingUrl(): Url {
        val builder = URLBuilder(webUrl)
        builder.path("ping")
        return builder.build()
    }

    val saveWorlds: Boolean
        get() = BingoPlugin.config.getBoolean("save_worlds", true)

    fun websocketUrl(gameCode: String, clientId: String): Url {
        val builder = URLBuilder(webUrl)
        builder.path("ws", "board-plugin", gameCode, clientId)
        builder.protocol = when(builder.protocol) {
            URLProtocol.HTTP -> URLProtocol.WS
            URLProtocol.HTTPS -> URLProtocol.WSS
            else -> throw IllegalArgumentException("Invalid web URL protocol ${builder.protocol}")
        }

        return builder.build()
    }
}
