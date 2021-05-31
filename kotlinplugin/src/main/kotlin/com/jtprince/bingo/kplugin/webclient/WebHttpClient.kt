package com.jtprince.bingo.kplugin.webclient

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jtprince.bingo.kplugin.BingoConfig
import com.jtprince.bingo.kplugin.BingoPlugin
import com.jtprince.bingo.kplugin.game.web.WebGameSettings
import org.apache.http.HttpStatus
import org.apache.http.client.HttpResponseException
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.bukkit.Bukkit
import java.util.logging.Level

object WebHttpClient {
    private val httpClient = HttpClientBuilder.create().build()
    private val objectMapper = jacksonObjectMapper()

    @JsonIgnoreProperties(ignoreUnknown = true)
    private class WebGameResponseOk(
        @JsonProperty("game_code") val gameCode: String,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private class WebGameResponseError(
        @JsonProperty("game_code") val gameCodeErrors: Array<String>,
    )

    fun generateBoard(settings: WebGameSettings, whenDoneAsync: (gameCode: String?) -> Unit) {
        Bukkit.getScheduler().runTaskAsynchronously(BingoPlugin) { ->
            var gameCodeOrError: String? = null
            /* HTTP request is blocking */
            try {
                val request = HttpPost(BingoConfig.boardCreateUrl())
                val settingsJson = objectMapper.writeValueAsString(settings)
                request.entity = StringEntity(settingsJson, ContentType.APPLICATION_JSON)
                httpClient.execute(request).use { response ->
                    val code = response.statusLine.statusCode
                    if (code >= HttpStatus.SC_OK && code < 300) {
                        val resp = objectMapper.readValue(
                            response.entity.content,
                            WebGameResponseOk::class.java
                        )
                        gameCodeOrError = resp.gameCode
                    } else if (code == HttpStatus.SC_BAD_REQUEST) {
                        val err = objectMapper.readValue(
                            response.entity.content,
                            WebGameResponseError::class.java
                        )
                        if (err.gameCodeErrors.any { it.contains("already exists") }) {
                            gameCodeOrError = settings.gameCode
                            BingoPlugin.logger.info(
                                "Board $gameCodeOrError already exists on the server. Using it."
                            )
                        } else {
                            throw HttpResponseException(code, "Unknown Bad Request response")
                        }
                    } else {
                        throw HttpResponseException(code, "Bad response from web backend")
                    }
                }
            } catch (e: Exception) {
                BingoPlugin.logger.log(Level.SEVERE, "Failed to generate board", e)
            }

            whenDoneAsync(gameCodeOrError)
        }
    }

    fun pingBackend() {
        Bukkit.getScheduler().runTaskAsynchronously(BingoPlugin) { ->
            val url = BingoConfig.webPingUrl()
            try {
                val response = httpClient.execute(HttpGet(url))
                response.use {
                    if (it.statusLine.statusCode != 200) {
                        throw HttpResponseException(it.statusLine.statusCode, "Unexpected status code")
                    }
                }
                BingoPlugin.logger.info("Successfully made a request to $url.")
            } catch (e: Exception) {
                BingoPlugin.logger.log(Level.SEVERE, "Failed to communicate with $url.", e)
            }
        }
    }
}
