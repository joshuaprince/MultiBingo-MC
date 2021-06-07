/**
 * This file contains all specification for the Kotlin Domain-Specific Language (DSL) used to
 * specify automated Bingo marking triggers. These triggers are defined separately in
 * TriggerDefinitions.kt.
 */

package com.jtprince.bingo.bukkit.automark.definitions

import com.jtprince.bingo.bukkit.automark.BingoInventory
import com.jtprince.bingo.bukkit.automark.trigger.BukkitEventTrigger
import com.jtprince.bingo.bukkit.automark.trigger.BukkitOccasionalTrigger
import com.jtprince.bingo.bukkit.automark.trigger.BukkitSpecialItemTrigger
import com.jtprince.bingo.bukkit.player.BukkitBingoPlayer
import com.jtprince.bingo.core.automark.PlayerTriggerProgress
import com.jtprince.bingo.core.automark.TriggerDefinition
import com.jtprince.bukkit.worldset.WorldSet
import org.bukkit.event.Event
import kotlin.reflect.KClass

/**
 * Container that provides mapping from Goal IDs to DSL-specified automated trigger definitions for
 * that goal. This currently includes Event Triggers, Occasional Triggers, and Special Item
 * Triggers.
 */
class TriggerDslRegistry private constructor(
    private val regs: Map<String, List<TriggerDslDefinition>>
) : Map<String, List<TriggerDslDefinition>> by regs {
    internal constructor(defs: TriggerDslRegistryBuilder.() -> Unit) : this (create(defs))

    companion object {
        private fun create(defs: TriggerDslRegistryBuilder.() -> Unit)
                : Map<String, List<TriggerDslDefinition>> {
            val builder = TriggerDslRegistryBuilder()
            builder.defs()
            return builder.build()
        }
    }
}

/**
 * Used to build a [TriggerDslRegistry] in a domain-specific language (DSL) style.
 */
internal class TriggerDslRegistryBuilder {
    private val triggers = mutableMapOf<String, MutableList<TriggerDslDefinition>>()

    internal fun ids(vararg goalIds: String) = arrayOf(*goalIds)
    internal fun vars(vararg variableNames: String) = arrayOf(*variableNames)

    internal inline fun <reified EventType : Event> eventTrigger(
        goalIds: Array<String>,
        vars: Array<String> = emptyArray(),
        revertAfterTicks: Int? = null,
        noinline check: EventTriggerDefinition.Parameters<out EventType>.() -> Boolean
    ) {
        for (goalId in goalIds) {
            val lst = triggers.getOrPut(goalId) { mutableListOf() }
            lst += EventTriggerDefinition(EventType::class, vars, revertAfterTicks, check)
        }
    }
    internal inline fun <reified EventType : Event> eventTrigger(  /* 1-goalId variant */
        goalId: String,
        vars: Array<String> = emptyArray(),
        revertAfterTicks: Int? = null,
        noinline check: EventTriggerDefinition.Parameters<out EventType>.() -> Boolean
    ) = eventTrigger(ids(goalId), vars, revertAfterTicks, check)

    internal fun occasionalTrigger(
        goalIds: Array<String>,
        ticks: Int,
        vars: Array<String> = emptyArray(),
        revertAfterTicks: Int? = null,
        check: OccasionalTriggerDefinition.Parameters.() -> Boolean
    ) {
        for (goalId in goalIds) {
            val lst = triggers.getOrPut(goalId) { mutableListOf() }
            lst += OccasionalTriggerDefinition(ticks, vars, revertAfterTicks, check)
        }
    }
    internal fun occasionalTrigger(  /* 1-goalId variant */
        goalId: String,
        ticks: Int,
        vars: Array<String> = emptyArray(),
        revertAfterTicks: Int? = null,
        check: OccasionalTriggerDefinition.Parameters.() -> Boolean
    ) = occasionalTrigger(ids(goalId), ticks, vars, revertAfterTicks, check)

    internal fun specialItemTrigger(
        goalIds: Array<String>,
        revertible: Boolean,
        vars: Array<String> = emptyArray(),
        check: SpecialItemTriggerDefinition.Parameters.() -> Boolean
    ) {
        for (goalId in goalIds) {
            val lst = triggers.getOrPut(goalId) { mutableListOf() }
            lst += SpecialItemTriggerDefinition(revertible, vars, check)
        }
    }
    internal fun specialItemTrigger(
        goalId: String,
        revertible: Boolean,
        vars: Array<String> = emptyArray(),
        check: SpecialItemTriggerDefinition.Parameters.() -> Boolean
    ) = specialItemTrigger(ids(goalId), revertible, vars, check)

    fun build(): Map<String, List<TriggerDslDefinition>> {
        // Have to convert each MutableList to an immutable one
        return triggers.map { (goalId, list) -> goalId to list.toList() }.toMap()
    }
}

/**
 * Container for a static mapping from one or more goal IDs to the condition that must be fulfilled
 * for those goals to be achieved.
 *
 * TriggerDslDefinitions commonly include an inner "Parameters" class that wraps any information
 * that can be passed to this definition when determining whether the goal is fulfilled.
 */
internal sealed class TriggerDslDefinition(
    override val neededVars: Array<String>
) : TriggerDefinition

internal class EventTriggerDefinition<EventType: Event>(
    val eventType: KClass<EventType>,
    neededVars: Array<String>,
    val revertAfterTicks: Int?,
    val function: (Parameters<EventType>) -> Boolean
) : TriggerDslDefinition(neededVars) {
    class Parameters<EventType: Event>(
        val event: EventType,
        val player: BukkitBingoPlayer,
        val trigger: BukkitEventTrigger<EventType>,
    ) {
        val goalId = trigger.space.goalId
        val vars = trigger.space.variables
        val playerState
            get() = trigger.space.playerProgress.getOrPut(player) { PlayerTriggerProgress(trigger.space, player, vars) }
    }
}

internal class OccasionalTriggerDefinition(
    val ticks: Int,
    neededVars: Array<String>,
    val revertAfterTicks: Int?,
    val function: (Parameters) -> Boolean
) : TriggerDslDefinition(neededVars) {
    class Parameters(
        val player: BukkitBingoPlayer,
        val worlds: WorldSet,
        val trigger: BukkitOccasionalTrigger,
    ) {
        val vars = trigger.space.variables
        val playerState
            get() = trigger.space.playerProgress.getOrPut(player) { PlayerTriggerProgress(trigger.space, player, vars) }
    }
}

internal class SpecialItemTriggerDefinition(
    val revertible: Boolean,
    neededVars: Array<String>,
    val function: (Parameters) -> Boolean
) : TriggerDslDefinition(neededVars) {
    class Parameters(
        val inventory: BingoInventory,
        trigger: BukkitSpecialItemTrigger,
    ) {
        val vars = trigger.space.variables
    }
}
