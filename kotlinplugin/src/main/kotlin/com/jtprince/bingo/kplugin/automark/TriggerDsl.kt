/**
 * This file contains all specification for the Kotlin Domain-Specific Language (DSL) used to
 * specify automated Bingo marking triggers. These triggers are defined separately in
 * TriggerDefinitions.kt.
 */

package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.player.BingoPlayer
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack
import kotlin.reflect.KClass

/**
 * Container that provides mapping from Goal IDs to any automated trigger definitions for that goal.
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

    val allAutomatedGoals
        get() = regs.keys
}

/**
 * Used to build a [TriggerDslRegistry] in a domain-specific language (DSL) style.
 */
internal class TriggerDslRegistryBuilder {
    private val triggers = mutableMapOf<String, MutableList<TriggerDslDefinition>>()

    internal inline fun <reified EventType : Event> eventTrigger(
        vararg goalIds: String,
        noinline check: EventTriggerDefinition.Parameters<out EventType>.() -> Boolean
    ) {
        for (goalId in goalIds) {
            val lst = triggers.getOrPut(goalId) { mutableListOf() }
            lst += EventTriggerDefinition(EventType::class, check)
        }
    }

    internal fun occasionalTrigger(
        vararg goalIds: String,
        ticks: Int,
        check: OccasionalTriggerDefinition.Parameters.() -> Boolean
    ) {
        for (goalId in goalIds) {
            val lst = triggers.getOrPut(goalId) { mutableListOf() }
            lst += OccasionalTriggerDefinition(ticks, check)
        }
    }

    internal fun specialItemTrigger(
        vararg goalIds: String,
        revertible: Boolean = true,
        check: SpecialItemTriggerDefinition.Parameters.() -> Boolean
    ) {
        for (goalId in goalIds) {
            val lst = triggers.getOrPut(goalId) { mutableListOf() }
            lst += SpecialItemTriggerDefinition(revertible, check)
        }
    }

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
internal abstract class TriggerDslDefinition

internal class EventTriggerDefinition<EventType: Event>(
    val eventType: KClass<EventType>,
    val function: (Parameters<EventType>) -> Boolean
) : TriggerDslDefinition() {
    class Parameters<EventType: Event>(
        val event: EventType,
        val trigger: EventTrigger<EventType>,
    ) {
        val vars = trigger.variables
    }
}

internal class OccasionalTriggerDefinition(
    val ticks: Int,
    val function: (Parameters) -> Boolean
) : TriggerDslDefinition() {
    class Parameters(
        val player: BingoPlayer,
        trigger: OccasionalTrigger,
    ) {
        val vars = trigger.variables
    }
}

internal class SpecialItemTriggerDefinition(
    val revertible: Boolean,
    val function: (Parameters) -> Boolean
) : TriggerDslDefinition() {
    class Parameters(
        val player: BingoPlayer,
        val inventory: Collection<ItemStack>,
        trigger: SpecialItemTrigger,
    ) {
        val vars = trigger.variables
    }
}
