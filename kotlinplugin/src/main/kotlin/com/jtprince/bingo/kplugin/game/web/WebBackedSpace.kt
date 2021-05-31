package com.jtprince.bingo.kplugin.game.web

import com.jtprince.bingo.kplugin.automark.AutoMarkConsumer
import com.jtprince.bingo.kplugin.automark.AutomatedSpace
import com.jtprince.bingo.kplugin.automark.EventPlayerMapper
import com.jtprince.bingo.kplugin.automark.PlayerTriggerProgress
import com.jtprince.bingo.kplugin.automark.trigger.AutoMarkTrigger
import com.jtprince.bingo.kplugin.automark.trigger.AutoMarkTriggerFactory
import com.jtprince.bingo.kplugin.player.LocalBingoPlayer
import com.jtprince.bingo.kplugin.webclient.model.WebModelSpace

class WebBackedSpace(
    modelSpace: WebModelSpace,
    playerMapper: EventPlayerMapper,
    autoMarkConsumer: AutoMarkConsumer,
) : AutomatedSpace {
    override val goalId = modelSpace.goalId
    override val text = modelSpace.text
    override val spaceId = modelSpace.spaceId
    override val variables = modelSpace.variables
    val goalType = GoalType.ofString(modelSpace.goalType)

    private var triggers: Collection<AutoMarkTrigger> =
        AutoMarkTriggerFactory().create(this, playerMapper, autoMarkConsumer)
    val hasAutoMarkTrigger: Boolean
        get() = triggers.isNotEmpty()

    enum class Marking(val value: Int) {
        UNMARKED(0),
        COMPLETE(1),
        REVERTED(2),
        INVALIDATED(3),
        NOT_INVALIDATED(4);

        companion object {
            private val map = values().associateBy(Marking::value)
            fun valueOf(value: Int) = map[value] ?: throw IllegalArgumentException("Unknown Marking value $value")
        }
    }

    enum class GoalType(val value: String) {
        DEFAULT("default"),
        NEGATIVE("negative");

        companion object {
            private val map = values().associateBy(GoalType::value)
            fun ofString(value: String) = map[value] ?: throw IllegalArgumentException("Unknown Marking value $value")
        }
    }

    override fun destroy() {
        triggers.forEach(AutoMarkTrigger::destroy)
    }

    override val playerProgress: MutableMap<LocalBingoPlayer, PlayerTriggerProgress> by lazy { mutableMapOf() }
}
