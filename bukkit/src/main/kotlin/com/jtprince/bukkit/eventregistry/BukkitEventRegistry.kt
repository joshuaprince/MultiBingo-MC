package com.jtprince.bukkit.eventregistry

import com.jtprince.bukkit.eventregistry.BukkitEventRegistry.RegistryId
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
import org.bukkit.plugin.Plugin
import java.util.logging.Level
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses

/**
 * A complete wrapper for the Bukkit event system.
 *
 * Basic usage is to register event callbacks with the [register] function. The provided callback
 * will be executed whenever that event is raised on the server.
 *
 * The [register] function returns a [RegistryId] object, which is a unique handle on that
 * Event-callback mapping. This mapping can be undone by providing that same RegistryId to the
 * [unregister] function.
 *
 * @param plugin The Bukkit plugin instance that will be used to register underlying Bukkit
 *               listeners.
 *
 * Goals of the BukkitEventRegistry:
 *   - Kotlin-first support.
 *   - Optimization for many individual registered listeners.
 *   - Ease of unregistering registered listeners.
 *   - Hiding of Bukkit's Handler system, whose use of abstract Event classes and HandlerLists makes
 *     the above confusing.
 *   - Addition of Inventory Change listeners.
 */
class BukkitEventRegistry(private val plugin: Plugin) : Listener, EventExecutor {
    class Callback<EventType : Event>(
        val eventType: KClass<EventType>,
        val callback: (EventType) -> Unit
    ) : (EventType) -> Unit by callback

    /** A handle on an EventType-callback mapping provided by [register] that can be [unregister]ed */
    @JvmInline
    value class RegistryId internal constructor(internal val id: Int)

    /** Event Classes that we already have a Bukkit Listener for. */
    private val registeredEventTypes = HashSet<KClass<out Event>>()
    private val registeredHandlerLists = HashSet<HandlerList>()

    /** Maps each Event Class to all of the listeners we have registered for that event. */
    private val activeEventListenerMap = HashMap<KClass<out Event>, HashSet<Callback<*>>>()

    /** Maps the ID given in [register] to a callback so we can unregister it. */
    private val regEventListeners = HashMap<RegistryId, Callback<*>>()

    /** Maps the ID given in [registerInventoryChange] to a callback so we can unregister. */
    private val regInvListeners = HashMap<RegistryId, Callback<Event>>()

    private var lastId = RegistryId(0)
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
                                    callback: Callback<EventType>
    ): RegistryId {
        listenToEvent(eventType)

        val list = activeEventListenerMap.getOrPut(eventType) { hashSetOf() }
        list += callback

        /* Give the caller a unique identifier that they can use to unregister */
        lastId = RegistryId(lastId.id + 1)
        regEventListeners[lastId] = callback
        return lastId
    }

    /**
     * Listen for any event that changes any Player's inventory, causing a callback to be executed
     * whenever an inventory changes.
     * @return A unique "registry ID" that can be used to unregister this callback.
     */
    fun registerInventoryChange(callback: Callback<Event>): RegistryId {
        for (clazz in inventoryChangeEventClasses) {
            listenToEvent(clazz)
        }

        lastId = RegistryId(lastId.id + 1)
        regInvListeners[lastId] = callback
        return lastId
    }

    /**
     * Stop passing an event to a callback.
     * @param registryId A registry ID provided by a [register] call.
     */
    fun unregister(registryId: RegistryId) {
        val eventMapEntry = regEventListeners[registryId]
        val invMapEntry = regInvListeners[registryId]

        when {
            eventMapEntry != null -> {
                val lst = activeEventListenerMap[eventMapEntry.eventType] ?: run {
                    plugin.logger.severe(
                        "${eventMapEntry.eventType} is not in the active Bukkit Event Listener map.")
                    return
                }
                lst -= eventMapEntry
                regEventListeners -= registryId
            }
            invMapEntry != null -> {
                regInvListeners -= registryId
            }
            else -> {
                plugin.logger.severe("Tried to unregister unknown listener registry ID $registryId")
                return
            }
        }
    }

    /**
     * Stop passing ALL events to callbacks registered with this BukkitEventRegistry instance, and
     * unregister all internal Bukkit listeners. No more callbacks should be registered after this
     * is called. This should only be called at Plugin disable.
     */
    fun unregisterAll() {
        HandlerList.unregisterAll(this)
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
            plugin.logger.log(Level.SEVERE, "Failed to listen to event of type ${eventType.simpleName}", exc)
            return
        }

        /* Both caches missed. Register a new listener for this Event. */
        Bukkit.getServer().pluginManager.registerEvent(
            eventType.java, this, EventPriority.MONITOR, this, plugin
        )
        registeredHandlerLists += handlerList
        registeredEventTypes += eventType
    }

    /**
     * Intermediate callback for inventory change events. Delay them by 1 tick so that the inventory
     * is updated when any callbacks receive the event.
     */
    private fun receiveInventoryChange(event: Event) {
        plugin.server.scheduler.scheduleSyncDelayedTask(plugin, {
            for (callback in regInvListeners.values) {
                callback(event)
            }
        }, 1)
    }
}
