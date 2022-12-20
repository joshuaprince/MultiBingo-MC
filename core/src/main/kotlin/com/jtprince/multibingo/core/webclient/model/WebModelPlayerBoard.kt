package com.jtprince.multibingo.core.webclient.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class WebModelPlayerBoard (
    @JsonProperty("player_name") val playerName: String,
    @JsonProperty("markings") val markings: List<WebPlayerBoardMarking>,
    @JsonProperty("win") win: List<Int>?,
) {
    val win: Boolean = win?.isNotEmpty() ?: false

    @JsonIgnoreProperties(ignoreUnknown = true)
    class WebPlayerBoardMarking (
        @JsonProperty("space_id") val spaceId: Int,
        @JsonProperty("color") val color: Int,
        @JsonProperty("marked_by_player") val markedByPlayer: Boolean,
    )
}
