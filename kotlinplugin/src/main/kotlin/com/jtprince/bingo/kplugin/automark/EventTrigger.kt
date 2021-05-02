package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.board.SetVariables
import org.bukkit.event.Event

class EventTrigger<EventType : Event> internal constructor(
    val goalId: String,
    val spaceId: Int,
    val variables: SetVariables,
    private val playerMapper: EventPlayerMapper,
    val callback: AutoMarkCallback,
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
            callback(player, spaceId, satisfied)
        }
    }
}
