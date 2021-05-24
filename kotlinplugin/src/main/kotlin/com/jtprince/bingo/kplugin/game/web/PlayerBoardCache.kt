package com.jtprince.bingo.kplugin.game.web

import com.jtprince.bingo.kplugin.player.BingoPlayer
import com.jtprince.bingo.kplugin.webclient.model.WebModelPlayerBoard

/**
 * Class to maintain the Plugin-side latest known markings on a single Player's board, and decide
 * whether we can send a marking to the web.
 *
 * The data maintained here is not authoritative; the web backend maintains the authoritative
 * version.
 */
internal class PlayerBoardCache(val owner: BingoPlayer) {
    private val knownMarkings = mutableMapOf<Int, WebBackedSpace.Marking>()
    private var ignoredSpaceIds = setOf<Int>()

    /**
     * Parse a change in this Player Board sent from the web.
     */
    fun updateFromWeb(webPlayerBoard: WebModelPlayerBoard) {
        for (marking in webPlayerBoard.markings) {
            knownMarkings[marking.spaceId] = WebBackedSpace.Marking.valueOf(marking.color)
        }

        ignoredSpaceIds = webPlayerBoard.markings
            .filter { m -> m.markedByPlayer }
            .map { m -> m.spaceId }.toSet()
    }

    /**
     * Determine whether we can send an updated marking to the board based on new goal satisfaction
     * status, and if so, what marking we should send.
     * @return The new marking that should be sent, or null if no marking should be sent (either
     *         because we aren't tracking this space or because it is already marked that way)
     */
    fun canSendMarking(spaceId: Int, goalType: WebBackedSpace.GoalType, satisfied: Boolean): WebBackedSpace.Marking? {
        val currentMarking = knownMarkings[spaceId] ?: return null

        if (spaceId in ignoredSpaceIds) return null

        val newMarking = when(goalType) {
            WebBackedSpace.GoalType.DEFAULT -> {
                when {
                    satisfied -> {
                        WebBackedSpace.Marking.COMPLETE
                    }
                    currentMarking == WebBackedSpace.Marking.COMPLETE -> {
                        WebBackedSpace.Marking.REVERTED
                    }
                    else -> {
                        null
                    }
                }
            }
            WebBackedSpace.GoalType.NEGATIVE -> {
                if (satisfied) WebBackedSpace.Marking.INVALIDATED else WebBackedSpace.Marking.NOT_INVALIDATED
            }
        }

        return if (currentMarking != newMarking) {
            newMarking
        } else {
            null
        }
    }
}
