package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.board.SetVariables
import com.jtprince.bingo.kplugin.game.PlayerManager
import com.jtprince.bingo.kplugin.player.BingoPlayer
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack
import kotlin.math.min

open class ItemTrigger internal constructor(
    goalId: String,
    spaceId: Int,
    variables: SetVariables,
    playerManager: PlayerManager,
    callback: AutoMarkCallback,
    private val rootMatchGroup: ItemTriggerYaml.MatchGroup?,
) : AutoMarkTrigger(goalId, spaceId, variables, playerManager, callback) {

    override val revertible = true

    private val listenerRegistryId = AutoMarkBukkitListener.registerInventoryChange(
        AutoMarkBukkitListener.Callback(Event::class) {
            eventRaised(it)
        })

    override fun destroy() {
        AutoMarkBukkitListener.unregister(listenerRegistryId)
    }

    /**
     * Listener callback that is called EVERY time anyone on the server's inventory changes.
     */
    private fun eventRaised(event: Event) {
        val player = EventTrigger.forWhom(playerManager, event) ?: return
        val satisfied = satisfiedBy(player)
        // Always call callback, since ItemTriggers are always revertible.
        callback(player, spaceId, satisfied)
    }

    /**
     * Returns whether a set of items meets the criteria for this Item Trigger.
     */
    protected open fun satisfiedBy(player: BingoPlayer): Boolean {
        val inventory = player.inventory
        val rootMatchGroup = rootMatchGroup ?: return false
        val rootUT = effectiveUT(rootMatchGroup, inventory)
        return rootUT.u >= rootMatchGroup.unique(variables)
                && rootUT.t >= rootMatchGroup.total(variables)
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
                if (ret.u < matchGroup.unique(variables)) {
                    ret.u++
                }
                seenItemNames.add(namespacedName)
            }
            ret.t = min(matchGroup.total(variables), ret.t + itemStack.amount)
        }

        for (child in matchGroup.children) {
            val childUT = effectiveUT(child, inventory)
            ret.t += childUT.t
            if (childUT.t >= child.total(variables)) {
                ret.u += childUT.u
            }
        }

        return ret
    }
}
