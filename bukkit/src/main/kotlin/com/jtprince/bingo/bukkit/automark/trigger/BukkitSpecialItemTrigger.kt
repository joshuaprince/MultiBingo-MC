package com.jtprince.bingo.bukkit.automark.trigger

import com.jtprince.bingo.bukkit.BingoPlugin
import com.jtprince.bingo.bukkit.automark.EventPlayerMapper
import com.jtprince.bingo.bukkit.automark.definitions.SpecialItemTriggerDefinition
import com.jtprince.bingo.core.automark.AutoMarkConsumer
import com.jtprince.bingo.core.automark.AutoMarkTrigger
import com.jtprince.bingo.core.automark.AutomatedSpace
import com.jtprince.bukkit.eventregistry.BukkitEventRegistry
import org.bukkit.event.Event
import org.bukkit.inventory.PlayerInventory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BukkitSpecialItemTrigger internal constructor(
    val space: AutomatedSpace,
    private val playerMapper: EventPlayerMapper,
    private val consumer: AutoMarkConsumer,
    private val triggerDefinition: SpecialItemTriggerDefinition,
) : AutoMarkTrigger, KoinComponent {

    private val plugin: BingoPlugin by inject()
    private val listener = plugin.eventRegistry
    private val revertible = triggerDefinition.revertible

    private val listenerRegistryId = listener.registerInventoryChange(
        BukkitEventRegistry.Callback(Event::class) {
            eventRaised(it)
        })

    private fun satisfiedBy(inventories: Collection<PlayerInventory>): Boolean {
        return triggerDefinition.function(
            SpecialItemTriggerDefinition.Parameters(inventories, this))
    }

    override fun destroy() {
        listenerRegistryId.let { listener.unregister(it) }
    }

    /**
     * Listener callback that is called EVERY time anyone on the server's inventory changes.
     */
    private fun eventRaised(event: Event) {
        val player = playerMapper.mapEvent(event) ?: return
        val satisfied = satisfiedBy(player.bukkitInventories)

        if (revertible || satisfied) {
            consumer.receiveAutoMark(player, space, satisfied)
        }
    }
}
