package com.jtprince.bingo.bukkit.game.web

import com.jtprince.bingo.core.automark.AutoMarkConsumer
import com.jtprince.bingo.core.game.BingoGame
import com.jtprince.bingo.core.webclient.model.WebGameSettings
import net.kyori.adventure.audience.Audience
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.logging.Logger

/**
 * A Bingo Game that does not yet have a WebSocket connection, because it is still being created.
 */
class WebBackedGameProto(
    override val creator: Audience,
    val settings: WebGameSettings,
) : BingoGame, KoinComponent {
    override val name: String = "Game Generating..."
    private val logger: Logger by inject()

    override fun destroy(sender: Audience?) {
        // Nothing to do.
    }

    override fun receiveAutoMark(activation: AutoMarkConsumer.Activation) {
        logger.severe(
            "Received an automarking for ${activation.player.name} during a Proto Game... " +
                    "that shouldn't be possible, nag the developer."
        )
    }
}
