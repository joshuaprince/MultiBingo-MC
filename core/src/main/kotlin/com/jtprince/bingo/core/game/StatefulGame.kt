package com.jtprince.bingo.core.game

import net.kyori.adventure.audience.Audience

/**
 * A Stateful Bingo Game is one that can be started or ended independent of game creation and
 * destruction. In other words, a Stateful game object may exist before "the game" has started and
 * after it is over.
 */
interface StatefulGame : BingoGame {
    /**
     * Causes this game to enter its "running" state if its current state allows this.
     * @param sender The Audience that sent this signal, or null if the signal was received from a
     *               source that cannot be responded to directly (such as a WebSocket).
     */
    fun signalStart(sender: Audience?)

    /**
     * Causes this game to enter its "finished" (but not destroyed) state if its current state
     * allows this.
     * @param sender The Audience that sent this signal, or null if the signal was received from a
     *               source that cannot be responded to directly (such as a WebSocket).
     */
    fun signalEnd(sender: Audience?)

    /**
     * Returns whether this game is in its "finished" state, and therefore may be spectated with
     * no gameplay consequences.
     */
    fun canSpectate(): Boolean
}
