package com.jtprince.bingo.core.webclient

import com.fasterxml.jackson.annotation.JsonProperty
import com.jtprince.bingo.core.webclient.model.*

class WebsocketRxMessage(
    @JsonProperty("board") val board: WebModelBoard?,
    @JsonProperty("pboards") val pboards: List<WebModelPlayerBoard>?,
    @JsonProperty("game_state") val gameState: WebModelGameState?,
    @JsonProperty("message_relay") val messageRelay: WebModelMessageRelay?,
    @JsonProperty("plugin_parity") val pluginParity: WebModelPluginParity?,
)
