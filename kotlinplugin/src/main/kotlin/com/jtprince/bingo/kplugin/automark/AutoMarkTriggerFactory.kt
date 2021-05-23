package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.BingoPlugin

class AutoMarkTriggerFactory(
    private var itemTriggerYaml: ItemTriggerYaml = ItemTriggerYaml.defaultYaml
) {
    fun create(
        space: AutomatedSpace, playerMapper: EventPlayerMapper, callback: AutoMarkTrigger.Callback
    ): Collection<AutoMarkTrigger> {
        val ret = HashSet<AutoMarkTrigger>()

        /* Create DSL-specified triggers */
        dslRegistry[space.goalId]?.forEach { def ->
            for (neededVar in def.neededVars) {
                if (neededVar !in space.variables.keys) {
                    throw MissingVariableException(neededVar)
                }
            }

            when (def) {
                is EventTriggerDefinition<*> ->
                    EventTrigger(space, playerMapper, callback, def)
                is OccasionalTriggerDefinition ->
                    OccasionalTrigger(space, playerMapper, callback, def)
                is SpecialItemTriggerDefinition ->
                    SpecialItemTrigger(space, playerMapper, callback, def)
                else -> {
                    BingoPlugin.logger.severe("Unknown DSL trigger type ${def::class}")
                    null
                }
            }?.also { ret += it }
        }

        /* Create Item Trigger YML-specified triggers */
        itemTriggerYaml[space.goalId]?.also { yamlDefinition ->
            for (neededVar in yamlDefinition.neededVariables()) {
                if (neededVar !in space.variables.keys) {
                    throw MissingVariableException(neededVar)
                }
            }
            val newTrigger = ItemTrigger(
                space, playerMapper, BingoPlugin.eventRegistry, callback, yamlDefinition
            )
            ret += newTrigger
        }

        return ret.toSet()
    }
}
