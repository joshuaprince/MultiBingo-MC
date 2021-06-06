package com.jtprince.bingo.bukkit.webclient.model

import com.fasterxml.jackson.annotation.JsonProperty

class WebModelMessageRelay(
    @JsonProperty("sender") val sender: String,
    @JsonProperty("json") val json: String,
)
