package com.jtprince.bingo.bukkit.game.web

import com.jtprince.bingo.bukkit.BingoPlugin
import com.jtprince.bingo.bukkit.Messages
import com.jtprince.bingo.bukkit.Messages.bingoTellNotReady
import com.jtprince.bingo.bukkit.automark.AutomatedSpace
import com.jtprince.bingo.bukkit.game.BingoGame
import com.jtprince.bingo.bukkit.player.BukkitBingoPlayer
import org.bukkit.command.CommandSender

/**
 * A Bingo Game that does not yet have a WebSocket connection, because it is still being created.
 */
class WebBackedGameProto(
    creator: CommandSender,
    val settings: WebGameSettings,
) : BingoGame(creator, "CreatingGame") {

    override var state: State = State.BOARD_GENERATING

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

    override fun receiveAutoMark(player: BukkitBingoPlayer, space: AutomatedSpace, fulfilled: Boolean) {
        BingoPlugin.logger.severe(
            "Received an automarking for ${player.name} during a Proto Game... " +
                    "that shouldn't be possible, nag the developer."
        )
    }
}
