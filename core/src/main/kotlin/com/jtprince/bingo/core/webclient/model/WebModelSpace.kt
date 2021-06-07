package com.jtprince.bingo.core.webclient.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.jtprince.bingo.core.SetVariables

class WebModelSpace(
    @JsonProperty("goal_id") val goalId: String,
    @JsonProperty("type") val goalType: String,
    @JsonProperty("text") val text: String,
    @JsonProperty("space_id") val spaceId: Int,
    @JsonProperty("variables") val variables: SetVariables,
)
