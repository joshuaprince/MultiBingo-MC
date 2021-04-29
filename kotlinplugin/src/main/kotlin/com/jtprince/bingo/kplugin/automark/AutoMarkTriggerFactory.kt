package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.BingoPlugin
import com.jtprince.bingo.kplugin.board.SetVariables

class AutoMarkTriggerFactory(
    private var itemTriggerYaml: ItemTriggerYaml = ItemTriggerYaml.defaultYaml
) {
    fun create(goalId: String, spaceId: Int, variables: SetVariables,
               playerMapper: EventPlayerMapper, callback: AutoMarkCallback
    ): Collection<AutoMarkTrigger> {
        val ret = HashSet<AutoMarkTrigger>()

        /* Create DSL-specified triggers */
        dslRegistry[goalId]?.forEach { def ->
            when (def) {
                is EventTriggerDefinition<*> ->
                    EventTrigger(goalId, spaceId, variables, playerMapper, callback, def)
                is OccasionalTriggerDefinition ->
                    OccasionalTrigger(goalId, spaceId, variables, playerMapper, callback, def)
                is SpecialItemTriggerDefinition ->
                    SpecialItemTrigger(goalId, spaceId, variables, playerMapper, callback, def)
                else -> {
                    BingoPlugin.logger.severe("Unknown DSL trigger type ${def::class}")
                    null
                }
            }?.also { ret += it }
        }

        /* Create Item Trigger YML-specified triggers */
        itemTriggerYaml[goalId]?.also {
            ret += ItemTrigger(goalId, spaceId, variables, playerMapper, AutoMarkBukkitListener, callback, it)
        }

        return ret.toSet()
    }
}
