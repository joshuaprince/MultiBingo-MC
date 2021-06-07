package com.jtprince.bingo.core.webclient.model

import com.fasterxml.jackson.annotation.JsonProperty

class WebModelMessageRelay(
    @JsonProperty("sender") val sender: String,
    @JsonProperty("json") val json: String,
)
