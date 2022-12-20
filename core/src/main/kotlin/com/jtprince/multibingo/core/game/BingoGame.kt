package com.jtprince.multibingo.core.game

import com.jtprince.multibingo.core.automark.AutoMarkConsumer
import com.jtprince.multibingo.core.automark.AutomatedSpace
import net.kyori.adventure.audience.Audience

sealed interface BingoGame {
    /**
     * Handler for all automated markings while this game is active.
     */
    val autoMarkConsumer: AutoMarkConsumer

    /**
     * Audience that will receive all administrative messaging for this game, such as errors.
     */
    var administrator: Audience

    enum class State(
        val canAutoMark: Boolean
    ) {
        CREATED(false),
        BOARD_GENERATING(false),
        WAITING_FOR_WEBSOCKET(false),
        WORLDS_GENERATING(false),
        READY(false),
        COUNTING_DOWN(false),
        RUNNING(true),
        DONE(false),
        FAILED(false),
        DESTROYING(false),
    }
    val state: State

    fun close()

    fun onPlayerEnterWorld()

    val automatedSpaces: Collection<AutomatedSpace>
}
