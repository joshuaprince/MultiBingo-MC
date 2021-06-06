package com.jtprince.bingo.bukkit.webclient.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

class WebModelPluginParity(
    @JsonProperty("sender") val sender: String,
    @JsonProperty("is_echo") val isEcho: Boolean,
    @JsonProperty("settings") val settings: Map<String, Serializable>,
)
