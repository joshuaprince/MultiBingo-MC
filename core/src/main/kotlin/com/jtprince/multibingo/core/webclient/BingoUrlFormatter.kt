package com.jtprince.multibingo.core.webclient

import com.jtprince.multibingo.core.player.BingoPlayer
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.URI

class BingoUrlFormatter(private val webUrl: String) {  // TODO: Make internal
    fun boardCreateUrl(): HttpUrl = webUrl.toHttpUrl().newBuilder()
        .addPathSegment("rest")
        .addPathSegment("generate_board")
        .build()

    fun gameUrl(gameCode: String, forPlayer: BingoPlayer): HttpUrl = webUrl.toHttpUrl().newBuilder()
        .addPathSegment("game")
        .addPathSegment(gameCode)
        .addQueryParameter("name", forPlayer.name)
        .build()

    fun webPingUrl(): HttpUrl = webUrl.toHttpUrl().newBuilder()
        .addPathSegment("ping")
        .build()

    fun websocketUrl(gameCode: String, clientId: String): URI {
        TODO("Not implemented")
//        val builder = URIBuilder(webUrl)
//        builder.setPathSegments("ws", "board-plugin", gameCode, clientId)
//        builder.scheme = when(builder.scheme) {
//            "http" -> "ws"
//            "https" -> "wss"
//            else -> throw IllegalArgumentException("Invalid web URL protocol ${builder.scheme}")
//        }
//
//        return builder.build()
    }
}
