package com.jtprince.bingo.bukkit.automark.trigger

import com.jtprince.bingo.bukkit.BingoPlugin
import com.jtprince.bingo.bukkit.automark.AutoMarkConsumer
import com.jtprince.bingo.bukkit.automark.AutomatedSpace
import com.jtprince.bingo.bukkit.automark.BingoInventory
import com.jtprince.bingo.bukkit.automark.EventPlayerMapper
import com.jtprince.bingo.bukkit.automark.definitions.SpecialItemTriggerDefinition

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
