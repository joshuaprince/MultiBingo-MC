package com.jtprince.bingo.kplugin.webclient

import com.jtprince.bingo.kplugin.BingoConfig
import com.jtprince.bingo.kplugin.BingoPlugin
import com.jtprince.bingo.kplugin.game.web.WebBackedGameProto
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import java.util.logging.Level

object WebHttpClient {
    private val httpClient = HttpClient {
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
    }

    fun generateBoard(settings: WebBackedGameProto.WebGameSettings,
                      whenDone: (gameCode: String?) -> Unit) {
        Bukkit.getScheduler().runTaskAsynchronously(BingoPlugin) { -> runBlocking {
            try {
                val response = httpClient.post<WebBackedGameProto.WebGameResponse>(BingoConfig.boardCreateUrl()) {
                    contentType(ContentType.Application.Json)
                    body = settings
                }

                whenDone(response.gameCode)
            } catch (e: Exception) {
                if (e is ClientRequestException
                    && e.response.readText().contains("already exists")
                ) {
                    val gameCode = settings.gameCode
                    BingoPlugin.logger.info("Board $gameCode already exists on the server. Using it.")
                    whenDone(gameCode)
                    return@runBlocking
                }

                BingoPlugin.logger.log(Level.SEVERE, "Failed to generate board", e)
                whenDone(null)
            }
        }}
    }

    fun pingBackend() {
        Bukkit.getScheduler().runTaskAsynchronously(BingoPlugin) { -> runBlocking {
            val url = BingoConfig.webPingUrl()
            try {
                httpClient.get<Unit>(url)
                BingoPlugin.logger.info("Successfully made a request to $url.")
            } catch (e: Exception) {
                BingoPlugin.logger.log(Level.SEVERE, "Failed to communicate with $url.", e)
            }
        }}
    }
}
