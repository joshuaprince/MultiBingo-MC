package com.jtprince.bingo.bukkit.automark.trigger

import com.jtprince.bingo.bukkit.BingoPlugin
import com.jtprince.bingo.bukkit.automark.EventPlayerMapper
import com.jtprince.bingo.bukkit.automark.definitions.EventTriggerDefinition
import com.jtprince.bingo.bukkit.automark.definitions.OccasionalTriggerDefinition
import com.jtprince.bingo.bukkit.automark.definitions.SpecialItemTriggerDefinition
import com.jtprince.bingo.core.automark.*
import com.jtprince.bingo.core.automark.itemtrigger.ItemTriggerYaml
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BukkitAutoMarkTriggerFactory(
    private var playerMapper: EventPlayerMapper,
) : AutoMarkTriggerFactory, KoinComponent {
    private val plugin: BingoPlugin by inject()
    
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

        val triggerDefs = plugin.platform.triggerDefinitionRegistry[space.goalId]
        for (triggerDef in triggerDefs) {
            /* Ensure variables needed for this trigger definition are specified */
            for (neededVar in triggerDef.neededVars) {
                if (neededVar !in space.variables.keys) {
                    throw MissingVariableException(neededVar)
                }
            }

            val newTrigger = when (triggerDef) {
                is ItemTriggerYaml.Definition ->
                    BukkitItemTrigger(space, triggerDef, playerMapper, plugin.platform.eventRegistry, consumer)
                is EventTriggerDefinition<*> ->
                    BukkitEventTrigger(space, plugin.platform.eventRegistry, playerMapper, consumer, triggerDef)
                is OccasionalTriggerDefinition ->
                    BukkitOccasionalTrigger(space, plugin, playerMapper, consumer, triggerDef)
                is SpecialItemTriggerDefinition ->
                    BukkitSpecialItemTrigger(space, playerMapper, consumer, triggerDef)
                else -> {
                    plugin.logger.warning("Unknown trigger definition: $triggerDef")
                    null
                }
            }

            newTrigger?.let { ret += it }
        }

        return ret.toSet()
    }
}
