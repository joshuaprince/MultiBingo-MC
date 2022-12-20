package com.jtprince.multibingo.core.webclient

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jtprince.multibingo.core.webclient.model.WebGameSettings
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection

class WebHttpClient internal constructor(
    urlFormatter: BingoUrlFormatter,
) {
    private val logger = LoggerFactory.getLogger("MultiBingo")
    private val boardCreateUrl = urlFormatter.boardCreateUrl()
    private val pingUrl = urlFormatter.webPingUrl()

    private val client = OkHttpClient()
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
        val request = Request.Builder()
            .url(boardCreateUrl)
            .post(objectMapper.writeValueAsString(settings).toRequestBody(MEDIA_TYPE_JSON))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logger.error("Failed to communicate with $boardCreateUrl: ${e.localizedMessage}")
            }

            override fun onResponse(call: Call, response: Response) {
                var gameCode: String? = null

                if (response.isSuccessful) {
                    val resp = objectMapper.readValue(response.body.byteStream(), WebGameResponseOk::class.java)
                    response.body.close()
                    gameCode = resp.gameCode
                } else if (response.code == HttpURLConnection.HTTP_BAD_REQUEST) {
                    val errResp = objectMapper.readValue(response.body.byteStream(), WebGameResponseError::class.java)
                    response.body.close()
                    if (errResp.gameCodeErrors.any { it.contains("already exists") }) {
                        gameCode = settings.gameCode
                        logger.info("Board $gameCode already exists on the server. Using it.")
                    } else {
                        logger.error("Unknown Bad Request response: $response")
                    }
                } else {
                    logger.error("Bad response from web backend: $response")
                }

                whenDoneAsync(gameCode)
            }
        })
    }

    fun pingBackend() {
        val request = Request.Builder().url(pingUrl).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logger.error("Failed to communicate with $pingUrl: ${e.localizedMessage}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    logger.info("Successfully made a request to $pingUrl.")
                } else {
                    throw IOException("Unexpected response from $pingUrl : $response")
                }
            }
        })
    }

    companion object {
        val MEDIA_TYPE_JSON = "application/json; charset=urf-8".toMediaType()
    }
}
