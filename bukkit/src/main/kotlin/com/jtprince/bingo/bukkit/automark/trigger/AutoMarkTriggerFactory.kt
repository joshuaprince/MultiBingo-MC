package com.jtprince.bingo.bukkit.automark.trigger

import com.jtprince.bingo.bukkit.BingoPlugin
import com.jtprince.bingo.bukkit.automark.AutoMarkConsumer
import com.jtprince.bingo.bukkit.automark.AutomatedSpace
import com.jtprince.bingo.bukkit.automark.EventPlayerMapper
import com.jtprince.bingo.bukkit.automark.MissingVariableException
import com.jtprince.bingo.bukkit.automark.definitions.*

class AutoMarkTriggerFactory(
    private var itemTriggerYaml: ItemTriggerYaml = ItemTriggerYaml.defaultYaml
) {
    /**
     * Create concrete automated triggers for a goal, such that whenever a player completes that
     * goal, a callback will be fired.
     *
     * @param space The Space that is being monitored for completion.
     * @param playerMapper Resolves Bukkit events to the BingoPlayer that completed it.
     * @param consumer Executed whenever the goal is completed or reverted.
     */
    fun create(
        space: AutomatedSpace, playerMapper: EventPlayerMapper, consumer: AutoMarkConsumer
    ): Collection<AutoMarkTrigger> {
        val ret = HashSet<AutoMarkTrigger>()

        val triggerDefs = TriggerDefinition.getDefinitions(space.goalId, itemTriggerYaml)
        for (triggerDef in triggerDefs) {
            /* Ensure variables needed for this trigger definition are specified */
            for (neededVar in triggerDef.neededVars) {
                if (neededVar !in space.variables.keys) {
                    throw MissingVariableException(neededVar)
                }
            }

            ret += when (triggerDef) {
                is ItemTriggerYaml.Definition ->
                    ItemTrigger(space, playerMapper, BingoPlugin.eventRegistry, consumer, triggerDef)
                is EventTriggerDefinition<*> ->
                    EventTrigger(space, BingoPlugin.eventRegistry, playerMapper, consumer, triggerDef)
                is OccasionalTriggerDefinition ->
                    OccasionalTrigger(space, BingoPlugin, playerMapper, consumer, triggerDef)
                is SpecialItemTriggerDefinition ->
                    SpecialItemTrigger(space, playerMapper, consumer, triggerDef)
            }
        }

        return ret.toSet()
    }
}
