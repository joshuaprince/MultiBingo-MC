package com.jtprince.bingo.kplugin.game

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.jtprince.bingo.kplugin.BingoPlugin
import com.jtprince.bingo.kplugin.Messages
import com.jtprince.bingo.kplugin.Messages.bingoTellNotReady
import com.jtprince.bingo.kplugin.automark.AutomatedSpace
import com.jtprince.bingo.kplugin.player.BingoPlayer
import org.bukkit.command.CommandSender

/**
 * A Bingo Game that does not yet have a WebSocket connection, because it is still being created.
 */
class WebBackedGameProto(
    creator: CommandSender,
    val settings: WebGameSettings,
) : BingoGame(creator, "CreatingGame", emptySet()) {

    override var state: State = State.BOARD_GENERATING

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class WebGameSettings(
        @JsonProperty("game_code") val gameCode: String? = null,
        val shape: String? = null,
        val seed: String? = null,
        @JsonProperty("forced_goals") val forcedGoals: Collection<String> = emptySet()
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class WebGameResponse(
        @JsonProperty("game_code") val gameCode: String,
    )

    override fun signalStart(sender: CommandSender?) {
        sender?.bingoTellNotReady()
    }

    override fun signalEnd(sender: CommandSender?) {
        state = State.DONE
        Messages.bingoAnnounceEnd(null)
    }

    override fun signalDestroy(sender: CommandSender?) {
        // Nothing to do.
    }

    override fun receiveAutomark(bingoPlayer: BingoPlayer, space: AutomatedSpace, satisfied: Boolean) {
        BingoPlugin.logger.severe(
            "Received an automarking for ${bingoPlayer.name} during a Proto Game... " +
                    "that shouldn't be possible, nag the developer."
        )
    }
}
