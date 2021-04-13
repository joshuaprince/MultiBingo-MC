package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.BingoPlugin
import com.jtprince.bingo.kplugin.board.SetVariables
import com.jtprince.bingo.kplugin.game.PlayerManager

class AutoMarkTriggerFactory(
    private var itemTriggerYaml: ItemTriggerYaml = ItemTriggerYaml.defaultYaml
) {
    fun create(goalId: String, spaceId: Int, variables: SetVariables,
               playerManager: PlayerManager, callback: AutoMarkCallback
    ): Collection<AutoMarkTrigger> {
        val ret = HashSet<AutoMarkTrigger>()

        /* Create DSL-specified triggers */
        dslRegistry[goalId]?.forEach { def ->
            when (def) {
                is EventTriggerDefinition<*> ->
                    EventTrigger(goalId, spaceId, variables, playerManager, callback, def)
                is OccasionalTriggerDefinition ->
                    OccasionalTrigger(goalId, spaceId, variables, playerManager, callback, def)
                is SpecialItemTriggerDefinition ->
                    SpecialItemTrigger(goalId, spaceId, variables, playerManager, callback, def)
                else -> {
                    BingoPlugin.logger.severe("Unknown DSL trigger type ${def::class}")
                    null
                }
            }?.also { ret += it }
        }

        /* Create Item Trigger YML-specified triggers */
        itemTriggerYaml[goalId]?.also {
            ret += ItemTrigger(goalId, spaceId, variables, playerManager, callback, it)
        }

        return ret.toSet()
    }
}
