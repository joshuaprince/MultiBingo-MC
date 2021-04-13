package com.jtprince.bingo.kplugin.board

import com.jtprince.bingo.kplugin.automark.AutoMarkCallback
import com.jtprince.bingo.kplugin.automark.AutoMarkTrigger
import com.jtprince.bingo.kplugin.automark.AutoMarkTriggerFactory
import com.jtprince.bingo.kplugin.game.PlayerManager

class Space(
    val spaceId: Int,
    val goalId: String,
    val goalType: GoalType,
    val text: String,
    val variables: SetVariables,
) {
    enum class Marking(val value: Int) {
        UNMARKED(0),
        COMPLETE(1),
        REVERTED(2),
        INVALIDATED(3),
        NOT_INVALIDATED(4);

        companion object {
            private val map = values().associateBy(Marking::value)
            fun valueOf(value: Int) = map[value] ?:
                throw IllegalArgumentException("Unknown Marking value $value")
        }
    }

    enum class GoalType(val value: String) {
        DEFAULT("default"),
        NEGATIVE("negative");

        companion object {
            private val map = values().associateBy(GoalType::value)
            fun ofString(value: String) = map[value] ?:
                throw IllegalArgumentException("Unknown Marking value $value")
        }
    }

    private lateinit var triggers: Collection<AutoMarkTrigger>

    val automarking: Boolean
        get() = triggers.isNotEmpty()

    /**
     * Receive callbacks when a player in this [PlayerManager] does something that should mark the
     * space.
     */
    fun startListening(playerManager: PlayerManager, callback: AutoMarkCallback) {
        triggers = AutoMarkTriggerFactory().create(
            goalId, spaceId, variables, playerManager, callback)
    }

    /**
     * Stop receiving callbacks for all players on this space.
     */
    fun stopListening() {
        triggers.forEach(AutoMarkTrigger::destroy)
    }

    fun destroy() {
        stopListening()
    }
}
