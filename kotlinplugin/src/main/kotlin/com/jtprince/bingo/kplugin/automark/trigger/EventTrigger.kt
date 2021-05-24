package com.jtprince.bingo.kplugin.automark.trigger

import com.jtprince.bingo.kplugin.automark.AutoMarkConsumer
import com.jtprince.bingo.kplugin.automark.AutomatedSpace
import com.jtprince.bingo.kplugin.automark.EventPlayerMapper
import com.jtprince.bingo.kplugin.automark.TimedGoalReverter
import com.jtprince.bingo.kplugin.automark.definitions.EventTriggerDefinition
import com.jtprince.bukkit.eventregistry.BukkitEventRegistry
import org.bukkit.event.Event

class EventTrigger<EventType : Event> internal constructor(
    val space: AutomatedSpace,
    private val eventRegistry: BukkitEventRegistry,
    private val playerMapper: EventPlayerMapper,
    private val consumer: AutoMarkConsumer,
    private val triggerDefinition: EventTriggerDefinition<EventType>,
) : AutoMarkTrigger() {

    private val listenerRegistryId = eventRegistry.register(triggerDefinition.eventType,
        BukkitEventRegistry.Callback(triggerDefinition.eventType) {
            eventRaised(it)
        })

    private val timedReverter: TimedGoalReverter? = when (triggerDefinition.revertAfterTicks) {
        null -> null
        else -> TimedGoalReverter(triggerDefinition.revertAfterTicks, consumer)
    }

    override fun destroy() {
        eventRegistry.unregister(listenerRegistryId)
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
            consumer.receiveAutoMark(player, space, satisfied)
            timedReverter?.revertLater(player, space)
        }
    }
}
