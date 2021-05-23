package com.jtprince.bingo.kplugin.automark.trigger

import com.jtprince.bingo.kplugin.BingoPlugin
import com.jtprince.bingo.kplugin.automark.AutomatedSpace
import com.jtprince.bingo.kplugin.automark.EventPlayerMapper
import com.jtprince.bingo.kplugin.automark.MissingVariableException
import com.jtprince.bingo.kplugin.automark.definitions.*
import com.jtprince.bingo.kplugin.automark.definitions.EventTriggerDefinition
import com.jtprince.bingo.kplugin.automark.definitions.OccasionalTriggerDefinition
import com.jtprince.bingo.kplugin.automark.definitions.SpecialItemTriggerDefinition

class AutoMarkTriggerFactory(
    private var itemTriggerYaml: ItemTriggerYaml = ItemTriggerYaml.defaultYaml
) {
    /**
     * Create concrete automated triggers for a goal, such that whenever a player completes that
     * goal, a callback will be fired.
     *
     * @param space The Space that is being monitored for completion.
     * @param playerMapper Resolves Bukkit events to the BingoPlayer that completed it.
     * @param callback Executed whenever the goal is completed or reverted.
     */
    fun create(
        space: AutomatedSpace, playerMapper: EventPlayerMapper, callback: AutoMarkTrigger.Callback
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
                    ItemTrigger(space, playerMapper, BingoPlugin.eventRegistry, callback, triggerDef)
                is EventTriggerDefinition<*> ->
                    EventTrigger(space, BingoPlugin.eventRegistry, playerMapper, callback, triggerDef)
                is OccasionalTriggerDefinition ->
                    OccasionalTrigger(space, BingoPlugin, playerMapper, callback, triggerDef)
                is SpecialItemTriggerDefinition ->
                    SpecialItemTrigger(space, playerMapper, callback, triggerDef)
            }
        }

        return ret.toSet()
    }
}
