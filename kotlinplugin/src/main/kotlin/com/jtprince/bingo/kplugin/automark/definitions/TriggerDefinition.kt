package com.jtprince.bingo.kplugin.automark.definitions

/**
 * A static mapping from one or more goal IDs to some abstract way of defining how that goal ID can
 * be marked automatically.
 */
sealed interface TriggerDefinition {
    val neededVars: Array<String>

    companion object {
        /**
         * Get all static mappings from a specified goal ID to its automated trigger definitions.
         */
        fun getDefinitions(
            goalId: String,
            itemTriggerYaml: ItemTriggerYaml
        ): Collection<TriggerDefinition> {
            val dslTriggerDefs = dslRegistry[goalId] ?: emptyList()
            val itemTriggerDef = itemTriggerYaml[goalId]

            return (dslTriggerDefs + itemTriggerDef).filterNotNull()
        }

        /**
         * Get a set of all goal IDs that are automated by some TriggerDefinition.
         */
        val allAutomatedGoals by lazy {
            ItemTriggerYaml.defaultYaml.allAutomatedGoals + dslRegistry.allAutomatedGoals
        }
    }
}
