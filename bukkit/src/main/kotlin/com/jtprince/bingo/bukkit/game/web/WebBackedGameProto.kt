package com.jtprince.bingo.bukkit.game.web

import com.jtprince.bingo.bukkit.BukkitMessages
import com.jtprince.bingo.bukkit.BukkitMessages.bingoTellNotReady
import com.jtprince.bingo.bukkit.game.BingoGame
import com.jtprince.bingo.core.automark.AutomatedSpace
import com.jtprince.bingo.core.player.LocalBingoPlayer
import com.jtprince.bingo.core.webclient.model.WebGameSettings
import org.bukkit.command.CommandSender
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.logging.Logger

/**
 * A Bingo Game that does not yet have a WebSocket connection, because it is still being created.
 */
class WebBackedGameProto(
    creator: CommandSender,
    val settings: WebGameSettings,
) : BingoGame(creator, "CreatingGame"), KoinComponent {
    private val logger: Logger by inject()

    override var state: State = State.BOARD_GENERATING

    override fun signalStart(sender: CommandSender?) {
        sender?.bingoTellNotReady()
    }

    override fun signalEnd(sender: CommandSender?) {
        state = State.DONE
        BukkitMessages.bingoAnnounceEnd(null)
    }

    override fun signalDestroy(sender: CommandSender?) {
        // Nothing to do.
    }

    override fun receiveAutoMark(player: LocalBingoPlayer, space: AutomatedSpace, fulfilled: Boolean) {
        logger.severe(
            "Received an automarking for ${player.name} during a Proto Game... " +
                    "that shouldn't be possible, nag the developer."
        )
    }
}
