package com.jtprince.multibingo.core.webclient.model

import com.fasterxml.jackson.annotation.JsonProperty

class WebModelSpace(
    @JsonProperty("goal_id") val goalId: String,
    @JsonProperty("type") val goalType: String,
    @JsonProperty("text") val text: String,
    @JsonProperty("space_id") val spaceId: Int,
    @JsonProperty("variables") val variables: Map<String, Int>,
)
