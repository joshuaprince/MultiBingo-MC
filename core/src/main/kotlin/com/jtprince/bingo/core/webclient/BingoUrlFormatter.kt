package com.jtprince.bingo.core.webclient

import com.jtprince.bingo.core.player.BingoPlayer
import org.apache.http.client.utils.URIBuilder
import java.net.URI

class BingoUrlFormatter(private val webUrl: String) {  // TODO: Make internal
    fun boardCreateUrl(): URI {
        val builder = URIBuilder(webUrl)
        builder.setPathSegments("rest", "generate_board")
        return builder.build()
    }

    fun gameUrl(gameCode: String, forPlayer: BingoPlayer): URI {
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
