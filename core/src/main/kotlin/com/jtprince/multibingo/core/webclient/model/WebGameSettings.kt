package com.jtprince.multibingo.core.webclient.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
class WebGameSettings(
    @JsonProperty("game_code") val gameCode: String? = null,
    val shape: String? = null,
    val seed: String? = null,
    @JsonProperty("forced_goals") val forcedGoals: Collection<String> = emptySet()
)
