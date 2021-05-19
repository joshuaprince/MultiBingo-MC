package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.BingoPlugin
import com.jtprince.bukkit.eventregistry.BukkitEventRegistry
import org.bukkit.event.Event

class EventTrigger<EventType : Event> internal constructor(
    val space: AutomatedSpace,
    private val playerMapper: EventPlayerMapper,
    private val callback: Callback,
    private val triggerDefinition: EventTriggerDefinition<EventType>,
) : AutoMarkTrigger() {

    private val listenerRegistryId = BingoPlugin.eventRegistry.register(triggerDefinition.eventType,
        BukkitEventRegistry.Callback(triggerDefinition.eventType) {
            eventRaised(it)
        })

    private val timedReverter: TimedGoalReverter? = when (triggerDefinition.revertAfterTicks) {
        null -> null
        else -> TimedGoalReverter(triggerDefinition.revertAfterTicks, callback)
    }

    override fun destroy() {
        BingoPlugin.eventRegistry.unregister(listenerRegistryId)
        timedReverter?.destroy()
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
            timedReverter?.revertLater(player, space)
        }
    }
}
