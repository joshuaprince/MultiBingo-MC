package com.jtprince.bingo.kplugin.automark.trigger

import com.jtprince.bingo.kplugin.BingoPlugin
import com.jtprince.bingo.kplugin.automark.AutoMarkConsumer
import com.jtprince.bingo.kplugin.automark.AutomatedSpace
import com.jtprince.bingo.kplugin.automark.BingoInventory
import com.jtprince.bingo.kplugin.automark.EventPlayerMapper
import com.jtprince.bingo.kplugin.automark.definitions.SpecialItemTriggerDefinition

class SpecialItemTrigger internal constructor(
    space: AutomatedSpace,
    playerMapper: EventPlayerMapper,
    consumer: AutoMarkConsumer,
    private val triggerDefinition: SpecialItemTriggerDefinition,
) : ItemTrigger(space, playerMapper, BingoPlugin.eventRegistry, consumer, null) {

    override val revertible = triggerDefinition.revertible

    override fun satisfiedBy(inventory: BingoInventory): Boolean {
        return triggerDefinition.function(
            SpecialItemTriggerDefinition.Parameters(inventory, this))
    }
}
