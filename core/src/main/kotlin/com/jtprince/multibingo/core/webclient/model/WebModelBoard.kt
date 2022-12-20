package com.jtprince.multibingo.core.webclient.model

import com.fasterxml.jackson.annotation.JsonProperty

class WebModelBoard(
    @JsonProperty("board_id") val boardId: Int,
    @JsonProperty("spaces") val spaces: List<WebModelSpace>
)
