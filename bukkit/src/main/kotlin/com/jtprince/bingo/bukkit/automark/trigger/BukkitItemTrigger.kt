package com.jtprince.bingo.bukkit.automark.trigger

import com.jtprince.bingo.bukkit.automark.EventPlayerMapper
import com.jtprince.bingo.core.automark.AutoMarkConsumer
import com.jtprince.bingo.core.automark.AutoMarkTrigger
import com.jtprince.bingo.core.automark.AutomatedSpace
import com.jtprince.bingo.core.automark.itemtrigger.ItemTrigger
import com.jtprince.bingo.core.automark.itemtrigger.ItemTriggerYaml
import com.jtprince.bukkit.eventregistry.BukkitEventRegistry
import org.bukkit.event.Event

class BukkitItemTrigger internal constructor(
    val space: AutomatedSpace,
    definition: ItemTriggerYaml.Definition,
    private val playerMapper: EventPlayerMapper,
    private val listener: BukkitEventRegistry,
    private val consumer: AutoMarkConsumer,
) : AutoMarkTrigger {

    private val itemTrigger = ItemTrigger(definition, space.variables)
    private val revertible = true

    private val listenerRegistryId = listener.registerInventoryChange(
        BukkitEventRegistry.Callback(Event::class) {
            eventRaised(it)
        })

    override fun destroy() {
        listener.unregister(listenerRegistryId)
    }

    /**
     * Listener callback that is called EVERY time anyone on the server's inventory changes.
     */
    private fun eventRaised(event: Event) {
        val player = playerMapper.mapEvent(event) ?: return
        val satisfied = itemTrigger.satisfiedBy(player.inventory)

        if (revertible || satisfied) {
            consumer.receiveAutoMark(player, space, satisfied)
        }
    }
}
