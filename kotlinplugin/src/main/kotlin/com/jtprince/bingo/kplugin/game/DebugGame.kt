package com.jtprince.bingo.kplugin.game

import com.jtprince.bingo.kplugin.Messages
import com.jtprince.bingo.kplugin.Messages.bingoTell
import com.jtprince.bingo.kplugin.automark.AutomatedSpace
import com.jtprince.bingo.kplugin.board.SetVariables
import com.jtprince.bingo.kplugin.board.Space
import com.jtprince.bingo.kplugin.player.BingoPlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * A "game" that can be used for debugging auto triggers. Does not communicate with the web backend.
 */
class DebugGame(
    creator: Player,
    players: Collection<BingoPlayer>,
    val goalId: String,
    variables: SetVariables,
) : BingoGame(creator, "DebugGame", players) {
    override var state: State = State.RUNNING

    init {
        /* Special case to make sure World events in the spawn world get properly directed to
         * this bingo player. */
        playerManager.worldPlayerMap[creator.world] = playerManager.bingoPlayer(creator)!!

        val spc = Space(0, goalId, Space.GoalType.DEFAULT, "Debug Goal Text", variables)
        spc.startListening(playerManager, ::receiveAutomark)
        spaces[0] = spc
        Messages.bingoAnnounce("Now debugging goal $goalId.")
    }

    override fun signalStart(sender: CommandSender?) {
        sender?.bingoTell("Debug game is active; no need to start it.")
    }

    override fun signalEnd(sender: CommandSender?) {
        sender?.bingoTell("Debug game does not need to be ended; try destroy instead.")
    }

    override fun signalDestroy(sender: CommandSender?) {
        // Nothing needs to be done. Spaces are destroyed in the superclass.
    }

    override fun receiveAutomark(bingoPlayer: BingoPlayer, space: AutomatedSpace, satisfied: Boolean) {
        if (satisfied) {
            bingoPlayer.bingoTell("You have activated $goalId.")
        }
    }
}
