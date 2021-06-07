package com.jtprince.bingo.core.webclient

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable

abstract class WebsocketTxMessage(
    @JsonProperty("action") val action: Action
) {
    enum class Action(@JsonValue val str: String) {
        GAME_STATE("game_state"),
        REVEAL_BOARD("reveal_board"),
        MARK_SPACE("board_mark_admin"),
        SET_AUTO_MARKS("set_automarks"),
        MESSAGE_RELAY("message_relay"),
        PLUGIN_PARITY("plugin_parity"),
    }
}

class TxMessageGameState(
    isStart: Boolean
) : WebsocketTxMessage(Action.GAME_STATE) {
    @JsonProperty("to_state") val toState = if (isStart) "start" else "end"
}

class TxMessageRevealBoard : WebsocketTxMessage(Action.REVEAL_BOARD)

class TxMessageMarkSpace(
    @JsonProperty("player") val player: String,
    @JsonProperty("space_id") val spaceId: Int,
    @JsonProperty("to_state") val marking: Int,
) : WebsocketTxMessage(Action.MARK_SPACE)

class TxMessageSetAutoMarks(
    @JsonProperty("space_ids") val playerSpaceIdsMap: Map<String, Collection<Int>>,
) : WebsocketTxMessage(Action.SET_AUTO_MARKS)

class TxMessageMessageRelay(  // Oh boy, this is a good one to rename.
    @JsonProperty("json") val json: String,
) : WebsocketTxMessage(Action.MESSAGE_RELAY)

class TxMessagePluginParity(
    @JsonProperty("is_echo") val isEcho: Boolean,
    @JsonProperty("my_settings") val mySettings: Map<String, Serializable>,
) : WebsocketTxMessage(Action.PLUGIN_PARITY)
