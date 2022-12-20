package com.jtprince.multibingo.core.webclient.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "state",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = WebModelGameState.Start::class, name = "start"),
    JsonSubTypes.Type(value = WebModelGameState.Marking::class, name = "marking"),
    JsonSubTypes.Type(value = WebModelGameState.End::class, name = "end"),
)
sealed class WebModelGameState(
    @JsonProperty("state") val state: String
) {
    class Start: WebModelGameState("start")

    class Marking(
        @JsonProperty("marking_type") val markingType: String,
        @JsonProperty("player") val player: String,
        @JsonProperty("goal") val goalText: String,
    ): WebModelGameState("marking")

    class End(
        @JsonProperty("winner") val winner: String?,
    ): WebModelGameState("end")
}
