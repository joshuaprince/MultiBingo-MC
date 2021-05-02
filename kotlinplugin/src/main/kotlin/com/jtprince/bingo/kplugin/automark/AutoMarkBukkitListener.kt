package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.BingoPlugin
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.plugin.EventExecutor
import org.bukkit.plugin.IllegalPluginAccessException
import java.util.logging.Level
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses

/**
 * Container for all Bukkit Event Listeners.
 *
 * Rather than each Goal having its own listener that hooks into Bukkit, we register all
 * Bukkit Event Listeners here. Individual goal triggers can register here to be called back
 * whenever that event occurs.
 */
object AutoMarkBukkitListener : Listener, EventExecutor {
    class Callback<EventType : Event>(
        internal val eventType: KClass<EventType>,
        internal val callback: (EventType) -> Unit
    ) : (EventType) -> Unit by callback

    /** Event Classes that we already have a Bukkit Listener for. */
    private val registeredEventTypes = HashSet<KClass<out Event>>()
    private val registeredHandlerLists = HashSet<HandlerList>()

    /** Maps each Event Class to all of the listeners we have registered for that event. */
    private val activeEventListenerMap = HashMap<KClass<out Event>, HashSet<Callback<*>>>()

    /** Maps the ID given in [register] to a callback so we can unregister it. */
    private val regEventListeners = HashMap<Int, Callback<*>>()

    /** Maps the ID given in [registerInventoryChange] to a callback so we can unregister. */
    private val regInvListeners = HashMap<Int, Callback<Event>>()

    private var lastId = 0
    private val inventoryChangeEventClasses = setOf(
        InventoryCloseEvent::class, EntityPickupItemEvent::class, InventoryClickEvent::class,
        PlayerDropItemEvent::class
    )

    /**
     * Listen for an Event on this server, causing a callback to be executed whenever that event
     * happens.
     * @return A unique "registry ID" that can be used to unregister this callback.
     */
    fun <EventType: Event> register(eventType: KClass<EventType>,
                                    callback: Callback<EventType>): Int {
        listenToEvent(eventType)

        val list = activeEventListenerMap.getOrPut(eventType) { hashSetOf() }
        list += callback

        /* Give the caller a unique identifier that they can use to unregister */
        val id = lastId++
        regEventListeners[id] = callback
        return id
    }

    /**
     * Listen for any event that changes any Player's inventory, causing a callback to be executed
     * whenever an inventory changes.
     * @return A unique "registry ID" that can be used to unregister this callback.
     */
    fun registerInventoryChange(callback: Callback<Event>): Int {
        inventoryChangeEventClasses.forEach{
            listenToEvent(it)
        }

        val id = lastId++
        regInvListeners[id] = callback
        return id
    }

    /**
     * Stop passing an event to a callback.
     * @param registryId A registry ID provided by a [register] call.
     */
    fun unregister(registryId: Int) {
        val eventMapEntry = regEventListeners[registryId]
        val invMapEntry = regInvListeners[registryId]

        when {
            eventMapEntry != null -> {
                val lst = activeEventListenerMap[eventMapEntry.eventType]!!
                lst -= eventMapEntry
                regEventListeners -= registryId
            }
            invMapEntry != null -> {
                regInvListeners -= registryId
            }
            else -> {
                BingoPlugin.logger.severe("Tried to unregister unknown listener registry ID $registryId")
                return
            }
        }
    }

    /**
     * Bukkit hook for receiving an Event.
     */
    override fun execute(listener: Listener, event: Event) {
        for (clazz in classAndSuperclasses(event::class)) {
            for (trigger in activeEventListenerMap[clazz] ?: emptySet()) {
                @Suppress("UNCHECKED_CAST")
                (trigger as Callback<in Event>).callback(event)
            }
        }

        if (event::class in inventoryChangeEventClasses) {
            receiveInventoryChange(event)
        }
    }

    /**
     * For an Event subclass, find all Event Classes that are its superclass, up to but not
     * including Event itself.
     *
     * Example: InventoryClickEvent ->
     *              (InventoryClickEvent, InventoryInteractEvent, InventoryEvent)
     */
    private fun classAndSuperclasses(clazz: KClass<out Event>): Collection<KClass<out Event>> {
        val ret = mutableSetOf<KClass<out Event>>()

        var c = clazz
        while (c.isSubclassOf(Event::class) && c != Event::class) {
            ret += c
            @Suppress("UNCHECKED_CAST")
            c = c.superclasses[0] as KClass<out Event>
        }

        return ret
    }

    /**
     * Ensure that we have a Bukkit listener for this Event class. If we are already listening to
     * this Event class, do nothing.
     */
    private fun listenToEvent(eventType: KClass<out Event>) {
        /* Check the first cache - have we listened to this specific Event class before? */
        if (registeredEventTypes.contains(eventType)) {
            return
        }

        /* Check the second cache - have we listened to any other Event that uses the same
         * HandlerList before?
         * Bukkit Events occasionally share HandlerLists for similar events, for example
         * HangingBreakEvent and HangingBreakByEntityEvent. Registering a listener for both would
         * result in the event firing twice. */
        val handlerList = try {
            val handlerList = eventType.java.getMethod("getHandlerList").invoke(null)
            if (handlerList as? HandlerList == null) {
                throw IllegalPluginAccessException("No getHandlerList method exists on class ${eventType.simpleName}")
            }

            if (handlerList in registeredHandlerLists) {
                /* This Event class uses the same handler list as an already-listened Event class.
                 * Add this one to the first cache and do nothing. */
                registeredEventTypes += eventType
                return
            }

            handlerList
        } catch (exc: Exception) {
            BingoPlugin.logger.log(Level.SEVERE, "Failed to listen to event of type ${eventType.simpleName}", exc)
            return
        }

        /* Both caches missed. Register a new listener for this Event. */
        Bukkit.getServer().pluginManager.registerEvent(
            eventType.java, this, EventPriority.MONITOR, this, BingoPlugin
        )
        registeredHandlerLists += handlerList
        registeredEventTypes += eventType
    }

    /**
     * Intermediate callback for inventory change events. Delay them by 1 tick so that the inventory
     * is updated when any callbacks receive the event.
     */
    private fun receiveInventoryChange(event: Event) {
        BingoPlugin.server.scheduler.scheduleSyncDelayedTask(BingoPlugin, {
            for (callback in regInvListeners.values) {
                callback(event)
            }
        }, 1)
    }
}
