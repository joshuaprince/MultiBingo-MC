package com.jtprince.bingo.core.automark.itemtrigger

import com.jtprince.bingo.core.SetVariables
import kotlin.math.min

/**
 * A concrete ItemTrigger instance, which means that all variables are set. With this combination
 * of a definition + variables, we can test a player's inventory to determine whether they have
 * satisfied the trigger - see [satisfiedBy].
 */
class ItemTrigger(
    private val definition: ItemTriggerYaml.Definition,
    private val variables: SetVariables,
) {
    /**
     * Returns whether a set of items meets the criteria for this Item Trigger.
     */
    fun satisfiedBy(inventory: Collection<BingoItemStack>): Boolean {
        val rootMatchGroup = definition.rootMatchGroup
        val rootUT = effectiveUT(rootMatchGroup, inventory)
        return rootUT.u >= rootMatchGroup.unique(variables)
                && rootUT.t >= rootMatchGroup.total(variables)
    }

    private class UT {
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
                            inventory: Collection<BingoItemStack>): UT {
        val ret = UT()
        val seenItemNames = HashSet<String>()

        for (itemStack in inventory) {
            val namespacedName = itemStack.namespacedId
            if (!matchGroup.nameMatches(namespacedName)) {
                continue
            }
            if (!seenItemNames.contains(namespacedName)) {
                if (ret.u < matchGroup.unique(variables)) {
                    ret.u++
                }
                seenItemNames.add(namespacedName)
            }
            ret.t = min(matchGroup.total(variables), ret.t + itemStack.quantity)
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
