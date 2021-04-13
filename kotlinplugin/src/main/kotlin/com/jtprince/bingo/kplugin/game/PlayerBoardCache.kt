package com.jtprince.bingo.kplugin.game

import com.jtprince.bingo.kplugin.board.Space
import com.jtprince.bingo.kplugin.player.BingoPlayer
import com.jtprince.bingo.kplugin.webclient.model.WebModelPlayerBoard

/**
 * Class to maintain the Plugin-side latest known markings on a single Player's board, and decide
 * whether we can send a marking to the web.
 *
 * The data maintained here is not authoritative; the web backend maintains the authoritative
 * version.
 */
class PlayerBoardCache(val owner: BingoPlayer) {
    private val knownMarkings = HashMap<Int, Space.Marking>()

    /**
     * Parse a change in this Player Board sent from the web.
     */
    fun updateFromWeb(webPlayerBoard: WebModelPlayerBoard) {
        for (marking in webPlayerBoard.markings) {
            knownMarkings[marking.spaceId] = Space.Marking.valueOf(marking.color)
        }
    }

    /**
     * Determine whether we can send an updated marking to the board based on new goal satisfaction
     * status, and if so, what marking we should send.
     * @return The new marking that should be sent, or null if no marking should be sent (either
     *         because we aren't tracking this space or because it is already marked that way)
     */
    fun canSendMarking(spaceId: Int, goalType: Space.GoalType, satisfied: Boolean): Space.Marking? {
        val currentMarking = knownMarkings[spaceId] ?: return null

        val newMarking = when(goalType) {
            Space.GoalType.DEFAULT -> {
                when {
                    satisfied -> {
                        Space.Marking.COMPLETE
                    }
                    currentMarking == Space.Marking.COMPLETE -> {
                        Space.Marking.REVERTED
                    }
                    else -> {
                        null
                    }
                }
            }
            Space.GoalType.NEGATIVE -> {
                if (satisfied) Space.Marking.INVALIDATED else Space.Marking.NOT_INVALIDATED
            }
        }

        return if (currentMarking != newMarking) {
            newMarking
        } else {
            null
        }
    }
}
