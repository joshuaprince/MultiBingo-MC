package com.jtprince.bingo.bukkit.game.debug

import com.jtprince.bingo.bukkit.BukkitMessages
import com.jtprince.bingo.bukkit.BukkitMessages.bingoTell
import com.jtprince.bingo.bukkit.BukkitMessages.bingoTellError
import com.jtprince.bingo.bukkit.automark.EventPlayerMapper
import com.jtprince.bingo.bukkit.player.BukkitBingoPlayer
import com.jtprince.bingo.core.SetVariables
import com.jtprince.bingo.core.automark.AutoMarkConsumer
import com.jtprince.bingo.core.game.BingoGame
import com.jtprince.bukkit.worldset.WorldSet
import net.kyori.adventure.audience.Audience
import org.bukkit.event.Event

/**
 * A "game" that can be used for debugging a single automated trigger.
 * Does not communicate with the web backend.
 */
class DebugGame(
    creator: Audience,
    val players: Collection<BukkitBingoPlayer>,
    val goalId: String,
    variables: SetVariables,
) : BingoGame(creator, "DebugGame"), EventPlayerMapper {
    override var state: State = State.RUNNING
    override val localPlayers = players

    val space = DebugSpace(this, goalId, variables)

    init {
        BukkitMessages.bingoAnnounce("Now debugging goal $goalId.")
    }

    override fun signalStart(sender: Audience?) {
        sender?.bingoTell("Debug game is active; no need to start it.")
    }

    override fun signalEnd(sender: Audience?) {
        signalDestroy(sender)
    }

    override fun signalDestroy(sender: Audience?) {
        space.destroy()
        sender?.bingoTell("Debug Game destroyed.")
    }

    override fun receiveAutoMark(activation: AutoMarkConsumer.Activation) {
        if (activation.fulfilled) {
            activation.player.bingoTell("You have activated ${space.text}.")
        } else {
            activation.player.bingoTellError("You have reverted ${space.text}.")
        }
    }

    override fun mapEvent(event: Event): BukkitBingoPlayer? =
        players.find { p -> creator in p.bukkitPlayers }

    override fun worldSet(player: BukkitBingoPlayer): WorldSet = WorldSet.defaultWorldSet
}
