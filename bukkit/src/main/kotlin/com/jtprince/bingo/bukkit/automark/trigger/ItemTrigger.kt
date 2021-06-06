package com.jtprince.bingo.bukkit.automark.trigger

import com.jtprince.bingo.bukkit.automark.AutoMarkConsumer
import com.jtprince.bingo.bukkit.automark.AutomatedSpace
import com.jtprince.bingo.bukkit.automark.BingoInventory
import com.jtprince.bingo.bukkit.automark.EventPlayerMapper
import com.jtprince.bingo.bukkit.automark.definitions.ItemTriggerYaml
import com.jtprince.bukkit.eventregistry.BukkitEventRegistry
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack
import kotlin.math.min

open class ItemTrigger internal constructor(
    val space: AutomatedSpace,
    private val playerMapper: EventPlayerMapper,
    private val listener: BukkitEventRegistry?,
    private val consumer: AutoMarkConsumer?,
    private val definition: ItemTriggerYaml.Definition?,  /* Nullable so SpecialItemTrigger overrides */
) : AutoMarkTrigger() {

    protected open val revertible = true

    private val listenerRegistryId = listener?.registerInventoryChange(
        BukkitEventRegistry.Callback(Event::class) {
            eventRaised(it)
        })

    override fun destroy() {
        listenerRegistryId?.let { listener?.unregister(it) }
    }

    /**
     * Listener callback that is called EVERY time anyone on the server's inventory changes.
     */
    private fun eventRaised(event: Event) {
        val player = playerMapper.mapEvent(event) ?: return
        val satisfied = satisfiedBy(player.inventory)

        if (revertible || satisfied) {
            consumer?.receiveAutoMark(player, space, satisfied)
        }
    }

    /**
     * Returns whether a set of items meets the criteria for this Item Trigger.
     */
    internal open fun satisfiedBy(inventory: BingoInventory): Boolean {
        val rootMatchGroup = definition?.rootMatchGroup ?: return false
        val rootUT = effectiveUT(rootMatchGroup, inventory.items)
        return rootUT.u >= rootMatchGroup.unique(space.variables)
                && rootUT.t >= rootMatchGroup.total(space.variables)
    }

    internal class UT {
        var u = 0
        var t = 0
    }

    /**
     * The effective U and T values for a Match Group reflect a combination of U and T values from
     * that match group and all of its children. At their simplest, "u" reflects how many unique
     * items in `inventory` match, and "t" reflects the total number of items that match.
     * See README.md for more details.
     */
    private fun effectiveUT(matchGroup: ItemTriggerYaml.MatchGroup,
                            inventory: Collection<ItemStack>): UT {
        val ret = UT()
        val seenItemNames = HashSet<String>()

        for (itemStack in inventory) {
            val namespacedName = itemStack.type.key.asString()
            if (!matchGroup.nameMatches(namespacedName)) {
                continue
            }
            if (!seenItemNames.contains(namespacedName)) {
                if (ret.u < matchGroup.unique(space.variables)) {
                    ret.u++
                }
                seenItemNames.add(namespacedName)
            }
            ret.t = min(matchGroup.total(space.variables), ret.t + itemStack.amount)
        }

        for (child in matchGroup.children) {
            val childUT = effectiveUT(child, inventory)
            ret.t += childUT.t
            if (childUT.t >= child.total(space.variables)) {
                ret.u += childUT.u
            }
        }

        return ret
    }
}
