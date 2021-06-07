package com.jtprince.bingo.bukkit.automark.trigger

import com.jtprince.bingo.bukkit.BingoPlugin
import com.jtprince.bingo.bukkit.automark.EventPlayerMapper
import com.jtprince.bingo.bukkit.automark.definitions.EventTriggerDefinition
import com.jtprince.bingo.bukkit.automark.definitions.OccasionalTriggerDefinition
import com.jtprince.bingo.bukkit.automark.definitions.SpecialItemTriggerDefinition
import com.jtprince.bingo.core.automark.*
import com.jtprince.bingo.core.automark.itemtrigger.ItemTriggerYaml

class BukkitAutoMarkTriggerFactory(
    private var playerMapper: EventPlayerMapper,
) : AutoMarkTriggerFactory {
    /**
     * Create concrete automated triggers for a goal, such that whenever a player completes that
     * goal, a callback will be fired.
     *
     * @param space The Space that is being monitored for completion.
     * @param consumer Executed whenever the goal is completed or reverted.
     */
    override fun create(
        space: AutomatedSpace, consumer: AutoMarkConsumer
    ): Collection<AutoMarkTrigger> {
        val ret = mutableSetOf<AutoMarkTrigger>()

        val triggerDefs = BingoPlugin.triggerDefinitionRegistry[space.goalId]
        for (triggerDef in triggerDefs) {
            /* Ensure variables needed for this trigger definition are specified */
            for (neededVar in triggerDef.neededVars) {
                if (neededVar !in space.variables.keys) {
                    throw MissingVariableException(neededVar)
                }
            }

            val newTrigger = when (triggerDef) {
                is ItemTriggerYaml.Definition ->
                    BukkitItemTrigger(space, triggerDef, playerMapper, BingoPlugin.eventRegistry, consumer)
                is EventTriggerDefinition<*> ->
                    BukkitEventTrigger(space, BingoPlugin.eventRegistry, playerMapper, consumer, triggerDef)
                is OccasionalTriggerDefinition ->
                    BukkitOccasionalTrigger(space, BingoPlugin, playerMapper, consumer, triggerDef)
                is SpecialItemTriggerDefinition ->
                    BukkitSpecialItemTrigger(space, playerMapper, consumer, triggerDef)
                else -> {
                    BingoPlugin.logger.warning("Unknown trigger definition: $triggerDef")
                    null
                }
            }

            newTrigger?.let { ret += it }
        }

        return ret.toSet()
    }
}
