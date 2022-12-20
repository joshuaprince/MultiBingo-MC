package com.jtprince.multibingo.core.game

import com.jtprince.multibingo.core.automark.AutoMarkConsumer
import com.jtprince.multibingo.core.automark.AutomatedSpace
import com.jtprince.multibingo.core.automark.PlayerTriggerProgress
import com.jtprince.multibingo.core.player.LocalBingoPlayer
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

class DebugGame internal constructor(): BingoGame {

    private var _administrator: Audience? = null
    override var administrator: Audience
        get() = _administrator ?: Audience.empty()
        set(value) { _administrator = value }

    override var state = BingoGame.State.CREATED
        private set

    class Space(override val goalId: String, override val variables: Map<String, Int>) : AutomatedSpace {
        override val text: String = goalId
        override val spaceId: Int = 0
        override val playerProgress: MutableMap<LocalBingoPlayer, PlayerTriggerProgress>
            get() = TODO("Not yet implemented")

        override fun destroy() {
            TODO("Not yet implemented")
        }
    }
    private var space: Space? = null
    override val automatedSpaces: Collection<AutomatedSpace>
        get() = listOfNotNull(space)

    override fun close() {}

    override val autoMarkConsumer = AutoMarkConsumer {
        administrator.sendMessage(Component.text("You have activated ${it.space.goalId}."))
    }

    fun setGoal(goalId: String, variables: Map<String, Int> = emptyMap()) {
        space = Space(goalId, variables)
        state = BingoGame.State.RUNNING
    }

    override fun onPlayerEnterWorld() {
        state = BingoGame.State.READY
        administrator.sendMessage(Component.text("Debug game is ready. Run /bingo debug [goal] to debug a goal."))
    }
}
