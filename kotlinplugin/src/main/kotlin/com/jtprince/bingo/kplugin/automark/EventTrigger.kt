package com.jtprince.bingo.kplugin.automark

import org.bukkit.event.Event

class EventTrigger<EventType : Event> internal constructor(
    val space: AutomatedSpace,
    private val playerMapper: EventPlayerMapper,
    private val callback: Callback,
    private val triggerDefinition: EventTriggerDefinition<EventType>,
) : AutoMarkTrigger() {

    private val listenerRegistryId = AutoMarkBukkitListener.register(triggerDefinition.eventType,
        AutoMarkBukkitListener.Callback(triggerDefinition.eventType) {
            eventRaised(it)
        })

    override fun destroy() {
        AutoMarkBukkitListener.unregister(listenerRegistryId)
    }

    /**
     * Listener callback that is called EVERY time this EventListener's tracked event is fired.
     */
    private fun eventRaised(event: EventType) {
        val player = playerMapper.mapEvent(event) ?: return

        val triggerDefParams = EventTriggerDefinition.Parameters(event, player, this)
        val satisfied = triggerDefinition.function.invoke(triggerDefParams)

        /* Event triggers are never revertible. */
        if (satisfied) {
            callback.trigger(player, space, satisfied)
        }
    }
}
