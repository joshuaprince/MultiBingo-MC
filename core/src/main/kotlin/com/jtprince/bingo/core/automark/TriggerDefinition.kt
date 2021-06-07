package com.jtprince.bingo.core.automark

/**
 * A static mapping from one or more goal IDs to some abstract way of defining how that goal ID can
 * be marked automatically.
 */
interface TriggerDefinition {
    val neededVars: Array<String>

    class Registry {
         // TODO Make immutable/builder
        private val regs = mutableMapOf<String, MutableList<TriggerDefinition>>()

        val registeredGoalIds: Set<String>
            get() = regs.keys

        fun register(goalId: String, definition: TriggerDefinition) {
            regs.getOrPut(goalId) { mutableListOf() }.add(definition)
        }

        fun registerAll(map: Map<String, List<TriggerDefinition>>) {
            for ((goalId, defs) in map) {
                for (def in defs) {
                    register(goalId, def)
                }
            }
        }

        fun registerItemTriggers(yaml: ItemTriggerYaml = ItemTriggerYaml.defaultYaml) {
            for ((goalId, def) in yaml.definitions) {
                register(goalId, def)
            }
        }

        /**
         * TODO: Comment
         */
        operator fun get(key: String): List<TriggerDefinition> {
            return regs.getOrDefault(key, mutableListOf())
        }
    }
}
